package de.timesnake.basic.proxy.core.punishment;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.api.message.ChannelUserMessage;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.user.DbUser;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class Punishments {

    public static void unbanPlayer(UUID uuid) {
        DbUser user = Database.getUsers().getUser(uuid);
        Type.Punishment type = user.getPunishment().getType();
        if (type != null) {
            if (!type.equals(Type.Punishment.BAN) && !type.equals(Type.Punishment.TEMP_BAN)) {
                Network.printWarning(Plugin.PUNISH, "This player (" + uuid.toString() + ") is not banned (Code: H101)");
            } else {
                user.getPunishment().delete();
                Network.printText(Plugin.PUNISH, "§4Unbanned player " + uuid.toString() + " by system");

                broadcastMessage(ChatColor.WARNING + "Player " + ChatColor.VALUE + user.getName() + ChatColor.WARNING + " was unbanned");
            }
        } else {
            Network.printText(Plugin.PUNISH, "§4This player (" + uuid.toString() + ") is not banned " + Network.getChat().getMessageCode("H", 101, Plugin.PUNISH));
        }

    }

    public static void unbanPlayer(Sender sender, UUID uuid) {
        DbUser user = Database.getUsers().getUser(uuid);
        if (sender.hasGroupRankLower(user)) {
            Type.Punishment type = user.getPunishment().getType();
            if (type != null) {
                if (!type.equals(Type.Punishment.BAN) && !type.equals(Type.Punishment.TEMP_BAN)) {

                    sender.sendPluginMessage(ChatColor.WARNING + "This player is not banned " + Network.getChat().getMessageCode("H", 101, Plugin.PUNISH));
                } else {
                    user.getPunishment().delete();

                    sender.sendPluginMessage(ChatColor.PERSONAL + "You unbanned player " + ChatColor.VALUE + user.getName());

                    broadcastMessage(ChatColor.WARNING + "Player " + ChatColor.VALUE + user.getName() + ChatColor.WARNING + " was unbanned");
                }
            } else {
                broadcastMessage(ChatColor.WARNING + "This player is not banned " + Network.getChat().getMessageCode("H", 101, Plugin.PUNISH));
            }
        }

    }

    public static void banPlayer(Sender sender, DbUser user, String reason) {
        if (sender.hasGroupRankLower(user)) {
            Type.Punishment type = user.getPunishment().getType();
            if (type == null) {
                banPlayerChecked(sender, user, reason);
            } else if (!type.equals(Type.Punishment.BAN)) {
                banPlayerChecked(sender, user, reason);
            } else {
                broadcastMessage(ChatColor.WARNING + "This player is already banned " + Network.getChat().getMessageCode("H", 102, Plugin.PUNISH));
            }
        }

    }

    private static void banPlayerChecked(Sender sender, DbUser user, String reason) {
        user.setPunishment(Type.Punishment.BAN, new Date(), sender.getName(), reason, "All");
        String name = user.getName();

        for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            if (p.getName().equalsIgnoreCase(name)) {
                p.disconnect(new TextComponent(ChatColor.WARNING + "You were banned. \nReason: " + ChatColor.VALUE + reason));
                break;
            }
        }

        sender.sendPluginMessage(ChatColor.PERSONAL + "You banned " + ChatColor.VALUE + name + ChatColor.PUBLIC + " with reason: " + ChatColor.VALUE + reason);

        broadcastMessage(ChatColor.WARNING + "Player " + ChatColor.VALUE + user.getName() + ChatColor.WARNING + " was banned with reason: " + ChatColor.VALUE + reason);
    }

    public static void tempBanPlayer(Sender sender, DbUser user, String date, String reason) {
        if (sender.hasGroupRankLower(user)) {
            Type.Punishment type = user.getPunishment().getType();
            if (type == null) {
                tempBanPlayerChecked(sender, user, date, reason);
            } else if (!type.equals(Type.Punishment.BAN)) {
                tempBanPlayerChecked(sender, user, date, reason);
            } else {
                broadcastMessage(ChatColor.WARNING + "This player is already banned " + Network.getChat().getMessageCode("H", 102, Plugin.PUNISH));
            }
        }

    }

    private static void tempBanPlayerChecked(Sender sender, DbUser user, String date, String reason) {
        Type.Punishment type = user.getPunishment().getType();
        Date datePunish = getTempBanDate(date, new Date());
        if (datePunish != null) {
            if (type != null) {
                if (type.equals(Type.Punishment.TEMP_BAN)) {
                    Date datePunishOld = user.getPunishment().getDate();
                    datePunish = getTempBanDate(date, datePunishOld);
                }

                if (type.equals(Type.Punishment.BAN)) {
                    sender.sendPluginMessage(ChatColor.WARNING + "This player is already banned " + Network.getChat().getMessageCode("H", 102, Plugin.PUNISH));
                }
            }

            user.setPunishment(Type.Punishment.TEMP_BAN, datePunish, sender.getName(), reason, "ALL");
            String name = user.getName();

            for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
                if (p.getName().equalsIgnoreCase(name)) {
                    assert datePunish != null;
                    p.disconnect(new TextComponent(ChatColor.WARNING + "You were banned \nReason: " + ChatColor.VALUE + reason + ChatColor.WARNING + "\nuntil " + ChatColor.VALUE + datePunish));
                    break;
                }
            }

            assert datePunish != null;

            sender.sendPluginMessage(ChatColor.PERSONAL + "You banned " + ChatColor.VALUE + name + ChatColor.PERSONAL + " with reason: " + ChatColor.VALUE + reason + ChatColor.PERSONAL + " until " + ChatColor.VALUE + datePunish);

            broadcastMessage(ChatColor.WARNING + "Player " + ChatColor.VALUE + user.getName() + ChatColor.WARNING + " was temporary banned until " + ChatColor.VALUE + datePunish + ChatColor.WARNING + " with reason: " + ChatColor.VALUE + reason);
        } else {
            sender.sendMessageNoDateTime(date);
        }

    }

    public static void kickPlayer(Sender sender, User user, String reason) {
        if (sender.hasGroupRankLower(user.getUniqueId())) {
            user.getPlayer().disconnect(new TextComponent(ChatColor.WARNING + "You were kicked with reason: " + ChatColor.VALUE + reason));
            sender.sendPluginMessage(ChatColor.PERSONAL + "You kicked " + ChatColor.VALUE + user.getChatName() + ChatColor.PERSONAL + " with reason: " + ChatColor.VALUE + reason);
            broadcastMessage(ChatColor.WARNING + "Player " + ChatColor.VALUE + user.getName() + ChatColor.WARNING + " was kicked with reason: " + ChatColor.VALUE + reason);
        }

    }

    public static void mutePlayer(Sender sender, DbUser user, String reason) {
        if (sender.hasGroupRankLower(user)) {
            Type.Punishment type = user.getPunishment().getType();
            if (type == null) {
                user.setPunishment(Type.Punishment.MUTE, new Date(), sender.getName(), reason, "ALL");
                Network.getChannel().sendMessage(user.getServer().getPort(), ChannelUserMessage.getPunishMessage(user.getUniqueId()));
                String name = user.getName();
                sender.sendPluginMessage(ChatColor.PERSONAL + "You muted " + ChatColor.VALUE + name + ChatColor.PUBLIC + " with reason: " + ChatColor.VALUE + reason);

                broadcastMessage(ChatColor.WARNING + "Player " + ChatColor.VALUE + user.getName() + ChatColor.WARNING + " was muted with reason: " + ChatColor.VALUE + reason);
            } else {
                broadcastMessage(ChatColor.WARNING + "Player is already punished " + Network.getChat().getMessageCode("H", 102, Plugin.PUNISH));
            }
        }

    }

    public static void unmutePlayer(Sender sender, DbUser user) {
        if (sender.hasGroupRankLower(user)) {
            Type.Punishment type = user.getPunishment().getType();
            if (type.equals(Type.Punishment.MUTE)) {
                user.getPunishment().delete();
                Network.getChannel().sendMessage(user.getServer().getPort(), ChannelUserMessage.getPunishMessage(user.getUniqueId()));
                sender.sendPluginMessage(ChatColor.PERSONAL + "You unmuted " + ChatColor.VALUE + user.getName());

                broadcastMessage(ChatColor.WARNING + "Player " + ChatColor.VALUE + user.getName() + ChatColor.WARNING + " was unmuted");
            } else {
                sender.sendMessage(ChatColor.WARNING + "This Player is already unmuted " + Network.getChat().getMessageCode("H", 104, Plugin.PUNISH));
            }
        }

    }

    private static Date getTempBanDate(String dateToAdd, Date date) {
        int years = 0;
        int months = 0;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        String[] splitDate = dateToAdd.split(";");

        for (String s : splitDate) {
            if (s.toLowerCase().endsWith("sec")) {
                seconds = getTimeFromString("sec", s);
            } else if (s.toLowerCase().endsWith("min")) {
                minutes = getTimeFromString("min", s);
            } else if (s.toLowerCase().endsWith("hour")) {
                hours = getTimeFromString("hour", s);
            } else if (s.toLowerCase().endsWith("day")) {
                days = getTimeFromString("day", s);
            } else if (s.toLowerCase().endsWith("month")) {
                months = getTimeFromString("month", s);
            } else {
                if (!s.toLowerCase().endsWith("year")) {
                    return null;
                }

                years = getTimeFromString("year", s);
            }
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.YEAR, years);
        cal.add(Calendar.MONTH, months);
        cal.add(Calendar.DATE, days);
        cal.add(Calendar.HOUR, hours);
        cal.add(Calendar.MINUTE, minutes);
        cal.add(Calendar.SECOND, seconds);
        return cal.getTime();
    }

    private static int getTimeFromString(String type, String time) {
        time = time.replace(type, "");
        return Integer.parseInt(time);
    }

    private static void broadcastMessage(String msg) {
        Network.broadcastMessage(Network.getChat().getSenderPlugin(Plugin.PUNISH) + msg);
    }
}