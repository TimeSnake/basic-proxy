package de.timesnake.basic.proxy.core.user;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.timesnake.basic.proxy.core.punishment.Punishments;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.user.PreUser;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.group.DbPermGroup;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.support.DbTicket;
import de.timesnake.database.util.user.DbPunishment;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.chat.ChatColor;
import de.timesnake.library.extension.util.chat.Chat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.*;

public class UserManager {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final ConcurrentHashMap<String, Future<PreUser>> preUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Future<String>> prePunishment = new ConcurrentHashMap<>();

    public UserManager() {
        Network.registerListener(this);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPreLogin(PreLoginEvent e) {
        String name = e.getUsername();

        this.preUsers.put(name, executor.submit(() -> {
            try {
                return new PreUser(name);
            } catch (UserNotInDatabaseException ex) {
                return null;
            }
        }));

        this.prePunishment.put(name, executor.submit(() -> checkIsPlayerPunished(name)));
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPostLogin(PostLoginEvent e) {
        Player p = e.getPlayer();

        Future<String> prePunishmentFuture = this.prePunishment.get(p.getUsername());

        String punishment;
        try {
            punishment = prePunishmentFuture.get();
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
            return;
        }

        if (punishment != null) {
            p.disconnect(Component.text(punishment));
        }


        DbUser dbUser = Database.getUsers().getUser(p.getUniqueId());

        Network.runTaskAsync(() -> this.checkDatabase(p, dbUser));

        Future<PreUser> preUserFuture = this.preUsers.get(p.getUsername());

        User user;
        try {
            user = Network.addUser(p, preUserFuture.get());
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
            return;
        }

        Network.runTaskAsync(() -> this.sendJoinMessages(p, user));

        for (User userOnline : Network.getNetworkMessageListeners()) {
            userOnline.sendPluginMessage(Plugin.NETWORK, ChatColor.VALUE + p.getUsername() + ChatColor.PUBLIC + " " +
                    "joined");
        }

        Network.printText(Plugin.NETWORK, "Players online " + Network.getUsers().size());
        Network.getChannel().sendMessage(new ChannelServerMessage<>(Network.getPort(),
                MessageType.Server.ONLINE_PLAYERS, Network.getUsers().size()));
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onLogin(LoginEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        String name = e.getPlayer().getUsername();
        DbUser user = Database.getUsers().getUser(uuid);

        Network.printText(Plugin.NETWORK, "Player: " + name + " (" + uuid.toString() + ") joined");

        if (user.exists()) {

            // check if user has service work permission
            Network.runTaskAsync(() -> {
                if (Network.isWork() && !user.hasPermission("network.work.join")) {
                    if (!Database.getUsers().getUser(user.getUniqueId()).hasPermGroup()) {
                        e.setResult(ResultedEvent.ComponentResult.denied(Component.text("§cService-Work    " +
                                "Wartungsarbeiten")));
                        return;
                    }

                    DbPermGroup group = user.getPermGroup();

                    while (Database.getGroups().containsPermGroup(group.getName()) && !group.hasPermission("network.work" +
                            ".join")) {
                        group = group.getInheritance();
                        if (group == null) {
                            e.setResult(ResultedEvent.ComponentResult.denied(Component.text("§cService-Work    " +
                                    "Wartungsarbeiten")));
                            return;
                        }
                    }
                }
            });

        } else {
            if (Network.isWork()) {
                e.setResult(ResultedEvent.ComponentResult.denied(Component.text("§cService-Work    Wartungsarbeiten")));
                return;
            }
        }

        if (Network.getOnlineLobbys() == 0) {
            e.setResult(ResultedEvent.ComponentResult.denied(Component.text("§6Server is starting, please wait.")));
            return;
        }
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent e) {
        Player p = e.getPlayer();
        DbUser user = Database.getUsers().getUser(p.getUniqueId());
        if (user != null) {
            user.setStatus(Status.User.OFFLINE, false);
            user.setTask(null, false);
            user.setTeam(null);
            user.setKit(null);
            user.setServer(null);
            user.setServerLast(null);
        }

        Network.runTaskAsync(() -> {
            for (User userOnline : Network.getNetworkMessageListeners()) {
                userOnline.sendPluginMessage(Plugin.NETWORK, ChatColor.VALUE + p.getUsername() +
                        ChatColor.PUBLIC + " left");
            }
        });

        Network.removeUser(p);
        Network.getChannel().sendMessage(new ChannelServerMessage<>(Network.getPort(),
                MessageType.Server.ONLINE_PLAYERS,
                Network.getUsers().size()));
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent e) {
        Player p = e.getPlayer();
        RegisteredServer server = e.getServer();
        User user = Network.getUser(p);

        if (user == null) {
            return;
        }

        RegisteredServer previous = e.getServer();

        if (e.getPreviousServer().isPresent()) {
            previous = e.getPreviousServer().get();
        }

        user.setServerLast(previous.getServerInfo().getName());

        ServerInfo serverInfo = server.getServerInfo();
        user.setServer(serverInfo.getName());
        if (Database.getServers().getServer(serverInfo.getAddress().getPort()).getType().equals(Type.Server.LOBBY)) {
            user.setLobby(serverInfo.getName());
        }

        for (User userOnline : Network.getNetworkMessageListeners()) {
            userOnline.sendPluginMessage(Plugin.NETWORK, ChatColor.VALUE + user.getChatNameComponent() +
                    ChatColor.PUBLIC + " connected to " + ChatColor.VALUE + serverInfo.getName());
        }

    }

    private void sendJoinMessages(Player player, User user) {
        player.sendMessage(Component.text(Chat.getSenderPlugin(Plugin.NETWORK) + ChatColor.WARNING +
                "You accepted the network rules!"));

        if (user.agreedDataProtection()) {
            user.sendPluginMessage(de.timesnake.library.basic.util.chat.Plugin.NETWORK, ChatColor.WARNING +
                    "You accepted our data protection declaration (dpd)");
            user.sendPluginMessage(de.timesnake.library.basic.util.chat.Plugin.NETWORK, ChatColor.WARNING +
                    "Type " + ChatColor.VALUE + "/dpd disagree" + ChatColor.WARNING + " to deny our dpd");
        } else {
            user.forceDataProtectionAgreement();
        }

        if (player.hasPermission("support.opentickets")) {
            player.sendMessage(Component.text(Chat.getSenderPlugin(Plugin.SUPPORT) + ChatColor.VALUE + "§l" +
                    Database.getSupport().getTickets().size() + ChatColor.PUBLIC + " open tickets"));
        }


        Collection<DbTicket> tickets = Database.getSupport().getTickets(player.getUniqueId());
        if (!tickets.isEmpty()) {
            int open = 0;
            int inProcess = 0;
            int solved = 0;
            int admin = 0;

            for (DbTicket ticket : tickets) {
                Status.Ticket status = ticket.getStatus();
                if (status.equals(Status.Ticket.OPEN)) {
                    ++open;
                } else if (status.equals(Status.Ticket.IN_PROCESS)) {
                    ++inProcess;
                } else if (status.equals(Status.Ticket.SOLVED)) {
                    ++solved;
                } else if (status.equals(Status.Ticket.ADMIN)) {
                    ++admin;
                }
            }

            if (open > 0) {
                player.sendMessage(Component.text(Chat.getSenderPlugin(Plugin.SUPPORT) + ChatColor.VALUE + open +
                        ChatColor.PERSONAL + " of your ticket(s) is/are open."));
            }

            if (inProcess > 0) {
                player.sendMessage(Component.text(Chat.getSenderPlugin(Plugin.SUPPORT) + ChatColor.VALUE + inProcess +
                        ChatColor.PERSONAL + " of your ticket(s) is/are in process."));
            }

            if (solved > 0) {
                player.sendMessage(Component.text(Chat.getSenderPlugin(Plugin.SUPPORT) + ChatColor.VALUE + solved +
                        ChatColor.PERSONAL + " of your ticket(s) is/are solved."));
            }

            if (admin > 0) {
                player.sendMessage(Component.text(Chat.getSenderPlugin(Plugin.SUPPORT) + ChatColor.VALUE + solved +
                        ChatColor.PERSONAL + " of your ticket(s) is/are forwarded to " + "an admin."));
            }

            if (open + inProcess + solved + admin > 0) {
                player.sendMessage(Component.text(Chat.getSenderPlugin(Plugin.SUPPORT) + ChatColor.PERSONAL + "Use" +
                        " " +
                        ChatColor.VALUE + "/ticket(s) " + ChatColor.PUBLIC + "to manage your tickets"));
            }
        }
    }

    public void checkDatabase(Player p, DbUser dbUser) {
        if (!Database.getUsers().containsUser(p.getUniqueId())) {
            Database.getUsers().addUser(p.getUniqueId(), p.getUsername(), Network.getGuestGroup().getName(), null);
            this.sendAcceptedRules(p);
        } else {
            dbUser.setName(p.getUsername());
            dbUser.setStatus(Status.User.ONLINE);
            if (!dbUser.hasPermGroup()) {
                dbUser.setPermGroup(Network.getGuestGroup().getName());
            }
        }
    }

    public void sendAcceptedRules(Player p) {
        Title title = Title.title(Component.text(ChatColor.WARNING + "You accepted the Network-Rules"),
                Component.text(ChatColor.PERSONAL + "For more infos use " + ChatColor.VALUE + "/rules"),
                Title.Times.of(Duration.ZERO, Duration.ofSeconds(10), Duration.ofMillis(500)));
        p.showTitle(title);
    }

    public String checkIsPlayerPunished(String name) {

        DbUser user = Database.getUsers().getUser(name);

        DbPunishment punishment = user.getPunishment();
        Type.Punishment type = punishment.getType();

        if (punishment == null || type == null) {
            return null;
        }

        if (type.equals(Type.Punishment.BAN)) {
            return ChatColor.WARNING + "You were permanently banned." + "\n" +
                    ChatColor.WARNING + "reason: " + ChatColor.VALUE + user.getPunishment().getReason() + "\n" +
                    ChatColor.PUBLIC + "For more info use our discord: " + ChatColor.VALUE +
                    de.timesnake.library.basic.util.server.Server.DISCORD_LINK + "\nor contact us by email: " +
                    de.timesnake.library.basic.util.server.Server.SUPPORT_EMAIL;

        } else if (type.equals(Type.Punishment.TEMP_BAN)) {
            Date dateSystem = new Date();
            Date date = user.getPunishment().getDate();
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String dateString = df.format(date);
            if (date.before(dateSystem)) {
                Punishments.unbanPlayer(user.getUniqueId());
                return null;
            } else {
                return ChatColor.WARNING + "You are banned " + ChatColor.WARNING + "\n" +
                        "until " + ChatColor.VALUE + dateString + ChatColor.PUBLIC + "." + ChatColor.WARNING + "\n" +
                        "Reason: " + ChatColor.VALUE + user.getPunishment().getReason() + ChatColor.PUBLIC + "\n" +
                        "For more info use our discord: " + de.timesnake.library.basic.util.server.Server.DISCORD_LINK + "\n" +
                        "or contact us by email: " + de.timesnake.library.basic.util.server.Server.SUPPORT_EMAIL;
            }
        }

        return null;
    }
}
