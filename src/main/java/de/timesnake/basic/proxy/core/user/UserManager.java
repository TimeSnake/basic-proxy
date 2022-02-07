package de.timesnake.basic.proxy.core.user;

import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.core.punishment.Punishments;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.user.PreUser;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.api.message.ChannelServerMessage;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.group.DbPermGroup;
import de.timesnake.database.util.object.Status;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.support.DbTicket;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.extension.util.chat.Chat;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class UserManager implements Listener {

    private final ConcurrentHashMap<String, Future<PreUser>> preUsers = new ConcurrentHashMap<>();

    public UserManager() {
        Network.registerListener(this);
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent e) {
        String name = e.getConnection().getName();

        this.preUsers.put(name, BasicProxy.getPlugin().getExecutorService().submit(() -> {
            try {
                return new PreUser(name);
            } catch (UserNotInDatabaseException ex) {
                return null;
            }
        }));
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent e) {
        ProxiedPlayer p = e.getPlayer();
        DbUser dbUser = Database.getUsers().getUser(p.getUniqueId());

        Network.runTaskAsync(() -> this.checkDatabase(p, dbUser));

        Future<PreUser> preUserFuture = this.preUsers.get(p.getName());

        User user;
        try {
            user = Network.addUser(p, preUserFuture.get());
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
            return;
        }

        Network.runTaskAsync(() -> this.sendJoinMessages(p, user));

        for (User userOnline : Network.getNetworkMessageListeners()) {
            userOnline.sendPluginMessage(Plugin.NETWORK, ChatColor.VALUE + p.getName() + ChatColor.PUBLIC + " joined the network");
        }

        Network.printText(Plugin.NETWORK, "Players online " + Network.getUsers().size());
        Network.getChannel().sendMessage(ChannelServerMessage.getOnlinePlayersMessage(Network.getPort(), Network.getUsers().size()));
    }

    @EventHandler
    public void onLogin(LoginEvent e) {
        UUID uuid = e.getConnection().getUniqueId();
        String name = e.getConnection().getName();
        DbUser user = Database.getUsers().getUser(uuid);

        Network.printText(Plugin.NETWORK, "Player: " + name + " (" + uuid.toString() + ") joined");

        if (user.exists()) {

            // check if user has service work permission
            Network.runTaskAsync(() -> {
                if (Network.isWork() && !user.hasPermission("network.work.join")) {
                    if (!Database.getUsers().getUser(user.getUniqueId()).hasPermGroup()) {
                        e.setCancelReason(new TextComponent("§cService-Work    Wartungsarbeiten"));
                        e.setCancelled(true);
                        return;
                    }

                    DbPermGroup group = user.getPermGroup();

                    while (Database.getGroups().containsGroup(group.getName()) && !group.hasPermission("network.work.join")) {
                        group = group.getInheritance();
                        if (group == null) {
                            e.setCancelReason(new TextComponent("§cService-Work    Wartungsarbeiten"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            });

            Type.Punishment type = user.getPunishment().getType();
            if (type != null) {
                checkIsPlayerPunished(uuid, user, type, e);
            }
        } else {
            if (Network.isWork()) {
                e.setCancelReason(new TextComponent("§cService-Work    Wartungsarbeiten"));
                e.setCancelled(true);
                return;
            }
        }

        if (Network.getOnlineLobbys() == 0) {
            e.setCancelReason(new TextComponent("§6Server is starting, please wait."));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        ProxiedPlayer p = e.getPlayer();
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
                userOnline.sendPluginMessage(Plugin.NETWORK, ChatColor.VALUE + p.getName() + ChatColor.PUBLIC + " left the network");
            }
        });

        Network.removeUser(p);
        Network.getChannel().sendMessage(ChannelServerMessage.getOnlinePlayersMessage(Network.getPort(), Network.getUsers().size()));
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent e) {
        ProxiedPlayer p = e.getPlayer();
        Server server = e.getServer();
        User user = Network.getUser(p);

        ServerInfo serverInfo = server.getInfo();
        user.setServer(serverInfo.getName());
        if (Database.getServers().getServer(serverInfo.getAddress().getPort()).getType().equals(Type.Server.LOBBY)) {
            user.setLobby(serverInfo.getName());
        }

        for (User userOnline : Network.getNetworkMessageListeners()) {
            userOnline.sendPluginMessage(Plugin.NETWORK, ChatColor.VALUE + user.getChatName() + ChatColor.PUBLIC + " connected to " + ChatColor.VALUE + e.getServer().getInfo().getName());
        }

    }

    @EventHandler
    public void onServerDisconnect(ServerDisconnectEvent e) {
        ProxiedPlayer p = e.getPlayer();
        ServerInfo serverInfo = e.getTarget();
        if (serverInfo != null && serverInfo.getName() != null && Network.getUser(p.getUniqueId()) != null) {
            Network.getUser(p).setServerLast(serverInfo.getName());
        }

    }

    private void sendJoinMessages(ProxiedPlayer player, User user) {
        player.sendMessage(new TextComponent(new TextComponent(Chat.getSenderPlugin(Plugin.NETWORK) + ChatColor.WARNING + "You accepted the network rules!")));

        if (user.agreedDataProtection()) {
            user.sendPluginMessage(de.timesnake.library.basic.util.chat.Plugin.NETWORK, ChatColor.WARNING + "You accepted our data protection declaration (dpd)");
            user.sendPluginMessage(de.timesnake.library.basic.util.chat.Plugin.NETWORK, ChatColor.WARNING + "Type " + ChatColor.VALUE + "/dpd disagree" + ChatColor.WARNING + " to deny our dpd");
        } else {
            user.forceDataProtectionAgreement();
        }

        if (player.hasPermission("support.opentickets")) {
            player.sendMessage(new TextComponent(Chat.getSenderPlugin(Plugin.SUPPORT) + ChatColor.VALUE + "§l" + Database.getSupport().getTickets().size() + ChatColor.PUBLIC + " open tickets"));
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
                player.sendMessage(new TextComponent(Chat.getSenderPlugin(Plugin.SUPPORT) + ChatColor.VALUE + open + ChatColor.PERSONAL + " of your ticket(s) is/are open."));
            }

            if (inProcess > 0) {
                player.sendMessage(new TextComponent(Chat.getSenderPlugin(Plugin.SUPPORT) + ChatColor.VALUE + inProcess + ChatColor.PERSONAL + " of your ticket(s) is/are in process."));
            }

            if (solved > 0) {
                player.sendMessage(new TextComponent(Chat.getSenderPlugin(Plugin.SUPPORT) + ChatColor.VALUE + solved + ChatColor.PERSONAL + " of your ticket(s) is/are solved."));
            }

            if (admin > 0) {
                player.sendMessage(new TextComponent(Chat.getSenderPlugin(Plugin.SUPPORT) + ChatColor.VALUE + solved + ChatColor.PERSONAL + " of your ticket(s) is/are forwarded to " + "an admin."));
            }

            if (open + inProcess + solved + admin > 0) {
                player.sendMessage(new TextComponent(Chat.getSenderPlugin(Plugin.SUPPORT) + ChatColor.PERSONAL + "Use " + ChatColor.VALUE + "/ticket(s) " + ChatColor.PUBLIC + "to manage your tickets"));
            }
        }
    }

    public void checkDatabase(ProxiedPlayer p, DbUser dbUser) {
        if (!Database.getUsers().containsUser(p.getUniqueId())) {
            Database.getUsers().addUser(p.getUniqueId(), p.getName(), Network.getGuestGroup().getName(), null);
            this.sendAcceptedRules(p);
        } else {
            dbUser.setName(p.getName());
            dbUser.setStatus(Status.User.ONLINE);
            if (!dbUser.hasPermGroup()) {
                dbUser.setPermGroup(Network.getGuestGroup().getName());
            }
        }
    }

    public void sendAcceptedRules(ProxiedPlayer p) {
        Title title = ProxyServer.getInstance().createTitle();
        title.title(new TextComponent(ChatColor.WARNING + "You accepted the Network-Rules"));
        title.subTitle(new TextComponent(ChatColor.PERSONAL + "For more infos use " + ChatColor.VALUE + "/rules"));
        title.stay(200);
        title.fadeOut(15);
        title.send(p);
    }

    public void checkIsPlayerPunished(UUID uuid, DbUser user, Type.Punishment type, LoginEvent e) {
        if (type.equals(Type.Punishment.BAN)) {
            e.setCancelReason(new TextComponent(ChatColor.WARNING + "You were permanently banned." + "\n" + ChatColor.WARNING + "reason: " + ChatColor.VALUE + user.getPunishment().getReason() + "\n" + ChatColor.PUBLIC + "For more info use our discord: " + ChatColor.VALUE + de.timesnake.library.basic.util.server.Server.DISCORD_LINK + "\nor contact us by email: " + de.timesnake.library.basic.util.server.Server.SUPPORT_EMAIL));
            e.setCancelled(true);
        } else if (type.equals(Type.Punishment.TEMP_BAN)) {
            Date dateSystem = new Date();
            Date date = user.getPunishment().getDate();
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String dateString = df.format(date);
            if (date.before(dateSystem)) {
                Punishments.unbanPlayer(uuid);
                e.setCancelReason(new TextComponent(new TextComponent(ChatColor.WARNING + "You are unbanned, please " + "reconnect " + "in a few moments")));
                e.setCancelled(false);
                Network.printText(de.timesnake.library.basic.util.chat.Plugin.NETWORK, "Player (" + uuid + ") joined the network");
            } else {
                e.setCancelReason(new TextComponent(new TextComponent(ChatColor.WARNING + "You are banned " + ChatColor.WARNING + "\nuntil " + ChatColor.VALUE + dateString + ChatColor.PUBLIC + "." + ChatColor.WARNING + "\nReason: " + ChatColor.VALUE + user.getPunishment().getReason() + ChatColor.PUBLIC + "\nFor more info use our discord: " + de.timesnake.library.basic.util.server.Server.DISCORD_LINK + "\nor contact us by email: " + de.timesnake.library.basic.util.server.Server.SUPPORT_EMAIL)));
                e.setCancelled(true);
            }
        }
    }
}
