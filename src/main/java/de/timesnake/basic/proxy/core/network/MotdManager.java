/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.network;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.library.chat.ChatColor;
import de.timesnake.library.chat.ExTextColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.List;
import java.util.Random;

public class MotdManager {

  private static final String VERSION = "1.21.1";
  private static final List<Component> MOTDS = List.of(
      Component.text("     The time is running!", ExTextColor.DARK_PURPLE)
          .append(Component.text("   tick", ExTextColor.WHITE))
          .append(Component.text("  tack", ExTextColor.GRAY))
          .append(Component.text("  tick", ExTextColor.DARK_GRAY))
          .append(Component.text("  tack", TextColor.color(40, 40, 40))),
      Component.text("     Have fun!", ExTextColor.AQUA),
      Component.text("     e k a n s e m i t", ExTextColor.WHITE)
  );

  private static final Random RANDOM = new Random();

  @Subscribe
  public void onProxyPing(ProxyPingEvent e) {
    if (Network.isWork()) {
      e.setPing(e.getPing().asBuilder()
          .version(new ServerPing.Version(0, ChatColor.RED + "Service-Work"))
          .description(Component.text("§6Time§2Snake§7-§9Network§c!" +
              "                                         §cService-Work"))
          .build());
    } else {
      if (Network.getServerManager().getOnlineLobbys() > 0) {
        e.setPing(e.getPing().asBuilder()
            .version(new ServerPing.Version(e.getPing().getVersion().getProtocol(),
                ChatColor.BLUE + VERSION))
            .description(Component.text("§6Time", ExTextColor.GOLD)
                .append(Component.text("§2Snake", ExTextColor.DARK_GREEN))
                .append(Component.text("-", ExTextColor.GRAY))
                .append(Component.text("Network", ExTextColor.BLUE))
                .append(Component.text("!", ExTextColor.RED))
                .append(Component.text(
                    "                                             "))
                .append(MOTDS.get(RANDOM.nextInt(MOTDS.size())))).build());
      } else {
        e.setPing(e.getPing().asBuilder()
            .version(new ServerPing.Version(0, ChatColor.GOLD + "Starting..."))
            .description(Component.text(ChatColor.GOLD +
                "§6Time§2Snake§7-§9Network§c!"
                + "                                        §6Server is" +
                " " +
                "starting, please wait."))
            .build());
      }

    }
  }
}
