/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.cmd;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.server.BuildServer;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.basic.proxy.util.server.ServerSetupResult;
import de.timesnake.library.basic.util.ServerType;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;

import java.util.Collection;
import java.util.List;

public class MapBuildCmd implements CommandListener {

  private final Code perm = Plugin.NETWORK.createPermssionCode("network.start.build");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    if (!sender.hasPermission(this.perm)) {
      return;
    }

    List<String> worldFiles = Network.getNetworkUtils().getWorldNames(ServerType.BUILD, null);

    if (!args.isLengthEquals(1, true)) {
      return;
    }

    String worldName = args.getString(0);

    if (!worldFiles.contains(worldName)) {
      sender.sendMessageWorldNotExist(worldName);
      return;
    }

    BuildServer buildServer = null;
    boolean worldLoaded = false;

    for (Server server : Network.getServers().stream().filter(s -> s.getType().equals(ServerType.BUILD)).toList()) {
      Collection<String> loadedWorlds = ((BuildServer) server).getDatabase().getWorldNames();
      if (loadedWorlds.contains(worldName)) {
        buildServer = ((BuildServer) server);
        worldLoaded = true;
        break;
      } else if (loadedWorlds.size() < Network.WORLDS_PER_BUILD_SERVER) {
        buildServer = ((BuildServer) server);
      }
    }

    if (!worldLoaded) {
      if (buildServer == null) {
        ServerSetupResult result = Network.getServerManager().createTmpServer(ServerType.BUILD,
            s -> s.configProperty("world-settings.default.entity-tracking-range.players", "128"));

        if (!result.isSuccessful()) {
          sender.sendPluginTDMessage("§wError while creating a build server! Please contact an administrator ("
                                     + ((ServerSetupResult.Fail) result).getReason() + ")");
          return;
        }

        buildServer = (BuildServer) ((ServerSetupResult.Success) result).getServer();
      }

      if (buildServer.getStatus() == Status.Server.OFFLINE) {
        Network.getBukkitCmdHandler().startServer(sender, buildServer);
      }

      boolean worldResult = buildServer.loadWorld(worldName);

      if (!worldResult) {
        sender.sendPluginTDMessage("§wError while loading the world! Please contact an administrator");
        return;
      }
    }

    buildServer.setMaxPlayers(Network.getMaxPlayersBuild());

    if (sender.isPlayer(false)) {
      sender.getUser().addJoinCommand(buildServer.getName(), "mw tp " + worldName);

      if (buildServer.getStatus().equals(Status.Server.ONLINE) || buildServer.getStatus().equals(Status.Server.SERVICE)) {
        sender.sendPluginTDMessage("§sLoaded world §v" + worldName);
        sender.getUser().connect(buildServer.getBungeeInfo());
      } else {
        sender.sendPluginTDMessage("§sLoading world §v" + worldName + "§s. You will be moved in a few moments.");
        sender.getUser().scheduledConnect(buildServer);
      }
    }
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.perm)
        .addArgument(new Completion(Network.getNetworkUtils().getWorldNames(ServerType.BUILD, null)));
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
