/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.permission;

import static de.timesnake.library.basic.util.chat.ExTextColor.PERSONAL;
import static de.timesnake.library.basic.util.chat.ExTextColor.VALUE;
import static de.timesnake.library.basic.util.chat.ExTextColor.WARNING;
import static net.kyori.adventure.text.Component.text;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.channel.util.message.ChannelGroupMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.group.DbPermGroup;
import de.timesnake.database.util.permission.DbPermission;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.extension.util.chat.Code;
import java.util.Arrays;
import java.util.UUID;
import net.kyori.adventure.text.Component;

public class PermissionManager {

    private final Code playerAddPerm;
    private final Code playerRemovePerm;
    private final Code playerGroupSetPerm;
    private final Code playerGroupRemovePerm;
    private final Code groupAddPerm;
    private final Code groupRemovePerm;
    private final Code groupCreatePerm;
    private final Code groupDeletePerm;
    private final Code groupInheritanceSetPerm;
    private final Code groupInheritanceRemovePerm;

    public PermissionManager() {
        this.playerAddPerm = Plugin.PERMISSION.createPermssionCode("permission.player.add");
        this.playerRemovePerm = Plugin.PERMISSION.createPermssionCode("permission.player.remove");
        this.playerGroupSetPerm = Plugin.PERMISSION.createPermssionCode(
                "permission.player.group.set");
        this.playerGroupRemovePerm = Plugin.PERMISSION.createPermssionCode(
                "permission.player.group.remove");
        this.groupAddPerm = Plugin.PERMISSION.createPermssionCode("permission.group.add");
        this.groupRemovePerm = Plugin.PERMISSION.createPermssionCode("permission.group.remove");
        this.groupCreatePerm = Plugin.PERMISSION.createPermssionCode("permission.group.create");
        this.groupDeletePerm = Plugin.PERMISSION.createPermssionCode("permission.group.delete");
        this.groupInheritanceSetPerm = Plugin.PERMISSION.createPermssionCode(
                "permission.group.inheritance.set");
        this.groupInheritanceRemovePerm = Plugin.PERMISSION.createPermssionCode(
                "permission.inheritance.remove");
    }

    public void addPlayerPermission(Sender sender, DbUser user, String permission,
            Status.Permission mode,
            String... servers) {

        if (!sender.hasPermission(this.playerAddPerm)) {
            return;
        }

        if (user.hasPermission(permission.toLowerCase())) {
            this.sendMessageHasAlreadyPermission(sender, user.getName(), permission);
            return;
        }

        UUID uuid = user.getUniqueId();

        if (Network.isUserOnline(uuid)) {
            user.addPermission(permission, mode,
                    () -> Network.getUser(uuid).updatePermissions(true), servers);
        } else {
            user.addPermission(permission, mode, servers);
        }
        this.sendMessageAddedPermission(sender, user.getName(), permission, mode, servers);
    }

    public void removePlayerPermission(Sender sender, DbUser user, String permission) {

        if (!sender.hasPermission(this.playerRemovePerm)) {
            return;
        }

        if (!user.hasPermission(permission.toLowerCase())) {
            this.sendMessageHasNotPermission(sender, user.getName(), permission);
            return;
        }

        UUID uuid = user.getUniqueId();

        if (Network.isUserOnline(uuid)) {
            user.removePermission(permission, () -> Network.getUser(uuid).updatePermissions(true));
        } else {
            user.removePermission(permission);
        }
        this.sendMessageRemovedPermission(sender, user.getName(), permission);

    }

