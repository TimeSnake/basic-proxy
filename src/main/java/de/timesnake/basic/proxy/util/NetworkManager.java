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
import de.timesnake.channel.core.Channel;
import de.timesnake.channel.proxy.channel.ProxyChannel;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.server.DbServer;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.basic.util.server.Task;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.chat.TimeDownParser;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.network.NetworkServer;
import de.timesnake.library.network.NetworkUtils;
import de.timesnake.library.network.ServerCreationResult;
import de.timesnake.library.network.ServerInitResult;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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
        Database.getNetwork()
                .addNetworkFile("templates", this.networkPath.resolve("templates").toFile());
        Database.getNetwork().addNetworkFile("network", this.networkPath.toFile());

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
                new NetworkServer("lobby0", 25001, Type.Server.LOBBY, this.getVelocitySecret())
                        .setMaxPlayers(50), true, false, false);

        if (res.getA().isSuccessful()) {
            this.printText(Plugin.NETWORK, "Created lobby0 server");
        } else {
            this.printWarning(Plugin.NETWORK, ((ServerCreationResult.Fail) res.getA()).getReason());
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

    public void broadcastMessage(Plugin plugin, Component msg) {
        BasicProxy.getServer().sendMessage(Chat.getSenderPlugin(plugin).append(msg));
    }

    public void sendConsoleMessage(String message) {
        BasicProxy.getServer().sendMessage(net.kyori.adventure.text.Component.text(message));
    }


    public Integer getPort() {
        return 25565;
    }

    public String getName() {
        return Channel.PROXY_NAME;
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

    public ScheduledTask runTaskLater(Task task, Duration delay) {
        return BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), task::run)
                .delay(delay).schedule();
    }

    public ScheduledTask runTaskAsync(Task task) {
        return BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), task::run)
                .schedule();
    }

    @Deprecated
    public final void printText(Plugin plugin, String text, String... subPlugins) {
        StringBuilder sb = new StringBuilder("[" + plugin.getName() + "]");
        for (String subPlugin : subPlugins) {
            sb.append("[");
            sb.append(subPlugin);
            sb.append("]");
        }
        sb.append(" ").append(text);
        BasicProxy.getLogger().info(sb.toString());
    }

    @Deprecated
    public final void printText(Plugin plugin, Component text, String... subPlugins) {
        this.printText(plugin, PlainTextComponentSerializer.plainText().serialize(text),
                subPlugins);
    }

    @Deprecated
    public final void printWarning(Plugin plugin, String warning, String... subPlugins) {
        StringBuilder sb = new StringBuilder("[" + plugin.getName() + "]");
        for (String subPlugin : subPlugins) {
            sb.append("[");
            sb.append(subPlugin);
            sb.append("]");
        }
        sb.append(" WARNING ").append(warning);
        BasicProxy.getLogger().warning(sb.toString());
    }

    public void runCommand(String command) {
        BasicProxy.getServer().getCommandManager()
                .executeAsync(BasicProxy.getServer().getConsoleCommandSource(), command);
    }

    public void registerListener(Object listener) {
        BasicProxy.getEventManager().register(BasicProxy.getPlugin(), listener);
    }

    public ProxyChannel getChannel() {
        return (ProxyChannel) Channel.getInstance();
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

    @Deprecated
    public Collection<Integer> getNotOfflineServerPorts() {
        return getServerManager().getNotOfflineServerPorts();
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

    public Tuple<ServerCreationResult, Optional<Server>> createTmpServer(NetworkServer server,
            boolean copyWorlds, boolean syncPlayerData, boolean registerServer) {
        return getServerManager().createTmpServer(server, copyWorlds, syncPlayerData,
                registerServer);
    }

    public Tuple<ServerCreationResult, Optional<Server>> createTmpServer(NetworkServer server,
            boolean copyWorlds, boolean syncPlayerData) {
        return getServerManager().createTmpServer(server, copyWorlds, syncPlayerData);
    }

    public ServerInitResult createPublicPlayerServer(Type.Server<?> type, String task,
            String name) {
        return getServerManager().createPublicPlayerServer(type, task, name);
    }

    public ServerInitResult createPlayerServer(UUID uuid, Type.Server<?> type, String task,
            String name) {
        return getServerManager().createPlayerServer(uuid, type, task, name);
    }

    public Tuple<ServerCreationResult, Optional<Server>> loadPlayerServer(UUID uuid,
            NetworkServer server) {
        return getServerManager().loadPlayerServer(uuid, server);
    }

    public Tuple<ServerCreationResult, Optional<Server>> loadPublicPlayerServer(
            NetworkServer server) {
        return getServerManager().loadPublicPlayerServer(server);
    }

    public Tuple<ServerCreationResult, Optional<Server>> loadPlayerGameServer(UUID uuid,
            NetworkServer server) {
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
