package de.timesnake.basic.proxy.util.chat;

import com.velocitypowered.api.proxy.Player;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.library.basic.util.chat.Plugin;
import de.timesnake.library.extension.util.chat.Chat;
import net.kyori.adventure.text.Component;

public class Sender extends de.timesnake.library.extension.util.cmd.Sender {

    public Sender(CommandSender cmdSender, Plugin plugin) {
        super(cmdSender, plugin);
    }

    public Player getPlayer() {
        return BasicProxy.getServer().getPlayer(this.cmdSender.getName()).get();
    }

    public String getChatName() {
        return null;
    }

    public User getUser() {
        return this.cmdSender.getUser();
    }

    @Override
    public void sendConsoleMessage(String message) {
        Network.printText(Plugin.PROXY, message);
    }

    public void sendMessage(Component component) {
        ((CommandSender) this.cmdSender).sendMessage(component);
    }

    public void sendPluginMessage(Component component) {
        ((CommandSender) this.cmdSender).sendMessage(Component.text(Chat.getSenderPlugin(this.plugin)).append(component));
    }
}
