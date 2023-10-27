/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.group;

import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.group.DbDisplayGroup;
import de.timesnake.library.basic.util.Loggers;

public class DisplayGroup extends de.timesnake.library.extension.util.chat.DisplayGroup<User> {

  public DisplayGroup(DbDisplayGroup database) {
    super(database);
    Loggers.GROUPS.info("Loaded display-group '" + this.name + "'");
  }
}
