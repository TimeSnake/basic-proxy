package de.timesnake.basic.proxy.util.chat;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.library.basic.util.chat.Plugin;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class Sender extends de.timesnake.library.extension.util.cmd.Sender {

    public Sender(CommandSender cmdSender, Plugin plugin) {
        super(cmdSender, plugin);
    }

    public ProxiedPlayer getPlayer() {
        return ProxyServer.getInstance().getPlayer(this.cmdSender.getName());
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
}
