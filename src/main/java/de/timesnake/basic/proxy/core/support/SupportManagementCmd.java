/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.support;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.database.util.support.DbTicket;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.chat.Code;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;

import java.util.Arrays;

public class SupportManagementCmd implements CommandListener {

  private final Code perm = Network.PLUGIN_SUPPORT.createPermssionCode("support.manage");
  private final Code showPerm = Network.PLUGIN_SUPPORT.createPermssionCode("support.show");
  private final Code statusPerm = Network.PLUGIN_SUPPORT.createPermssionCode("support.status");
  private final Code answerPerm = Network.PLUGIN_SUPPORT.createPermssionCode("support.answer");
  private final Code notifyPerm = Network.PLUGIN_SUPPORT.createPermssionCode("support.notify");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    sender.hasPermissionElseExit(this.perm);

    args.isLengthHigherEqualsElseExit(1, true);

    Argument action = args.get(0);

    if (action.equalsIgnoreCase("show")) {
      sender.hasPermissionElseExit(this.showPerm);

      if (args.isLengthEquals(1, false)) {
        for (DbTicket ticket : Network.getSupportManager().getNewestTickets(3)) {
          this.sendTicketMessage(sender, ticket);
        }
        return;
      }

      Argument filter = args.get(1);

      if (filter.isTicketStatus(false)) {
        Status.Ticket status = filter.toTicketStatusOrExit(true);
        for (DbTicket ticket : Network.getSupportManager().getNewestTickets(status, 3)) {
          this.sendTicketMessage(sender, ticket);
        }
      } else if (filter.isInt(false)) {
        Integer id = filter.toIntOrExit(true);
        DbTicket ticket = Network.getSupportManager().getTicketById(id);
        if (ticket == null) {
          sender.sendPluginTDMessage("§wNo ticket found for id §v" + id);
          return;
        }

        this.sendTicketMessage(sender, ticket);
      }

      return;
    } else if (action.equalsIgnoreCase("msg", "notify")) {
      sender.hasPermissionElseExit(this.notifyPerm);
      sender.isPlayerElseExit(true);
      boolean result = Network.getSupportManager().toggleTicketListener(sender.getUser().getUniqueId());
      sender.sendPluginTDMessage("§s" + (result ? "Enabled" : "Disabled") + " ticket notifications");
      return;
    }

    args.isLengthHigherEqualsElseExit(2, true);
    Integer id = args.get(1).toIntOrExit(true);
    DbTicket ticket = Network.getSupportManager().getTicketById(id);

    if (ticket == null) {
      sender.sendPluginTDMessage("§wNo ticket found for id §v" + id);
      return;
    }

    if (action.equalsIgnoreCase("status")) {
      sender.hasPermissionElseExit(this.statusPerm);
      args.isLengthEqualsElseExit(3, true);
      Status.Ticket status = args.get(2).toTicketStatusOrExit(true);
      ticket.setStatus(status);
      Network.getSupportManager().notifyTicketUpdate(id);
      sender.sendPluginTDMessage("§sUpdated status of ticket §v" + id + "§s to §v" + status.getDisplayName());
    } else if (action.equalsIgnoreCase("answer", "response")) {
      sender.hasPermissionElseExit(this.answerPerm);
      args.isLengthHigherEquals(3, true);
      String message = args.toMessage(2);
      ticket.setAnswer(message);
      Network.getSupportManager().notifyTicketUpdate(id);
      sender.sendPluginTDMessage("§sUpdated answer of ticket §v" + id + "§s to §v" + message);
    }
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.perm)
        .addArgument(new Completion(this.showPerm, "show"))
        .addArgument(new Completion(this.notifyPerm, "notify", "msg"))
        .addArgument(new Completion(this.statusPerm, "status")
            .addArgument(new Completion("<ticket id>")
                .addArgument(new Completion(Arrays.stream(Status.Ticket.values()).map(Status.Ticket::getName).toList()))))
        .addArgument(new Completion(this.answerPerm, "answer", "response")
            .addArgument(new Completion("<ticket id>")
                .addArgument(new Completion("<answer>"))));
  }

  @Override
  public String getPermission() {
    return "";
  }

  private void sendTicketMessage(Sender sender, DbTicket ticket) {
    sender.sendPluginTDMessage("§v#" + ticket.getId() + " " + ticket.getStatus().getDisplayName()
                               + "§s(" + SupportManager.formatDateTime(ticket.getDate())
                               + "§s) by §v" + ticket.getName() + "§s: ");
    sender.sendPluginTDMessage("§sMessage: §v" + ticket.getMessage());
    if (ticket.getAnswer() != null) {
      sender.sendPluginTDMessage("§sAnswer: §v" + ticket.getAnswer());
    }
  }
}
