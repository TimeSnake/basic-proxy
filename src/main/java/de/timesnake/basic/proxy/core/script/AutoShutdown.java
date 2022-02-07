package de.timesnake.basic.proxy.core.script;

import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.core.server.BukkitCmdHandler;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.NetworkManager;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AutoShutdown implements CommandListener<Sender, Argument> {

    private boolean enabled = true;

    private int requiredVotes = 1;
    private Set<UUID> votedUsers = new HashSet<>();

    private ScheduledTask task;

    public AutoShutdown() {
        NetworkManager.getInstance().getCommandHandler().addCommand(BasicProxy.getPlugin(), BasicProxy.getPlugin().getProxy().getPluginManager(), "shutdown", this, Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(BasicProxy.getPlugin(), BasicProxy.getPlugin().getProxy().getPluginManager(), "autoshutdown", this, Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(BasicProxy.getPlugin(), BasicProxy.getPlugin().getProxy().getPluginManager(), "hello", List.of("hallo", "hi", "helo"), this, Plugin.NETWORK);
    }

    public void start() {
        if (enabled) {
            Network.printText(Plugin.SYSTEM, "AutoShutdown started");
            if (Network.getUsers().size() == 0) {
                task = ProxyServer.getInstance().getScheduler().schedule(BasicProxy.getPlugin(), this::infoShutdown, 10, TimeUnit.MINUTES);
            } else {
                task = ProxyServer.getInstance().getScheduler().schedule(BasicProxy.getPlugin(), this::infoShutdown, 15, TimeUnit.MINUTES);
            }
        }
    }

    public void infoShutdown() {
        Network.broadcastMessage(Plugin.NETWORK, ChatColor.WARNING + "§lWrite " + ChatColor.VALUE + "/hello" + ChatColor.WARNING + " to keep the server online.");
        Network.broadcastMessage(Plugin.NETWORK, ChatColor.WARNING + "§lThe server will shutdown in 5 minutes ");
        task.cancel();
        task = ProxyServer.getInstance().getScheduler().schedule(BasicProxy.getPlugin(), this::warnShutdown, 4, TimeUnit.MINUTES);
    }

    public void warnShutdown() {
        Network.broadcastMessage(Plugin.NETWORK, ChatColor.WARNING + "§lWrite " + ChatColor.VALUE + "/hello" + ChatColor.WARNING + " to keep the server online.");
        Network.broadcastMessage(Plugin.NETWORK, ChatColor.WARNING + "§lThe server will shutdown in 1 minute ");
        task.cancel();
        task = ProxyServer.getInstance().getScheduler().schedule(BasicProxy.getPlugin(), this::beginShutdown, 1, TimeUnit.MINUTES);
    }

    private void beginShutdown() {
        Network.broadcastMessage(Plugin.NETWORK, ChatColor.WARNING + "§lThe server will shutdown in 10 seconds ");
        task.cancel();
        task = ProxyServer.getInstance().getScheduler().schedule(BasicProxy.getPlugin(), () -> {
            Network.broadcastMessage(Plugin.NETWORK, ChatColor.WARNING + "§lSHUTDOWN");
            new BukkitCmdHandler().stopAllServers();
            shutdown();
        }, 10, TimeUnit.SECONDS);
    }

    private void shutdown() {
        task.cancel();
        task = ProxyServer.getInstance().getScheduler().schedule(BasicProxy.getPlugin(), () -> {
            ProxyServer.getInstance().stop();
            try {
                Runtime.getRuntime().exec("shutdown -t 60");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 30, TimeUnit.SECONDS);
    }

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (cmd.getName().equalsIgnoreCase("hello") || cmd.getName().equalsIgnoreCase("hallo")) {
            if (sender.hasPermission("network.hello", 35)) {
                if (sender.isConsole(false)) {
                    this.cancelShutdown();
                    return;
                }

                User user = sender.getUser();

                if (this.votedUsers.contains(user.getUniqueId())) {
                    sender.sendPluginMessage(ChatColor.WARNING + "You already voted against the shutdown");
                    return;
                }

                this.votedUsers.add(user.getUniqueId());

                if (this.votedUsers.size() >= this.requiredVotes) {
                    this.cancelShutdown();
                } else {
                    Network.broadcastMessage(Plugin.NETWORK, ChatColor.WARNING + "Against Server shutdown: " + ChatColor.VALUE + this.votedUsers.size() + " / " + this.requiredVotes);
                }
            }
        } else if (cmd.getName().equalsIgnoreCase("shutdown")) {
            if (sender.hasPermission("network.shutdown", 41)) {
                this.beginShutdown();
            }
        } else if (cmd.getName().equalsIgnoreCase("autoshutdown")) {
            if (sender.hasPermission("network.autoshutdown", 10)) {
                if (args.isLengthEquals(1, false) && args.get(0).isInt(true)) {
                    this.requiredVotes = args.get(0).toInt();
                    if (this.requiredVotes <= 0) {
                        this.requiredVotes = 1;
                    }

                    this.enabled = false;

                    sender.sendPluginMessage(ChatColor.PERSONAL + "Updated shutdown votes to " + ChatColor.VALUE + this.requiredVotes);
                }

                if (this.enabled) {
                    this.enabled = false;
                    if (this.task != null) {
                        this.task.cancel();
                    }
                    this.votedUsers.clear();

                    Network.broadcastMessage(Plugin.NETWORK, ChatColor.WARNING + "Shutdown cancelled");
                    sender.sendPluginMessage(ChatColor.PERSONAL + "Disabled auto-shutdown");
                } else {
                    this.enabled = true;
                    this.votedUsers.clear();
                    this.start();
                    sender.sendPluginMessage(ChatColor.PERSONAL + "Enabled auto-shutdown");
                }
            }
        }

    }

    private void cancelShutdown() {
        if (this.task != null) {
            this.task.cancel();
            this.votedUsers.clear();
            Network.broadcastMessage(Plugin.NETWORK, ChatColor.WARNING + "Shutdown cancelled");
            if (enabled) {
                this.start();
            }
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        return null;
    }
}
