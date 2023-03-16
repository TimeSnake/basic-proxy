/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.server;

import com.velocitypowered.api.proxy.server.ServerInfo;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.util.listener.ChannelHandler;
import de.timesnake.channel.util.listener.ChannelListener;
import de.timesnake.channel.util.listener.ListenerType;
import de.timesnake.channel.util.message.ChannelListenerMessage;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.server.DbServer;
import de.timesnake.library.basic.util.MultiKeyMap;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.network.NetworkServer;
import de.timesnake.library.network.ServerCreationResult;
import de.timesnake.library.network.ServerInitResult;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.FileUtils;

public class ServerManager implements ChannelListener {

    private final MultiKeyMap<String, Integer, Server> servers = new MultiKeyMap<>();
    private final Map<String, Path> tmpDirsByServerName = new HashMap<>();

    private int onlineLobbys = 0;

    public ServerManager() {
        Network.getChannel().addListener(this);
    }

    public Collection<Server> getServers() {
        return servers.values();
    }

    @Deprecated
    public Collection<Integer> getNotOfflineServerPorts() {
        Collection<Integer> ports = new HashSet<>();
        for (Server server : this.getServers()) {
            if (server.getStatus() != null && !server.getStatus().equals(Status.Server.OFFLINE)
                    && !server.getStatus().equals(Status.Server.LAUNCHING)) {
                ports.add(server.getPort());
            }
        }
        return ports;
    }

