package de.timesnake.basic.proxy.util.user;

import de.timesnake.basic.proxy.core.group.Group;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.core.permission.Permission;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.basic.proxy.util.chat.CommandSender;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.channel.util.listener.ChannelHandler;
import de.timesnake.channel.util.listener.ChannelListener;
import de.timesnake.channel.util.listener.ListenerType;
import de.timesnake.channel.util.message.ChannelUserMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.group.DbPermGroup;
import de.timesnake.database.util.permission.DbPermission;
import de.timesnake.database.util.user.DataProtectionAgreement;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.chat.Plugin;
import de.timesnake.library.extension.util.chat.Chat;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class User implements de.timesnake.library.extension.util.player.User, ChannelListener {

    private final DbUser dbUser;
    private final ProxiedPlayer player;
    private boolean service;
    private String nameChat;

    private boolean airMode;

    private DataProtectionAgreement dataProtectionAgreement;

    private String lastChatMessage = "";

    private boolean isListeningNetworkMessages = false;
    private boolean isListeningPrivateMessages = false;
    private boolean isListeningSupportMessages = false;

    private Server server;
    private Server serverLast;
    private Server lobby;

    private Group group;

    private String prefix;
    private String suffix;
    private String nick;

    private float coins;

    private final Set<Permission> databasePermissions = new HashSet<>();
    private final Set<Permission> permissions = new HashSet<>();

    private ScheduledTask dpdInfoTask;

    public User(ProxiedPlayer player, PreUser user) {
        this.player = player;

        if (user == null) {
            Database.getUsers().addUser(player.getUniqueId(), player.getName(), Network.getGuestGroup().getName(), null);
            user = new PreUser(this.player.getName());
        }

        this.dbUser = user.getDbUser();
        this.airMode = user.isAirMode();
        this.prefix = user.getPrefix();
        this.suffix = user.getSuffix();
        this.nick = user.getNick();
        this.group = user.getGroup();
        this.service = user.isService();
        this.nameChat = user.getNameChat();
        this.dataProtectionAgreement = user.getDataProtectionAgreement();

        this.group.addUser(this);
        this.updatePermissions();

        Network.getChannel().addListener(this, () -> Collections.singleton(this.getUniqueId()));
    }

    public DbUser getDbUser() {
        return this.dbUser;
    }

    public boolean isAirMode() {
        return airMode;
    }

    public void setAirMode(boolean airMode) {
        this.airMode = airMode;
        this.dbUser.setAirMode(airMode);
    }

    public void quit() {
        Network.getChannel().removeListener(this);
    }

    public Group getGroup() {
        return group;
    }

    public void updateGroup() {
        if (this.group != null) {
            this.group.removeUser(this);
        }

        DbPermGroup group = this.getDatabase().getPermGroup();
        if (group.exists()) {
            this.group = Network.getGroup(group.getName());
        }

        if (this.group != null) {
            this.group.addUser(this);
        }

        Network.getChannel().sendMessage(new ChannelUserMessage<>(this.getUniqueId(), MessageType.User.GROUP, this.group.getName()));
        this.updatePermissions();
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.dbUser.setPrefix(prefix);
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.dbUser.setSuffix(suffix);
        this.suffix = suffix;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.dbUser.setNick(nick);
        this.nick = nick;
    }

    public boolean isService() {
        return service;
    }

    public void setService(boolean service) {
        if (this.service != service) {
            this.dbUser.setService(service);
        }
        this.service = service;
        this.updatePermissions();
    }

    public ProxiedPlayer getPlayer() {
        return player;
    }

    public String getChatName() {
        return nameChat;
    }

    public Sender getAsSender(de.timesnake.library.basic.util.chat.Plugin plugin) {
        return new Sender(new CommandSender(player), plugin);
    }

    public boolean hasPermission(String permission, Integer code, Plugin plugin) {
        return this.getAsSender(plugin).hasPermission(permission, code);
    }

    public boolean hasPermission(String permission) {
        return this.getPlayer().hasPermission(permission);
    }

    @SuppressWarnings("deprecation")
    public void sendMessage(String message) {
        this.player.sendMessage(message);
    }

    @SuppressWarnings("deprecation")
    public void sendPluginMessage(de.timesnake.library.basic.util.chat.Plugin plugin, String message) {
        this.getPlayer().sendMessage(Chat.getSenderPlugin(plugin) + message);
    }

    public UUID getUniqueId() {
        return this.player.getUniqueId();
    }

    public DbUser getDatabase() {
        return this.dbUser;
    }

    public Server getServer() {
        return this.server;
    }

    public void setServer(String server) {
        this.dbUser.setServer(server);
        this.server = Network.getServer(server);
        Integer port = this.server != null ? this.server.getPort() : null;
        Network.getChannel().setUserServer(this.getUniqueId(), port);
    }

    public Server getServerLast() {
        return serverLast;
    }

    public void setServerLast(String serverLast) {
        this.dbUser.setServerLast(serverLast);
        this.serverLast = Network.getServer(serverLast);
    }

    public Server getLobby() {
        return lobby;
    }

    public void setLobby(String lobby) {
        this.dbUser.setServerLobby(lobby);
        this.lobby = Network.getServer(lobby);
    }

    public String getName() {
        return this.player.getName();
    }

    public void setStatus(Status.User status) {
        this.dbUser.setStatus(status);
    }

    public void resetTeam() {
        this.dbUser.setTeam(null);
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void updatePermissions() {
        Network.runTaskAsync(() -> {
            this.databasePermissions.clear();

            for (DbPermission perm : this.dbUser.getPermissions()) {
                this.databasePermissions.add(new Permission(perm.getName(), perm.getMode(), perm.getServers()));
            }

            if (this.group != null) {
                DbPermGroup group = this.dbUser.getPermGroup();
                while (Database.getGroups().containsGroup(group.getName())) {
                    for (DbPermission perm : group.getPermissions()) {
                        this.databasePermissions.add(new Permission(perm.getName(), perm.getMode(), perm.getServers()));
                    }
                    group = group.getInheritance();
                    if (group == null) {
                        break;
                    }
                }
            }

            Network.runTaskLater(this::loadPermissions, Duration.ZERO);

            Network.getChannel().sendMessage(new ChannelUserMessage<>(this.getUniqueId(), MessageType.User.PERMISSION));
            Network.printText(de.timesnake.basic.proxy.util.chat.Plugin.PERMISSION, "Updated permissions for user " + this.getName() + " from database");
        });
    }

    private void loadPermissions() {
        for (Permission perm : this.databasePermissions) {
            this.addPermission(perm);
        }
    }

    private void addPermission(Permission perm) {
        Status.Permission mode = perm.getMode();
        Status.Server statusServer;
        if (this.getServer() == null) {
            statusServer = Status.Server.ONLINE;
        } else {
            statusServer = this.getServer().getStatus();
        }
        Status.User statusPlayer = this.dbUser.getStatus();
        Collection<String> server = perm.getServer();

        if (perm.getPermission() != null) {
            if (server == null || (this.getServer() != null && server.contains(this.getServer().getName())) || server.isEmpty()) {
                if (mode == Status.Permission.IN_GAME) {
                    this.permissions.add(perm);
                } else if (statusServer == Status.Server.SERVICE) {
                    this.permissions.add(perm);
                } else if (this.isService()) {
                    this.permissions.add(perm);
                } else if (mode == Status.Permission.ONLINE && (statusServer == Status.Server.ONLINE && statusPlayer == Status.User.ONLINE)) {
                    this.permissions.add(perm);
                }
            }
        }
    }

    public String getLastChatMessage() {
        return lastChatMessage;
    }

    public void setLastChatMessage(String lastChatMessage) {
        this.lastChatMessage = lastChatMessage;
    }

    public boolean isListeningNetworkMessages() {
        return isListeningNetworkMessages;
    }

    public void setListeningNetworkMessages(boolean isListeningNetworkMessages) {
        this.isListeningNetworkMessages = isListeningNetworkMessages;
        if (isListeningNetworkMessages) {
            Network.addNetworkMessageListener(this);
        } else {
            Network.removeNetworkMessageListener(this);
        }
    }

    public boolean isListeningPrivateMessages() {
        return isListeningPrivateMessages;
    }

    public void setListeningPrivateMessages(boolean isListeningPrivateMessages) {
        this.isListeningPrivateMessages = isListeningPrivateMessages;
        if (isListeningPrivateMessages) {
            Network.addPrivateMessageListener(this);
        } else {
            Network.removePrivateMessageListener(this);
        }
    }

    public boolean isListeningSupportMessages() {
        return isListeningSupportMessages;
    }

    public void setListeningSupportMessages(boolean isListeningSupportMessages) {
        this.isListeningSupportMessages = isListeningSupportMessages;
        if (isListeningSupportMessages) {
            Network.addSupportMessageListener(this);
        } else {
            Network.removeSupportMessageListener(this);
        }
    }

    public void setTask(String task) {
        this.dbUser.setTask(task);
    }

    //coins

    public void setCoins(float coins) {
        this.coins = coins;
        this.dbUser.setCoins(coins);
        this.sendPluginMessage(Plugin.TIME_COINS, ChatColor.PERSONAL + "Balance changed to " + ChatColor.VALUE + coins);
    }

    public void addCoins(float coins) {
        this.coins += coins;
        this.dbUser.addCoins(coins);
        this.sendPluginMessage(Plugin.TIME_COINS, ChatColor.PERSONAL + "Added " + ChatColor.VALUE + coins + ChatColor.PERSONAL + " timecoin(s)");
    }

    public void removeCoins(float coins) {
        this.coins -= coins;
        this.dbUser.removeCoins(coins);
        this.sendPluginMessage(Plugin.TIME_COINS, ChatColor.PERSONAL + "Removed" + ChatColor.VALUE + coins + ChatColor.PERSONAL + " timecoin(s)");
    }

    public float getCoins() {
        return this.coins;
    }

    public void connect(ServerInfo serverInfo) {
        this.player.connect(serverInfo);
    }

    //cmd

    /**
     * @param cmd Command without slash
     */
    public void executeCommand(String cmd) {
        Network.getChannel().sendMessage(new ChannelUserMessage<>(this.getUniqueId(), MessageType.User.COMMAND, cmd));
    }

    //dataProtection

    /**
     * Agrees data-protection
     *
     * @param agreement The {@link DataProtectionAgreement}
     */
    public void agreeDataProtection(DataProtectionAgreement agreement) {
        this.dataProtectionAgreement = agreement;
        this.dbUser.agreeDataProtection(agreement);
    }

    /**
     * Disagrees data-protection
     * After that, the user will be kicked and deleted
     */
    public void disagreeDataProtection() {
        this.dataProtectionAgreement = null;
        this.dbUser.disagreeDataProtection();
    }

    /**
     * Gets the data-protection-agreement (date, ip, version)
     *
     * @return the {@link DataProtectionAgreement}
     */
    public DataProtectionAgreement getDataProtectionAgreement() {
        return this.dataProtectionAgreement;
    }

    /**
     * Agreed data-protection
     *
     * @return if user has agreed the data-protection
     */
    public boolean agreedDataProtection() {
        if (this.dataProtectionAgreement != null) {
            if (this.dataProtectionAgreement.getVersion() != null) {
                return this.dataProtectionAgreement.getVersion().equalsIgnoreCase(BasicProxy.DATA_PROTECTION_VERSION);
            }
        }
        return false;
    }

    /**
     * Sends the data-protection-declaration message
     */
    public void forceDataProtectionAgreement() {
        this.dpdInfoTask = ProxyServer.getInstance().getScheduler().schedule(BasicProxy.getPlugin(), () -> {
            this.sendPluginMessage(Plugin.NETWORK, ChatColor.WARNING + "Please accept our data protection declaration");
            this.sendPluginMessage(Plugin.NETWORK, ChatColor.WARNING + "Type " + ChatColor.VALUE + "/dpd agree" + ChatColor.PERSONAL + " to accept");
            this.sendPluginMessage(Plugin.NETWORK, ChatColor.WARNING + "Type " + ChatColor.VALUE + "/dpd disagree" + ChatColor.PERSONAL + " to deny");
            if (!this.getPlayer().isConnected()) {
                this.dpdInfoTask.cancel();
            }
        }, 0, 5, TimeUnit.SECONDS);

    }

    @ChannelHandler(type = {ListenerType.USER_SERVICE, ListenerType.USER_PERMISSION, ListenerType.USER_SWITCH_NAME, ListenerType.USER_SWITCH_PORT}, filtered = true)
    public void onUserMessage(ChannelUserMessage<?> msg) {
        MessageType<?> type = msg.getMessageType();
        if (type.equals(MessageType.User.SERVICE)) {
            this.service = this.dbUser.isService();
        } else if (type.equals(MessageType.User.PERMISSION)) {
            this.updatePermissions();
        } else if (type.equals(MessageType.User.SWITCH_NAME)) {
            Network.sendUserToServer(this, (String) msg.getValue());
        } else if (type.equals(MessageType.User.SWITCH_PORT)) {
            Network.sendUserToServer(this, (Integer) msg.getValue());
        }
    }

    public void playSound(ChannelUserMessage.Sound sound) {
        Network.getChannel().sendMessage(new ChannelUserMessage<>(this.getUniqueId(), MessageType.User.SOUND, sound));
    }

}
