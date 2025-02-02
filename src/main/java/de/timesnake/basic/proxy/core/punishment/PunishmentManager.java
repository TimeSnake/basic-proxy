/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.punishment;

import com.velocitypowered.api.proxy.Player;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.util.listener.ChannelHandler;
import de.timesnake.channel.util.listener.ChannelListener;
import de.timesnake.channel.util.listener.ListenerType;
import de.timesnake.channel.util.message.ChannelUserMessage;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.user.DbPunishment;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.basic.util.PunishType;
import de.timesnake.library.basic.util.Punishment;
import de.timesnake.library.chat.Chat;
import de.timesnake.library.chat.ChatColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static de.timesnake.library.chat.ExTextColor.VALUE;
import static de.timesnake.library.chat.ExTextColor.WARNING;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

public class PunishmentManager implements ChannelListener {

  private final Logger logger = LogManager.getLogger("punish.manager");

  public PunishmentManager() {
    Network.getChannel().addListener(this);
  }

  @ChannelHandler(type = ListenerType.USER_PUNISH)
  public void onChannelUserMessage(ChannelUserMessage<Punishment> msg) {
    if (msg.getValue() == null) {
      DbUser user = Database.getUsers().getUser(msg.getUniqueId());
      if (user != null) {
        this.deleteExpiredPunishment(user);
      }

      return;
    }

    this.logger.info("Received punish update: {}", msg.getValue());
    this.punishPlayer(null, msg.getValue(), true);
  }

  public void punishPlayer(@Nullable Sender sender, @NotNull Punishment punishment, boolean deleteExpiredPunishment) {
    DbUser user = Database.getUsers().getUser(punishment.getUuid());
    PunishType type = punishment.getType();

    if (user == null) {
      return;
    }

    if (deleteExpiredPunishment) {
      PunishType oldType = this.deleteExpiredPunishment(user);

      if (oldType != null) {
        this.logger.info("Deleted expired punishment '{}' of user '{}'", oldType.getShortName(), user.getName());
      }
    }

    if (type.equals(PunishType.BAN)) {
      this.banPlayer(sender, user, punishment);
    } else if (type.equals(PunishType.TEMP_BAN)) {
      this.tempBanPlayer(sender, user, punishment);
    } else if (type.equals(PunishType.TEMP_MUTE)) {
      this.tempMutePlayer(sender, user, punishment);
    } else if (type.equals(PunishType.JAIL)) {
      this.jailPlayer(sender, user, punishment);
    }
  }

  public PunishType deleteExpiredPunishment(DbUser user) {
    DbPunishment dbPunishment = user.getPunishment();

    if (!dbPunishment.exists()) {
      return null;
    }

    Punishment punishment = dbPunishment.asPunishment();

    if (punishment.isExpired()) {
      dbPunishment.delete();
      return punishment.getType();
    }

    return null;
  }

  public void unbanPlayer(UUID uuid) {
    DbUser user = Database.getUsers().getUser(uuid);

    if (user == null) {
      return;
    }

    PunishType type = user.getPunishment().getType();
    if (type == null) {
      this.logger.info("Failed to unban '{}', player is not banned", uuid.toString());
      return;
    }

    if (!type.equals(PunishType.BAN) && !type.equals(PunishType.TEMP_BAN)) {
      this.logger.warn("Failed to unban '{}', player is not banned", uuid.toString());
    } else {
      user.getPunishment().delete();
      this.logger.info("Unbanned player '{}' by system", uuid.toString());

      Network.broadcastTDMessage(Network.PLUGIN_PUNISH, "§v" + user.getName() + "§w was unbanned");
    }
  }

  public void unbanPlayer(@Nullable Sender sender, UUID uuid) {
    DbUser user = Database.getUsers().getUser(uuid);

    if (user == null) {
      return;
    }

    if (sender != null && !sender.hasGroupRankLower(user.getUniqueId(), true)) {
      return;
    }

    PunishType type = user.getPunishment().getType();
    if (type == null) {
      if (sender != null) {
        sender.sendPluginTDMessage("§wThis player is not banned");
      }
      return;
    }

    if (!type.equals(PunishType.BAN) && !type.equals(PunishType.TEMP_BAN)) {
      if (sender != null) {
        sender.sendPluginTDMessage("§wThis player is not banned");
      }
    } else {
      user.getPunishment().delete();
      if (sender != null) {
        sender.sendPluginTDMessage("§sUnbanned player §v" + user.getName());
      }
      Network.broadcastTDMessage(Network.PLUGIN_PUNISH, "§v" + user.getName() + "§w was unbanned");
    }
  }

