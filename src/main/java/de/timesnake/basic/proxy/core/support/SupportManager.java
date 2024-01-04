/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.support;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.*;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.util.listener.ChannelHandler;
import de.timesnake.channel.util.listener.ChannelListener;
import de.timesnake.channel.util.listener.ListenerType;
import de.timesnake.channel.util.message.ChannelSupportMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class SupportManager implements ChannelListener, CommandListener {

  private final HashMap<Integer, Tuple<String, ScheduledTask>> lockedTicketsById = new HashMap<>();

  private final Set<UUID> ticketListeners = new HashSet<>();

  private final Code msgPerm = Plugin.SUPPORT.createPermssionCode("support.message");

  public SupportManager() {
    Network.getCommandManager().addCommand(BasicProxy.getPlugin(),
        "supportmsg", List.of("supportmessage", "supportmessages", "supportmsgs", "smsg"),
        this, Plugin.SUPPORT);
    Network.getChannel().addListener(this);
    Network.registerListener(this);
  }

  @ChannelHandler(type = ListenerType.SUPPORT)
  public void onSupportMessage(ChannelSupportMessage<?> msg) {
    if (msg.getMessageType().equals(MessageType.Support.TICKET_LOCK)) {
      String name = msg.getName();
      Integer id = (Integer) msg.getValue();
      Tuple<String, ScheduledTask> value = this.lockedTicketsById.get(id);
      if (value != null && !value.getA().equals(name)) {
        Network.getChannel().sendMessage(
            new ChannelSupportMessage<>(name, MessageType.Support.REJECT, id));
      } else {
        ScheduledTask task = BasicProxy.getServer().getScheduler()
            .buildTask(BasicProxy.getPlugin(),
                () -> this.lockedTicketsById.remove(id)).delay(3, TimeUnit.MINUTES)
            .schedule();

        this.lockedTicketsById.put(id, new Tuple<>(name, task));
      }
    } else if (msg.getMessageType().equals(MessageType.Support.SUBMIT)) {
      String name = msg.getName();
      Integer id = (Integer) msg.getValue();

      Tuple<String, ScheduledTask> tuple = this.lockedTicketsById.get(id);

      if (tuple == null || tuple.getA() == null || !tuple.getA().equals(name)) {
        Network.getChannel().sendMessage(
            new ChannelSupportMessage<>(name, MessageType.Support.REJECT, id));
        return;
      }

      Network.getChannel()
          .sendMessage(new ChannelSupportMessage<>(name, MessageType.Support.ACCEPT, id));

      ScheduledTask task = this.lockedTicketsById.remove(id).getB();

      if (task != null) {
        task.cancel();
      }
    } else if (msg.getMessageType().equals(MessageType.Support.CREATION)) {
      Integer id = (Integer) msg.getValue();

      for (UUID uuid : this.ticketListeners) {
        User user = Network.getUser(uuid);

        if (user != null) {
          user.sendPluginMessage(Plugin.SUPPORT,
              Component.text("New ticket: ", ExTextColor.WARNING)
                  .append(Component.text("ID " + id, ExTextColor.VALUE)));
        }
      }
    }

  }

  @Override
  public void onCommand(Sender sender, PluginCommand cmd,
      Arguments<Argument> args) {
    if (!sender.isPlayer(true)) {
      return;
    }

    User user = sender.getUser();

    if (!sender.hasPermission(this.msgPerm)) {
      return;
    }

    if (this.ticketListeners.contains(user.getUniqueId())) {
      this.ticketListeners.remove(user.getUniqueId());
      sender.sendPluginMessage(
          Component.text("Disabled support messages", ExTextColor.PERSONAL));
    } else {
      this.ticketListeners.add(user.getUniqueId());
      sender.sendPluginMessage(
          Component.text("Enabled support messages", ExTextColor.PERSONAL));
    }

  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.msgPerm);
  }

  @Override
  public String getPermission() {
    return this.msgPerm.getPermission();
  }

  @Subscribe
  public void onPlayerDisconnect(DisconnectEvent e) {
    this.ticketListeners.remove(e.getPlayer().getUniqueId());
  }
}
