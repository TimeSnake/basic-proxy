/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.server.DbLobbyServer;
import de.timesnake.library.network.NetworkServer;
import java.nio.file.Path;

public class LobbyServer extends Server {

    public LobbyServer(DbLobbyServer database, Path folderPath, NetworkServer networkServer) {
        super(database, folderPath, networkServer);
    }
}
