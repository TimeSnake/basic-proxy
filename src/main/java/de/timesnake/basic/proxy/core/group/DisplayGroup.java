/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.group;

import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.group.DbDisplayGroup;

public class DisplayGroup extends de.timesnake.library.permissions.DisplayGroup<User> {

  public DisplayGroup(DbDisplayGroup database) {
    super(database);
    this.logger.info("Loaded display-group '{}'", this.name);
  }
}
