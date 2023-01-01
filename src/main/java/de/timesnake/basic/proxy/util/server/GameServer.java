/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.server.DbNonTmpGameServer;
import de.timesnake.library.network.NetworkServer;
import java.nio.file.Path;

public abstract class GameServer extends TaskServer {

    public GameServer(DbNonTmpGameServer database, Path folderPath, NetworkServer networkServer) {
        super(database, folderPath, networkServer);
    }

}
