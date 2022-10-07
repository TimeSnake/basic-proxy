package de.timesnake.basic.proxy.core.file;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.file.ExFile;
import de.timesnake.database.util.object.Type;
import de.timesnake.library.network.NetworkServer;

import java.nio.file.Path;

public class ServerConfig extends ExFile {

    public ServerConfig() {
        super("basic-proxy", "server_config.toml");
    }

    public void loadServers() {
        this.load();

        for (RegisteredServer server : BasicProxy.getServer().getAllServers()) {
            String serverName = server.getServerInfo().getName();
            String path = ExFile.toPath("servers", serverName);
            if (!this.config.containsTable("servers." + serverName)) {
                continue;
            }

            int port = super.getLong(ExFile.toPath(path, "port")).intValue();
            String typeString = super.getString(ExFile.toPath(path, "type"));
            String task = super.getString(ExFile.toPath(path, "task"));
            Path folder = Network.getNetworkPath().resolve(super.getString(ExFile.toPath(path, "folder")));


            Type.Server<?> type = Type.Server.getByDatabaseValue(typeString.toLowerCase());

            if (type == null) {
                Network.printWarning(Plugin.NETWORK, "Error while reading server-config " + "(" + serverName + ")",
                        "ServerConfig");
                continue;
            }

            NetworkServer networkServer = new NetworkServer(serverName, port, type, Network.getVelocitySecret());

            if (Type.Server.TEMP_GAME.equals(type)) {
                Network.addTempGame(port, serverName, task, folder, networkServer);
            } else if (Type.Server.LOUNGE.equals(type)) {
                Network.addLounge(port, serverName, folder, networkServer);
            } else if (Type.Server.GAME.equals(type)) {
                Network.addGame(port, serverName, task, folder, networkServer);
            } else if (Type.Server.BUILD.equals(type)) {
                Network.addBuild(port, serverName, task, folder, networkServer);
            } else if (Type.Server.LOBBY.equals(type)) {
                Network.addLobby(port, serverName, folder, networkServer);
            }

            Network.printText(Plugin.NETWORK, "Loaded server " + serverName, "ServerConfig");
        }
    }

    public Integer getMaxPlayersLobby() {
        return super.getLong("max_players.lobby").intValue();
    }

    public Integer getMaxPlayersBuild() {
        return super.getLong("max_players.build").intValue();
    }
}
