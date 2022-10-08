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

package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.server.DbLoungeServer;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.network.NetworkServer;

import java.nio.file.Path;

public class LoungeServer extends TaskServer {

    public LoungeServer(DbLoungeServer database, Path folderPath, NetworkServer networkServer) {
        super(database, folderPath, networkServer);
    }

    @Override
    public void setStatus(Status.Server status, boolean updateDatabase) {
        super.status = status;
        if (updateDatabase) {
            super.database.setStatus(status);
        }

        if (super.status.equals(Status.Server.OFFLINE)) {
            ((DbLoungeServer) super.database).setTask(null);
        }
    }

    @Override
    public void updateStatus() {
        super.updateStatus();
        if (super.status.equals(Status.Server.OFFLINE)) {
            ((DbLoungeServer) super.database).setTask(null);
        }
    }

    @Override
    public String getServerTask() {
        return this.getType().getDatabaseValue();
    }
}
