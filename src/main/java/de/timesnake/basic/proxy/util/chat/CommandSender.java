/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.chat;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import net.kyori.adventure.text.Component;

public class CommandSender implements de.timesnake.library.commands.CommandSender {

  private final CommandSource cmdSender;

  public CommandSender(CommandSource cmdSender) {
    this.cmdSender = cmdSender;
  }

  @Override
  public void sendMessage(String s) {
    this.cmdSender.sendMessage(Component.text(s));
  }

  @Override
  public void sendMessage(String[] messages) {
    for (String s : messages) {
      this.sendMessage(s);
    }
  }

  @Override
  public void sendMessage(Component... components) {
    for (Component component : components) {
      this.cmdSender.sendMessage(component);
    }
  }

  public void sendMessage(Component message) {
    this.cmdSender.sendMessage(message);
  }

  @Override
  public String getName() {
    if (cmdSender instanceof ConsoleCommandSource) {
      return "console";
    } else if (cmdSender instanceof Player) {
      return ((Player) cmdSender).getUsername();
    }
    return "unknown";
  }

  @Override
  public boolean hasPermission(String s) {
    return this.cmdSender.hasPermission(s);
  }

  @Override
  public boolean isConsole() {
    return cmdSender instanceof ConsoleCommandSource;
  }

  @Override
  public Player getPlayer() {
    return (Player) this.cmdSender;
  }

  @Override
  public User getUser() {
    return this.cmdSender instanceof Player ? Network.getUser((Player) this.cmdSender) : null;
  }
}
