/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.server;

import com.velocitypowered.api.proxy.server.ServerInfo;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.proxy.main.ChannelProxy;
import de.timesnake.channel.util.ChannelConfig;
import de.timesnake.channel.util.listener.ChannelHandler;
import de.timesnake.channel.util.listener.ChannelListener;
import de.timesnake.channel.util.listener.ListenerType;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.server.DbServer;
import de.timesnake.library.basic.util.MultiKeyMap;
import de.timesnake.library.basic.util.ServerType;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.network.NetworkServer;
import de.timesnake.library.network.NetworkServer.CopyType;
import de.timesnake.library.network.ServerCreationResult;
import de.timesnake.library.network.ServerInitResult;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ServerManager implements ChannelListener {

  private final Logger logger = LogManager.getLogger("server-manager");

  private final MultiKeyMap<String, Integer, Server> servers = new MultiKeyMap<>();
  private final Map<String, Path> tmpDirsByServerName = new HashMap<>();

  private final Set<Integer> serverIds = new HashSet<>();

  private final ChannelConfig channelConfig;

  private final ReentrantLock serverCreationLock = new ReentrantLock();

  private int onlineLobbys = 0;


  public ServerManager() {
    this.channelConfig = ChannelProxy.getConfig();
    Network.getChannel().addListener(this);
  }

  public Collection<Server> getServers() {
    return servers.values();
  }

  public Collection<String> getNotOfflineServerNames() {
    Collection<String> names = new HashSet<>();
    for (Server server : this.getServers()) {
      if (!server.getStatus().equals(Status.Server.OFFLINE) && !server.getStatus().equals(Status.Server.LAUNCHING)) {
        names.add(server.getName());
      }
    }
    return names;
  }


  public Server getServer(Integer port) {
    return servers.get2(port);
  }

  public Server getServer(String name) {
    return servers.get1(name);
  }

  public Server getServer(DbServer server) {
    return servers.get1(server.getName());
  }

  public void updateServerTaskAll() {
    for (Server server : servers.values()) {
      server.setStatus(Database.getServers().getServer(server.getName()).getStatus(), false);
    }
  }

  public void updateServerTask(int port) {
    getServer(port).setStatus(Database.getServers().getServer(port).getStatus(), false);
  }

  private void applyDefaults(NetworkServer server) {
    server.setVelocitySecret(Network.getVelocitySecret())
        .setChannelHostName(this.channelConfig.getServerHostName())
        .setChannelListenHostName(this.channelConfig.getListenHostName())
        .setChannelPortOffset(this.channelConfig.getPortOffset())
        .setChannelProxyHostName(this.channelConfig.getProxyHostName())
        .setChannelProxyPort(this.channelConfig.getProxyPort())
        .setChannelProxyServerName(this.channelConfig.getProxyServerName());

  }

  public ServerSetupResult createTmpServer(ServerType type) {
    return this.createTmpServer(type, s -> {
    });
  }

  public ServerSetupResult createTmpServer(ServerType type, Consumer<NetworkServer> optionsApplier) {
    return this.createTmpServer(type, optionsApplier, true);
  }

  public ServerSetupResult createTmpServer(ServerType type, Consumer<NetworkServer> optionsApplier,
                                           boolean registerServer) {
    int id = this.nextServerId();
    return this.createTmpServer(type.getTag() + id, type, optionsApplier, registerServer);
  }

  public ServerSetupResult createTmpServer(ServerType type, Consumer<NetworkServer> optionsApplier,
                                           int port, boolean registerServer) {
    int id = this.nextServerId();
    return this.createTmpServer(type.getTag() + id, type, optionsApplier, port, registerServer);
  }

  private ServerSetupResult createTmpServer(String name, ServerType type, Consumer<NetworkServer> optionsApplier) {
    return this.createTmpServer(name, type, optionsApplier, true);
  }

  private ServerSetupResult createTmpServer(String name, ServerType type, Consumer<NetworkServer> optionsApplier,
                                            boolean registerServer) {
    NetworkServer server = new NetworkServer(name, type);
    optionsApplier.accept(server);
    return this.createTmpServer(server, this::nextEmptyPort, registerServer);
  }

  private ServerSetupResult createTmpServer(String name, ServerType type, Consumer<NetworkServer> optionsApplier,
                                            int port, boolean registerServer) {
    NetworkServer server = new NetworkServer(name, type);
    optionsApplier.accept(server);
    return this.createTmpServer(server, () -> port, registerServer);
  }

  public Tuple<ServerSetupResult, ServerSetupResult> createTmpTwinServers(ServerType type1,
                                                                          Consumer<NetworkServer> optionsApplier1,
                                                                          ServerType type2,
                                                                          Consumer<NetworkServer> optionsApplier2) {
    int id1 = this.nextServerId();
    int id2 = this.nextServerId();
    return new Tuple<>(
        this.createTmpServer(type1.getTag() + id1 + "_" + type2.getTag() + id2, type1, optionsApplier1),
        this.createTmpServer(type2.getTag() + id2 + "_" + type1.getTag() + id1, type2, optionsApplier2));
  }


  private ServerSetupResult createTmpServer(NetworkServer networkServer, Supplier<Integer> portSupplier,
                                            boolean registerServer) {
    ServerCreationResult result;
    Server server;

    try {
      this.serverCreationLock.lock();

      if (this.getServer(networkServer.getName()) != null) {
        return new ServerSetupResult.Fail(null, networkServer, "server already exists");
      }

      networkServer.setPort(portSupplier.get());
      this.applyDefaults(networkServer);

      if (networkServer.getType().equals(ServerType.LOBBY)) {
        result = Network.getNetworkUtils().createServer(networkServer
            .options(o -> o
                .setWorldCopyType(CopyType.SYNC)
                .setSyncPlayerData(false)
                .setSyncLogs(true)));

      } else if (networkServer.getType().equals(ServerType.BUILD)) {
        result = Network.getNetworkUtils().createServer(networkServer
            .options(o -> o
                .setWorldCopyType(CopyType.NONE)
                .setSyncPlayerData(true)
                .setSyncLogs(true)));

      } else if (networkServer.getType().equals(ServerType.LOUNGE)) {
        result = Network.getNetworkUtils().createServer(networkServer
            .options(o -> o
                .setWorldCopyType(CopyType.COPY)
                .setSyncPlayerData(false)
                .setSyncLogs(true)));

      } else if (networkServer.getType().equals(ServerType.TEMP_GAME)) {
        result = Network.getNetworkUtils().createServer(networkServer
            .options(o -> o
                // copy type determined by map option
                .setSyncPlayerData(false)
                .setSyncLogs(true)));

      } else if (networkServer.getType().equals(ServerType.GAME)) {
        result = Network.getNetworkUtils().createServer(networkServer
            .options(o -> o
                .setWorldCopyType(CopyType.COPY)
                .setSyncPlayerData(false)
                .setSyncLogs(true)));
      } else {
        return new ServerSetupResult.Fail(null, networkServer, "unknown server type");
      }

      if (result.isSuccessful()) {
        Path serverPath = ((ServerCreationResult.Success) result).getServerPath();
        try {
          server = this.addServer(networkServer, serverPath, registerServer);
          return new ServerSetupResult.Success((ServerCreationResult.Success) result, networkServer, server);
        } catch (IllegalArgumentException e) {
          return new ServerSetupResult.Fail(result, networkServer, e.getMessage());
        }
      } else {
        return new ServerSetupResult.Fail(result, networkServer, "server creation failed");
      }
    } finally {
      this.serverCreationLock.unlock();
    }
  }

  public ServerInitResult initNewPublicPlayerServer(ServerType type, String task, String name) {
    this.serverCreationLock.lock();

    ServerInitResult result;

    try {
      if (this.getServer(name) != null) {
        return new ServerInitResult.Fail("server already exists");
      }
      result = Network.getNetworkUtils().initNewPublicPlayerServer(type, task, name);
    } finally {
      this.serverCreationLock.unlock();
    }
    return result;
  }

  public ServerInitResult initNewPlayerServer(UUID uuid, ServerType type, String task, String name) {
    this.serverCreationLock.lock();

    ServerInitResult result;

    try {
      if (this.getServer(name) != null) {
        return new ServerInitResult.Fail("server already exists");
      }
      result = Network.getNetworkUtils().initNewPlayerServer(uuid, type, task, name);
    } finally {
      this.serverCreationLock.unlock();
    }
    return result;
  }

  public ServerSetupResult loadPlayerServer(String categoryName, UUID uuid, String serverName,
                                            Consumer<NetworkServer> optionsApplier) {
    this.serverCreationLock.lock();

    try {
      String serverId = categoryName + "_" + uuid.hashCode() + "-" + serverName;

      if (this.getServer(serverId) != null) {
        return new ServerSetupResult.Fail(null, null, "server already exists");
      }

      int port = this.nextEmptyPort();
      NetworkServer networkServer = new NetworkServer(serverId, ServerType.GAME)
          .setPort(port)
          .setFolderName(serverName)
          .setTask(categoryName)
          .setMaxPlayers(20);

      optionsApplier.accept(networkServer);
      this.applyDefaults(networkServer);

      ServerCreationResult result = Network.getNetworkUtils().createPlayerServer(uuid, networkServer);

      if (result.isSuccessful()) {
        Server server = this.addServer(networkServer, ((ServerCreationResult.Success) result).getServerPath(), true);
        return new ServerSetupResult.Success((ServerCreationResult.Success) result, networkServer, server);
      } else {
        return new ServerSetupResult.Fail(result, networkServer, "server creation failed");
      }
    } finally {
      this.serverCreationLock.unlock();
    }
  }

  public ServerSetupResult loadPublicPlayerServer(String categoryName, String serverName,
                                                  Consumer<NetworkServer> optionsApplier) {
    this.serverCreationLock.lock();

    try {
      String serverId = categoryName + "_" + serverName;
      if (this.getServer(serverId) != null) {
        return new ServerSetupResult.Fail(null, null, "server already exists");
      }

      int port = this.nextEmptyPort();
      NetworkServer networkServer = new NetworkServer(serverId, ServerType.GAME)
          .setPort(port)
          .setFolderName(serverName)
          .setTask(categoryName)
          .setMaxPlayers(20);

      optionsApplier.accept(networkServer);
      this.applyDefaults(networkServer);

      ServerCreationResult result = Network.getNetworkUtils().createPublicPlayerServer(networkServer);

      if (result.isSuccessful()) {
        Server server = this.addServer(networkServer, ((ServerCreationResult.Success) result).getServerPath(), true);
        return new ServerSetupResult.Success((ServerCreationResult.Success) result, networkServer, server);
      } else {
        return new ServerSetupResult.Fail(result, networkServer, "server creation failed");
      }
    } finally {
      this.serverCreationLock.unlock();
    }
  }

  private Server addServer(NetworkServer server, Path serverPath, boolean registerServer) {
    Server newServer;
    if (ServerType.LOBBY.equals(server.getType())) {
      newServer = this.addLobby(server.getPort(), server.getName(), serverPath, server);
    } else if (ServerType.LOUNGE.equals(server.getType())) {
      newServer = this.addLounge(server.getPort(), server.getName(), serverPath, server);
    } else if (ServerType.GAME.equals(server.getType())) {
      newServer = this.addGame(server.getPort(), server.getName(), server.getTask(), serverPath, server);
    } else if (ServerType.BUILD.equals(server.getType())) {
      newServer = this.addBuild(server.getPort(), server.getName(), server.getTask(), serverPath, server);
    } else if (ServerType.TEMP_GAME.equals(server.getType())) {
      newServer = this.addTempGame(server.getPort(), server.getName(), server.getTask(), serverPath, server);
    } else {
      throw new IllegalArgumentException("invalid server type: " + server.getType());
    }

    if (registerServer) {
      BasicProxy.getServer().registerServer(new ServerInfo(server.getName(), new InetSocketAddress(server.getPort())));
    }

    this.tmpDirsByServerName.put(server.getName(), serverPath);

    return newServer;
  }

  public boolean deleteServer(String name, boolean force) {
    if (!this.tmpDirsByServerName.containsKey(name)) {
      return false;
    }

    Server server = this.getServer(name);

    if (server == null) {
      return false;
    }

    if (!force && !server.getStatus().equals(Status.Server.OFFLINE)) {
      return false;
    }

    server.getDatabase().delete();
    BasicProxy.getServer().unregisterServer(server.getBungeeInfo().getServerInfo());

    try {
      FileUtils.deleteDirectory(this.tmpDirsByServerName.remove(name).toFile());
    } catch (IOException ex) {
      return false;
    }

    this.logger.info("Deleted tmp server {}", name);

    return true;
  }

  public CompletableFuture<Boolean> killAndDeleteServer(String name, Long pid) {
    Optional<ProcessHandle> process = ProcessHandle.of(pid);

    if (process.isEmpty()) {
      return CompletableFuture.completedFuture(false);
    }

    if (!process.get().destroy()) {
      return CompletableFuture.completedFuture(false);
    }

    CompletableFuture<ProcessHandle> future = process.get().onExit();

    return CompletableFuture.supplyAsync(() -> {
      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        return false;
      }

      this.logger.info("Killed server {}", name);
      return this.deleteServer(name, true);
    });
  }

  private int nextEmptyPort() {
    int port = Network.PORT_BASE;
    while (this.servers.containsKey2(port)) {
      port++;
    }
    return port;
  }

  public int nextServerId() {
    int id = this.serverIds.size();
    while (this.serverIds.contains(id)) {
      id++;
    }
    this.serverIds.add(id);
    return id;
  }

  public LobbyServer addLobby(int port, String name, Path folderPath,
                              NetworkServer networkServer) {
    Database.getServers().addLobby(name, port, Status.Server.OFFLINE, folderPath);
    LobbyServer server = new LobbyServer(Database.getServers().getServer(ServerType.LOBBY, port), folderPath,
        networkServer);
    servers.put(name, port, server);
    return server;
  }


  public GameServer addGame(int port, String name, String task, Path folderPath,
                            NetworkServer networkServer) {
    Database.getServers().addGame(name, port, task, Status.Server.OFFLINE, folderPath);
    GameServer server = new NonTmpGameServer(Database.getServers().getServer(ServerType.GAME, port), folderPath,
        networkServer);
    servers.put(name, port, server);
    return server;
  }


  public LoungeServer addLounge(int port, String name, Path folderPath,
                                NetworkServer networkServer) {
    Database.getServers().addLounge(name, port, Status.Server.OFFLINE, folderPath);
    LoungeServer server = new LoungeServer(Database.getServers().getServer(ServerType.LOUNGE, port), folderPath,
        networkServer);
    servers.put(name, port, server);
    return server;
  }


  public TmpGameServer addTempGame(int port, String name, String task, Path folderPath,
                                   NetworkServer networkServer) {
    Database.getServers().addTempGame(name, port, task, Status.Server.OFFLINE, folderPath);
    TmpGameServer server = new TmpGameServer(Database.getServers().getServer(ServerType.TEMP_GAME, port), folderPath,
        networkServer);
    servers.put(name, port, server);
    return server;
  }


  public BuildServer addBuild(int port, String name, String task, Path folderPath,
                              NetworkServer networkServer) {
    Database.getServers().addBuild(name, port, task, Status.Server.OFFLINE, folderPath);
    BuildServer server = new BuildServer(Database.getServers().getServer(ServerType.BUILD, port), folderPath,
        networkServer);
    servers.put(name, port, server);
    return server;
  }

  @ChannelHandler(type = {ListenerType.SERVER_PERMISSION, ListenerType.SERVER_STATUS})
  public void onServerMessage(ChannelServerMessage<?> msg) {
    MessageType<?> type = msg.getMessageType();
    if (type.equals(MessageType.Server.PERMISSION)) {

      for (User user : Network.getUsers()) {
        if (user.getServer().getName().equals(msg.getName())) {
          user.updatePermissions(false);
        }
      }
    } else if (type.equals(MessageType.Server.STATUS)) {
      Server server = this.getServer(msg.getName());

      if (server == null) {
        return;
      }

      server.updateStatus();

      if (server.getType().equals(ServerType.LOBBY)) {
        if (server.getStatus().equals(Status.Server.ONLINE)) {
          this.onlineLobbys++;
        } else if (server.getStatus().equals(Status.Server.OFFLINE)) {
          this.onlineLobbys--;
        }
      }

      if (msg.getValue() != null && msg.getValue().equals(Status.Server.OFFLINE)) {
        getServer(msg.getName()).setStatus(Status.Server.OFFLINE, true);
      }
    }
  }

  // TODO implement new detection
  //@ChannelHandler(type = ListenerType.LISTENER_UNREGISTER)
  //public void onChannelRegisterMessage(ChannelListenerMessage<String> msg) {
  //  if (msg.getMessageType().equals(MessageType.Listener.UNREGISTER_SERVER)) {
  //    Server server = this.getServer(msg.getValue());
  //    if (server != null) {
  //      server.setStatus(Status.Server.OFFLINE, true);
  //      Loggers.NETWORK.info("Updated status of server " + server.getName() + " to offline");
  //    }
  //  }
  //}

  public Map<String, Path> getTmpDirsByServerName() {
    return tmpDirsByServerName;
  }

  public int getOnlineLobbys() {
    return onlineLobbys;
  }
}
