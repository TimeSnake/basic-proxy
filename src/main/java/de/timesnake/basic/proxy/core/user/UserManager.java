/*
 * Copyright (C) 2023 timesnake
 */

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
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.server.Server;
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
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.extension.util.player.UserMap;
import de.timesnake.library.extension.util.player.UserSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public class UserManager {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final ConcurrentHashMap<String, Future<PreUser>> preUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Future<Component>> prePunishment = new ConcurrentHashMap<>();

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

        Future<Component> prePunishmentFuture = this.prePunishment.get(p.getUsername());

        Component punishment;
        try {
            punishment = prePunishmentFuture.get();
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
            p.disconnect(Component.text("An error occurred, please contact an admin"));
            return;
        }

        if (punishment != null) {
            p.disconnect(punishment);
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
            userOnline.sendPluginMessage(Plugin.NETWORK,
                    Component.text(p.getUsername(), ExTextColor.VALUE)
                            .append(Component.text(" joined", ExTextColor.PUBLIC)));
        }

        Network.printText(Plugin.NETWORK, "Players online " + Network.getUsers().size());
        Network.getChannel().sendMessage(
                new ChannelServerMessage<>(Network.getName(), MessageType.Server.ONLINE_PLAYERS,
                        Network.getUsers().size()));
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
                        e.setResult(ResultedEvent.ComponentResult.denied(
                                Component.text("§cService-Work    " + "Wartungsarbeiten")));
                        return;
                    }

                    DbPermGroup group = user.getPermGroup();

                    while (Database.getGroups().containsPermGroup(group.getName())
                            && !group.hasPermission("network.work" + ".join")) {
                        group = group.getInheritance();
                        if (group == null) {
                            e.setResult(ResultedEvent.ComponentResult.denied(
                                    Component.text("§cService-Work    " + "Wartungsarbeiten")));
                            return;
                        }
                    }
                }
            });

        } else {
            if (Network.isWork()) {
                e.setResult(ResultedEvent.ComponentResult.denied(
                        Component.text("§cService-Work    Wartungsarbeiten")));
                return;
            }
        }

        if (Network.getOnlineLobbys() == 0) {
            e.setResult(ResultedEvent.ComponentResult.denied(
                    Component.text("§6Server is starting, please wait.")));
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
                userOnline.sendPluginMessage(Plugin.NETWORK,
                        Component.text(p.getUsername(), ExTextColor.VALUE)
                                .append(Component.text(" left", ExTextColor.PUBLIC)));
            }
        });

        UserSet.LISTS.forEach(l -> l.remove(Network.getUser(p)));
        UserMap.MAPS.forEach(l -> l.remove(Network.getUser(p)));

        Network.removeUser(p);
        Network.getChannel().sendMessage(
                new ChannelServerMessage<>(Network.getName(), MessageType.Server.ONLINE_PLAYERS,
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

        Server s = Network.getServer(
                serverInfo.getAddress().getPort());

        if (s.getType().equals(Type.Server.LOBBY)) {
            user.setLobby(serverInfo.getName());
        }

        for (User userOnline : Network.getNetworkMessageListeners()) {
            userOnline.sendPluginMessage(Plugin.NETWORK, user.getChatNameComponent()
                    .append(Component.text(" connected to ", ExTextColor.PUBLIC))
                    .append(Component.text(serverInfo.getName(), ExTextColor.VALUE)));
        }

        Network.runTaskLater(() -> user.runJoinCommands(s), Duration.ofSeconds(3));
    }

    private void sendJoinMessages(Player player, User user) {
        user.sendPluginMessage(Plugin.NETWORK,
                Component.text("You accepted the network rules!", ExTextColor.WARNING));

        if (user.agreedPrivacyPolicy()) {
            user.sendPluginMessage(Plugin.NETWORK,
                    Component.text("You accepted our data protection declaration (dpd)",
                            ExTextColor.WARNING));
            user.sendPluginMessage(Plugin.NETWORK, Component.text("Type ", ExTextColor.WARNING)
                    .append(Component.text("/dpd disagree", ExTextColor.VALUE))
                    .append(Component.text(" to deny our dpd", ExTextColor.WARNING)));
        } else {
            user.forceToAcceptPrivacyPolicy();
        }

        if (player.hasPermission("support.opentickets")) {
            user.sendPluginMessage(Plugin.SUPPORT,
                    Component.text(Database.getSupport().getTickets().size(), ExTextColor.VALUE)
                            .append(Component.text(" open tickets", ExTextColor.PERSONAL)));
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
                player.sendMessage(Chat.getSenderPlugin(Plugin.SUPPORT)
                        .append(Component.text(open, ExTextColor.VALUE))
                        .append(Component.text(" of your ticket(s) is/are open.",
                                ExTextColor.PERSONAL)));
            }

            if (inProcess > 0) {
                player.sendMessage(Chat.getSenderPlugin(Plugin.SUPPORT)
                        .append(Component.text(inProcess, ExTextColor.VALUE))
                        .append(Component.text(" of your ticket(s) is/are in process.",
                                ExTextColor.PERSONAL)));
            }

            if (solved > 0) {
                player.sendMessage(Chat.getSenderPlugin(Plugin.SUPPORT)
                        .append(Component.text(solved, ExTextColor.VALUE))
                        .append(Component.text(" of your ticket(s) is/are solved.",
                                ExTextColor.PERSONAL)));
            }

            if (admin > 0) {
                player.sendMessage(Chat.getSenderPlugin(Plugin.SUPPORT)
                        .append(Component.text(solved, ExTextColor.VALUE))
                        .append(Component.text(
                                " of your ticket(s) is/are forwarded to " + "an admin.",
                                ExTextColor.PERSONAL)));
            }

            if (open + inProcess + solved + admin > 0) {
                player.sendMessage(Chat.getSenderPlugin(Plugin.SUPPORT)
                        .append(Component.text("User ", ExTextColor.PERSONAL))
                        .append(Component.text("/ticket(s) ", ExTextColor.VALUE))
                        .append(Component.text("to manage your tickets", ExTextColor.PERSONAL)));
            }
        }
    }

    public void checkDatabase(Player p, DbUser dbUser) {
        if (!Database.getUsers().containsUser(p.getUniqueId())) {
            Database.getUsers()
                    .addUser(p.getUniqueId(), p.getUsername(),
                            Network.getGroupManager().getGuestPermGroup().getName(),
                            null);
            this.sendAcceptedRules(p);
        } else {
            dbUser.setName(p.getUsername());
            dbUser.setStatus(Status.User.ONLINE);
            if (!dbUser.hasPermGroup()) {
                dbUser.setPermGroup(Network.getGroupManager().getGuestPermGroup().getName());
            }
        }
    }

    public void sendAcceptedRules(Player p) {
        Title title = Title.title(
                Component.text("You accepted the Network-Rules", ExTextColor.WARNING),
                Component.text("For more infos use ", ExTextColor.PERSONAL)
                        .append(Component.text("/rules", ExTextColor.VALUE)),
                Title.Times.of(Duration.ZERO, Duration.ofSeconds(10), Duration.ofMillis(500)));
        p.showTitle(title);
    }

    public Component checkIsPlayerPunished(String name) {

        DbUser user = Database.getUsers().getUser(name);

        DbPunishment punishment = user.getPunishment();
        Type.Punishment type = punishment.getType();

        if (punishment == null || type == null) {
            return null;
        }

        if (type.equals(Type.Punishment.BAN)) {
            return Component.text("You were permanently banned.", ExTextColor.WARNING)
                    .append(Component.newline())
                    .append(Component.text("Reason: ", ExTextColor.WARNING))
                    .append(Component.text(user.getPunishment().getReason(), ExTextColor.VALUE))
                    .append(Component.newline())
                    .append(Component.text("For more info use our discord: ", ExTextColor.PERSONAL))
                    .append(Component.text(Network.DISCORD_LINK, ExTextColor.VALUE))
                    .append(Component.newline())
                    .append(Component.text("or contact us by email: ", ExTextColor.PERSONAL))
                    .append(Component.text(Network.SUPPORT_EMAIL, ExTextColor.VALUE));

        } else if (type.equals(Type.Punishment.TEMP_BAN)) {
            LocalDateTime dateSystem = LocalDateTime.now();
            LocalDateTime date = user.getPunishment().getDate();
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String dateString = df.format(date);
            if (date.isBefore(dateSystem)) {
                Network.getPunishmentManager().unbanPlayer(user.getUniqueId());
                return null;
            } else {
                return Component.text("You are banned ", ExTextColor.WARNING)
                        .append(Component.text("until ", ExTextColor.WARNING))
                        .append(Component.text(dateString, ExTextColor.VALUE))
                        .append(Component.text(".", ExTextColor.WARNING))
                        .append(Component.newline())
                        .append(Component.text("Reason: ", ExTextColor.WARNING))
                        .append(Component.text(user.getPunishment().getReason(), ExTextColor.VALUE))
                        .append(Component.newline())
                        .append(Component.text("For more info use our discord: ",
                                ExTextColor.PERSONAL))
                        .append(Component.text(Network.DISCORD_LINK, ExTextColor.VALUE))
                        .append(Component.newline())
                        .append(Component.text("or contact us by email: ", ExTextColor.PERSONAL))
                        .append(Component.text(Network.SUPPORT_EMAIL, ExTextColor.VALUE));
            }
        }

        return null;
    }
}
