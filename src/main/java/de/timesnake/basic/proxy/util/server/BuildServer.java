/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.server.DbBuildServer;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.network.NetworkServer;
import de.timesnake.library.network.WorldSyncResult;
import java.nio.file.Path;

public class BuildServer extends TaskServer {

    public BuildServer(DbBuildServer database, Path folderPath, NetworkServer networkServer) {
        super(database, folderPath, networkServer);
    }

    @Override
    public DbBuildServer getDatabase() {
        return (DbBuildServer) super.getDatabase();
    }

    @Override
    public String getServerTask() {
        return super.getType().getShortName();
    }

    public boolean loadWorld(String worldName) {
        WorldSyncResult result = Network.getNetworkUtils().syncWorld(this.networkServer, worldName);

        if (!result.isSuccessful()) {
            Network.printWarning(Plugin.NETWORK, ((WorldSyncResult.Fail) result).getReason());
            return false;
        }

        Network.getChannel().sendMessage(new ChannelServerMessage<>(this.getName(), MessageType.Server.LOAD_WORLD, worldName));
        return true;
    }
}
