/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.timesnake.basic.proxy.core.channel.ChannelPingPong;
import de.timesnake.basic.proxy.core.file.CmdFile;
import de.timesnake.basic.proxy.core.file.Config;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.core.network.AutoShutdown;
import de.timesnake.basic.proxy.core.permission.PermissionManager;
import de.timesnake.basic.proxy.core.punishment.PunishmentManager;
import de.timesnake.basic.proxy.core.server.ServerCmd;
import de.timesnake.basic.proxy.core.support.SupportManager;
import de.timesnake.basic.proxy.core.user.UserManager;
import de.timesnake.basic.proxy.util.chat.CommandManager;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.basic.proxy.util.server.ServerManager;
import de.timesnake.basic.proxy.util.user.PreUser;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.core.ServerChannel;
import de.timesnake.channel.proxy.channel.ProxyChannel;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.server.DbServer;
import de.timesnake.library.basic.util.Loggers;
import de.timesnake.library.basic.util.ServerType;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.chat.Chat;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.chat.TimeDownParser;
import de.timesnake.library.network.*;
import de.timesnake.library.network.NetworkServer.CopyType;
import net.kyori.adventure.text.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkManager {

  public static NetworkManager getInstance() {
    return instance;
  }

  private static final NetworkManager instance = new NetworkManager();


  private final ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();

  private final LinkedList<User> networkMessageListeners = new LinkedList<>();
  private final LinkedList<User> privateMessageListeners = new LinkedList<>();
  private final LinkedList<User> supportMessageListeners = new LinkedList<>();

  private TimeDownParser timeDownParser;
  private final CommandManager commandManager = new CommandManager();
  private final PermissionManager permissionManager = new PermissionManager();
  private final ServerCmd serverCmd = new ServerCmd();
  private final ChannelPingPong channelPingPong = new ChannelPingPong();
  private Integer maxPlayersLobby = 20;
  private Integer maxPlayersBuild = 20;
  private UserManager userManager;
  private AutoShutdown autoShutdown;
  private SupportManager supportManager;
  private String velocitySecret;
  private boolean tmuxEnabled;
  private Path networkPath;
  private NetworkUtils networkUtils;
  private PunishmentManager punishmentManager;
  private NetworkVariables variables;

  private GroupManager groupManager;

  private ServerManager serverManager;

  private boolean isWork = false;


  public void onEnable() {
    this.timeDownParser = this.initTimeDownParser();
    this.userManager = new UserManager();
    this.groupManager = new GroupManager();
    this.groupManager.loadGroups();

    Config config = new Config();
    this.networkPath = Path.of(config.getNetworkPath());
    Database.getNetwork().addNetworkFile("templates", this.networkPath.resolve("templates").toFile());
    Database.getNetwork().addNetworkFile("network", this.networkPath.toFile());

    String networkName = Database.getNetwork().getValue(NetworkVariables.NETWORK_NAME);
    if (networkName == null) {
      networkName = Network.DEFAULT_NETWORK_NAME;
      Database.getNetwork().setValue(NetworkVariables.NETWORK_NAME, networkName);
    }

    this.variables = new NetworkVariables();
    this.variables.load();

    this.velocitySecret = config.getVelocitySecret();
    this.tmuxEnabled = config.isTmuxEnabled();

    if (!Database.getGroups().containsPermGroup(Network.GUEST_PERM_GROUP_NAME)) {
      Database.getGroups()
          .addPermGroup(Network.GUEST_PERM_GROUP_NAME, Network.GUEST_PERM_GROUP_RANK);
    }

    if (!Database.getGroups().containsPermGroup(Network.MEMBER_PERM_GROUP_NAME)) {
      Database.getGroups()
          .addPermGroup(Network.MEMBER_PERM_GROUP_NAME, Network.MEMBER_PERM_GROUP_RANK);
    }

    if (!Database.getGroups().containsDisplayGroup(Network.GUEST_DISPLAY_GROUP_NAME)) {
      Database.getGroups().addDisplayGroup(Network.GUEST_DISPLAY_GROUP_NAME,
          Network.GUEST_DISPLAY_GROUP_RANK,
          "Guest", ExTextColor.GRAY);
    }

    if (!Database.getGroups().containsDisplayGroup(Network.MEMBER_DISPLAY_GROUP_NAME)) {
      Database.getGroups().addDisplayGroup(Network.MEMBER_DISPLAY_GROUP_NAME,
          Network.MEMBER_DISPLAY_GROUP_RANK,
          null, null);
    }

    this.punishmentManager = new PunishmentManager();

    maxPlayersLobby = config.getMaxPlayersLobby();
    maxPlayersBuild = config.getMaxPlayersBuild();

    this.serverManager = new ServerManager();

    this.channelPingPong.startPingPong();
    this.getChannel().addTimeOutListener(this.channelPingPong);

    this.supportManager = new SupportManager();

    this.autoShutdown = new AutoShutdown();
    this.autoShutdown.start(AutoShutdown.START_TIME);

    CmdFile cmdFile = new CmdFile();
    cmdFile.executeStartCommands();

    this.networkUtils = new NetworkUtils(this.networkPath);

    Tuple<ServerCreationResult, Optional<Server>> res = this.createTmpServer(
        new NetworkServer("lobby0", 25001, ServerType.LOBBY)
            .setMaxPlayers(50)
            .options(o -> o.setWorldCopyType(CopyType.SYNC)),
        false);

    if (res.getA().isSuccessful()) {
      Loggers.NETWORK.info("Created lobby server");
    } else {
      Loggers.NETWORK.warning("Failed to start lobby server: " + ((ServerCreationResult.Fail) res.getA()).getReason());
    }
  }

  protected TimeDownParser initTimeDownParser() {
    return new TimeDownParser();
  }

  public void broadcastMessage(String msg) {
    BasicProxy.getServer().sendMessage(net.kyori.adventure.text.Component.text(msg));
  }

  public void broadcastMessage(Plugin plugin, String msg) {
    BasicProxy.getServer()
        .sendMessage(Chat.getSenderPlugin(plugin).append(Component.text(msg)));
  }

  public void broadcastTDMessage(Plugin plugin, String msg) {
    BasicProxy.getServer().sendMessage(Chat.getSenderPlugin(plugin)
        .append(this.getTimeDownParser().parse2Component(msg)));
  }

  public void broadcastMessage(Plugin plugin, Component msg) {
    BasicProxy.getServer().sendMessage(Chat.getSenderPlugin(plugin).append(msg));
  }

  public void sendConsoleMessage(String message) {
    BasicProxy.getServer().sendMessage(net.kyori.adventure.text.Component.text(message));
  }

  public NetworkVariables getVariables() {
    return this.variables;
  }


  public Integer getPort() {
    return 25565;
  }

  public String getName() {
    return this.getChannel().getProxyName();
  }

  public User getUser(UUID uuid) {
    return users.get(uuid);
  }

  public User getUser(Player p) {
    return users.get(p.getUniqueId());
  }

  public boolean isUserOnline(UUID uuid) {
    return users.containsKey(uuid);
  }

  public Collection<User> getUsers() {
    return users.values();
  }

  public void sendUserToServer(User user, String server) {
    user.connect(BasicProxy.getServer().getServer(server).get());
  }

  public void sendUserToServer(User user, Integer port) {
    user.connect(BasicProxy.getServer().getServer(this.getServer(port).getName()).get());
  }

  public void removeUser(Player p) {
    if (users.get(p.getUniqueId()) != null) {
      users.get(p.getUniqueId()).quit();
    }
    users.remove(p.getUniqueId());
  }

  public User addUser(Player p, PreUser user) {
    users.put(p.getUniqueId(), new User(p, user));
    return users.get(p.getUniqueId());
  }

  public boolean isWork() {
    return isWork;
  }

  public void setWork(boolean isWork) {
    this.isWork = isWork;
  }

  public List<User> getNetworkMessageListeners() {
    return networkMessageListeners;
  }

  public List<User> getPrivateMessageListeners() {
    return privateMessageListeners;
  }

  public List<User> getSupportMessageListeners() {
    return supportMessageListeners;
  }

  public void addNetworkMessageListener(User user) {
    networkMessageListeners.add(user);
  }

  public void addPrivateMessageListener(User user) {
    privateMessageListeners.add(user);
  }

  public void addSupportMessageListener(User user) {
    supportMessageListeners.add(user);
  }

  public void removeNetworkMessageListener(User user) {
    networkMessageListeners.remove(user);
  }

  public void removePrivateMessageListener(User user) {
    privateMessageListeners.remove(user);
  }

  public void removeSupportMessageListener(User user) {
    supportMessageListeners.remove(user);
  }

  public ScheduledTask runTaskLater(Runnable task, Duration delay) {
    return BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), task).delay(delay).schedule();
  }

  public ScheduledTask runTaskAsync(Runnable task) {
    return BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), task).schedule();
  }

  public void runCommand(String command) {
    BasicProxy.getServer().getCommandManager().executeAsync(BasicProxy.getServer().getConsoleCommandSource(), command);
  }

  public void registerListener(Object listener) {
    BasicProxy.getEventManager().register(BasicProxy.getPlugin(), listener);
  }

  public ProxyChannel getChannel() {
    return (ProxyChannel) ServerChannel.getInstance();
  }

  public Integer getMaxPlayersLobby() {
    return maxPlayersLobby;
  }

  public Integer getMaxPlayersBuild() {
    return maxPlayersBuild;
  }

  public CommandManager getCommandManager() {
    return this.commandManager;
  }

  public PermissionManager getPermissionHandler() {
    return this.permissionManager;
  }

  public ServerCmd getBukkitCmdHandler() {
    return this.serverCmd;
  }

  public String getVelocitySecret() {
    return velocitySecret;
  }

  public Path getNetworkPath() {
    return this.networkPath;
  }

  public boolean isTmuxEnabled() {
    return tmuxEnabled;
  }

  public NetworkUtils getNetworkUtils() {
    return this.networkUtils;
  }

  public ServerManager getServerManager() {
    return serverManager;
  }

  public GroupManager getGroupManager() {
    return this.groupManager;
  }

  // server manager

  public int getOnlineLobbys() {
    return getServerManager().getOnlineLobbys();
  }

  public Collection<Server> getServers() {
    return getServerManager().getServers();
  }

  public Collection<String> getNotOfflineServerNames() {
    return getServerManager().getNotOfflineServerNames();
  }

  public Server getServer(Integer port) {
    return getServerManager().getServer(port);
  }

  public Server getServer(String name) {
    return getServerManager().getServer(name);
  }

  public Server getServer(DbServer server) {
    return getServerManager().getServer(server);
  }

  public void updateServerTaskAll() {
    getServerManager().updateServerTaskAll();
  }

  public void updateServerTask(int port) {
    getServerManager().updateServerTask(port);
  }

  public Tuple<ServerCreationResult, Optional<Server>> createTmpServer(NetworkServer server, boolean registerServer) {
    return getServerManager().createTmpServer(server, registerServer);
  }

  public Tuple<ServerCreationResult, Optional<Server>> createTmpServer(NetworkServer server) {
    return this.createTmpServer(server, true);
  }

  public ServerInitResult createPublicPlayerServer(ServerType type, String task, String name) {
    return getServerManager().initNewPublicPlayerServer(type, task, name);
  }

  public ServerInitResult createPlayerServer(UUID uuid, ServerType type, String task, String name) {
    return getServerManager().initNewPlayerServer(uuid, type, task, name);
  }

  public Tuple<ServerCreationResult, Optional<Server>> loadPlayerServer(UUID uuid, NetworkServer server) {
    return getServerManager().loadPlayerServer(uuid, server);
  }

  public Tuple<ServerCreationResult, Optional<Server>> loadPublicPlayerServer(
      NetworkServer server) {
    return getServerManager().loadPublicPlayerServer(server);
  }

  public Tuple<ServerCreationResult, Optional<Server>> loadPlayerGameServer(UUID uuid, NetworkServer server) {
    return getServerManager().loadPlayerGameServer(uuid, server);
  }

  public Tuple<ServerCreationResult, Optional<Server>> loadPublicPlayerGameServer(
      NetworkServer server) {
    return getServerManager().loadPublicPlayerGameServer(server);
  }

  public boolean deleteServer(String name, boolean force) {
    return getServerManager().deleteServer(name, force);
  }

  public CompletableFuture<Boolean> killAndDeleteServer(String name, Long pid) {
    return getServerManager().killAndDeleteServer(name, pid);
  }

  public int nextEmptyPort() {
    return getServerManager().nextEmptyPort();
  }

  public Map<String, Path> getTmpDirsByServerName() {
    return getServerManager().getTmpDirsByServerName();
  }

  public TimeDownParser getTimeDownParser() {
    return timeDownParser;
  }

  public PunishmentManager getPunishmentManager() {
    return punishmentManager;
  }
}
