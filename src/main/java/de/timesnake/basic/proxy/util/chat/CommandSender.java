package de.timesnake.basic.proxy.util.chat;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.command.ConsoleCommandSender;

public class CommandSender implements de.timesnake.library.basic.util.cmd.CommandSender {

    private final net.md_5.bungee.api.CommandSender cmdSender;

    public CommandSender(net.md_5.bungee.api.CommandSender cmdSender) {
        this.cmdSender = cmdSender;
    }

    @Override
    public void sendMessage(String s) {
        this.cmdSender.sendMessage(new TextComponent(s));
    }

    @Override
    public void sendMessage(String[] strings) {

    }

    @Override
    public String getName() {
        return this.cmdSender.getName();
    }

    @Override
    public boolean hasPermission(String s) {
        return this.cmdSender.hasPermission(s);
    }

    @Override
    public boolean isConsole() {
        return this.cmdSender instanceof ConsoleCommandSender;
    }

    @Override
    public ProxiedPlayer getPlayer() {
        return (ProxiedPlayer) this.cmdSender;
    }

    @Override
    public User getUser() {
        return this.cmdSender instanceof ProxiedPlayer ? (User) Network.getUser((ProxiedPlayer) this.cmdSender) : null;
    }
}