  public void banPlayer(@Nullable Sender sender, DbUser user, Punishment punishment) {
    if (sender != null && !sender.hasGroupRankLower(user.getUniqueId(), true)) {
      return;
    }

    PunishType type = user.getPunishment().getType();
    if (type == null) {
      banPlayerChecked(sender, user, punishment);
    } else if (!type.equals(PunishType.BAN)) {
      banPlayerChecked(sender, user, punishment);
    } else {
      if (sender != null) {
        sender.sendPluginTDMessage("§wThis player is already banned");
      }
    }
  }

  private void banPlayerChecked(@Nullable Sender sender, DbUser user, Punishment punishment) {
    user.setPunishment(punishment);
    String name = user.getName();

    for (Player p : BasicProxy.getServer().getAllPlayers()) {
      if (p.getUsername().equalsIgnoreCase(name)) {
        p.disconnect(text(ChatColor.WARNING + "You were banned. \nReason: " + ChatColor.VALUE + punishment.getReason()));
        break;
      }
    }

    if (sender != null) {
      sender.sendPluginTDMessage("§sBanned §v" + name + "§s with reason: §s" + punishment.getReason());
    }

    Network.broadcastTDMessage(Network.PLUGIN_PUNISH,
        "§v" + user.getName() + "§w was banned with reason: §v" + punishment.getReason());
  }

  public void tempBanPlayer(@Nullable Sender sender, DbUser user, Punishment punishment) {
    if (sender != null && !sender.hasGroupRankLower(user.getUniqueId(), true)) {
      return;
    }

    PunishType type = user.getPunishment().getType();
    if (type == null) {
      tempBanPlayerChecked(sender, user, punishment);
    } else if (!type.equals(PunishType.BAN)) {
      tempBanPlayerChecked(sender, user, punishment);
    } else {
      if (sender != null) {
        sender.sendPluginTDMessage("§wThis player is already banned");
      }
    }
  }

  private void tempBanPlayerChecked(@Nullable Sender sender, DbUser user, Punishment punishment) {
    PunishType type = user.getPunishment().getType();

    if (type != null) {
      if (type.equals(PunishType.TEMP_BAN)) {
        punishment.setDuration(punishment.getDuration().plusSeconds(user.getPunishment().getDuration().toSeconds()));
      }

      if (type.equals(PunishType.BAN)) {
        if (sender != null) {
          sender.sendPluginTDMessage("§wThis player is already banned");
        }
        return;
      }
    }

    user.setPunishment(punishment);

    String name = user.getName();

    for (Player p : BasicProxy.getServer().getAllPlayers()) {
      if (p.getUsername().equalsIgnoreCase(name)) {
        p.disconnect(text("You were banned", WARNING)
            .append(newline())
            .append(text("Reason: ", WARNING))
            .append(text(punishment.getReason(), VALUE))
            .append(newline())
            .append(text("for ", WARNING))
            .append(text(Chat.getTimeString(punishment.getDuration()), VALUE)));
        break;
      }
    }

    if (sender != null) {
      sender.sendPluginTDMessage("§sBanned §v" + name + "§s with reason: §v" + punishment.getReason() + "§s for §v"
          + Chat.getTimeString(punishment.getDuration()));
    }
    Network.broadcastTDMessage(Network.PLUGIN_PUNISH, "§v" + user.getName() + "§w was banned for §v"
        + Chat.getTimeString(punishment.getDuration()) + "§w with reason: §v" + punishment.getReason());
  }

