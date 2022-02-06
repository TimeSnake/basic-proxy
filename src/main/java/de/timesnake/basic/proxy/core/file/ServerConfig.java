package de.timesnake.basic.proxy.core.file;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.server.GameServer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ServerConfig {
    private File serverConfigFile;
    private Configuration serverConfig;

    public ServerConfig(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdir();
        }

        this.serverConfigFile = new File(path + "/serverconfig.yml");

        if (!serverConfigFile.exists()) {
            try {
                serverConfigFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        load();

        if (!serverConfig.contains("maxplayers.lobby")) {
            serverConfig.set("maxplayers.lobby", 20);
        }
        if (!serverConfig.contains("maxplayers.build")) {
            serverConfig.set("maxplayers.build", 20);
        }

        for (ServerInfo serverInfo : ProxyServer.getInstance().getServersCopy().values()) {
            if (!serverConfig.contains("servers." + serverInfo.getName() + ".port")) {
                serverConfig.set("servers." + serverInfo.getName() + ".port", serverInfo.getAddress().getPort());
            }

            if (!serverConfig.contains("servers." + serverInfo.getName() + ".type")) {
                serverConfig.set("servers." + serverInfo.getName() + ".type", "type");
            }

            if (!serverConfig.contains("servers." + serverInfo.getName() + ".task")) {
                serverConfig.set("servers." + serverInfo.getName() + ".task", "notask");
            }

            if (!serverConfig.contains("servers." + serverInfo.getName() + ".folderPath")) {
                serverConfig.set("servers." + serverInfo.getName() + ".folderPath", "nopath");
            }
        }

        for (String serverName : serverConfig.getStringList("servers")) {
            if (ProxyServer.getInstance().getServerInfo(serverName) == null) {
                if (serverConfig.contains("servers." + serverName + ".port")) {
                    serverConfig.set("servers." + serverName + ".port", null);
                }

                if (serverConfig.contains("servers." + serverName + ".type")) {
                    serverConfig.set("servers." + serverName + ".type", "");
                }

                if (serverConfig.contains("servers." + serverName + ".task")) {
                    serverConfig.set("servers." + serverName + ".task", "");
                }
                if (serverConfig.contains("servers." + serverName + ".folderPath")) {
                    serverConfig.set("servers." + serverName + ".folderPath", "");
                }
            }
        }

        save();
    }

    public void load() {
        try {
            serverConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(serverConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void save() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(serverConfig, serverConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void loadServers() {
        load();

        for (ServerInfo server : ProxyServer.getInstance().getServersCopy().values()) {
            String serverName = server.getName().toLowerCase();
            switch (serverConfig.getString("servers." + serverName + ".type").toLowerCase()) {
                case "tempgame":
                    Network.addTempGame(serverConfig.getInt("servers." + serverName + ".port"), serverName, serverConfig.getString("servers." + serverName + ".task").toLowerCase(), serverConfig.getString("servers." + serverName + ".folderPath"));
                    System.out.println("[BasicSystem][ServerConfig] Loaded server " + serverName + " successfully");
                    break;
                case "lounge":
                    Network.addLounge(serverConfig.getInt("servers." + serverName + ".port"), serverName, serverConfig.getString("servers." + serverName + ".folderPath"));
                    System.out.println("[BasicSystem][ServerConfig] Loaded server " + serverName + " successfully");
                    break;
                case "game":
                    Network.addGame(serverConfig.getInt("servers." + serverName + ".port"), serverName, serverConfig.getString("servers." + serverName + ".task").toLowerCase(), serverConfig.getString("servers." + serverName + ".folderPath"));
                    System.out.println("[BasicSystem][ServerConfig] Loaded server " + serverName + "successfully");
                    break;
                case "build":
                    Network.addBuild(serverConfig.getInt("servers." + serverName + ".port"), serverName, serverConfig.getString("servers." + serverName + ".folderPath"));
                    System.out.println("[BasicSystem][ServerConfig] Loaded server " + serverName + "successfully");
                    break;
                case "lobby":
                    Network.addLobby(serverConfig.getInt("servers." + serverName + ".port"), serverName, serverConfig.getString("servers." + serverName + ".folderPath"));
                    System.out.println("[BasicSystem][ServerConfig] Loaded server " + serverName + " successfully");
                    break;
                default:
                    System.out.println("[BasicSystem][ServerConfig] Error while reading server-config " + "(" + serverName + ")");
            }
        }
    }

    public String getServerTask(GameServer server) {
        return serverConfig.getString("servers." + server.getName() + ".task").toLowerCase();
    }

    public Integer getMaxPlayersLobby() {
        return serverConfig.getInt("maxplayers.lobby");
    }

    public Integer getMaxPlayersBuild() {
        return serverConfig.getInt("maxplayers.build");
    }
}
