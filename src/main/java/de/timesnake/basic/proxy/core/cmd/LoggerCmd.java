/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.cmd;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.basic.util.LogHelper;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import de.timesnake.library.permissions.Permission;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class LoggerCmd implements CommandListener {

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    sender.isConsoleElseExit(true);
    args.isLengthEqualsElseExit(2, true);

    String loggerName = args.getString(0);
    Logger logger = LogHelper.LOGGER_BY_NAME.get(loggerName);

    if (logger == null) {
      sender.sendPluginTDMessage("§wLogger §v" + loggerName + "§w not found");
      return;
    }

    String levelName = args.getString(1).toUpperCase();

    Level level;
    try {
      level = Level.parse(levelName);
    } catch (IllegalArgumentException e) {
      sender.sendPluginTDMessage("§wUnable to parse log-level §v" + loggerName);
      return;
    }

    logger.setLevel(level);
    logger.setUseParentHandlers(level == Level.INFO);
    sender.sendPluginTDMessage("Updated log-level of §v" + loggerName + "§s to §v" + levelName);
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion((sender, cmd, args) -> sender.isConsole(false) ? LogHelper.LOGGER_BY_NAME.keySet() : List.of())
        .addArgument(new Completion(Stream.of(Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, Level.ALL)
            .map(Level::getName).toList()));
  }

  @Override
  public String getPermission() {
    return Permission.CONSOLE_PERM;
  }
}
