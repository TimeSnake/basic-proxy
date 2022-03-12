package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.object.Type;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class FallbackListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerKickEvent(ServerKickEvent e) {
        if (!e.getCause().equals(ServerKickEvent.Cause.SERVER)) {
            return;
        }

        if (Network.getServer(e.getKickedFrom().getName()).getType().equals(Type.Server.LOBBY)) {
            return;
        }

        User user = Network.getUser(e.getPlayer());

        if (user == null || user.getLobby() == null) {
            return;
        }

        e.setCancelServer(user.getLobby().getBungeeInfo());
        e.setCancelled(true);
    }
}
