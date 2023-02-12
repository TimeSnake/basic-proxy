/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.punishment;

import static de.timesnake.library.basic.util.chat.ExTextColor.PERSONAL;
import static de.timesnake.library.basic.util.chat.ExTextColor.VALUE;
import static de.timesnake.library.basic.util.chat.ExTextColor.WARNING;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

import com.velocitypowered.api.proxy.Player;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.basic.util.chat.ChatColor;
import de.timesnake.library.extension.util.chat.Chat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import net.kyori.adventure.text.Component;

public class PunishmentManager {

    private static LocalDateTime getTempBanDate(String dateToAdd, LocalDateTime date) {
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

        return date
                .plus(years, ChronoUnit.YEARS)
                .plus(months, ChronoUnit.MONTHS)
                .plus(days, ChronoUnit.DAYS)
                .plus(hours, ChronoUnit.HOURS)
                .plus(minutes, ChronoUnit.MINUTES)
                .plus(seconds, ChronoUnit.SECONDS);
    }

    private static int getTimeFromString(String type, String time) {
        time = time.replace(type, "");
        return Integer.parseInt(time);
    }

    public void unbanPlayer(UUID uuid) {
        DbUser user = Database.getUsers().getUser(uuid);
        Type.Punishment type = user.getPunishment().getType();
        if (type == null) {
            Network.printText(Plugin.PUNISH,
                    "§4This player (" + uuid.toString() + ") is not banned " +
                            Chat.getMessageCode("H", 101, Plugin.PUNISH));
        }

        if (!type.equals(Type.Punishment.BAN) && !type.equals(Type.Punishment.TEMP_BAN)) {
            Network.printWarning(Plugin.PUNISH,
                    "This player (" + uuid.toString() + ") is not banned (Code: H101)");
        } else {
            user.getPunishment().delete();
            Network.printText(Plugin.PUNISH, "Unbanned player " + uuid.toString() + " by system");

            broadcastMessage(text("Player ", WARNING)
                    .append(text(user.getName(), VALUE))
                    .append(text(" was unbanned", WARNING)));
        }

    }

    public void unbanPlayer(Sender sender, UUID uuid) {
        DbUser user = Database.getUsers().getUser(uuid);
        if (!sender.hasGroupRankLower(user)) {
            return;
        }

        Type.Punishment type = user.getPunishment().getType();
        if (type == null) {
            sender.sendPluginMessage(text("This player is not banned ", WARNING)
                    .append(Chat.getMessageCode("H", 101, Plugin.PUNISH)));
            return;
        }

        if (!type.equals(Type.Punishment.BAN) && !type.equals(Type.Punishment.TEMP_BAN)) {

            sender.sendPluginMessage(text("This player is not banned ", WARNING)
                    .append(Chat.getMessageCode("H", 101, Plugin.PUNISH)));
        } else {
            user.getPunishment().delete();

            sender.sendPluginMessage(text("You unbanned player ", PERSONAL)
                    .append(text(user.getName(), VALUE)));

            broadcastMessage(text("Player ", WARNING)
                    .append(text(user.getName(), VALUE))
                    .append(text(" was unbanned", WARNING)));
        }
    }

    public void banPlayer(Sender sender, DbUser user, String reason) {
        if (!sender.hasGroupRankLower(user)) {
            return;
        }

        Type.Punishment type = user.getPunishment().getType();
        if (type == null) {
            banPlayerChecked(sender, user, reason);
        } else if (!type.equals(Type.Punishment.BAN)) {
            banPlayerChecked(sender, user, reason);
        } else {
            sender.sendPluginMessage(text("This player is already banned ", WARNING)
                    .append(Chat.getMessageCode("H", 102, Plugin.PUNISH)));
        }
    }

    private void banPlayerChecked(Sender sender, DbUser user, String reason) {
        user.setPunishment(Type.Punishment.BAN, LocalDateTime.now(), sender.getName(), reason,
                "All");
        String name = user.getName();

        for (Player p : BasicProxy.getServer().getAllPlayers()) {
            if (p.getUsername().equalsIgnoreCase(name)) {
                p.disconnect(text(ChatColor.WARNING + "You were banned. \nReason: " +
                        ChatColor.VALUE + reason));
                break;
            }
        }

        sender.sendPluginMessage(text("You banned ", PERSONAL)
                .append(text(name, VALUE))
                .append(text(" with reason: ", PERSONAL))
                .append(text(reason, VALUE)));

        broadcastMessage(text("Player ", WARNING)
                .append(text(user.getName(), VALUE))
                .append(text(" was banned with reason: ", WARNING))
                .append(text(reason, VALUE)));
    }

