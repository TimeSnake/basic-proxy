package de.timesnake.basic.proxy.util.chat;

import com.velocitypowered.api.proxy.Player;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.basic.proxy.util.user.User;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

public class Argument extends de.timesnake.library.extension.util.cmd.Argument {

    public Argument(Sender sender, String string) {
        super(sender, string);
    }

    public boolean isPlayerName(boolean sendMessage) {
        Optional<Player> player = BasicProxy.getServer().getPlayer(this.string);
        if (player.isPresent()) {
            User user = Network.getUser(player.get());
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
        NamedTextColor color = NamedTextColor.NAMES.value(this.string);
        if (color == null) {
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


    public Player toPlayer() {
        return BasicProxy.getServer().getPlayer(this.string).get();
    }

    public Server toServer() {
        return Network.getServer(this.string);
    }

    public NamedTextColor toChatColor() {
        return NamedTextColor.NAMES.value(this.string);
    }

}
