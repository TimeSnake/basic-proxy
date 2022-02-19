package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.database.util.object.Type;
import de.timesnake.library.basic.util.Status;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FallbackListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerKickEvent(ServerKickEvent e) {

        if (!e.getCause().equals(ServerKickEvent.Cause.SERVER)) {
            return;
        }

        if (Network.getServer(e.getKickedFrom().getName()).getType().equals(Type.Server.LOBBY)) {
            return;
        }

        List<Server> fallbacks = new ArrayList<>();

        for (Server server : Network.getServers()) {
            if (server.getType().equals(Type.Server.LOBBY)) {
                if (server.getStatus().equals(Status.Server.ONLINE) && server.getOnlinePlayers() < server.getMaxPlayers()) {
                    fallbacks.add(server);
                }
            }
        }

        if (!fallbacks.isEmpty()) {
            e.setCancelServer(fallbacks.get(new Random().nextInt(fallbacks.size())).getBungeeInfo());
            e.setCancelled(true);
        }
    }
}
