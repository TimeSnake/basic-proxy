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
import de.timesnake.database.util.group.DbDisplayGroup;

public class DisplayGroup extends de.timesnake.library.extension.util.chat.DisplayGroup<User> {

    public DisplayGroup(DbDisplayGroup database) {
        super(database);
        Network.printText(Plugin.PROXY, "Loaded display-group " + this.name, "Group");
    }
}
