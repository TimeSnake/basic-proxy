/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.group;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.util.message.ChannelGroupMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.group.DbPermGroup;
import de.timesnake.database.util.permission.DbPermission;
import de.timesnake.library.basic.util.Loggers;
import de.timesnake.library.permissions.ExPermission;

public class PermGroup extends de.timesnake.library.permissions.PermGroup<User> {

  public PermGroup(DbPermGroup database) {
    super(database);
    Loggers.GROUPS.info("Loaded perm-group '" + this.name + "'");
  }

  @Override
  public void updatePermissions() {
    this.updatePermissions(true);
  }

  public void updatePermissions(boolean updateInheritances) {
    this.permissions.clear();

    for (DbPermission dbPermission : this.database.getPermissions()) {
      this.permissions.add(new ExPermission(dbPermission.getName(), dbPermission.getMode(),
          dbPermission.getServers()));
    }

    DbPermGroup group = this.database.getInheritance();
    if (group != null) {
      this.permissions.addAll(
          Network.getGroupManager().getPermGroup(group.getName()).getPermissions());
    }

    Network.getChannel()
        .sendMessage(new ChannelGroupMessage<>(this.name, MessageType.Group.PERMISSION));
    Loggers.PERMISSIONS.info("Updated permissions for group '" + this.name + "' from database");

    if (updateInheritances) {
      for (DbPermGroup g : this.database.getGroupsInherit()) {
        Network.getGroupManager().getPermGroup(g.getName()).updatePermissions();
      }
    }

    for (User user : this.users) {
      user.updatePermissions(true);
    }
  }
}
