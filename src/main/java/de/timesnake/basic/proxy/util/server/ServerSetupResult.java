/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.server;

import de.timesnake.library.network.NetworkServer;
import de.timesnake.library.network.ServerCreationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ServerSetupResult {

  ServerCreationResult getCreationResult();

  NetworkServer getNetworkServer();

  boolean isSuccessful();

  class Success implements ServerSetupResult {

    private final ServerCreationResult.Success creationResult;
    private final NetworkServer networkServer;
    private final Server server;

    public Success(@NotNull ServerCreationResult.Success serverCreationResult, @NotNull NetworkServer networkServer,
                   @NotNull Server server) {
      this.creationResult = serverCreationResult;
      this.networkServer = networkServer;
      this.server = server;
    }

    @Override
    public @NotNull ServerCreationResult.Success getCreationResult() {
      return creationResult;
    }

    public @NotNull NetworkServer getNetworkServer() {
      return networkServer;
    }

    public @NotNull Server getServer() {
      return server;
    }

    @Override
    public boolean isSuccessful() {
      return true;
    }
  }

  class Fail implements ServerSetupResult {

    private final ServerCreationResult creationResult;
    private final NetworkServer networkServer;
    private final String reason;

    public Fail(@Nullable ServerCreationResult creationResult, @Nullable NetworkServer networkServer,
                @NotNull String reason) {
      this.creationResult = creationResult;
      this.networkServer = networkServer;
      this.reason = reason;
    }

    public @Nullable ServerCreationResult getCreationResult() {
      return creationResult;
    }

    @Override
    public @Nullable NetworkServer getNetworkServer() {
      return networkServer;
    }

    public @NotNull String getReason() {
      return this.creationResult == null || this.creationResult.isSuccessful() ? reason :
          (reason + ";" + ((ServerCreationResult.Fail) this.creationResult).getReason());
    }

    @Override
    public boolean isSuccessful() {
      return false;
    }
  }
}
