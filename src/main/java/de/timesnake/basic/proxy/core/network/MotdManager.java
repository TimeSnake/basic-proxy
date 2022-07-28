package de.timesnake.basic.proxy.core.network;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.library.basic.util.chat.ChatColor;
import net.kyori.adventure.text.Component;

public class MotdManager {

    private static final String VERSION = "1.19";
    private static final String MOTD = "§f§kaaa §62 Years Anniversary! §f§kaaa";

    @Subscribe
    public void onProxyPing(ProxyPingEvent e) {
        if (Network.isWork()) {
            e.setPing(e.getPing().asBuilder()
                    .version(new ServerPing.Version(0,
                            ChatColor.RED + "Service-Work"))
                    .description(Component.text("§6Time§2Snake§7-§9Network§c!" +
                            "                                         §cService-Work"))
                    .build());
        } else {
            if (Network.getOnlineLobbys() > 0) {
                e.setPing(e.getPing().asBuilder()
                        .version(new ServerPing.Version(e.getPing().getVersion().getProtocol(),
                                ChatColor.BLUE + VERSION))
                        .description(Component.text(ChatColor.GOLD +
                                "§6Time§2Snake§7-§9Network§c!                                          " +
                                "   §b" + MOTD))
                        .build());
            } else {
                e.setPing(e.getPing().asBuilder()
                        .version(new ServerPing.Version(0, ChatColor.GOLD + "Starting..."))
                        .description(Component.text(ChatColor.GOLD +
                                "§6Time§2Snake§7-§9Network§c!" + "                                        §6Server is" +
                                " " +
                                "starting, please wait."))
                        .build());
            }

        }
    }
}