    public Collection<String> getNotOfflineServerNames() {
        Collection<String> names = new HashSet<>();
        for (Server server : this.getServers()) {
            if (server.getStatus() != null && !server.getStatus().equals(Status.Server.OFFLINE)
                    && !server.getStatus().equals(Status.Server.LAUNCHING)) {
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

    public Tuple<ServerCreationResult, Optional<Server>> createTmpServer(NetworkServer server,
            boolean copyWorlds, boolean syncPlayerData, boolean registerServer) {
        ServerCreationResult result = Network.getNetworkUtils()
                .createServer(server, copyWorlds, syncPlayerData);
        Optional<Server> serverOpt = Optional.empty();
        if (result.isSuccessful()) {
            Path serverPath = ((ServerCreationResult.Successful) result).getServerPath();
            serverOpt = Optional.ofNullable(this.addServer(server, serverPath, registerServer));
        }
        return new Tuple<>(result, serverOpt);
    }

    public Tuple<ServerCreationResult, Optional<Server>> createTmpServer(NetworkServer server,
            boolean copyWorlds, boolean syncPlayerData) {
        return this.createTmpServer(server, copyWorlds, syncPlayerData, true);
    }

    public ServerInitResult createPublicPlayerServer(Type.Server<?> type, String task,
            String name) {
        return Network.getNetworkUtils().initPublicPlayerServer(type, task, name);
    }

    public ServerInitResult createPlayerServer(UUID uuid, Type.Server<?> type, String task,
            String name) {
        return Network.getNetworkUtils().initPlayerServer(uuid, type, task, name);
    }

    public Tuple<ServerCreationResult, Optional<Server>> loadPlayerServer(UUID uuid,
            NetworkServer server) {
        ServerCreationResult result = Network.getNetworkUtils().createPlayerServer(uuid, server);
        Optional<Server> serverOpt = Optional.empty();
        if (result.isSuccessful()) {
            Path serverPath = ((ServerCreationResult.Successful) result).getServerPath();
            serverOpt = Optional.ofNullable(this.addServer(server, serverPath, true));
        }
        return new Tuple<>(result, serverOpt);
    }

    public Tuple<ServerCreationResult, Optional<Server>> loadPublicPlayerServer(
            NetworkServer server) {
        ServerCreationResult result = Network.getNetworkUtils().createPublicPlayerServer(server);
        Optional<Server> serverOpt = Optional.empty();
        if (result.isSuccessful()) {
            Path serverPath = ((ServerCreationResult.Successful) result).getServerPath();
            serverOpt = Optional.ofNullable(this.addServer(server, serverPath, true));
        }
        return new Tuple<>(result, serverOpt);
    }

    public Tuple<ServerCreationResult, Optional<Server>> loadPlayerGameServer(UUID uuid,
            NetworkServer server) {
        Tuple<ServerCreationResult, Optional<Server>> result = this.loadPlayerServer(uuid, server);

        if (result.getA().isSuccessful()) {
            ((NonTmpGameServer) result.getB().get()).setOwnerUuid(uuid);
        }

        return result;
    }

    public Tuple<ServerCreationResult, Optional<Server>> loadPublicPlayerGameServer(
            NetworkServer server) {
        return this.loadPublicPlayerServer(server);
    }

    private Server addServer(NetworkServer server, Path serverPath, boolean registerServer) {
        Server newServer = null;
        if (Type.Server.LOBBY.equals(server.getType())) {
            newServer = this.addLobby(server.getPort(), server.getName(), serverPath, server);
        } else if (Type.Server.LOUNGE.equals(server.getType())) {
            newServer = this.addLounge(server.getPort(), server.getName(), serverPath, server);
        } else if (Type.Server.GAME.equals(server.getType())) {
            newServer = this.addGame(server.getPort(), server.getName(), server.getTask(),
                    serverPath, server);
        } else if (Type.Server.BUILD.equals(server.getType())) {
            newServer = this.addBuild(server.getPort(), server.getName(), server.getTask(),
                    serverPath, server);
        } else if (Type.Server.TEMP_GAME.equals(server.getType())) {
            newServer = this.addTempGame(server.getPort(), server.getName(), server.getTask(),
                    serverPath, server);
        }

        if (registerServer) {
            BasicProxy.getServer().registerServer(
                    new ServerInfo(server.getName(), new InetSocketAddress(server.getPort())));
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

        BasicProxy.getLogger().info("Deleted tmp server " + name);

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

            BasicProxy.getLogger().info("Killed server " + name);
            return this.deleteServer(name, true);
        });
    }

    public int nextEmptyPort() {
        int port = Network.PORT_BASE;
        while (this.servers.containsKey2(port)) {
            port++;
        }
        return port;
    }

    public LobbyServer addLobby(int port, String name, Path folderPath,
            NetworkServer networkServer) {
        Database.getServers().addLobby(name, port, Status.Server.OFFLINE, folderPath);
        LobbyServer server = new LobbyServer(
                Database.getServers().getServer(Type.Server.LOBBY, port), folderPath,
                networkServer);
        servers.put(name, port, server);
        return server;
    }


    public GameServer addGame(int port, String name, String task, Path folderPath,
            NetworkServer networkServer) {
        Database.getServers().addGame(name, port, task, Status.Server.OFFLINE, folderPath);
        GameServer server = new NonTmpGameServer(
                Database.getServers().getServer(Type.Server.GAME, port), folderPath, networkServer);
        servers.put(name, port, server);
        return server;
    }


    public LoungeServer addLounge(int port, String name, Path folderPath,
            NetworkServer networkServer) {
        Database.getServers().addLounge(name, port, Status.Server.OFFLINE, folderPath);
        LoungeServer server = new LoungeServer(
                Database.getServers().getServer(Type.Server.LOUNGE, port), folderPath,
                networkServer);
        servers.put(name, port, server);
        return server;
    }


    public TmpGameServer addTempGame(int port, String name, String task, Path folderPath,
            NetworkServer networkServer) {
        Database.getServers().addTempGame(name, port, task, Status.Server.OFFLINE, folderPath);
        TmpGameServer server = new TmpGameServer(
                Database.getServers().getServer(Type.Server.TEMP_GAME, port), folderPath,
                networkServer);
        servers.put(name, port, server);
        return server;
    }


    public BuildServer addBuild(int port, String name, String task, Path folderPath,
            NetworkServer networkServer) {
        Database.getServers().addBuild(name, port, task, Status.Server.OFFLINE, folderPath);
        BuildServer server = new BuildServer(
                Database.getServers().getServer(Type.Server.BUILD, port), folderPath,
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

            if (server.getType().equals(Type.Server.LOBBY)) {
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

    @ChannelHandler(type = ListenerType.LISTENER_UNREGISTER)
    public void onChannelRegisterMessage(ChannelListenerMessage<String> msg) {
        if (msg.getMessageType().equals(MessageType.Listener.UNREGISTER_SERVER)) {
            Server server = this.getServer(msg.getValue());
            if (server != null) {
                server.setStatus(Status.Server.OFFLINE, true);
                Network.printText(Plugin.NETWORK,
                        "Updated status from server " + server.getName() + " to offline");
            }
        }
    }

    public Map<String, Path> getTmpDirsByServerName() {
        return tmpDirsByServerName;
    }

    public int getOnlineLobbys() {
        return onlineLobbys;
    }
}
