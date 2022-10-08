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

import de.timesnake.database.util.server.DbTaskServer;
import de.timesnake.library.network.NetworkServer;

import java.nio.file.Path;

public abstract class TaskServer extends Server {

    protected String task;

    protected TaskServer(DbTaskServer database, Path folderPath, NetworkServer networkServer) {
        super(database, folderPath, networkServer);
        this.task = database.getTask();
    }

    public String getTask() {
        return this.task;
    }

    public void setTask(String task) {
        this.task = task;
        ((DbTaskServer) super.database).setTask(task);
    }

    @Override
    public String getServerTask() {
        return this.getTask();
    }

    public void setTaskSynchronized(String task) {
        this.task = task;
        ((DbTaskServer) super.database).setTaskSynchronized(task);
    }
}
