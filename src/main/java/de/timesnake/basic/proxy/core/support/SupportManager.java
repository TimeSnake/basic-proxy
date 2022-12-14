/*
 * Copyright (C) 2022 timesnake
 */

package de.timesnake.basic.proxy.core.support;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.util.listener.ChannelHandler;
import de.timesnake.channel.util.listener.ChannelListener;
import de.timesnake.channel.util.listener.ListenerType;
import de.timesnake.channel.util.message.ChannelSupportMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;

public class SupportManager implements ChannelListener, CommandListener<Sender, Argument> {

    private final HashMap<Integer, Tuple<String, ScheduledTask>> lockedTicketsById = new HashMap<>();

    private final Set<UUID> ticketListeners = new HashSet<>();

    private Code.Permission msgPerm;

    public SupportManager() {
        Network.getCommandHandler().addCommand(BasicProxy.getPlugin(),
                "supportmsg", List.of("supportmessage", "supportmessages", "supportmsgs", "smsg"), this,
                Plugin.SUPPORT);
        Network.getChannel().addListener(this);
        Network.registerListener(this);
    }

    @ChannelHandler(type = ListenerType.SUPPORT)
    public void onSupportMessage(ChannelSupportMessage<?> msg) {
        if (msg.getMessageType().equals(MessageType.Support.TICKET_LOCK)) {
            String name = msg.getName();
            Integer id = (Integer) msg.getValue();
            Tuple<String, ScheduledTask> value = this.lockedTicketsById.get(id);
            if (value != null && !value.getA().equals(name)) {
                Network.getChannel().sendMessage(new ChannelSupportMessage<>(name, MessageType.Support.REJECT, id));
            } else {
                ScheduledTask task = BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(),
                        () -> this.lockedTicketsById.remove(id)).delay(3, TimeUnit.MINUTES).schedule();

                this.lockedTicketsById.put(id, new Tuple<>(name, task));
            }
        } else if (msg.getMessageType().equals(MessageType.Support.SUBMIT)) {
            String name = msg.getName();
            Integer id = (Integer) msg.getValue();

            Tuple<String, ScheduledTask> tuple = this.lockedTicketsById.get(id);

            if (tuple == null || tuple.getA() == null || !tuple.getA().equals(name)) {
                Network.getChannel().sendMessage(new ChannelSupportMessage<>(name, MessageType.Support.REJECT, id));
                return;
            }

            Network.getChannel().sendMessage(new ChannelSupportMessage<>(name, MessageType.Support.ACCEPT, id));

            ScheduledTask task = this.lockedTicketsById.remove(id).getB();

            if (task != null) {
                task.cancel();
            }
        } else if (msg.getMessageType().equals(MessageType.Support.CREATION)) {
            Integer id = (Integer) msg.getValue();

            for (UUID uuid : this.ticketListeners) {
                User user = Network.getUser(uuid);

                if (user != null) {
                    user.sendPluginMessage(Plugin.SUPPORT, Component.text("New ticket: ", ExTextColor.WARNING)
                            .append(Component.text("ID " + id, ExTextColor.VALUE)));
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

        if (!sender.hasPermission(this.msgPerm)) {
            return;
        }

        if (this.ticketListeners.contains(user.getUniqueId())) {
            this.ticketListeners.remove(user.getUniqueId());
            sender.sendPluginMessage(Component.text("Disabled support messages", ExTextColor.PERSONAL));
        } else {
            this.ticketListeners.add(user.getUniqueId());
            sender.sendPluginMessage(Component.text("Enabled support messages", ExTextColor.PERSONAL));
        }

    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        return null;
    }

    @Override
    public void loadCodes(de.timesnake.library.extension.util.chat.Plugin plugin) {
        this.msgPerm = plugin.createPermssionCode("sup", "support.message");
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent e) {
        this.ticketListeners.remove(e.getPlayer().getUniqueId());
    }
}
