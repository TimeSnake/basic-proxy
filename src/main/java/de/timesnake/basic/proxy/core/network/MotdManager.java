package de.timesnake.basic.proxy.core.network;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.database.util.object.Type;
import de.timesnake.library.basic.util.Status;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class MotdManager implements Listener {

    private static final String MOTD = "Have fun!";

    @EventHandler
    public void onProxyPing(ProxyPingEvent e) {
        if (Network.isWork()) {
            ServerPing conn = e.getResponse();
            conn.setVersion(new ServerPing.Protocol(net.md_5.bungee.api.ChatColor.RED + "Service-Work", 2));
            conn.setDescriptionComponent(new TextComponent("§6Time§2Snake§7-§9Network§c!" + "                                         §cService-Work"));
            e.setResponse(conn);
        } else {
            for (Server server : Network.getServers()) {
                if (server.getType().equals(Type.Server.LOBBY)) {
                    if (server.getStatus() != null && server.getStatus().equals(Status.Server.ONLINE)) {
                        ServerPing conn = e.getResponse();
                        conn.setDescriptionComponent(new TextComponent(net.md_5.bungee.api.ChatColor.GOLD + "§6Time§2Snake§7-§9Network§c!                                 §31.18.1     " + "    §b " + MOTD + ""));
                        return;
                    }
                }
            }
            ServerPing conn = e.getResponse();
            conn.setVersion(new ServerPing.Protocol(net.md_5.bungee.api.ChatColor.GOLD + "Starting...", 2));
            conn.setDescriptionComponent(new TextComponent(net.md_5.bungee.api.ChatColor.GOLD + "§6Time§2Snake§7-§9Network§c!" + "                                        §6Server is starting, please wait."));
            e.setResponse(conn);
        }
    }
}
