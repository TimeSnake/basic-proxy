/*
 * workspace.basic-proxy.main
 * Copyright (C) 2022 timesnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package de.timesnake.basic.proxy.core.punishment;

import com.velocitypowered.api.proxy.Player;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.util.message.ChannelUserMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.basic.util.chat.ChatColor;
import de.timesnake.library.extension.util.chat.Chat;
import net.kyori.adventure.text.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static de.timesnake.library.basic.util.chat.ExTextColor.*;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

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

                broadcastMessage(text("Player ", WARNING)
                        .append(text(user.getName(), VALUE))
                        .append(text(" was unbanned", WARNING)));
            }
        } else {
            Network.printText(Plugin.PUNISH, "§4This player (" + uuid.toString() + ") is not banned " +
                    Chat.getMessageCode("H", 101, Plugin.PUNISH));
        }

    }

    public static void unbanPlayer(Sender sender, UUID uuid) {
        DbUser user = Database.getUsers().getUser(uuid);
        if (sender.hasGroupRankLower(user)) {
            Type.Punishment type = user.getPunishment().getType();
            if (type != null) {
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
            } else {
                sender.sendPluginMessage(text("This player is not banned ", WARNING)
                        .append(Chat.getMessageCode("H", 101, Plugin.PUNISH)));
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
                sender.sendPluginMessage(text("This player is already banned ", WARNING)
                        .append(Chat.getMessageCode("H", 102, Plugin.PUNISH)));
            }
        }

    }

    private static void banPlayerChecked(Sender sender, DbUser user, String reason) {
        user.setPunishment(Type.Punishment.BAN, new Date(), sender.getName(), reason, "All");
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

    public static void tempBanPlayer(Sender sender, DbUser user, String date, String reason) {
        if (sender.hasGroupRankLower(user)) {
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
                    sender.sendPluginMessage(text("This player is already banned ", WARNING)
                            .append(Chat.getMessageCode("H", 102, Plugin.PUNISH)));
                }
            }

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
                            .append(text(datePunish.toString(), VALUE)));
                    break;
                }
            }

            assert datePunish != null;

            sender.sendPluginMessage(text("You banned ", PERSONAL)
                    .append(text(name, VALUE))
                    .append(text(" with reason: ", PERSONAL))
                    .append(text(reason, VALUE))
                    .append(text(" until ", PERSONAL))
                    .append(text(datePunish.toString(), VALUE)));

            broadcastMessage(text("Player ", WARNING)
                    .append(text(user.getName(), VALUE))
                    .append(text(" was temporary banned until ", WARNING))
                    .append(text(datePunish.toString(), VALUE))
                    .append(text(" with reason: ", WARNING))
                    .append(text(reason, VALUE)));
        } else {
            sender.sendMessageNoDateTime(date);
        }

    }

    public static void kickPlayer(Sender sender, User user, String reason) {
        if (sender.hasGroupRankLower(user.getUniqueId())) {
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

    }

    public static void mutePlayer(Sender sender, DbUser user, String reason) {
        if (sender.hasGroupRankLower(user)) {
            Type.Punishment type = user.getPunishment().getType();
            if (type == null) {
                user.setPunishment(Type.Punishment.MUTE, new Date(), sender.getName(), reason, "ALL");
                Network.getChannel().sendMessage(new ChannelUserMessage<>(user.getUniqueId(), MessageType.User.PUNISH));
                String name = user.getName();
                sender.sendPluginMessage(text("You muted ", PERSONAL)
                        .append(text(name, VALUE))
                        .append(text(" with reason: ", PERSONAL))
                        .append(text(reason, VALUE)));

                broadcastMessage(text("Player ", WARNING)
                        .append(text(user.getName(), VALUE))
                        .append(text(" was muted with reason: ", WARNING))
                        .append(text(reason, VALUE)));
            } else {
                sender.sendPluginMessage(text("Player is already punished ", WARNING)
                        .append(Chat.getMessageCode("H", 102, Plugin.PUNISH)));
            }
        }

    }

    public static void unmutePlayer(Sender sender, DbUser user) {
        if (sender.hasGroupRankLower(user)) {
            Type.Punishment type = user.getPunishment().getType();
            if (type.equals(Type.Punishment.MUTE)) {
                user.getPunishment().delete();
                Network.getChannel().sendMessage(new ChannelUserMessage<>(user.getUniqueId(), MessageType.User.PUNISH));
                sender.sendPluginMessage(text("You unmuted ", PERSONAL)
                        .append(text(user.getName(), VALUE)));

                broadcastMessage(text("Player ", WARNING)
                        .append(text(user.getName(), VALUE))
                        .append(text(" was unmuted", WARNING)));
            } else {
                sender.sendMessage(text("This Player is already unmuted ", WARNING)
                        .append(Chat.getMessageCode("H", 104, Plugin.PUNISH)));
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

    private static void broadcastMessage(Component msg) {
        Network.broadcastMessage(Plugin.PUNISH, msg);
    }
}
