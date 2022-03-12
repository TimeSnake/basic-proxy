package de.timesnake.basic.proxy.core.file;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.file.ExFile;
import de.timesnake.database.util.object.Type;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

public class ServerConfig extends ExFile {

    public ServerConfig() {
        super("basic-proxy", "server_config");
    }

    public void loadServers() {
        this.load();

        for (ServerInfo server : ProxyServer.getInstance().getServersCopy().values()) {
            String serverName = server.getName().toLowerCase();
            String path = ExFile.toPath("servers", serverName);
            int port = super.getInt(ExFile.toPath(path, "port"));
            String typeString = super.getString(ExFile.toPath(path, "type"));
            String task = super.getString(ExFile.toPath(path, "task"));
            String folder = super.getString(ExFile.toPath(path, "folder"));


            Type.Server<?> type = Type.Server.getByDatabaseValue(typeString.toLowerCase());

            if (type == null) {
                Network.printWarning(Plugin.NETWORK, "Error while reading server-config " + "(" + serverName + ")", "ServerConfig");
                continue;
            }

            if (Type.Server.TEMP_GAME.equals(type)) {
                Network.addTempGame(port, serverName, task, folder);
                Network.printText(Plugin.NETWORK, "Loaded server " + serverName, "ServerConfig");
            } else if (Type.Server.LOUNGE.equals(type)) {
                Network.addLounge(port, serverName, folder);
                Network.printText(Plugin.NETWORK, "Loaded server " + serverName, "ServerConfig");
            } else if (Type.Server.GAME.equals(type)) {
                Network.addGame(port, serverName, task, folder);
                Network.printText(Plugin.NETWORK, "Loaded server " + serverName, "ServerConfig");
            } else if (Type.Server.BUILD.equals(type)) {
                Network.addBuild(port, serverName, task, folder);
                Network.printText(Plugin.NETWORK, "Loaded server " + serverName, "ServerConfig");
            } else if (Type.Server.LOBBY.equals(type)) {
                Network.addLobby(port, serverName, folder);
                Network.printText(Plugin.NETWORK, "Loaded server " + serverName, "ServerConfig");
            } else {
                Network.printWarning(Plugin.NETWORK, "Error while reading server-config " + "(" + serverName + ")", "ServerConfig");
            }
        }
    }

    public Integer getMaxPlayersLobby() {
        return super.getInt("max_players.lobby");
    }

    public Integer getMaxPlayersBuild() {
        return super.getInt("max_players.build");
    }
}
