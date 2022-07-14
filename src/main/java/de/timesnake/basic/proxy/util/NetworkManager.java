package de.timesnake.basic.proxy.util;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.timesnake.basic.proxy.core.channel.ChannelPingPong;
import de.timesnake.basic.proxy.core.file.Config;
import de.timesnake.basic.proxy.core.file.ServerConfig;
import de.timesnake.basic.proxy.core.group.Group;
import de.timesnake.basic.proxy.core.group.GroupNotInDatabaseException;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.core.permission.PermissionManager;
import de.timesnake.basic.proxy.core.script.AutoShutdown;
import de.timesnake.basic.proxy.core.script.CmdFile;
import de.timesnake.basic.proxy.core.server.BukkitCmdHandler;
import de.timesnake.basic.proxy.core.support.SupportManager;
import de.timesnake.basic.proxy.core.user.UserManager;
import de.timesnake.basic.proxy.util.chat.Chat;
import de.timesnake.basic.proxy.util.chat.CommandHandler;
import de.timesnake.basic.proxy.util.server.*;
import de.timesnake.basic.proxy.util.user.PreUser;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.core.NetworkChannel;
import de.timesnake.channel.proxy.channel.Channel;
import de.timesnake.channel.util.listener.ChannelHandler;
import de.timesnake.channel.util.listener.ChannelListener;
import de.timesnake.channel.util.listener.ListenerType;
import de.timesnake.channel.util.message.ChannelListenerMessage;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.server.DbServer;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.basic.util.chat.Plugin;
import de.timesnake.library.basic.util.server.Task;
import de.timesnake.library.network.NetworkServer;
import de.timesnake.library.network.NetworkUtils;
import de.timesnake.library.network.ServerCreationResult;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkManager implements ChannelListener, Network {

    public static NetworkManager getInstance() {
        return instance;
    }

    private static final NetworkManager instance = new NetworkManager();
    private final HashMap<String, Group> groups = new HashMap<>();
    private final ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();
    private final ArrayList<User> networkMessageListeners = new ArrayList<>();
    private final ArrayList<User> privateMessageListeners = new ArrayList<>();
    private final ArrayList<User> supportMessageListeners = new ArrayList<>();
    private final HashMap<Integer, Server> servers = new HashMap<>();
    private final Map<String, Path> tmpDirsByServerName = new HashMap<>();
    private final CommandHandler commandHandler = new CommandHandler();
    private final PermissionManager permissionManager = new PermissionManager();
    private final BukkitCmdHandler bukkitCmdHandler = new BukkitCmdHandler();
    private final ChannelPingPong channelPingPong = new ChannelPingPong();
    public Chat chat;
    public ServerConfig serverConfig;
    private CmdFile cmdFile;
    private boolean isWork = false;
    private Group guestGroup;
    private Integer maxPlayersLobby = 20;
    private Integer maxPlayersBuild = 20;
    private UserManager userManager;
    private AutoShutdown autoShutdown;
    private SupportManager supportManager;
    private int onlineLobbys = 0;
    private Config config;
    private Path networkPath;
    private String velocitySecret;
    private NetworkUtils networkUtils;

    public void onEnable() {

        chat = new de.timesnake.basic.proxy.util.chat.Chat();

        this.userManager = new UserManager();

        this.getChannel().addListener(this);

        this.config = new Config();
        this.networkPath = Path.of(this.config.getNetworkPath());
        this.velocitySecret = this.config.getVelocitySecret();

        String guestGroupName = config.getGuestGroupName();
        if (guestGroupName != null) {
            try {
                guestGroup = new Group(guestGroupName);
            } catch (GroupNotInDatabaseException e) {
                BasicProxy.getLogger().warning(e.getMessage());
            }
        } else {
            this.printWarning(Plugin.PROXY, "Guest group is not loaded");
        }

        for (String groupName : Database.getGroups().getPermGroupNames()) {
            try {
                groups.put(groupName, new Group(groupName));
            } catch (GroupNotInDatabaseException e) {
                BasicProxy.getLogger().warning(e.getMessage());
            }
            this.printText(Plugin.PROXY, "Loaded group: " + groupName);
        }

        serverConfig = new ServerConfig();
        serverConfig.loadServers();

        maxPlayersLobby = serverConfig.getMaxPlayersLobby();
        maxPlayersBuild = serverConfig.getMaxPlayersBuild();

        this.channelPingPong.startPingPong();
        this.getChannel().addTimeOutListener(this.channelPingPong);

        this.supportManager = new SupportManager();

        this.autoShutdown = new AutoShutdown();
        this.autoShutdown.start(AutoShutdown.START_TIME);

        this.cmdFile = new CmdFile();
        this.cmdFile.executeStartCommands();

        this.networkUtils = new NetworkUtils(this.networkPath);
    }

    public void broadcastMessage(String msg) {
        BasicProxy.getServer().sendMessage(net.kyori.adventure.text.Component.text(msg));
    }


    public void broadcastMessage(Plugin plugin, String msg) {
        BasicProxy.getServer().sendMessage(net.kyori.adventure.text.Component.text(
                de.timesnake.library.extension.util.chat.Chat.getSenderPlugin(plugin) + msg));
    }


    public void sendConsoleMessage(String message) {
        BasicProxy.getServer().sendMessage(net.kyori.adventure.text.Component.text(message));
    }


    public Integer getPort() {
        return 25565;
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


    public Group getGroup(String group) {
        return groups.get(group);
    }


    public Collection<Group> getGroups() {
        return groups.values();
    }


    public Collection<User> getUsers() {
        return users.values();
    }


    public Collection<Server> getServers() {
        return servers.values();
    }

    public Collection<Integer> getNotOfflineServerPorts() {
        Collection<Integer> ports = new HashSet<>();
        for (Server server : this.getServers()) {
            if (server.getStatus() != null && !server.getStatus().equals(Status.Server.OFFLINE) && !server.getStatus().equals(Status.Server.STARTING)) {
                ports.add(server.getPort());
            }
        }
        return ports;
    }

    public Server getServer(Integer port) {
        return servers.get(port);
    }


    public Server getServer(String name) {
        for (Server server : servers.values()) {
            if (server.getName().equalsIgnoreCase(name)) {
                return server;
            }
        }
        return null;
    }

    public Server getServer(DbServer server) {
        return servers.get(server.getPort());
    }

    public void updateServerTaskAll() {
        for (Server server : servers.values()) {
            server.setStatus(Database.getServers().getServer(server.getName()).getStatus(), false);
        }

    }

    public void updateServerTask(int port) {
        getServer(port).setStatus(Database.getServers().getServer(port).getStatus(), false);
    }

    public Tuple<ServerCreationResult, Optional<Server>> newServer(NetworkServer server, boolean copyWorlds) {
        ServerCreationResult result = this.networkUtils.createServer(server, copyWorlds);
        Optional<Server> serverOpt = Optional.empty();
        if (result.isSuccessful()) {
            Path serverPath = ((ServerCreationResult.Successful) result).getServerPath();
            if (Type.Server.LOBBY.equals(server.getType())) {
                serverOpt = Optional.of(this.addLobby(server.getPort(), server.getName(), serverPath));
            } else if (Type.Server.LOUNGE.equals(server.getType())) {
                serverOpt = Optional.of(this.addLounge(server.getPort(), server.getName(), serverPath));
            } else if (Type.Server.GAME.equals(server.getType())) {
                serverOpt = Optional.of(this.addGame(server.getPort(), server.getName(), server.getTask(), serverPath));
            } else if (Type.Server.BUILD.equals(server.getType())) {
                serverOpt = Optional.of(this.addBuild(server.getPort(), server.getName(), server.getTask(),
                        serverPath));
            } else if (Type.Server.TEMP_GAME.equals(server.getType())) {
                serverOpt = Optional.of(this.addTempGame(server.getPort(), server.getName(), server.getTask(),
                        serverPath));
            }

            BasicProxy.getServer().registerServer(new ServerInfo(server.getName(),
                    new InetSocketAddress(server.getPort())));

            this.tmpDirsByServerName.put(server.getName(), serverPath);
        }
        return new Tuple<>(result, serverOpt);
    }

    public boolean deleteServer(String name) {
        if (!this.tmpDirsByServerName.containsKey(name)) {
            return false;
        }

        Server server = this.getServer(name);

        if (!server.getStatus().equals(Status.Server.OFFLINE)) {
            return false;
        }

        BasicProxy.getServer().unregisterServer(server.getBungeeInfo().getServerInfo());

        try {
            FileUtils.deleteDirectory(this.tmpDirsByServerName.get(name).toFile());
        } catch (IOException ex) {
            return false;
        }

        BasicProxy.getLogger().info("Deleted tmp server " + name);

        return true;
    }

    public int nextEmptyPort() {
        int port = Network.PORT_BASE;
        while (this.servers.containsKey(port)) port++;
        return port;
    }

    public LobbyServer addLobby(int port, String name, Path folderPath) {
        Database.getServers().addLobby(port, name, Status.Server.OFFLINE, folderPath);
        LobbyServer server = new LobbyServer(Database.getServers().getServer(Type.Server.LOBBY, port), folderPath);
        servers.put(port, server);
        return server;
    }


    public GameServer addGame(int port, String name, String task, Path folderPath) {
        Database.getServers().addGame(port, name, task, Status.Server.OFFLINE, folderPath);
        GameServer server = new GameServer(Database.getServers().getServer(Type.Server.GAME, port), folderPath);
        servers.put(port, server);
        return server;
    }


    public LoungeServer addLounge(int port, String name, Path folderPath) {
        Database.getServers().addLounge(port, name, Status.Server.OFFLINE, folderPath);
        LoungeServer server = new LoungeServer(Database.getServers().getServer(Type.Server.LOUNGE, port), folderPath);
        servers.put(port, server);
        return server;
    }


    public TempGameServer addTempGame(int port, String name, String task, Path folderPath) {
        Database.getServers().addTempGame(port, name, task, Status.Server.OFFLINE, folderPath);
        TempGameServer server = new TempGameServer(Database.getServers().getServer(Type.Server.TEMP_GAME, port),
                folderPath);
        servers.put(port, server);
        return server;
    }


    public BuildServer addBuild(int port, String name, String task, Path folderPath) {
        Database.getServers().addBuild(port, name, task, Status.Server.OFFLINE, folderPath);
        BuildServer server = new BuildServer(Database.getServers().getServer(Type.Server.BUILD, port), folderPath);
        servers.put(port, server);
        return server;
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


    public ArrayList<User> getNetworkMessageListeners() {
        return networkMessageListeners;
    }


    public ArrayList<User> getPrivateMessageListeners() {
        return privateMessageListeners;
    }


    public ArrayList<User> getSupportMessageListeners() {
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


    public Group getGuestGroup() {
        return guestGroup;
    }


    public Group getMemberGroup() {
        Group guest = this.getGuestGroup();
        Group member = null;
        int rank = guest.getRank() + 1;
        while (member != null) {
            for (Group group : this.getGroups()) {
                if (group.getRank() == rank) {
                    member = group;
                    break;
                }
            }
            rank++;
        }
        return guest;
    }

    public void runTaskLater(Task task, Duration delay) {
        BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), task::run).delay(delay).schedule();
    }

    public void runTaskAsync(Task task) {
        BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), task::run).schedule();
    }

    @ChannelHandler(type = {ListenerType.SERVER_PERMISSION, ListenerType.SERVER_STATUS})
    public void onServerMessage(ChannelServerMessage<?> msg) {
        MessageType<?> type = msg.getMessageType();
        if (type.equals(MessageType.Server.PERMISSION)) {

            for (User user : getUsers()) {
                if (user.getServer().getPort().equals(msg.getPort())) {
                    user.updatePermissions();
                }
            }
        } else if (type.equals(MessageType.Server.STATUS)) {
            Server server = this.getServer(msg.getPort());
            server.updateStatus();

            if (server.getType().equals(Type.Server.LOBBY)) {
                if (server.getStatus().equals(Status.Server.ONLINE)) {
                    this.onlineLobbys++;
                } else if (server.getStatus().equals(Status.Server.OFFLINE)) {
                    this.onlineLobbys--;
                }
            }

            if (msg.getValue() != null && msg.getValue().equals(Status.Server.OFFLINE)) {
                getServer(msg.getPort()).setStatus(Status.Server.OFFLINE, true);
            }
        }
    }

    @ChannelHandler(type = ListenerType.LISTENER_UNREGISTER)
    public void onChannelRegisterMessage(ChannelListenerMessage<Integer> msg) {
        if (msg.getMessageType().equals(MessageType.Listener.UNREGISTER_SERVER)) {
            Server server = this.getServer(msg.getValue());
            server.setStatus(Status.Server.OFFLINE, true);
            this.printText(Plugin.NETWORK, "Updated status from server " + server.getName() + " to offline");
        }
    }

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
        BasicProxy.getServer().getCommandManager().executeAsync(BasicProxy.getServer().getConsoleCommandSource(),
                command);
    }

    public void registerListener(Object listener) {
        BasicProxy.getEventManager().register(BasicProxy.getPlugin(), listener);
    }

    public Chat getChat() {
        return chat;
    }


    public Channel getChannel() {
        return (Channel) NetworkChannel.getChannel();
    }


    public Integer getMaxPlayersLobby() {
        return maxPlayersLobby;
    }


    public Integer getMaxPlayersBuild() {
        return maxPlayersBuild;
    }


    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public CommandHandler getCommandHandler() {
        return this.commandHandler;
    }

    public PermissionManager getPermissionHandler() {
        return this.permissionManager;
    }

    public BukkitCmdHandler getBukkitCmdHandler() {
        return this.bukkitCmdHandler;
    }

    public int getOnlineLobbys() {
        return onlineLobbys;
    }

    public String getVelocitySecret() {
        return velocitySecret;
    }

    public Map<String, Path> getTmpDirsByServerName() {
        return tmpDirsByServerName;
    }

    public Path getNetworkPath() {
        return this.networkPath;
    }
}
