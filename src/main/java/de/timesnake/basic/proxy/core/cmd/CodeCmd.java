/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.cmd;

import de.timesnake.basic.proxy.util.chat.*;
import de.timesnake.library.chat.Code;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.extended.ExArguments;

public class CodeCmd implements ExCommandListener {

  private final Code perm = Plugin.NETWORK.createPermssionCode("system.code");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, ExArguments<Argument> args) {
    sender.hasPermissionElseExit(this.perm);
    args.assertElseExit(a -> a.isLengthEquals(1, true));

    Code code = Code.getCodeById().get(args.get(0).toIntOrExit(true));

    if (code == null) {
      sender.sendPluginTDMessage("§wNo valid code");
      return;
    }

    String message = "§sType: §v" + code.getType().getSymbol() + "§s, plugin: §v" + code.getPlugin().getCode();

    if (code.getCommand() != null) {
      message += "§s, cmd: §v" + code.getCommand();
    }

    if (code.getPermission() != null) {
      message += "§s, perm: §v" + code.getPermission();
    }

    if (code.getDescription() != null) {
      message += "§s, desc: §v" + code.getDescription();
    }

    if (args.containsFlag('v')) {
      if (code.getReference() != null) {
        message += "§s, ref: §v" + code.getReference().getName();
      }
    }

    sender.sendPluginTDMessage(message);
  }

  @Override
  public ExCompletion getTabCompletion() {
    return new ExCompletion(this.perm);
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
