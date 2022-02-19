package de.timesnake.basic.proxy.core.permission;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.ChatColor;
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

import java.util.Arrays;
import java.util.UUID;

public class PermissionManager {

    public void addPlayerPermission(Sender sender, DbUser user, String permission, Status.Permission mode, String... servers) {

        if (!sender.hasPermission("permission.player.add", 20)) {
            return;
        }

        if (user.hasPermission(permission.toLowerCase())) {
            this.sendMessageHasAlreadyPermission(sender, user.getName(), permission);
            return;
        }

        UUID uuid = user.getUniqueId();

        if (Network.isUserOnline(uuid)) {
            user.addPermission(permission, mode, () -> Network.getUser(uuid).updatePermissions(), servers);
        } else {
            user.addPermission(permission, mode, servers);
        }
        this.sendMessageAddedPermission(sender, user.getName(), permission, mode, servers);
    }

    public void removePlayerPermission(Sender sender, DbUser user, String permission) {

        if (!sender.hasPermission("permission.player.remove", 21)) {
            return;
        }

        if (!user.hasPermission(permission.toLowerCase())) {
            this.sendMessageHasNotPermission(sender, user.getName(), permission);
            return;
        }

        UUID uuid = user.getUniqueId();

        if (Network.isUserOnline(uuid)) {
            user.removePermission(permission, () -> Network.getUser(uuid).updatePermissions());
        } else {
            user.removePermission(permission);
        }
        this.sendMessageRemovedPermission(sender, user.getName(), permission);

    }

    public void setPlayerGroup(Sender sender, DbUser user, String groupName) {
        if (!sender.hasPermission("permission.player.group.set", 22)) {
            return;
        }

        if (!sender.hasGroupRankLower(user)) {
            return;
        }

        String message = ChatColor.PERSONAL + "Updated group from " + ChatColor.VALUE + user.getName() + ChatColor.PERSONAL + " to " + ChatColor.VALUE + groupName.toLowerCase();

        UUID uuid = user.getUniqueId();

        if (user.getPermGroup() != null) {
            if (!user.getPermGroup().getName().equalsIgnoreCase(groupName) && sender.hasGroupRankLower(Database.getGroups().getPermGroup(groupName))) {
                if (Network.isUserOnline(uuid)) {
                    user.setPermGroup(groupName.toLowerCase(), () -> Network.getUser(uuid).updateGroup());
                } else {
                    user.setPermGroup(groupName.toLowerCase());
                }

                sender.sendPluginMessage(message);

            } else {
                sender.sendPluginMessage(ChatColor.VALUE + user.getName() + ChatColor.WARNING + " is " + "already in " + ChatColor.VALUE + groupName.toLowerCase() + ChatColor.WARNING + " group");
            }
        } else if (sender.hasGroupRankLower(Database.getGroups().getPermGroup(groupName))) {
            if (Network.isUserOnline(uuid)) {
                user.setPermGroup(groupName.toLowerCase(), () -> Network.getUser(uuid).updateGroup());
            } else {
                user.setPermGroup(groupName.toLowerCase());
            }
            sender.sendPluginMessage(message);

        }

    }

    public void removePlayerGroup(Sender sender, DbUser user) {

        if (!sender.hasPermission("permission.player.group.remove", 23)) {
            return;
        }

        if (user.getPermGroup() == null) {
            sender.sendPluginMessage(ChatColor.WARNING + "Player " + ChatColor.VALUE + user.getName() + ChatColor.WARNING + " hasn't a group");
            return;
        }

        if (!sender.hasGroupRankLower(user)) {
            return;
        }

        user.removePermGroup(() -> Network.getUser(user.getUniqueId()).updateGroup());
        sender.sendPluginMessage(ChatColor.PERSONAL + "Removed group from " + ChatColor.VALUE + user.getName());

        UUID uuid = user.getUniqueId();
    }

    public void addGroupPermission(Sender sender, String groupName, String permission, Status.Permission mode, String... servers) {
        if (!sender.hasPermission("permission.group.add", 24)) {
            return;
        }

        DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());

        if (!Database.getGroups().containsGroup(groupName)) {
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

        group.addPermission(permission, mode, () -> Network.getGroup(groupName).updatePermissions(), servers);
        this.sendMessageAddedPermission(sender, groupName, permission, mode, servers);

    }

    public void removeGroupPermission(Sender sender, String groupName, String permission) {
        if (!sender.hasPermission("permission.group.remove", 25)) {
            return;
        }

        DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());

        if (!Database.getGroups().containsGroup(groupName)) {
            this.sendMessagePermGroupNotExists(sender, groupName);
        }

        if (!group.hasPermission(permission.toLowerCase())) {
            this.sendMessageHasNotPermission(sender, groupName, permission);
            return;
        }

        if (!sender.hasGroupRankLower(group)) {
            return;
        }

