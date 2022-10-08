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

import de.timesnake.database.util.server.DbNonTmpGameServer;
import de.timesnake.library.network.NetworkServer;

import java.nio.file.Path;
import java.util.UUID;

public class NonTmpGameServer extends GameServer {

    private UUID ownerUuid;

    public NonTmpGameServer(DbNonTmpGameServer database, Path folderPath, NetworkServer networkServer) {
        super(database, folderPath, networkServer);
        this.ownerUuid = this.getDatabase().getOwnerUuid();
    }

    @Override
    public DbNonTmpGameServer getDatabase() {
        return (DbNonTmpGameServer) super.getDatabase();
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public void setOwnerUuid(UUID uuid) {
        this.ownerUuid = uuid;
        this.getDatabase().setOwnerUuid(uuid);
    }

    public boolean hasOwner() {
        return this.getOwnerUuid() != null;
    }
}
