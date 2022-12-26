/*
 * Copyright (C) 2022 timesnake
 */

package de.timesnake.basic.proxy.core.user;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;

public class ChatManager {

    @Subscribe
    public void onChat(PlayerChatEvent e) {
        String msg = e.getMessage();
        Player p = e.getPlayer();
        User user = Network.getUser(p);

        user.setLastChatMessage(msg);
    }
}
