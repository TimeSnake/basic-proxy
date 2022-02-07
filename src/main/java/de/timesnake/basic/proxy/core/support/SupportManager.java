package de.timesnake.basic.proxy.core.support;

import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.api.message.ChannelSupportMessage;
import de.timesnake.channel.listener.ChannelSupportListener;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class SupportManager implements ChannelSupportListener, CommandListener<Sender, Argument>, Listener {

    private final HashMap<Integer, Tuple<Integer, ScheduledTask>> lockedTicketsById = new HashMap<>();

    private final Set<UUID> ticketListeners = new HashSet<>();

    public SupportManager() {
        Network.getCommandHandler().addCommand(BasicProxy.getPlugin(), ProxyServer.getInstance().getPluginManager(), "supportmsg", List.of("supportmessage", "supportmessages", "supportmsgs", "smsg"), this, Plugin.SUPPORT);
        Network.getChannel().addSupportListener(this);
        Network.registerListener(this);
    }

    @Override
    public void onSupportMessage(ChannelSupportMessage msg) {
        if (msg.getType().equals(ChannelSupportMessage.MessageType.TICKET_LOCK)) {
            Integer port = msg.getPort();
            Integer id = Integer.parseInt(msg.getValue());
            Tuple<Integer, ScheduledTask> value = this.lockedTicketsById.get(id);
            if (value != null && !value.getA().equals(port)) {
                Network.getChannel().sendMessage(ChannelSupportMessage.getTicketRejctMessage(port, id));
            } else {
                ScheduledTask task = ProxyServer.getInstance().getScheduler().schedule(BasicProxy.getPlugin(), () -> this.lockedTicketsById.remove(id), 3, TimeUnit.MINUTES);

                this.lockedTicketsById.put(id, new Tuple<>(port, task));
            }
        } else if (msg.getType().equals(ChannelSupportMessage.MessageType.SUBMIT)) {
            Integer port = msg.getPort();
            Integer id = Integer.parseInt(msg.getValue());

            Tuple<Integer, ScheduledTask> tuple = this.lockedTicketsById.get(id);

            if (tuple == null || tuple.getA() == null || !tuple.getA().equals(port)) {
                Network.getChannel().sendMessage(ChannelSupportMessage.getTicketRejctMessage(port, id));
                return;
            }

            Network.getChannel().sendMessage(ChannelSupportMessage.getTicketAcceptMessage(port, id));

            ScheduledTask task = this.lockedTicketsById.remove(id).getB();

            if (task != null) {
                task.cancel();
            }
        } else if (msg.getType().equals(ChannelSupportMessage.MessageType.CREATION)) {
            int id = Integer.parseInt(msg.getValue());

            for (UUID uuid : this.ticketListeners) {
                User user = Network.getUser(uuid);

                if (user != null) {
                    user.sendPluginMessage(Plugin.SUPPORT, ChatColor.WARNING + "New ticket: " + ChatColor.VALUE + "ID " + id);
                }
            }
        }

    }

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (!sender.isPlayer(true)) {
            return;
        }

        User user = sender.getUser();

        if (!sender.hasPermission("support.message", 31)) {
            return;
        }

        if (this.ticketListeners.contains(user.getUniqueId())) {
            this.ticketListeners.remove(user.getUniqueId());
            sender.sendPluginMessage(ChatColor.PERSONAL + "Disabled support messages");
        } else {
            this.ticketListeners.add(user.getUniqueId());
            sender.sendPluginMessage(ChatColor.PERSONAL + "Enabled support messages");
        }

    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        return null;
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        this.ticketListeners.remove(e.getPlayer().getUniqueId());
    }
}
