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
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;

public class AutoShutdown implements CommandListener<Sender, Argument> {

  public static final int START_TIME = 15;
  private static final int PLAYER_TIME_0 = 5;
  private static final int PLAYER_TIME_1 = 30;
  private final Set<UUID> votedUsers = new HashSet<>();
  private boolean enabled = true;
  private boolean cancelable = false;
  private int requiredVotes = 1;
  private ScheduledTask task;

  private Code helloPerm;
  private Code shutdownPerm;
  private Code autoShutdownPerm;

  public AutoShutdown() {
    NetworkManager.getInstance().getCommandManager().addCommand(BasicProxy.getPlugin(),
        "shutdown", this, Plugin.NETWORK);

    NetworkManager.getInstance().getCommandManager().addCommand(BasicProxy.getPlugin(),
        "autoshutdown", this, Plugin.NETWORK);

    NetworkManager.getInstance().getCommandManager().addCommand(BasicProxy.getPlugin(),
        "hello", List.of("hallo", "hi", "helo"),
        this, Plugin.NETWORK);
  }

  @Subscribe
  public void onPlayerJoin(PostLoginEvent e) {
    if (Network.getUsers().size() > 0) {
      this.start(PLAYER_TIME_1);
    }
  }

  @Subscribe
  public void onPlayerDisconnect(DisconnectEvent e) {
    if (Network.getUsers().size() == 0) {
      this.start(PLAYER_TIME_0);
    }
  }

  public void start(int time) {
    if (this.task != null) {
      this.task.cancel();
    }

    if (enabled) {
      this.cancelable = false;
      Network.printText(Plugin.SYSTEM, "AutoShutdown started");
      task = BasicProxy.getServer().getScheduler()
          .buildTask(BasicProxy.getPlugin(), this::infoShutdown)
          .delay(time, TimeUnit.MINUTES).schedule();
    }
  }

  public void infoShutdown() {
    this.cancelable = true;
    Network.broadcastMessage(Plugin.NETWORK,
        Component.text("The server will shutdown in 5 minutes ", ExTextColor.WARNING,
            TextDecoration.BOLD));

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

    Network.broadcastMessage(Plugin.NETWORK,
        Component.text("The server will shutdown in 1 minute ",
            ExTextColor.WARNING, TextDecoration.BOLD));

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
    task = BasicProxy.getServer().getScheduler()
        .buildTask(BasicProxy.getPlugin(), this::beginShutdown).delay(1,
            TimeUnit.MINUTES).schedule();
  }

  private void beginShutdown() {
    this.cancelable = true;
    Network.broadcastMessage(Plugin.NETWORK,
        Component.text("The server will shutdown in 10 seconds ", ExTextColor.WARNING,
            TextDecoration.BOLD));
    task.cancel();
    task = BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), () -> {
      Network.broadcastMessage(Plugin.NETWORK,
          Component.text("§lSHUTDOWN", ExTextColor.WARNING));
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
        this.start(Network.getUsers().size() > 0 ? PLAYER_TIME_1 : PLAYER_TIME_0);
      }
    }
  }

  @Override
  public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd,
      Arguments<Argument> args) {
    if (cmd.getName().equalsIgnoreCase("hello")
        || cmd.getName().equalsIgnoreCase("hallo")
        || cmd.getName().equalsIgnoreCase("hi")) {
      if (sender.hasPermission(this.helloPerm)) {
        if (sender.isConsole(false)) {
          this.cancelShutdown();
          return;
        }

        if (!this.cancelable) {
          sender.sendPluginMessage(
              Component.text("There is no running shutdown", ExTextColor.WARNING));
          return;
        }

        User user = sender.getUser();

        if (this.votedUsers.contains(user.getUniqueId())) {
          sender.sendPluginMessage(
              Component.text("You already voted against the shutdown",
                  ExTextColor.WARNING));
          return;
        }

        this.votedUsers.add(user.getUniqueId());

        if (this.votedUsers.size() >= this.requiredVotes) {
          this.cancelShutdown();
        } else {
          Network.broadcastMessage(Plugin.NETWORK,
              Component.text("Against Server shutdown: ", ExTextColor.WARNING)
                  .append(Component.text(
                      this.votedUsers.size() + " / " + this.requiredVotes,
                      ExTextColor.VALUE)));
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

          sender.sendPluginMessage(
              Component.text("Updated shutdown votes to ", ExTextColor.PERSONAL)
                  .append(Component.text(this.requiredVotes, ExTextColor.VALUE)));
        }

        if (this.enabled) {
          this.enabled = false;
          if (this.task != null) {
            this.task.cancel();
          }
          this.votedUsers.clear();

          Network.broadcastMessage(Plugin.NETWORK,
              Component.text("Shutdown cancelled", ExTextColor.WARNING));
          sender.sendPluginMessage(
              Component.text("Disabled auto-shutdown", ExTextColor.PERSONAL));
        } else {
          this.enabled = true;
          this.votedUsers.clear();
          this.start(Network.getUsers().size() > 0 ? PLAYER_TIME_1 : PLAYER_TIME_0);
          sender.sendPluginMessage(
              Component.text("Enabled auto-shutdown", ExTextColor.PERSONAL));
        }
      }
    }

  }

  @Override
  public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd,
      Arguments<Argument> args) {
    return List.of();
  }

  @Override
  public void loadCodes(de.timesnake.library.extension.util.chat.Plugin plugin) {
    this.helloPerm = plugin.createPermssionCode("network.hello");
    this.shutdownPerm = plugin.createPermssionCode("network.shutdown");
    this.autoShutdownPerm = plugin.createPermssionCode("network.autoshutdown");
  }
}
