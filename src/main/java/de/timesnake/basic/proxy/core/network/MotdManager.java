/*
 * Copyright (C) 2022 timesnake
 */

package de.timesnake.basic.proxy.core.network;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.library.basic.util.chat.ChatColor;
import de.timesnake.library.basic.util.chat.ExTextColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class MotdManager {

    private static final String VERSION = "1.19";
    private static final Component MOTD = Component.text("     The time is running!", ExTextColor.DARK_PURPLE)
            .append(Component.text("   tick", ExTextColor.WHITE))
            .append(Component.text("  tack", ExTextColor.GRAY))
            .append(Component.text("  tick", ExTextColor.DARK_GRAY))
            .append(Component.text("  tack", TextColor.color(40, 40, 40)));

    @Subscribe
    public void onProxyPing(ProxyPingEvent e) {
        if (Network.isWork()) {
            e.setPing(e.getPing().asBuilder()
                    .version(new ServerPing.Version(0, ChatColor.RED + "Service-Work"))
                    .description(Component.text("§6Time§2Snake§7-§9Network§c!" +
                            "                                         §cService-Work"))
                    .build());
        } else {
            if (Network.getOnlineLobbys() > 0) {
                e.setPing(e.getPing().asBuilder()
                        .version(new ServerPing.Version(e.getPing().getVersion().getProtocol(),
                                ChatColor.BLUE + VERSION))
                        .description(Component.text("§6Time", ExTextColor.GOLD)
                                .append(Component.text("§2Snake", ExTextColor.DARK_GREEN))
                                .append(Component.text("-", ExTextColor.GRAY))
                                .append(Component.text("Network", ExTextColor.BLUE))
                                .append(Component.text("!", ExTextColor.RED))
                                .append(Component.text("                                             "))
                                .append(MOTD)).build());
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
