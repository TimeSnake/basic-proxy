/*
 * basic-proxy.main
 * Copyright (C) 2022 timesnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package de.timesnake.basic.proxy.core.group;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.util.message.ChannelGroupMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.group.DbPermGroup;
import de.timesnake.database.util.permission.DbPermission;
import de.timesnake.library.extension.util.permission.ExPermission;

public class PermGroup extends de.timesnake.library.extension.util.permission.PermGroup<User> {

    public PermGroup(DbPermGroup database) {
        super(database);
        Network.printText(Plugin.PROXY, "Loaded perm-group " + this.name, "Group");
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
            this.permissions.addAll(Network.getPermGroup(group.getName()).getPermissions());
        }

        Network.getChannel().sendMessage(new ChannelGroupMessage<>(this.name, MessageType.Group.PERMISSION));
        Network.printText(Plugin.PERMISSION, "Updated permissions for group " + this.name + " from database");

        if (updateInheritances) {
            for (DbPermGroup g : this.database.getGroupsInherit()) {
                Network.getPermGroup(g.getName()).updatePermissions();
            }
        }

        for (User user : this.users) {
            user.updatePermissions(true);
        }
    }
}
