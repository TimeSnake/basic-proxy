/*
 * Copyright (C) 2023 timesnake
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
