/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.cmd;

import de.timesnake.basic.proxy.util.chat.*;
import de.timesnake.library.chat.Chat;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class RuleCmd implements CommandListener {

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    Component text = Chat.getSenderPlugin(Plugin.NETWORK)
        .append(Component.text("https://timesnake.de/rules/", ExTextColor.PERSONAL))
        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL,
            "https://timesnake.de/rules/"))
        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
            Component.text("Click to open")));

    sender.sendMessage(text);
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
