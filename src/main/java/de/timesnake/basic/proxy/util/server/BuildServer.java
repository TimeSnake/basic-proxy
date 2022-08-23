package de.timesnake.basic.proxy.util.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.server.DbBuildServer;
import de.timesnake.library.basic.util.chat.Plugin;
import de.timesnake.library.network.NetworkServer;
import de.timesnake.library.network.WorldSyncResult;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class BuildServer extends TaskServer {

    private final List<String> loadedWorlds = new LinkedList<>();

    public BuildServer(DbBuildServer database, Path folderPath, NetworkServer networkServer) {
        super(database, folderPath, networkServer);
    }

    @Override
    public String getServerTask() {
        return super.getType().getDatabaseValue();
    }

    public List<String> getLoadedWorlds() {
        return loadedWorlds;
    }

    public boolean loadWorld(String worldName) {
        WorldSyncResult result = Network.getNetworkUtils().syncWorld(this.networkServer, worldName);

        if (!result.isSuccessful()) {
            Network.printWarning(Plugin.NETWORK, ((WorldSyncResult.Fail) result).getReason());
            return false;
        }

        Network.getChannel().sendMessage(new ChannelServerMessage<>(this.getPort(), MessageType.Server.LOAD_WORLD, worldName));

        this.loadedWorlds.add(worldName);
        return true;
    }
}
