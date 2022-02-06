package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.server.DbLobbyServer;

public class LobbyServer extends Server {

    public LobbyServer(DbLobbyServer database, String folderPath) {
        super(database, folderPath);
    }
}
