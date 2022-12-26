/*
 * Copyright (C) 2022 timesnake
 */

package de.timesnake.basic.proxy.core.group;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.group.DbDisplayGroup;

public class DisplayGroup extends de.timesnake.library.extension.util.chat.DisplayGroup<User> {

    public DisplayGroup(DbDisplayGroup database) {
        super(database);
        Network.printText(Plugin.PROXY, "Loaded display-group " + this.name, "Group");
    }
}