        group.removePermission(permission, () -> Network.getGroup(groupName).updatePermissions());
        this.sendMessageRemovedPermission(sender, groupName, permission);

    }

    public void createGroup(Sender sender, String groupName, int rank, String prefix, net.md_5.bungee.api.ChatColor chatColor) {
        if (!sender.hasPermission("permission.group.create", 26)) {
            return;
        }

        DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());
        if (Database.getGroups().containsGroup(groupName)) {
            sender.sendPluginMessage(ChatColor.WARNING + "Group " + ChatColor.VALUE + group.getName() + ChatColor.PERSONAL + " already exists");
        }

        if (!sender.hasGroupRankLower(rank)) {
            return;
        }

        if (!(rank > 0)) {
            sender.sendPluginMessage(ChatColor.WARNING + "Invalid rank (rank: >0) " + Chat.getMessageCode("H", 110, Plugin.PERMISSION));
        }

        Database.getGroups().addPermGroup(groupName.toLowerCase(), rank, prefix, chatColor.getName());

        sender.sendPluginMessage(ChatColor.PERSONAL + "Group " + ChatColor.VALUE + groupName.toLowerCase() + ChatColor.PERSONAL + " created");
        sender.sendPluginMessage(ChatColor.WARNING + "Restart all servers to load the new group");
    }

    public void deleteGroup(Sender sender, String groupName) {
        if (!sender.hasPermission("permission.group.delete", 27)) {
            return;
        }

        DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());
        if (!Database.getGroups().containsGroup(groupName)) {
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

        Database.getGroups().removePermGroup(group);
        sender.sendPluginMessage(ChatColor.PERSONAL + "Group " + ChatColor.VALUE + group.getName() + ChatColor.PERSONAL + " deleted");
        Network.getChannel().sendMessage(new ChannelGroupMessage<>(groupName, MessageType.Group.PERMISSION));

    }

    public void setGroupInheritance(Sender sender, String groupName, String inheritGroupName) {
        if (!sender.hasPermission("permission.group.inheritance.set", 28)) {
            return;
        }

        DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());
        if (!Database.getGroups().containsGroup(groupName)) {
            this.sendMessagePermGroupNotExists(sender, groupName);
            return;
        }

        if (!sender.hasGroupRankLower(group)) {
            return;
        }

        DbPermGroup inheritGroup = Database.getGroups().getPermGroup(inheritGroupName.toLowerCase());
        if (!(Database.getGroups().containsGroup(inheritGroupName) && sender.hasGroupRankLower(inheritGroup))) {
            return;
        }

        group.setInheritance(inheritGroup.getName(), () -> Network.getGroup(groupName).updatePermissions());
        sender.sendPluginMessage(ChatColor.PERSONAL + "Added Inheritance " + ChatColor.VALUE + inheritGroup.getName() + ChatColor.PERSONAL + " to " + ChatColor.VALUE + group.getName());

    }

    public void removeGroupInheritance(Sender sender, String groupName) {
        if (!sender.hasPermission("permission.group.inheritance.remove", 29)) {
            return;
        }

        DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());
        if (Database.getGroups().containsGroup(groupName)) {
            this.sendMessagePermGroupNotExists(sender, groupName);
            return;
        }

        if (!sender.hasGroupRankLower(group)) {
            return;
        }

        group.removeInheritance(() -> Network.getGroup(groupName).updatePermissions());
        sender.sendPluginMessage(ChatColor.PERSONAL + "Removed inheritance from " + ChatColor.VALUE + group.getName());
    }

    private void sendMessagePermGroupNotExists(Sender sender, String groupName) {
        sender.sendPluginMessage(ChatColor.WARNING + "Group " + ChatColor.VALUE + groupName + ChatColor.PERSONAL + " does not exist");
        sender.sendMessageCommandHelp("Create a group", "perms group <name> create");
    }

    private void sendMessageHasAlreadyPermission(Sender sender, String name, String permission) {
        sender.sendPluginMessage(ChatColor.VALUE + name + ChatColor.WARNING + " has already " + "permission " + ChatColor.VALUE + permission);
    }

    private void sendMessageAddedPermission(Sender sender, String name, String permission, Status.Permission mode, String... servers) {
        sender.sendPluginMessage(ChatColor.PERSONAL + "Added permission " + ChatColor.VALUE + permission + ChatColor.PERSONAL + " to " + ChatColor.VALUE + name + ChatColor.PERSONAL + " with mode " + ChatColor.VALUE + mode.getName() + ChatColor.PERSONAL + " on server(s): " + ChatColor.VALUE + Arrays.toString(servers));
    }

    private void sendMessageRemovedPermission(Sender sender, String name, String permission) {
        sender.sendPluginMessage(ChatColor.PERSONAL + "Removed permission " + ChatColor.VALUE + permission + ChatColor.PERSONAL + " from " + ChatColor.VALUE + name);
    }

    private void sendMessageHasNotPermission(Sender sender, String name, String permission) {
        sender.sendPluginMessage(ChatColor.VALUE + name + ChatColor.WARNING + " has not " + "permission " + ChatColor.VALUE + permission);
    }

}