  public void kickPlayer(Sender sender, User user, String reason) {
    if (!sender.hasGroupRankLower(user.getUniqueId(), true)) {
      return;
    }

    user.getPlayer().disconnect(text(ChatColor.WARNING + "You were kicked with reason: " + ChatColor.VALUE + reason));
    sender.sendPluginTDMessage("§sKicked " + user.getChatName() + "§s with reason: §v" + reason);
    Network.broadcastTDMessage(Network.PLUGIN_PUNISH,
        "§v" + user.getChatName() + "§w was kicked with reason: §v" + reason);
  }

  public void mutePlayer(@Nullable Sender sender, DbUser user, Punishment punishment) {
    if (sender != null && !sender.hasGroupRankLower(user.getUniqueId(), true)) {
      return;
    }

    PunishType type = user.getPunishment().getType();
    if (type != null && !type.equals(PunishType.TEMP_MUTE)) {
      if (sender != null) {
        sender.sendPluginTDMessage("§wPlayer is already punished");
      }
      return;
    }

    user.setPunishment(punishment);

    String name = user.getName();

    if (sender != null) {
      sender.sendPluginTDMessage("§sMuted §v" + name + "§s with reason: §v" + punishment.getReason());
    }
    Network.broadcastTDMessage(Network.PLUGIN_PUNISH,
        "§v" + user.getName() + "§w was muted with reason: §v" + punishment.getReason());
  }

  public void tempMutePlayer(@Nullable Sender sender, DbUser user, Punishment punishment) {
    if (sender != null && !sender.hasGroupRankLower(user.getUniqueId(), true)) {
      return;
    }

    PunishType type = user.getPunishment().getType();
    if (type != null) {
      if (sender != null) {
        sender.sendPluginTDMessage("§wPlayer is already punished");
      }
      return;
    }

    user.setPunishment(punishment);

    String name = user.getName();

    if (sender != null) {
      sender.sendPluginTDMessage("§sMuted §v" + name + "§s for §v" + Chat.getTimeString(punishment.getDuration())
          + "§s with reason: §v" + punishment.getReason());
    }

    Network.broadcastTDMessage(Network.PLUGIN_PUNISH, "§v" + user.getName() + "§w was muted for §v" +
        Chat.getTimeString(punishment.getDuration()) + "§w with reason: §v" + punishment.getReason());
  }

  public void unmutePlayer(@Nullable Sender sender, DbUser user) {
    if (sender != null && !sender.hasGroupRankLower(user.getUniqueId(), true)) {
      return;
    }

    PunishType type = user.getPunishment().getType();
    if (type == null || !type.equals(PunishType.TEMP_MUTE)) {
      if (sender != null) {
        sender.sendPluginTDMessage("§wThis Player is already unmuted");
      }
      return;
    }

    user.getPunishment().delete();

    if (sender != null) {
      sender.sendPluginTDMessage("§sUnmuted §v" + user.getName());
    }
    Network.broadcastTDMessage(Network.PLUGIN_PUNISH, "§v" + user.getName() + "§w was unmuted");
  }

  public void jailPlayer(@Nullable Sender sender, DbUser dbUser, Punishment punishment) {
    if (sender != null && !sender.hasGroupRankLower(dbUser.getUniqueId(), true)) {
      return;
    }

    PunishType type = dbUser.getPunishment().getType();
    if (type == PunishType.BAN || type == PunishType.TEMP_BAN) {
      if (sender != null) {
        sender.sendPluginTDMessage("§wPlayer is already punished");
      }
      return;
    }

    dbUser.setPunishment(punishment);

    String name = dbUser.getName();

    if (sender != null) {
      sender.sendPluginTDMessage("§sJailed " + name + "§s for §v" + Chat.getTimeString(punishment.getDuration())
          + "§s with reason: " + punishment.getReason());
    }

    Network.broadcastTDMessage(Network.PLUGIN_PUNISH, "§p" + dbUser.getName() + "§w was jailed for §v"
        + Chat.getTimeString(punishment.getDuration()) + "§w with reason: §v" + punishment.getReason());

    User user = Network.getUser(dbUser.getUniqueId());

    if (user != null) {
      user.getPlayer().disconnect(text(ChatColor.WARNING + "You were kicked with reason: " +
          ChatColor.VALUE + punishment.getReason()));
    }
  }
}
