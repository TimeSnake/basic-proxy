/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.support;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.chat.Code;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;

public class TicketCmd implements CommandListener {

  private final Code perm = SupportManager.PLUGIN.createPermssionCode("support.ticket");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    sender.isPlayerElseExit(true);
    sender.hasPermissionElseExit(this.perm);
    args.isLengthHigherEqualsElseExit(1, true);

    String message = args.toMessage();

    int ticketId = Network.getSupportManager().createTicket(sender.getUser(), message);
    sender.sendPluginTDMessage("§sYour request §v#" + ticketId + "§s will be processed as soon as possible");
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.perm)
        .addArgument(new Completion("<message>"));
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
