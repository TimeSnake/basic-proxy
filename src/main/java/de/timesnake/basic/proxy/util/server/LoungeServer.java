/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.server.DbLoungeServer;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.network.NetworkServer;
import java.nio.file.Path;

public class LoungeServer extends TaskServer {

  public LoungeServer(DbLoungeServer database, Path folderPath, NetworkServer networkServer) {
    super(database, folderPath, networkServer);
  }

  @Override
  public void setStatus(Status.Server status, boolean updateDatabase) {
    super.status = status;
    if (updateDatabase) {
      super.database.setStatus(status);
    }

    if (super.status.equals(Status.Server.OFFLINE)) {
      ((DbLoungeServer) super.database).setTask(null);
    }
  }

  @Override
  public void updateStatus() {
    super.updateStatus();
    if (super.status.equals(Status.Server.OFFLINE)) {
      ((DbLoungeServer) super.database).setTask(null);
    }
  }

  @Override
  public String getServerTask() {
    return this.getType().getShortName();
  }
}
