/*
 * Copyright (C) 2022 timesnake
 */

package de.timesnake.basic.proxy.core.cmd;

import static de.timesnake.library.basic.util.chat.ExTextColor.WARNING;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.server.BuildServer;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.database.util.object.Type;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import de.timesnake.library.network.NetworkServer;
import de.timesnake.library.network.ServerCreationResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;

public class MapBuildCmd implements CommandListener<Sender, Argument> {

    private Code.Permission perm;

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (!sender.hasPermission(this.perm)) {
            return;
        }

        List<String> worldNames = Network.getNetworkUtils().getWorldNames(Type.Server.BUILD, null);

        if (!args.isLengthEquals(1, true)) {
            return;
        }

        String worldName = args.getString(0);

        if (!worldNames.contains(worldName)) {
            sender.sendMessageWorldNotExist(worldName);
            return;
        }

        BuildServer buildServer = null;
        boolean worldLoaded = false;

        for (Server server : Network.getServers().stream().filter(s -> s.getType().equals(Type.Server.BUILD)).toList()) {
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
                int port = Network.nextEmptyPort();
                Tuple<ServerCreationResult, Optional<Server>> result =
                        Network.createTmpServer(new NetworkServer("build" + (port % 1000), port, Type.Server.BUILD,
                                Network.getVelocitySecret()).setPlayerTrackingRange(128), false, true);

                if (!result.getA().isSuccessful()) {
                    sender.sendPluginMessage(Component.text("Error while creating a" +
                            " build server! Please contact an administrator (" +
                            ((ServerCreationResult.Fail) result.getA()).getReason() + ")", WARNING));
                    return;
                }

                buildServer = ((BuildServer) result.getB().get());

            }

            if (buildServer.getStatus() == Status.Server.OFFLINE) {
                Network.getBukkitCmdHandler().handleServerCmd(sender, buildServer);
            }

            boolean worldResult = buildServer.loadWorld(worldName);

            if (!worldResult) {
                sender.sendPluginMessage(Component.text("Error while loading" +
                        " the world! Please contact an administrator", WARNING));
                return;
            }
        }

        buildServer.setMaxPlayers(Network.getMaxPlayersBuild());

        if (sender.isPlayer(false)) {
            if (buildServer.getStatus().equals(Status.Server.ONLINE) || buildServer.getStatus().equals(Status.Server.SERVICE)) {
                sender.sendPluginMessage(Component.text("Loaded world ", ExTextColor.PERSONAL)
                        .append(Component.text(worldName, ExTextColor.VALUE)));
                sender.getUser().connect(buildServer.getBungeeInfo());
            } else {
                sender.sendPluginMessage(Component.text("Loading world ", ExTextColor.PERSONAL)
                        .append(Component.text(worldName, ExTextColor.VALUE))
                        .append(Component.text(". You will be moved in a few moments.", ExTextColor.PERSONAL)));
                sender.getUser().scheduledConnect(buildServer);
            }
        }

        Network.printText(Plugin.SYSTEM, "Loaded world '" + worldName + "' on server '" + buildServer.getName() +
                "' by user " + sender.getName());
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (args.length() == 1) {
            return Network.getNetworkUtils().getWorldNames(Type.Server.BUILD, null);
        }
        return List.of();
    }

    @Override
    public void loadCodes(Plugin plugin) {
        this.perm = plugin.createPermssionCode("prx", "network.start.build");
    }
}
