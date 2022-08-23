package de.timesnake.basic.proxy.util.user;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.timesnake.basic.proxy.core.group.DisplayGroup;
import de.timesnake.basic.proxy.core.group.PermGroup;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
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
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.basic.util.chat.Plugin;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.extension.util.permission.ExPermission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class User implements de.timesnake.library.extension.util.player.User, ChannelListener {

    private final DbUser dbUser;
    private final Player player;
    private final Collection<ExPermission> databasePermissions;
    private final Collection<ExPermission> permissions = ConcurrentHashMap.newKeySet();
    private final SortedSet<DisplayGroup> displayGroups;
    private boolean service;
    private boolean airMode;
    private DataProtectionAgreement dataProtectionAgreement;
    private String lastChatMessage = "";
    private boolean isListeningNetworkMessages = false;
    private boolean isListeningPrivateMessages = false;
    private boolean isListeningSupportMessages = false;
    private Server server;
    private Server serverLast;
    private Server lobby;
    private PermGroup permGroup;
    private Component prefix;
    private Component suffix;
    private Component nick;
    private float coins;
    private Component chatName;

    private ScheduledTask dpdInfoTask;

    public User(Player player, PreUser user) {
        this.player = player;

        if (user == null) {
            Database.getUsers().addUser(player.getUniqueId(), player.getUsername(), Network.getGuestGroup().getName(),
                    null);
            user = new PreUser(this.player.getUsername());
        }

        this.dbUser = user.getDbUser();
        this.airMode = user.isAirMode();
        this.prefix = user.getPrefix();
        this.suffix = user.getSuffix();
        this.nick = user.getNick();
        this.permGroup = user.getPermGroup();
        this.displayGroups = user.getDisplayGroups();
        this.service = user.isService();
        this.updateChatName();
        this.dataProtectionAgreement = user.getDataProtectionAgreement();

        this.permGroup.addUser(this);

        this.databasePermissions = user.getDatabasePermissions();
        this.loadPermissions();

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

    public PermGroup getGroup() {
        return permGroup;
    }

    public void updateGroup() {
        if (this.permGroup != null) {
            this.permGroup.removeUser(this);
        }

        DbPermGroup group = this.getDatabase().getPermGroup();
        if (group.exists()) {
            this.permGroup = Network.getPermGroup(group.getName());
        }

        if (this.permGroup != null) {
            this.permGroup.addUser(this);
        }

        Network.getChannel().sendMessage(new ChannelUserMessage<>(this.getUniqueId(), MessageType.User.PERM_GROUP,
                this.permGroup.getName()));
        this.updatePermissions(false);
    }

    public void updateChatName() {
        Component component = Component.text("");

        if (this.getNick() == null) {
            for (DisplayGroup group : this.getMainDisplayGroups()) {
                if (group.getPrefix() != null) {
                    component = component.append(Component.text(group.getPrefix()).color(group.getPrefixColor()));
                }
            }

            if (this.getPrefix() != null) {
                component = component.append(this.getPrefix());
            }

            component = component.append(Component.text(this.getPlayer().getUsername(), ExTextColor.WHITE));

            if (this.getSuffix() != null) {
                component = component.append(this.getSuffix());
            }
        } else {
            DisplayGroup group = Network.getMemberDisplayGroup();
            if (group.getPrefix() != null) {
                component = component.append(Component.text(group.getPrefix(), group.getPrefixColor()));
            }
            component.append(this.getNick());

        }
        this.chatName = component;

    }

    public Component getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.dbUser.setPrefix(prefix);
        this.prefix = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix);
    }

    public Component getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.dbUser.setSuffix(suffix);
        this.suffix = LegacyComponentSerializer.legacyAmpersand().deserialize(suffix);
    }

    public Component getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.dbUser.setNick(nick);
        this.nick = LegacyComponentSerializer.legacyAmpersand().deserialize(nick);
    }

    public boolean isService() {
        return service;
    }

    public void setService(boolean service) {
        if (this.service != service) {
            this.dbUser.setService(service);
        }
        this.service = service;
        this.updatePermissions(true);
    }

    public Player getPlayer() {
        return player;
    }

    public Component getChatNameComponent() {
        return chatName;
    }

    @Override
    @Deprecated
    public String getChatName() {
        return chatName.toString();
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

    @Deprecated
    public void sendMessage(String message) {
        this.player.sendMessage(Component.text(message));
    }

    public void sendMessage(Component message) {
        this.player.sendMessage(message);
    }

    @Deprecated
    public void sendPluginMessage(de.timesnake.library.basic.util.chat.Plugin plugin, String message) {
        this.getPlayer().sendMessage(Chat.getSenderPlugin(plugin).append(Chat.parseStringToComponent(message)));
    }

    public void sendPluginMessage(de.timesnake.library.basic.util.chat.Plugin plugin, Component message) {
        this.getPlayer().sendMessage(Chat.getSenderPlugin(plugin).append(message));
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
        return this.player.getUsername();
    }

    public void setStatus(Status.User status) {
        this.dbUser.setStatus(status);
    }

    public void resetTeam() {
        this.dbUser.setTeam(null);
    }

    public Collection<ExPermission> getPermissions() {
        return permissions;
    }

    public void updatePermissions(boolean broadcastUpdate) {
        Network.runTaskAsync(() -> {
            if (broadcastUpdate) {
                Network.getChannel().sendMessage(new ChannelUserMessage<>(this.getUniqueId(), MessageType.User.PERMISSION));
            }

            this.databasePermissions.clear();

            for (DbPermission perm : this.dbUser.getPermissions()) {
                this.databasePermissions.add(new ExPermission(perm.getName(), perm.getMode(), perm.getServers()));
            }

            this.databasePermissions.addAll(this.permGroup.getPermissions());

            Network.runTaskLater(this::loadPermissions, Duration.ZERO);

            Network.printText(de.timesnake.basic.proxy.util.chat.Plugin.PERMISSION,
                    "Updated permissions for user " + this.getName() + " from database");
        });
    }

    private void loadPermissions() {
        for (ExPermission perm : this.databasePermissions) {
            this.addPermission(perm);
        }
    }

    private void addPermission(ExPermission perm) {
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

    public DisplayGroup getMasterDisplayGroup() {
        return displayGroups.first();
    }

    public List<DisplayGroup> getMainDisplayGroups() {
        return this.displayGroups.stream().filter(displayGroup -> displayGroup.isShowAlways()
                || displayGroup.equals(this.getMasterDisplayGroup())).sorted().limit(DisplayGroup.MAX_PREFIX_LENGTH).toList();
    }

    public SortedSet<DisplayGroup> getDisplayGroups() {
        return displayGroups;
    }

    public void updateDisplayGroup() {
        this.displayGroups.clear();
        for (String groupName : this.dbUser.getDisplayGroupNames()) {
            this.displayGroups.add(Network.getDisplayGroup(groupName));
        }

        if (this.displayGroups.isEmpty()) {
            this.displayGroups.add(Network.getGuestDisplayGroup());
        }

        Network.getChannel().sendMessage(new ChannelUserMessage<>(this.getUniqueId(), MessageType.User.DISPLAY_GROUP));
        this.updateChatName();
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

    public void addCoins(float coins) {
        this.coins += coins;
        this.dbUser.addCoins(coins);
        this.sendPluginMessage(Plugin.TIME_COINS, Component.text("Added ", ExTextColor.PERSONAL)
                .append(Component.text(Chat.roundCoinAmount(coins), ExTextColor.VALUE))
                .append(Component.text(" timecoin(s)", ExTextColor.PERSONAL)));
    }

    public void removeCoins(float coins) {
        this.coins -= coins;
        this.dbUser.removeCoins(coins);
        this.sendPluginMessage(Plugin.TIME_COINS, Component.text("Removed ", ExTextColor.PERSONAL)
                .append(Component.text(Chat.roundCoinAmount(coins), ExTextColor.VALUE))
                .append(Component.text(" timecoin(s)", ExTextColor.PERSONAL)));
    }

    public float getCoins() {
        return this.coins;
    }

    public void setCoins(float coins) {
        this.coins = coins;
        this.dbUser.setCoins(coins);
        this.sendPluginMessage(Plugin.TIME_COINS, Component.text("Balance changed to ", ExTextColor.PERSONAL)
                .append(Component.text(Chat.roundCoinAmount(coins), ExTextColor.VALUE)));
    }

    public void connect(RegisteredServer server) {
        CompletableFuture<ConnectionRequestBuilder.Result> resultFuture =
                this.player.createConnectionRequest(server).connect();
        Network.runTaskAsync(() -> {
            resultFuture.join();
            try {
                ConnectionRequestBuilder.Result result = resultFuture.get();
                if (!result.isSuccessful()) {
                    this.connect(server, 1);
                }
            } catch (InterruptedException | ExecutionException e) {
                this.connect(server, 1);
            }
        });
    }

    public void connect(RegisteredServer server, int retries) {
        if (retries > 3) {
            return;
        }
        CompletableFuture<ConnectionRequestBuilder.Result> resultFuture =
                this.player.createConnectionRequest(server).connect();
        Network.runTaskAsync(() -> {
            resultFuture.join();
            try {
                ConnectionRequestBuilder.Result result = resultFuture.get();
                if (!result.isSuccessful()) {
                    this.connect(server, retries + 1);
                }
            } catch (InterruptedException | ExecutionException e) {
                this.connect(server, retries + 1);
            }
        });
    }

    public void scheduledConnect(Server server) {
        server.addWaitingUser(this);
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
        this.dpdInfoTask = BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), () -> {
            this.sendPluginMessage(Plugin.NETWORK, Component.text("Please accept our data protection declaration", ExTextColor.WARNING));
            this.sendPluginMessage(Plugin.NETWORK, Component.text("Type ", ExTextColor.WARNING)
                    .append(Component.text("/dpd agree", ExTextColor.VALUE))
                    .append(Component.text(" to accept", ExTextColor.WARNING)));
            this.sendPluginMessage(Plugin.NETWORK, Component.text("Type ", ExTextColor.WARNING)
                    .append(Component.text("/dpd disagree", ExTextColor.VALUE))
                    .append(Component.text(" to deny", ExTextColor.WARNING)));
            if (!this.getPlayer().isActive()) {
                this.dpdInfoTask.cancel();
            }
        }).repeat(Duration.ofSeconds(5)).schedule();

    }

    @ChannelHandler(type = {ListenerType.USER_SERVICE, ListenerType.USER_PERMISSION, ListenerType.USER_SWITCH_NAME,
            ListenerType.USER_SWITCH_PORT}, filtered = true)
    public void onUserMessage(ChannelUserMessage<?> msg) {
        MessageType<?> type = msg.getMessageType();
        if (type.equals(MessageType.User.SERVICE)) {
            this.service = this.dbUser.isService();
        } else if (type.equals(MessageType.User.PERMISSION)) {
            this.updatePermissions(false);
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
