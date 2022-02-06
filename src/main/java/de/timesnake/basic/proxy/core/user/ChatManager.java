package de.timesnake.basic.proxy.core.user;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ChatManager implements Listener {

    @EventHandler
    public void onChat(ChatEvent e) {
        String msg = e.getMessage();
        ProxiedPlayer p = (ProxiedPlayer) e.getSender();
        User user = Network.getUser(p);

        user.setLastChatMessage(msg);
    }
}
