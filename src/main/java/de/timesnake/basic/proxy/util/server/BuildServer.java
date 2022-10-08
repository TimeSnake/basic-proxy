/*
 * basic-proxy.main
 * Copyright (C) 2022 timesnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
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
        return super.getType().getDatabaseValue();
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
