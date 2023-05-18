/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.user;

import de.timesnake.basic.proxy.core.group.DisplayGroup;
import de.timesnake.basic.proxy.core.group.PermGroup;
import de.timesnake.basic.proxy.core.user.UserNotInDatabaseException;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.group.DbPermGroup;
import de.timesnake.database.util.permission.DbPermission;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.extension.util.permission.ExPermission;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import net.kyori.adventure.text.Component;

public class PreUser {

  private final UUID uuid;

  private final String name;

  private final DbUser dbUser;

  private final boolean service;

  private final boolean airMode;

  private final LocalDateTime privacyPolicyDateTime;

  private final PermGroup permGroup;
  private final SortedSet<DisplayGroup> displayGroups;

  private final Component prefix;
  private final Component suffix;
  private final Component nick;

  private final float coins;

  private final Collection<ExPermission> databasePermissions = new HashSet<>();

  public PreUser(String name) throws UserNotInDatabaseException {
    if (!Database.getUsers().containsUser(name)) {
      throw new UserNotInDatabaseException();
    }

    this.dbUser = Database.getUsers().getUser(name);

    this.uuid = dbUser.getUniqueId();

    DbUser dbLocalUser = this.dbUser.toLocal();

    this.name = dbLocalUser.getName();

    this.airMode = dbLocalUser.isAirMode();

    this.prefix = Chat.parseStringToComponent(dbLocalUser.getPrefix());

    this.suffix = Chat.parseStringToComponent(dbLocalUser.getSuffix());

    this.nick = Chat.parseStringToComponent(dbLocalUser.getNick());

    DbPermGroup dbGroup = dbLocalUser.getPermGroup();
    String permGroupName;
    if (dbGroup != null) {
      permGroupName = dbGroup.getName();
    } else {
      permGroupName = Network.getGroupManager().getGuestPermGroup().getName();
    }

    this.permGroup = Network.getGroupManager().getPermGroup(permGroupName);

    this.displayGroups = new TreeSet<>(Comparator.comparingInt(DisplayGroup::getRank));
    for (String groupName : dbLocalUser.getDisplayGroupNames()) {
      this.displayGroups.add(Network.getGroupManager().getDisplayGroup(groupName));
    }

    if (this.displayGroups.isEmpty()) {
      this.displayGroups.add(Network.getGroupManager().getGuestDisplayGroup());
    }

    this.service = dbLocalUser.isService();

    this.coins = dbLocalUser.getCoins();

    this.privacyPolicyDateTime = dbLocalUser.getPrivacyPolicyDateTime();

    for (DbPermission perm : dbLocalUser.getPermissions()) {
      this.databasePermissions.add(
          new ExPermission(perm.getName(), perm.getMode(), perm.getServers()));
    }
    this.databasePermissions.addAll(this.permGroup.getPermissions());
  }

  public UUID getUuid() {
    return uuid;
  }

  public String getName() {
    return name;
  }

  public DbUser getDbUser() {
    return dbUser;
  }

  public boolean isService() {
    return service;
  }

  public boolean isAirMode() {
    return airMode;
  }

  public LocalDateTime getPrivacyPolicyDateTime() {
    return privacyPolicyDateTime;
  }

  public PermGroup getPermGroup() {
    return permGroup;
  }

  public SortedSet<DisplayGroup> getDisplayGroups() {
    return displayGroups;
  }

  public DisplayGroup getMainDisplayGroup() {
    return displayGroups.first();
  }

  public Component getPrefix() {
    return prefix;
  }

  public Component getSuffix() {
    return suffix;
  }

  public Component getNick() {
    return nick;
  }

  public float getCoins() {
    return coins;
  }

  public Collection<ExPermission> getDatabasePermissions() {
    return databasePermissions;
  }
}