    public void setPlayerGroup(Sender sender, DbUser user, String groupName) {
        if (!sender.hasPermission(this.playerGroupSetPerm)) {
            return;
        }

        if (!sender.hasGroupRankLower(user)) {
            return;
        }

        Component message = text("Updated group from ", PERSONAL)
                .append(text(user.getName(), VALUE))
                .append(text(" to ", PERSONAL))
                .append(text(groupName.toLowerCase(), VALUE));

        UUID uuid = user.getUniqueId();

        if (user.getPermGroup() != null) {
            if (!user.getPermGroup().getName().equalsIgnoreCase(groupName)
                    && sender.hasGroupRankLower(Database.getGroups().getPermGroup(groupName))) {
                if (Network.isUserOnline(uuid)) {
                    user.setPermGroup(groupName.toLowerCase(),
                            () -> Network.getUser(uuid).updateGroup());
                } else {
                    user.setPermGroup(groupName.toLowerCase());
                }

                sender.sendPluginMessage(message);

            } else {
                sender.sendPluginMessage(text(user.getName(), VALUE)
                        .append(text(" is already in ", WARNING))
                        .append(text(groupName.toLowerCase(), VALUE))
                        .append(text(" group", WARNING)));
            }
        } else if (sender.hasGroupRankLower(Database.getGroups().getPermGroup(groupName))) {
            if (Network.isUserOnline(uuid)) {
                user.setPermGroup(groupName.toLowerCase(),
                        () -> Network.getUser(uuid).updateGroup());
            } else {
                user.setPermGroup(groupName.toLowerCase());
            }
            sender.sendPluginMessage(message);

        }

    }

    public void removePlayerGroup(Sender sender, DbUser user) {

        if (!sender.hasPermission(this.playerGroupRemovePerm)) {
            return;
        }

        if (user.getPermGroup() == null) {
            sender.sendPluginMessage(text("Player ", WARNING)
                    .append(text(user.getName(), VALUE))
                    .append(text(" hasn't a group", WARNING)));
            return;
        }

        if (!sender.hasGroupRankLower(user)) {
            return;
        }

        user.removePermGroup(() -> Network.getUser(user.getUniqueId()).updateGroup());
        sender.sendPluginMessage(text("Removed group from ", PERSONAL)
                .append(text(user.getName(), VALUE)));

        UUID uuid = user.getUniqueId();
    }

    public void addGroupPermission(Sender sender, String groupName, String permission,
            Status.Permission mode,
            String... servers) {
        if (!sender.hasPermission(this.groupAddPerm)) {
            return;
        }

        DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());

        if (!Database.getGroups().containsPermGroup(groupName)) {
            this.sendMessagePermGroupNotExists(sender, groupName);
            return;
        }

        if (group.hasPermission(permission.toLowerCase())) {
            this.sendMessageHasAlreadyPermission(sender, groupName, permission);
            return;
        }

        if (!sender.hasGroupRankLower(group)) {
            return;
        }

