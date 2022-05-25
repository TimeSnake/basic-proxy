package de.timesnake.basic.proxy.util;

import de.timesnake.basic.proxy.core.channel.ChannelPingPong;
import de.timesnake.basic.proxy.core.file.Config;
import de.timesnake.basic.proxy.core.file.ServerConfig;
import de.timesnake.basic.proxy.core.group.Group;
import de.timesnake.basic.proxy.core.group.GroupNotInDatabaseException;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.core.permission.PermissionManager;
import de.timesnake.basic.proxy.core.rule.RuleManager;
import de.timesnake.basic.proxy.core.script.AutoShutdown;
import de.timesnake.basic.proxy.core.script.CmdFile;
import de.timesnake.basic.proxy.core.server.BukkitCmdHandler;
import de.timesnake.basic.proxy.core.support.SupportManager;
import de.timesnake.basic.proxy.core.user.UserManager;
import de.timesnake.basic.proxy.util.chat.Chat;
import de.timesnake.basic.proxy.util.chat.ChatColor;
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
import de.timesnake.library.basic.util.chat.Plugin;
import de.timesnake.library.basic.util.server.Task;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.plugin.Listener;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NetworkManager implements ChannelListener, Network {

    private static final NetworkManager instance = new NetworkManager();
    private final HashMap<String, Group> groups = new HashMap<>();
    private final ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();
    private final ArrayList<User> networkMessageListeners = new ArrayList<>();
    private final ArrayList<User> privateMessageListeners = new ArrayList<>();
    private final ArrayList<User> supportMessageListeners = new ArrayList<>();
    private final HashMap<Integer, Server> servers = new HashMap<>();
    private final CommandHandler commandHandler = new CommandHandler();
    private final PermissionManager permissionManager = new PermissionManager();
    private final BukkitCmdHandler bukkitCmdHandler = new BukkitCmdHandler();
    private final ChannelPingPong channelPingPong = new ChannelPingPong();
    private final CmdFile cmdFile = new CmdFile();
    public Chat chat;
    public ServerConfig serverConfig;
    private boolean isWork = false;
    private Group guestGroup;
    private Integer maxPlayersLobby = 20;
    private Integer maxPlayersBuild = 20;
    private UserManager userManager;
    private RuleManager ruleManager;
    private AutoShutdown autoShutdown;
    private SupportManager supportManager;
    private int onlineLobbys = 0;

    public static NetworkManager getInstance() {
        return instance;
    }

    public void onEnable() {

        chat = new de.timesnake.basic.proxy.util.chat.Chat();

        this.userManager = new UserManager();

        this.getChannel().addListener(this);

        Config.onEnable();
        String guestGroupName = Config.getGuestGroupName();
        if (guestGroupName != null) {
            try {
                guestGroup = new Group(guestGroupName);
            } catch (GroupNotInDatabaseException e) {
                System.out.println(e.getMessage());
            }
        } else {
            this.printWarning(Plugin.PROXY, "Guest group is not loaded");
        }

        for (String groupName : Database.getGroups().getPermGroupNames()) {
            try {
                groups.put(groupName, new Group(groupName));
            } catch (GroupNotInDatabaseException e) {
                System.out.println(e.getMessage());
            }
            this.printText(Plugin.PROXY, "Loaded group: " + groupName);
        }

        serverConfig = new ServerConfig();
        serverConfig.loadServers();

        maxPlayersLobby = serverConfig.getMaxPlayersLobby();
        maxPlayersBuild = serverConfig.getMaxPlayersBuild();

        this.ruleManager = new RuleManager();

        this.channelPingPong.startPingPong();
        this.getChannel().addTimeOutListener(this.channelPingPong);

        this.supportManager = new SupportManager();

        this.autoShutdown = new AutoShutdown();
        this.autoShutdown.start(AutoShutdown.START_TIME);

        this.cmdFile.onEnable();
        this.cmdFile.executeStartCommands();

    }

    public void broadcastMessage(String msg) {
        ProxyServer.getInstance().broadcast(new TextComponent(msg));
    }


    public void broadcastMessage(Plugin plugin, String msg) {
        ProxyServer.getInstance().broadcast(new TextComponent(de.timesnake.library.extension.util.chat.Chat.getSenderPlugin(plugin) + msg));
    }


    public void sendConsoleMessage(String message) {
        ProxyServer.getInstance().getConsole().sendMessage(new TextComponent(message));
    }


    public Integer getPort() {
        return 25565;
    }


    public User getUser(UUID uuid) {
        return users.get(uuid);
    }


    public User getUser(ProxiedPlayer p) {
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

    public void addLobby(int port, String name, String folderPath) {
        Database.getServers().addLobby(port, name, Status.Server.OFFLINE, folderPath);
        servers.put(port, new LobbyServer(Database.getServers().getServer(Type.Server.LOBBY, port), folderPath));
    }


    public void addGame(int port, String name, String task, String folderPath) {
        Database.getServers().addGame(port, name, task, Status.Server.OFFLINE, folderPath);
        servers.put(port, new GameServer(Database.getServers().getServer(Type.Server.GAME, port), folderPath));
    }


    public void addLounge(int port, String name, String folderPath) {
        Database.getServers().addLounge(port, name, Status.Server.OFFLINE, folderPath);
        servers.put(port, new LoungeServer(Database.getServers().getServer(Type.Server.LOUNGE, port), folderPath));
    }


    public void addTempGame(int port, String name, String task, String folderPath) {
        Database.getServers().addTempGame(port, name, task, Status.Server.OFFLINE, folderPath);
        servers.put(port, new TempGameServer(Database.getServers().getServer(Type.Server.TEMP_GAME, port), folderPath));
    }


    public void addBuild(int port, String name, String task, String folderPath) {
        Database.getServers().addBuild(port, name, task, Status.Server.OFFLINE, folderPath);
        servers.put(port, new BuildServer(Database.getServers().getServer(Type.Server.BUILD, port), folderPath));
    }


    public void sendUserToServer(User user, String server, String message) {
        user.getPlayer().connect(ProxyServer.getInstance().getServerInfo(server), Reason.valueOf(message));
    }


    public void sendUserToServer(User user, String server) {
        user.getPlayer().connect(ProxyServer.getInstance().getServerInfo(server));
    }


    public void sendUserToServer(User user, Integer port) {
        user.getPlayer().connect(ProxyServer.getInstance().getServerInfo(this.getServer(port).getName()),
                Reason.PLUGIN);
    }


    public void removeUser(ProxiedPlayer p) {
        if (users.get(p.getUniqueId()) != null) {
            users.get(p.getUniqueId()).quit();
        }
        users.remove(p.getUniqueId());
    }


    public User addUser(ProxiedPlayer p, PreUser user) {
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
        BasicProxy.getPlugin().getProxy().getScheduler().schedule(BasicProxy.getPlugin(), task::run, delay.getNano(),
                TimeUnit.NANOSECONDS);
    }

    public void runTaskAsync(Task task) {
        BasicProxy.getPlugin().getProxy().getScheduler().runAsync(BasicProxy.getPlugin(), task::run);
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
        System.out.println(sb);
    }

    public final void printWarning(Plugin plugin, String warning, String... subPlugins) {
        StringBuilder sb = new StringBuilder("[" + plugin.getName() + "]");
        for (String subPlugin : subPlugins) {
            sb.append("[");
            sb.append(subPlugin);
            sb.append("]");
        }
        sb.append(" WARNING ").append(warning);
        System.out.println(ChatColor.YELLOW + sb.toString());
    }

    public final void printError(Plugin plugin, String error, String... subPlugins) {
        StringBuilder sb = new StringBuilder("[" + plugin.getName() + "]");
        for (String subPlugin : subPlugins) {
            sb.append("[");
            sb.append(subPlugin);
            sb.append("]");
        }
        sb.append(" ERROR ").append(error);
        System.out.println(ChatColor.RED + sb.toString());
    }


    public void runCommand(String command) {
        ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), command);
    }


    public void registerListener(Listener listener) {
        ProxyServer.getInstance().getPluginManager().registerListener(BasicProxy.getPlugin(), listener);
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

    public RuleManager getRuleManager() {
        return ruleManager;
    }

    public int getOnlineLobbys() {
        return onlineLobbys;
    }
}
