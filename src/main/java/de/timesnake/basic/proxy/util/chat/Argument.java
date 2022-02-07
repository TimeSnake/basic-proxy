package de.timesnake.basic.proxy.util.chat;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.basic.proxy.util.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class Argument extends de.timesnake.library.extension.util.cmd.Argument {

    public Argument(Sender sender, String string) {
        super(sender, string);
    }

    public boolean isPlayerName(boolean sendMessage) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(this.string);
        if (player != null) {
            User user = Network.getUser(player);
            return !user.isAirMode();
        }
        if (sendMessage) {
            this.sender.sendMessagePlayerNotExist(this.string);
        }
        return false;
    }

    public User toUser() {
        return Network.getUser(this.toPlayer());
    }

    public boolean isChatColor(boolean sendMessage) {
        try {
            ChatColor.of(this.string.toUpperCase());
        } catch (IllegalArgumentException e) {
            if (sendMessage) {
                this.sender.sendMessageNoChatColor(this.string);
            }
            return false;
        }
        return true;
    }

    public boolean isServerName(boolean sendMessage) {
        if (Network.getServer(this.string) == null) {
            if (sendMessage) {
                this.sender.sendMessageServerNameNotExist(this.string);
            }
            return false;
        }
        return true;
    }


    public ProxiedPlayer toPlayer() {
        return ProxyServer.getInstance().getPlayer(this.string);
    }

    public ChatColor toChatColor(boolean sendMessage) {
        return ChatColor.of(this.string.toUpperCase());
    }

    public Server toServer() {
        return Network.getServer(this.string);
    }

    public ChatColor toChatColor() {
        return ChatColor.of(this.string);
    }

}
