/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.server;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.library.basic.util.ServerType;

public class FallbackManager {

  @Subscribe(order = PostOrder.FIRST)
  public void onServerKickEvent(KickedFromServerEvent e) {
    if (!(e.getResult() instanceof KickedFromServerEvent.DisconnectPlayer)) {
      return;
    }

    if (Network.getServer(e.getServer().getServerInfo().getName()).getType().equals(ServerType.LOBBY)) {
      return;
    }

    User user = Network.getUser(e.getPlayer());

    if (user == null || user.getLobby() == null) {
      return;
    }

    e.setResult(KickedFromServerEvent.RedirectPlayer.create(user.getLobby().getBungeeInfo()));
  }
}
