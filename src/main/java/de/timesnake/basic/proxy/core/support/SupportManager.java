/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.support;

import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.support.DbTicket;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.UserSet;
import de.timesnake.library.chat.Plugin;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SupportManager {

  public static final Plugin PLUGIN = new Plugin("Support", "PSS");

  public static String formatDateTime(LocalDateTime dateTime) {
    return DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss").format(dateTime.atZone(ZoneId.systemDefault()));
  }

  private final HashMap<Integer, DbTicket> cachedTicketById = new HashMap<>();

  private final UserSet<UUID> ticketListeners = new UserSet<>();

  public SupportManager() {
    this.refreshTickets();
  }

  public void refreshTickets() {
    for (DbTicket ticket : Database.getSupport().getTickets()) {
      this.cachedTicketById.put(ticket.getId(), ticket.toLocal());
    }
  }

  public boolean toggleTicketListener(UUID uuid) {
    if (this.ticketListeners.contains(uuid)) {
      this.ticketListeners.remove(uuid);
      return false;
    } else {
      this.ticketListeners.add(uuid);
      return true;
    }
  }

  public DbTicket getTicketById(int id) {
    return this.cachedTicketById.get(id);
  }

  public Collection<DbTicket> getTickets() {
    return this.cachedTicketById.values();
  }

  public List<DbTicket> getNewestTickets(int size) {
    return this.cachedTicketById.values().stream()
        .sorted(Comparator.comparing(DbTicket::getDate))
        .limit(size)
        .toList();
  }

  public List<DbTicket> getNewestTickets(Status status, int size) {
    return this.cachedTicketById.values().stream()
        .filter(t -> t.getStatus().equals(status))
        .sorted(Comparator.comparing(DbTicket::getDate))
        .limit(size)
        .toList();
  }

  public int createTicket(User user, String message) {
    DbTicket ticket = Database.getSupport().addTicket(user.getUniqueId().toString(), user.getName(), message).toLocal();
    this.cachedTicketById.put(ticket.getId(), ticket);
    return ticket.getId();
  }

  public void notifyTicketUpdate(Integer id) {

  }
}
