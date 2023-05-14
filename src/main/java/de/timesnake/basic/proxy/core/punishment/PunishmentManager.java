/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.punishment;

import static de.timesnake.library.chat.ExTextColor.VALUE;
import static de.timesnake.library.chat.ExTextColor.WARNING;
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
import de.timesnake.library.chat.ChatColor;
import de.timesnake.library.extension.util.chat.Chat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class PunishmentManager {

    private static Duration parseDuration(String s) {
        String[] plusParts = s.split("\\+");

        long result = 0;

        for (String plusPart : plusParts) {
            String[] multParts = plusPart.split("\\*");
            long multResult = 1;
            for (String multPart : multParts) {
                multResult *= Integer.parseInt(multPart);
            }
            result += multResult;
        }

        return Duration.ofSeconds(result);
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

            broadcastTDMessage("§v" + user.getName() + "§w was unbanned");
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
            sender.sendPluginTDMessage("§sUnbanned player §v" + user.getName());
            broadcastTDMessage("§v" + user.getName() + "§w was unbanned");
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
        user.setPunishment(Type.Punishment.BAN, LocalDateTime.now(), Duration.ZERO,
                sender.getName(), reason);
        String name = user.getName();

        for (Player p : BasicProxy.getServer().getAllPlayers()) {
            if (p.getUsername().equalsIgnoreCase(name)) {
                p.disconnect(text(ChatColor.WARNING + "You were banned. \nReason: " +
                        ChatColor.VALUE + reason));
                break;
            }
        }

        sender.sendPluginTDMessage("§sBanned §v" + name + "§s with reason: §s" + reason);

        broadcastTDMessage("§v" + user.getName() + "§w was banned with reason: §v" + reason);
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

    private void tempBanPlayerChecked(Sender sender, DbUser user, String durString, String reason) {
        Type.Punishment type = user.getPunishment().getType();

        Duration duration = parseDuration(durString);

        if (duration == null) {
            sender.sendMessageNoDateTime(durString);
            return;
        }

        if (type != null) {
            if (type.equals(Type.Punishment.TEMP_BAN)) {
                duration = duration.plusSeconds(user.getPunishment().getDuration().toSeconds());
            }

            if (type.equals(Type.Punishment.BAN)) {
                sender.sendPluginMessage(text("This player is already banned ", WARNING)
                        .append(Chat.getMessageCode("H", 102, Plugin.PUNISH)));
            }
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String dateString = df.format(LocalDateTime.now().plusSeconds(duration.toSeconds()));

        user.setPunishment(Type.Punishment.TEMP_BAN, LocalDateTime.now(), duration,
                sender.getName(), reason);
        String name = user.getName();

        for (Player p : BasicProxy.getServer().getAllPlayers()) {
            if (p.getUsername().equalsIgnoreCase(name)) {
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

        sender.sendPluginTDMessage("§sBanned §v" + name + "§s with reason: §v" + reason
                + "§s until §v" + dateString);

        broadcastTDMessage("§v" + user.getName() + "§w was banned until §v" + dateString
                + "§w with reason: §v" + reason);

    }

    public void kickPlayer(Sender sender, User user, String reason) {
        if (!sender.hasGroupRankLower(user.getUniqueId())) {
            return;
        }

        user.getPlayer().disconnect(text(ChatColor.WARNING + "You were kicked with reason: " +
                ChatColor.VALUE + reason));
        sender.sendPluginTDMessage(
                "§sKicked " + user.getChatName() + "§s with reason: §v" + reason);
        broadcastTDMessage("§v" + user.getChatName() + "§w was kicked with reason: §v" + reason);

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

        user.setPunishment(Type.Punishment.MUTE, LocalDateTime.now(), Duration.ZERO,
                sender.getName(), reason);

        String name = user.getName();
        sender.sendPluginTDMessage("§sMuted §v" + name + "§s with reason: §v" + reason);

        broadcastTDMessage("§v" + user.getName() + "§w was muted with reason: §v" + reason);

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
        sender.sendPluginTDMessage("§sUnmuted §v" + user.getName());

        broadcastTDMessage("§v" + user.getName() + "§w was unmuted");

    }

    public void jailPlayer(Sender sender, DbUser dbUser, String durString, String reason) {
        if (!sender.hasGroupRankLower(dbUser)) {
            return;
        }

        Type.Punishment type = dbUser.getPunishment().getType();
        if (type == Type.Punishment.BAN || type == Type.Punishment.TEMP_BAN) {
            sender.sendPluginMessage(text("Player is already punished ", WARNING)
                    .append(Chat.getMessageCode("H", 105, Plugin.PUNISH)));
            return;
        }

        Duration duration = parseDuration(durString);

        dbUser.setPunishment(Type.Punishment.JAIL, LocalDateTime.now(), duration,
                sender.getName(), reason);

        String name = dbUser.getName();
        long s = duration.toSeconds();
        String time = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));

        sender.sendPluginTDMessage("§sJailed " + name + "§s until §v" + time
                + "§s with reason: " + reason);

        broadcastTDMessage("§p" + dbUser.getName() + "§w was jailed for §v" + time
                + "§w with reason: §v" + reason);

        User user = Network.getUser(dbUser.getUniqueId());

        if (user != null) {
            user.getPlayer().disconnect(text(ChatColor.WARNING + "You were kicked with reason: " +
                    ChatColor.VALUE + reason));
        }

    }

    private void broadcastTDMessage(String msg) {
        Network.broadcastTDMessage(Plugin.PUNISH, msg);
    }
}
