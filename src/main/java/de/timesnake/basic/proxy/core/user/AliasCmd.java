package de.timesnake.basic.proxy.core.user;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.channel.util.message.ChannelUserMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import net.kyori.adventure.text.Component;

import java.util.List;

public class AliasCmd implements CommandListener<Sender, Argument> {

    public static void setAlias(Sender sender, DbUser user, Argument type, Argument name) {
        Component msg;
        switch (type.getString()) {
            case "info" -> {
                sender.sendPluginMessage(Component.text("Player: ", ExTextColor.PERSONAL)
                        .append(Component.text(user.getName(), ExTextColor.VALUE)));
                sender.sendPluginMessage(Component.text("Prefix: ", ExTextColor.PERSONAL)
                        .append(Component.text(user.getPrefix(), ExTextColor.VALUE)));
                sender.sendPluginMessage(Component.text("Suffix: ", ExTextColor.PERSONAL)
                        .append(Component.text(user.getSuffix(), ExTextColor.VALUE)));
                sender.sendPluginMessage(Component.text("Nick: ", ExTextColor.PERSONAL)
                        .append(Component.text(user.getNick(), ExTextColor.VALUE)));
            }
            case "prefix" -> {
                if (name == null) {
                    user.setPrefix(null);
                    msg = Component.text("Reset prefix", ExTextColor.PERSONAL);
                } else {
                    user.setPrefix(name.getString());
                    msg = Component.text("Updated prefix to ", ExTextColor.PERSONAL)
                            .append(Component.text(name.getString(), ExTextColor.VALUE));
                }
                sender.sendPluginMessage(msg.append(Component.text(" from ", ExTextColor.PERSONAL)
                        .append(Component.text(user.getName(), ExTextColor.VALUE))));
                Network.getChannel().sendMessage(new ChannelUserMessage<>(user.getUniqueId(), MessageType.User.ALIAS));
            }
            case "suffix" -> {
                if (name == null) {
                    user.setSuffix(null);
                    msg = Component.text("Reset suffix", ExTextColor.PERSONAL);
                } else {
                    user.setSuffix(name.getString());
                    msg = Component.text("Updated suffix to ", ExTextColor.PERSONAL)
                            .append(Component.text(name.getString(), ExTextColor.VALUE));
                }
                sender.sendPluginMessage(msg.append(Component.text(" from ", ExTextColor.PERSONAL)
                        .append(Component.text(user.getName(), ExTextColor.VALUE))));
                Network.getChannel().sendMessage(new ChannelUserMessage<>(user.getUniqueId(), MessageType.User.ALIAS));
            }
            case "nick" -> {
                if (name == null) {
                    user.setNick(null);
                    msg = Component.text("Reset nick", ExTextColor.PERSONAL);
                } else {
                    user.setNick(name.getString());
                    msg = Component.text("Updated nick to ", ExTextColor.PERSONAL)
                            .append(Component.text(name.getString(), ExTextColor.VALUE));
                }
                sender.sendPluginMessage(msg.append(Component.text(" from ", ExTextColor.PERSONAL)
                        .append(Component.text(user.getName(), ExTextColor.VALUE))));
                Network.getChannel().sendMessage(new ChannelUserMessage<>(user.getUniqueId(), MessageType.User.ALIAS));
            }
            default -> {
                sender.sendMessageCommandHelp("Set alias for player", "alias [player] " + "<prefix/suffix/nick> " +
                        "[value]");
                sender.sendMessageCommandHelp("Get alias from player", "alias [player] info");
            }
        }
    }

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (args.isLengthHigherEquals(1, true)) {
            if (Database.getUsers().containsUser(args.get(0).getString())) {
                if (sender.hasPermission("alias.other", 48)) {
                    if (args.isLengthEquals(3, false)) {
                        AliasCmd.setAlias(sender, args.get(0).toDbUser(), args.get(1), args.get(2));
                    } else {
                        AliasCmd.setAlias(sender, args.get(0).toDbUser(), args.get(1), null);
                    }
                }
            } else if (args.get(0).equalsIgnoreCase("help")) {
                sender.sendMessageCommandHelp("Set alias for player", "alias [player] " + "<prefix/suffix/nick> " +
                        "[value]");
                sender.sendMessageCommandHelp("Get alias from player", "alias [player] info");
            } else if (sender.isPlayer(true)) {
                if (args.isLengthEquals(2, false)) {
                    AliasCmd.setAlias(sender, sender.getDbUser(), args.get(0), args.get(1));
                } else {
                    AliasCmd.setAlias(sender, sender.getDbUser(), args.get(0), null);
                }
            } else {
                sender.sendMessageCommandHelp("For help", "alias help");
            }
        } else {
            sender.sendMessageCommandHelp("For help", "alias help");
        }

    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        int length = args.getLength();
        if (length == 1) {
            return Network.getCommandHandler().getPlayerNames();
        }

        if (length == 2) {
            return List.of("info", "prefix", "suffix", "nick");
        }
        return null;
    }

}
