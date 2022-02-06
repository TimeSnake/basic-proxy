package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.object.Status;
import de.timesnake.database.util.server.DbLoungeServer;
import de.timesnake.database.util.server.DbTempGameServer;

public class TempGameServer extends PvPServer {

    private DbLoungeServer twinServer;
    protected boolean kitsEnabled;
    protected boolean mapsEnabled;
    protected Integer teamAmount;
    protected boolean teamMerging;
    protected Integer maxPlayersPerTeam;

    public TempGameServer(DbTempGameServer database, String folderPath) {
        super(database, folderPath);
        this.twinServer = database.getTwinServer();
    }

    public DbLoungeServer getTwinServer() {
        return twinServer;
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

    public void setTwinServer(DbLoungeServer twinServer) {
        this.twinServer = twinServer;
        if (twinServer != null) {
            ((DbTempGameServer) this.database).setTwinServerPort(twinServer.getPort());
        } else {
            ((DbTempGameServer) this.database).setTwinServerPort(null);
        }

    }

    public void setTeamAmount(Integer number) {
        this.teamAmount = teamAmount;
        ((DbTempGameServer) super.database).setTeamAmount(number);
    }

    public Integer getTeamAmount() {
        return teamAmount;
    }

    public void setTeamMerging(boolean teamMerging) {
        this.teamMerging = teamMerging;
        ((DbTempGameServer) super.database).setTeamMerging(teamMerging);
    }

    public boolean isTeamMerging() {
        return teamMerging;
    }

    public void setMapsEnabled(boolean mapsEnabled) {
        this.mapsEnabled = mapsEnabled;
        ((DbTempGameServer) super.database).setMapsEnabled(mapsEnabled);
    }

    public boolean areMapsEnabled() {
        return mapsEnabled;
    }

    public void setKitsEnabled(boolean kitsEnabled) {
        this.kitsEnabled = kitsEnabled;
        ((DbTempGameServer) super.database).setKitsEnabled(this.kitsEnabled);
    }

    public boolean areKitsEnabled() {
        return kitsEnabled;
    }

    public Integer getMaxPlayersPerTeam() {
        return maxPlayersPerTeam;
    }

    public void setMaxPlayersPerTeam(Integer maxPlayersPerTeam) {
        this.maxPlayersPerTeam = maxPlayersPerTeam;
        ((DbTempGameServer) super.database).setMaxPlayersPerTeam(maxPlayersPerTeam);
    }
}