/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.network;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.core.server.ServerCmd;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.NetworkManager;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.library.chat.Chat;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AutoShutdown implements CommandListener {

  private final Logger logger = LogManager.getLogger("network.auto-shutdown");

  public static final int START_TIME = 15;
  private static final int PLAYER_TIME_0 = 5;
  private static final int PLAYER_TIME_1 = 30;
  private final Set<UUID> votedUsers = new HashSet<>();
  private boolean enabled = true;
  private boolean cancelable = false;
  private int requiredVotes = 1;
  private ScheduledTask task;

  private final Code helloPerm = Plugin.NETWORK.createPermssionCode("network.hello");
  private final Code shutdownPerm = Plugin.NETWORK.createPermssionCode("network.shutdown");
  private final Code autoShutdownPerm = Plugin.NETWORK.createPermssionCode("network.autoshutdown");

  public AutoShutdown() {
    NetworkManager.getInstance().getCommandManager().addCommand(BasicProxy.getPlugin(), "shutdown", this, Plugin.NETWORK);
    NetworkManager.getInstance().getCommandManager().addCommand(BasicProxy.getPlugin(), "autoshutdown", this, Plugin.NETWORK);
    NetworkManager.getInstance().getCommandManager().addCommand(BasicProxy.getPlugin(), "hello", List.of("hallo", "hi", "helo"), this, Plugin.NETWORK);
  }

  @Subscribe
  public void onPlayerJoin(PostLoginEvent e) {
    if (!Network.getUsers().isEmpty()) {
      this.start(PLAYER_TIME_1);
    }
  }

  @Subscribe
  public void onPlayerDisconnect(DisconnectEvent e) {
    if (Network.getUsers().isEmpty()) {
      this.start(PLAYER_TIME_0);
    }
  }

  public void start(int time) {
    if (this.task != null) {
      this.task.cancel();
    }

    if (enabled) {
      this.cancelable = false;
      this.logger.info("AutoShutdown started");
      task = BasicProxy.getServer().getScheduler()
          .buildTask(BasicProxy.getPlugin(), this::infoShutdown)
          .delay(time, TimeUnit.MINUTES).schedule();
    }
  }

  public void infoShutdown() {
    this.cancelable = true;
    Network.broadcastTDMessage(Plugin.NETWORK, "§w§lThe server will shutdown in 5 minutes");

    Component text = Chat.getSenderPlugin(Plugin.NETWORK)
        .append(Component.text("Write ", ExTextColor.WARNING))
        .append(Component.text("/hello", ExTextColor.VALUE))
        .append(Component.text(" to keep the server online.", ExTextColor.WARNING))
        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/hello"))
        .hoverEvent(HoverEvent.showText(Component.text("Click to execute /hello")));

    for (User user : Network.getUsers()) {
      user.getPlayer().sendMessage(text);
    }

    task.cancel();
    task = BasicProxy.getServer().getScheduler()
        .buildTask(BasicProxy.getPlugin(), this::warnShutdown).delay(4,
            TimeUnit.MINUTES).schedule();
  }

  public void warnShutdown() {
    this.cancelable = true;

    Network.broadcastTDMessage(Plugin.NETWORK, "§w§lThe server will shutdown in 1 minute");

    Component text = Chat.getSenderPlugin(Plugin.NETWORK)
        .append(Component.text("Write ", ExTextColor.WARNING))
        .append(Component.text("/hello", ExTextColor.VALUE, TextDecoration.UNDERLINED))
        .append(Component.text(" to keep the server online.", ExTextColor.WARNING))
        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/hello"))
        .hoverEvent(HoverEvent.showText(Component.text("Click to execute /hello")));

    for (User user : Network.getUsers()) {
      user.getPlayer().sendMessage(text);
    }

    task.cancel();
    task = BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), this::beginShutdown)
        .delay(1, TimeUnit.MINUTES).schedule();
  }

  private void beginShutdown() {
    this.cancelable = true;
    Network.broadcastTDMessage(Plugin.NETWORK, "§w§lThe server will shutdown in 10 seconds");
    task.cancel();
    task = BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), () -> {
      Network.broadcastTDMessage(Plugin.NETWORK, "§w§lSHUTDOWN");
      new ServerCmd().stopAllServers();
      shutdown();
    }).delay(10, TimeUnit.SECONDS).schedule();
  }

  private void shutdown() {
    this.cancelable = false;
    task.cancel();

    task = BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(),
        () -> BasicProxy.getServer().shutdown()).delay(30, TimeUnit.SECONDS).schedule();
  }

  private void cancelShutdown() {
    if (this.task != null) {
      this.task.cancel();
      this.votedUsers.clear();
      Network.broadcastMessage(Plugin.NETWORK,
          Component.text("Shutdown cancelled", ExTextColor.WARNING));
      if (enabled) {
        this.start(!Network.getUsers().isEmpty() ? PLAYER_TIME_1 : PLAYER_TIME_0);
      }
    }
  }

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    if (cmd.getName().equalsIgnoreCase("hello")
        || cmd.getName().equalsIgnoreCase("hallo")
        || cmd.getName().equalsIgnoreCase("hi")) {
      if (sender.hasPermission(this.helloPerm)) {
        if (sender.isConsole(false)) {
          this.cancelShutdown();
          return;
        }

        if (!this.cancelable) {
          sender.sendPluginTDMessage("§wThere is no running shutdown");
          return;
        }

        User user = sender.getUser();

        if (this.votedUsers.contains(user.getUniqueId())) {
          sender.sendPluginTDMessage("You already voted against the shutdown");
          return;
        }

        this.votedUsers.add(user.getUniqueId());

        if (this.votedUsers.size() >= this.requiredVotes) {
          this.cancelShutdown();
        } else {
          Network.broadcastTDMessage(Plugin.NETWORK, "§wAgainst Server shutdown: §v"
                                                     + this.votedUsers.size() + " / " + this.requiredVotes);
        }
      }
    } else if (cmd.getName().equalsIgnoreCase("shutdown")) {
      if (sender.hasPermission(this.shutdownPerm)) {
        this.beginShutdown();
      }
    } else if (cmd.getName().equalsIgnoreCase("autoshutdown")) {
      if (sender.hasPermission(this.autoShutdownPerm)) {
        if (args.isLengthEquals(1, false) && args.get(0).isInt(true)) {
          this.requiredVotes = args.get(0).toInt();
          if (this.requiredVotes <= 0) {
            this.requiredVotes = 1;
          }

          this.enabled = false;

          sender.sendPluginTDMessage("§sUpdated shutdown votes to §v" + this.requiredVotes);
        }

        if (this.enabled) {
          this.enabled = false;
          if (this.task != null) {
            this.task.cancel();
          }
          this.votedUsers.clear();

          Network.broadcastTDMessage(Plugin.NETWORK, "§wShutdown cancelled");
          sender.sendPluginTDMessage("§sDisabled auto-shutdown");
        } else {
          this.enabled = true;
          this.votedUsers.clear();
          this.start(!Network.getUsers().isEmpty() ? PLAYER_TIME_1 : PLAYER_TIME_0);
          sender.sendPluginTDMessage("§sEnabled auto-shutdown");
        }
      }
    }

  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.helloPerm);
  }

  @Override
  public String getPermission() {
    return this.helloPerm.getPermission();
  }
}