        group.addPermission(permission, mode,
                () -> Network.getPermGroup(groupName).updatePermissions(), servers);
        this.sendMessageAddedPermission(sender, groupName, permission, mode, servers);

    }

    public void removeGroupPermission(Sender sender, String groupName, String permission) {
        if (!sender.hasPermission(this.groupRemovePerm)) {
            return;
        }

        DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());

        if (!Database.getGroups().containsPermGroup(groupName)) {
            this.sendMessagePermGroupNotExists(sender, groupName);
        }

        if (!group.hasPermission(permission.toLowerCase())) {
            this.sendMessageHasNotPermission(sender, groupName, permission);
            return;
        }

        if (!sender.hasGroupRankLower(group)) {
            return;
        }

        group.removePermission(permission,
                () -> Network.getPermGroup(groupName).updatePermissions());
        this.sendMessageRemovedPermission(sender, groupName, permission);

    }

    public void createGroup(Sender sender, String groupName, int rank) {
        if (!sender.hasPermission(this.groupCreatePerm)) {
            return;
        }

        DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());
        if (Database.getGroups().containsPermGroup(groupName)) {
            sender.sendPluginMessage(text("Group ", WARNING)
                    .append(text(group.getName(), VALUE))
                    .append(text(" already exists", WARNING)));
        }

        if (!sender.hasGroupRankLower(rank)) {
            return;
        }

        if (!(rank > 0)) {
            sender.sendPluginMessage(text("Invalid rank (rank: >0) ", WARNING)
                    .append(Chat.getMessageCode("H", 110, Plugin.PERMISSION)));
        }

        Database.getGroups().addPermGroup(groupName.toLowerCase(), rank);

        sender.sendPluginMessage(text("Group ", PERSONAL)
                .append(text(groupName.toLowerCase(), VALUE))
                .append(text(" created", PERSONAL)));
        sender.sendPluginMessage(text("Restart all servers to load the new group", WARNING));
    }

    public void deleteGroup(Sender sender, String groupName) {
        if (!sender.hasPermission(this.groupDeletePerm)) {
            return;
        }

        DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());
        if (!Database.getGroups().containsPermGroup(groupName)) {
            this.sendMessagePermGroupNotExists(sender, groupName);
        }

        if (!sender.hasGroupRankLower(group)) {
            return;
        }

        for (DbPermission perm : group.getPermissions()) {
            group.removePermission(perm.getName());
        }
        for (UUID uuid : Database.getUsers().getUsersUuid()) {
            DbUser user = Database.getUsers().getUser(uuid);
            if (user.getPermGroup().getName().equals(groupName.toLowerCase())) {
                user.removePermGroup();
            }
        }

        Database.getGroups().removePermGroup(group.getName());
        sender.sendPluginMessage(text("Group ", PERSONAL)
                .append(text(group.getName(), VALUE))
                .append(text(" deleted", PERSONAL)));
        Network.getChannel()
                .sendMessage(new ChannelGroupMessage<>(groupName, MessageType.Group.PERMISSION));

    }

    public void setGroupInheritance(Sender sender, String groupName, String inheritGroupName) {
        if (!sender.hasPermission(this.groupInheritanceSetPerm)) {
            return;
        }

        DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());
        if (!Database.getGroups().containsPermGroup(groupName)) {
            this.sendMessagePermGroupNotExists(sender, groupName);
            return;
        }

        if (!sender.hasGroupRankLower(group)) {
            return;
        }

        DbPermGroup inheritGroup = Database.getGroups()
                .getPermGroup(inheritGroupName.toLowerCase());
        if (!(Database.getGroups().containsPermGroup(inheritGroupName) && sender.hasGroupRankLower(
                inheritGroup))) {
            return;
        }

        group.setInheritance(inheritGroup.getName(),
                () -> Network.getPermGroup(groupName).updatePermissions());
        sender.sendPluginMessage(text("Added Inheritance ", PERSONAL)
                .append(text(inheritGroup.getName(), VALUE))
                .append(text(" to ", PERSONAL))
                .append(text(group.getName(), VALUE)));

    }

    public void removeGroupInheritance(Sender sender, String groupName) {
        if (!sender.hasPermission(this.groupInheritanceRemovePerm)) {
            return;
        }

        DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());
        if (Database.getGroups().containsPermGroup(groupName)) {
            this.sendMessagePermGroupNotExists(sender, groupName);
            return;
        }

        if (!sender.hasGroupRankLower(group)) {
            return;
        }

        group.removeInheritance(() -> Network.getPermGroup(groupName).updatePermissions());
        sender.sendPluginMessage(text("Removed inheritance from ", PERSONAL)
                .append(text(group.getName(), VALUE)));
    }

    private void sendMessagePermGroupNotExists(Sender sender, String groupName) {
        sender.sendPluginMessage(text("Group ", WARNING)
                .append(text(groupName, VALUE))
                .append(text(" does not exist", WARNING)));
        sender.sendMessageCommandHelp("Create a group", "perms group <name> create");
    }

    private void sendMessageHasAlreadyPermission(Sender sender, String name, String permission) {
        sender.sendPluginMessage(text(name, VALUE)
                .append(text(" has already permission ", WARNING))
                .append(text(permission, VALUE)));
    }

    private void sendMessageAddedPermission(Sender sender, String name, String permission,
            Status.Permission mode,
            String... servers) {
        sender.sendPluginMessage(text("Added permission ", PERSONAL)
                .append(text(permission, VALUE))
                .append(text(" to ", PERSONAL))
                .append(text(name, VALUE))
                .append(text(" with mode ", PERSONAL))
                .append(text(mode.getShortName(), VALUE))
                .append(text(" on server(s): ", PERSONAL))
                .append(Chat.listToComponent(Arrays.stream(servers).toList(), VALUE, PERSONAL)));
    }

    private void sendMessageRemovedPermission(Sender sender, String name, String permission) {
        sender.sendPluginMessage(text("Removed permission ", PERSONAL)
                .append(text(permission, VALUE))
                .append(text(" from ", PERSONAL))
                .append(text(name, VALUE)));
    }

    private void sendMessageHasNotPermission(Sender sender, String name, String permission) {
        sender.sendPluginMessage(text(name, VALUE)
                .append(text(" has not permission ", WARNING))
                .append(text(permission, VALUE)));
    }

}
