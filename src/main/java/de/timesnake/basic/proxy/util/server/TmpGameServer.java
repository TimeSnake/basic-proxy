/*
 * workspace.basic-proxy.main
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

import de.timesnake.database.util.server.DbLoungeServer;
import de.timesnake.database.util.server.DbTmpGameServer;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.network.NetworkServer;

import java.nio.file.Path;

public class TmpGameServer extends PvPServer {

    protected boolean kitsEnabled;
    protected boolean mapsEnabled;
    protected Integer teamAmount;
    protected boolean teamMerging;
    protected Integer maxPlayersPerTeam;
    private DbLoungeServer twinServer;

    public TmpGameServer(DbTmpGameServer database, Path folderPath, NetworkServer networkServer) {
        super(database, folderPath, networkServer);
        this.twinServer = database.getTwinServer();
    }

    public DbLoungeServer getTwinServer() {
        return twinServer;
    }

    public void setTwinServer(DbLoungeServer twinServer) {
        this.twinServer = twinServer;
        if (twinServer != null) {
            ((DbTmpGameServer) this.database).setTwinServerName(twinServer.getName());
        } else {
            ((DbTmpGameServer) this.database).setTwinServerName(null);
        }

    }

    @Override
    public void setStatus(Status.Server status, boolean updateDatabase) {
        super.setStatus(status, updateDatabase);
        if (status.equals(Status.Server.OFFLINE)) {
            if (this.getTwinServer() != null && this.getTwinServer().exists()) {
                if (this.getTwinServer().getStatus().equals(Status.Server.OFFLINE)) {
                    this.setTwinServer(null);
                }
            }
        }
    }

    public Integer getTeamAmount() {
        return teamAmount;
    }

    public void setTeamAmount(Integer number) {
        this.teamAmount = number;
        ((DbTmpGameServer) super.database).setTeamAmount(number);
    }

    public boolean isTeamMerging() {
        return teamMerging;
    }

    public void setTeamMerging(boolean teamMerging) {
        this.teamMerging = teamMerging;
        ((DbTmpGameServer) super.database).setTeamMerging(teamMerging);
    }

    public void setMapsEnabled(boolean mapsEnabled) {
        this.mapsEnabled = mapsEnabled;
        ((DbTmpGameServer) super.database).setMapsEnabled(mapsEnabled);
    }

    public boolean areMapsEnabled() {
        return mapsEnabled;
    }

    public void setKitsEnabled(boolean kitsEnabled) {
        this.kitsEnabled = kitsEnabled;
        ((DbTmpGameServer) super.database).setKitsEnabled(this.kitsEnabled);
    }

    public boolean areKitsEnabled() {
        return kitsEnabled;
    }

    public Integer getMaxPlayersPerTeam() {
        return maxPlayersPerTeam;
    }

    public void setMaxPlayersPerTeam(Integer maxPlayersPerTeam) {
        this.maxPlayersPerTeam = maxPlayersPerTeam;
        ((DbTmpGameServer) super.database).setMaxPlayersPerTeam(maxPlayersPerTeam);
    }
}
