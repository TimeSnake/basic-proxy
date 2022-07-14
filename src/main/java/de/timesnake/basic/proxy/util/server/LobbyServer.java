package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.server.DbLobbyServer;

import java.nio.file.Path;

public class LobbyServer extends Server {

    public LobbyServer(DbLobbyServer database, Path folderPath) {
        super(database, folderPath);
    }
}
