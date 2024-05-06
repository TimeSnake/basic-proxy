/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.cmd;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.*;
import de.timesnake.library.chat.Chat;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import de.timesnake.library.network.NetworkVariables;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class RuleCmd implements CommandListener {

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    sender.sendMessage(Chat.getSenderPlugin(Plugin.NETWORK)
        .append(Component.text(NetworkVariables.RULES_LINK, ExTextColor.PERSONAL))
        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL,
            Network.getVariables().getValue(NetworkVariables.RULES_LINK)))
        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click to open"))));
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion();
  }

  @Override
  public String getPermission() {
    return null;
  }
}
