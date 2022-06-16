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
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AutoShutdown implements CommandListener<Sender, Argument>, Listener {

    public static final int START_TIME = 15;
    private static final int PLAYER_TIME_0 = 5;
    private static final int PLAYER_TIME_1 = 30;
    private final Set<UUID> votedUsers = new HashSet<>();
    private boolean enabled = true;
    private int requiredVotes = 1;
    private ScheduledTask task;

    public AutoShutdown() {
        NetworkManager.getInstance().getCommandHandler().addCommand(BasicProxy.getPlugin(),
                BasicProxy.getPlugin().getProxy().getPluginManager(), "shutdown", this, Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(BasicProxy.getPlugin(),
                BasicProxy.getPlugin().getProxy().getPluginManager(), "autoshutdown", this, Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(BasicProxy.getPlugin(),
                BasicProxy.getPlugin().getProxy().getPluginManager(), "hello", List.of("hallo", "hi", "helo"),
                this, Plugin.NETWORK);
    }

    @EventHandler
    public void onPlayerJoin(PostLoginEvent e) {
        if (Network.getUsers().size() > 0) {
            this.start(PLAYER_TIME_1);
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        if (Network.getUsers().size() == 0) {
            this.start(PLAYER_TIME_0);
        }
    }

    public void start(int time) {
        if (this.task != null) {
            this.task.cancel();
        }

        if (enabled) {
            Network.printText(Plugin.SYSTEM, "AutoShutdown started");
            task = ProxyServer.getInstance().getScheduler().schedule(BasicProxy.getPlugin(), this::infoShutdown, time
                    , TimeUnit.MINUTES);
        }
    }

    public void infoShutdown() {
        Network.broadcastMessage(Plugin.NETWORK, ChatColor.WARNING + "§lThe server will shutdown in 5 minutes ");

        TextComponent text = new TextComponent(Chat.getSenderPlugin(Plugin.NETWORK) + ChatColor.WARNING +
                "§lWrite " + ChatColor.VALUE + ChatColor.UNDERLINE + "/hello" + ChatColor.WARNING + " to keep the " +
                "server online.");
        text.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hello"));
        text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to execute /hello")));

        for (User user : Network.getUsers()) {
            user.getPlayer().sendMessage(text);
        }

        task.cancel();
        task = ProxyServer.getInstance().getScheduler().schedule(BasicProxy.getPlugin(), this::warnShutdown, 4,
                TimeUnit.MINUTES);
    }

    public void warnShutdown() {
        Network.broadcastMessage(Plugin.NETWORK, ChatColor.WARNING + "§lWrite " + ChatColor.VALUE + "/hello" +
                ChatColor.WARNING + " to keep the server online.");
        Network.broadcastMessage(Plugin.NETWORK, ChatColor.WARNING + "§lThe server will shutdown in 1 minute ");
        task.cancel();
        task = ProxyServer.getInstance().getScheduler().schedule(BasicProxy.getPlugin(), this::beginShutdown, 1,
                TimeUnit.MINUTES);
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

    private void cancelShutdown() {
        if (this.task != null) {
            this.task.cancel();
            this.votedUsers.clear();
            Network.broadcastMessage(Plugin.NETWORK, ChatColor.WARNING + "Shutdown cancelled");
            if (enabled) {
                this.start(Network.getUsers().size() > 0 ? PLAYER_TIME_1 : PLAYER_TIME_0);
            }
        }
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
                    Network.broadcastMessage(Plugin.NETWORK, ChatColor.WARNING + "Against Server shutdown: " +
                            ChatColor.VALUE + this.votedUsers.size() + " / " + this.requiredVotes);
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

                    sender.sendPluginMessage(ChatColor.PERSONAL + "Updated shutdown votes to " +
                            ChatColor.VALUE + this.requiredVotes);
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
                    this.start(Network.getUsers().size() > 0 ? PLAYER_TIME_1 : PLAYER_TIME_0);
                    sender.sendPluginMessage(ChatColor.PERSONAL + "Enabled auto-shutdown");
                }
            }
        }

    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        return List.of();
    }
}