    public void tempBanPlayer(Sender sender, DbUser user, String date, String reason) {
        if (!sender.hasGroupRankLower(user)) {
            return;
        }

        Type.Punishment type = user.getPunishment().getType();
        if (type == null) {
            tempBanPlayerChecked(sender, user, date, reason);
        } else if (!type.equals(Type.Punishment.BAN)) {
            tempBanPlayerChecked(sender, user, date, reason);
        } else {
            sender.sendPluginMessage(text("This player is already banned ", WARNING)
                    .append(Chat.getMessageCode("H", 102, Plugin.PUNISH)));
        }

    }

    private void tempBanPlayerChecked(Sender sender, DbUser user, String date, String reason) {
        Type.Punishment type = user.getPunishment().getType();
        LocalDateTime datePunish = getTempBanDate(date, LocalDateTime.now());
        if (datePunish == null) {
            sender.sendMessageNoDateTime(date);
        }

        if (type != null) {
            if (type.equals(Type.Punishment.TEMP_BAN)) {
                LocalDateTime datePunishOld = user.getPunishment().getDate();
                datePunish = getTempBanDate(date, datePunishOld);
            }

            if (type.equals(Type.Punishment.BAN)) {
                sender.sendPluginMessage(text("This player is already banned ", WARNING)
                        .append(Chat.getMessageCode("H", 102, Plugin.PUNISH)));
            }
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String dateString = df.format(datePunish);

        user.setPunishment(Type.Punishment.TEMP_BAN, datePunish, sender.getName(), reason, "ALL");
        String name = user.getName();

        for (Player p : BasicProxy.getServer().getAllPlayers()) {
            if (p.getUsername().equalsIgnoreCase(name)) {
                assert datePunish != null;
                p.disconnect(text("You were banned", WARNING)
                        .append(newline())
                        .append(text("Reason: ", WARNING))
                        .append(text(reason, VALUE))
                        .append(newline())
                        .append(text("until ", WARNING))
                        .append(text(dateString, VALUE)));
                break;
            }
        }

        sender.sendPluginMessage(text("You banned ", PERSONAL)
                .append(text(name, VALUE))
                .append(text(" with reason: ", PERSONAL))
                .append(text(reason, VALUE))
                .append(text(" until ", PERSONAL))
                .append(text(dateString, VALUE)));

        broadcastMessage(text("Player ", WARNING)
                .append(text(user.getName(), VALUE))
                .append(text(" was temporary banned until ", WARNING))
                .append(text(dateString, VALUE))
                .append(text(" with reason: ", WARNING))
                .append(text(reason, VALUE)));

    }

    public void kickPlayer(Sender sender, User user, String reason) {
        if (!sender.hasGroupRankLower(user.getUniqueId())) {
            return;
        }

        user.getPlayer().disconnect(text(ChatColor.WARNING + "You were kicked with reason: " +
                ChatColor.VALUE + reason));
        sender.sendPluginMessage(text()
                .append(text("You kicked ", PERSONAL))
                .append(user.getChatNameComponent())
                .append(text(" with reason: ", PERSONAL))
                .append(text(reason, VALUE)).build());
        broadcastMessage(text("Player ", WARNING)
                .append(text(user.getName(), VALUE))
                .append(text(" was kicked with reason: ", WARNING))
                .append(text(reason, VALUE)));

    }

    public void mutePlayer(Sender sender, DbUser user, String reason) {
        if (!sender.hasGroupRankLower(user)) {
            return;
        }

        Type.Punishment type = user.getPunishment().getType();
        if (type != null) {
            sender.sendPluginMessage(text("Player is already punished ", WARNING)
                    .append(Chat.getMessageCode("H", 102, Plugin.PUNISH)));
            return;
        }

        user.setPunishment(Type.Punishment.MUTE, LocalDateTime.now(), sender.getName(), reason,
                "ALL");
        String name = user.getName();
        sender.sendPluginMessage(text("You muted ", PERSONAL)
                .append(text(name, VALUE))
                .append(text(" with reason: ", PERSONAL))
                .append(text(reason, VALUE)));

        broadcastMessage(text("Player ", WARNING)
                .append(text(user.getName(), VALUE))
                .append(text(" was muted with reason: ", WARNING))
                .append(text(reason, VALUE)));

    }

    public void unmutePlayer(Sender sender, DbUser user) {
        if (!sender.hasGroupRankLower(user)) {
            return;
        }

        Type.Punishment type = user.getPunishment().getType();
        if (type == null || !type.equals(Type.Punishment.MUTE)) {
            sender.sendMessage(text("This Player is already unmuted ", WARNING)
                    .append(Chat.getMessageCode("H", 104, Plugin.PUNISH)));
            return;
        }

        user.getPunishment().delete();
        sender.sendPluginMessage(text("You unmuted ", PERSONAL)
                .append(text(user.getName(), VALUE)));

        broadcastMessage(text("Player ", WARNING)
                .append(text(user.getName(), VALUE))
                .append(text(" was unmuted", WARNING)));

    }

    private void broadcastMessage(Component msg) {
        Network.broadcastMessage(Plugin.PUNISH, msg);
    }
}
