/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.permission;

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
import de.timesnake.library.chat.Code;

import java.util.UUID;

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
    this.playerGroupSetPerm = Plugin.PERMISSION.createPermssionCode("permission.player.group.set");
    this.playerGroupRemovePerm = Plugin.PERMISSION.createPermssionCode("permission.player.group.remove");
    this.groupAddPerm = Plugin.PERMISSION.createPermssionCode("permission.group.add");
    this.groupRemovePerm = Plugin.PERMISSION.createPermssionCode("permission.group.remove");
    this.groupCreatePerm = Plugin.PERMISSION.createPermssionCode("permission.group.create");
    this.groupDeletePerm = Plugin.PERMISSION.createPermssionCode("permission.group.delete");
    this.groupInheritanceSetPerm = Plugin.PERMISSION.createPermssionCode("permission.group.inheritance.set");
    this.groupInheritanceRemovePerm = Plugin.PERMISSION.createPermssionCode("permission.inheritance.remove");
  }

  public void addPlayerPermission(Sender sender, DbUser user, String permission, Status.Permission mode) {
    if (!sender.hasPermission(this.playerAddPerm)) {
      return;
    }

    if (user.hasPermission(permission.toLowerCase())) {
      this.sendMessageHasAlreadyPermission(sender, user.getName(), permission);
      return;
    }

    UUID uuid = user.getUniqueId();

    if (Network.isUserOnline(uuid)) {
      user.addPermission(permission, mode, () -> Network.getUser(uuid).updatePermissions(true));
    } else {
      user.addPermission(permission, mode);
    }
    this.sendMessageAddedPermission(sender, user.getName(), permission, mode);
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
    sender.hasPermissionElseExit(this.playerGroupSetPerm);

    UUID uuid = user.getUniqueId();
    sender.hasGroupRankLowerElseExit(uuid, true);

    int groupRank = Database.getGroups().getPermGroup(groupName).getPriority();

    if (user.getPermGroup() != null) {
      if (!user.getPermGroup().getName().equals(groupName)
          && sender.hasGroupRankLower(groupRank, false)) {
        if (Network.isUserOnline(uuid)) {
          user.setPermGroup(groupName, () -> Network.getUser(uuid).updateGroup());
        } else {
          user.setPermGroup(groupName);
        }
        sender.sendPluginTDMessage("§sUpdated group of player §v" + user.getName() + "§s to §v" + groupName);
      } else {
        sender.sendPluginTDMessage("§v" + user.getName() + "§w is already in group §v" + groupName);
      }
    } else if (sender.hasGroupRankLower(groupRank, false)) {
      if (Network.isUserOnline(uuid)) {
        user.setPermGroup(groupName, () -> Network.getUser(uuid).updateGroup());
      } else {
        user.setPermGroup(groupName);
      }
      sender.sendPluginTDMessage("§sUpdated group of player §v" + user.getName() + "§s to §v" + groupName);
    }

  }

  public void removePlayerGroup(Sender sender, DbUser user) {
    sender.hasPermissionElseExit(this.playerGroupRemovePerm);

    if (user.getPermGroup() == null) {
      sender.sendPluginTDMessage("§wPlayer §v" + user.getName() + "§w has no group");
      return;
    }

    sender.hasGroupRankLowerElseExit(user.getUniqueId(), true);

    user.removePermGroup(() -> Network.getUser(user.getUniqueId()).updateGroup());
    sender.sendPluginTDMessage("§sCleared group of player §v" + user.getName());
  }

  public void addGroupPermission(Sender sender, String groupName, String permission, Status.Permission mode) {
    sender.hasPermissionElseExit(this.groupAddPerm);

    DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());

    if (!Database.getGroups().containsPermGroup(groupName)) {
      this.sendMessagePermGroupNotExists(sender, groupName);
      return;
    }

    if (group.hasPermission(permission.toLowerCase())) {
      this.sendMessageHasAlreadyPermission(sender, groupName, permission);
      return;
    }

    sender.hasGroupRankLowerElseExit(group.getPriority(), true);

    group.addPermission(permission, mode, () -> Network.getGroupManager().getPermGroup(groupName).updatePermissions());
    this.sendMessageAddedPermission(sender, groupName, permission, mode);

  }

  public void removeGroupPermission(Sender sender, String groupName, String permission) {
    sender.hasPermissionElseExit(this.groupRemovePerm);

    DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());

    if (!Database.getGroups().containsPermGroup(groupName)) {
      this.sendMessagePermGroupNotExists(sender, groupName);
    }

    if (!group.hasPermission(permission.toLowerCase())) {
      this.sendMessageHasNotPermission(sender, groupName, permission);
      return;
    }

    sender.hasGroupRankLowerElseExit(group.getPriority(), true);

    group.removePermission(permission, () -> Network.getGroupManager().getPermGroup(groupName).updatePermissions());
    this.sendMessageRemovedPermission(sender, groupName, permission);

  }

  public void createGroup(Sender sender, String groupName, int rank) {
    sender.hasPermissionElseExit(this.groupCreatePerm);

    DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());
    if (Database.getGroups().containsPermGroup(groupName)) {
      sender.sendPluginTDMessage("§wGroup §v" + group.getName() + "§w already exists");
    }

    sender.hasGroupRankLowerElseExit(rank, true);

    if (!(rank > 0)) {
      sender.sendPluginTDMessage("§wInvalid rank (rank: >0)");
      return;
    }

    Database.getGroups().addPermGroup(groupName.toLowerCase(), rank);

    sender.sendPluginTDMessage("§sCreated group §v" + groupName.toLowerCase());
    sender.sendPluginTDMessage("§wRestart all servers to load the new group");
  }

  public void deleteGroup(Sender sender, String groupName) {
    sender.hasPermissionElseExit(this.groupDeletePerm);

    DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());
    if (!Database.getGroups().containsPermGroup(groupName)) {
      this.sendMessagePermGroupNotExists(sender, groupName);
    }

    sender.hasGroupRankLowerElseExit(group.getPriority(), true);

    for (DbPermission perm : group.getPermissions()) {
      group.removePermission(perm.getPermission());
    }
    for (UUID uuid : Database.getUsers().getUsersUuid()) {
      DbUser user = Database.getUsers().getUser(uuid);
      if (user.getPermGroup().getName().equals(groupName.toLowerCase())) {
        user.removePermGroup();
      }
    }

    Database.getGroups().removePermGroup(group.getName());
    Network.getChannel().sendMessage(new ChannelGroupMessage<>(groupName, MessageType.Group.PERMISSION));
    sender.sendPluginTDMessage("§sDeleted group §v" + group.getName());
  }

  public void setGroupInheritance(Sender sender, String groupName, String inheritGroupName) {
    sender.hasPermissionElseExit(this.groupInheritanceSetPerm);

    DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());
    if (!Database.getGroups().containsPermGroup(groupName)) {
      this.sendMessagePermGroupNotExists(sender, groupName);
      return;
    }

    sender.hasGroupRankLowerElseExit(group.getPriority(), true);

    DbPermGroup inheritGroup = Database.getGroups().getPermGroup(inheritGroupName.toLowerCase());
    if (!(Database.getGroups().containsPermGroup(inheritGroupName)
          && sender.hasGroupRankLower(inheritGroup.getPriority(), true))) {
      return;
    }

    group.setInheritance(inheritGroup.getName(),
        () -> Network.getGroupManager().getPermGroup(groupName).updatePermissions());
    sender.sendPluginTDMessage("§sUpdated inheritance of group §v" + inheritGroup.getName() + "§s to §v" + group.getName());

  }

  public void removeGroupInheritance(Sender sender, String groupName) {
    sender.hasPermissionElseExit(this.groupInheritanceRemovePerm);

    DbPermGroup group = Database.getGroups().getPermGroup(groupName.toLowerCase());
    if (Database.getGroups().containsPermGroup(groupName)) {
      this.sendMessagePermGroupNotExists(sender, groupName);
      return;
    }

    sender.hasGroupRankLowerElseExit(group.getPriority(), true);

    group.removeInheritance(() -> Network.getGroupManager().getPermGroup(groupName).updatePermissions());
    sender.sendPluginTDMessage("§sRemoved inheritance from §v" + group.getName());
  }

  private void sendMessagePermGroupNotExists(Sender sender, String groupName) {
    sender.sendPluginTDMessage("§wGroup §v" + groupName + "§w does not exist");
  }

  private void sendMessageHasAlreadyPermission(Sender sender, String name, String permission) {
    sender.sendPluginTDMessage("§v" + name + "§w has already permission §v" + permission);
  }

  private void sendMessageAddedPermission(Sender sender, String name, String permission, Status.Permission mode) {
    sender.sendPluginTDMessage("§sAdded permission §v" + permission + "§s to §v" + name + "§s with mode §v" + mode.getShortName());
  }

  private void sendMessageRemovedPermission(Sender sender, String name, String permission) {
    sender.sendPluginTDMessage("§sRemoved permission §v" + permission + "§s from §v" + name);
  }

  private void sendMessageHasNotPermission(Sender sender, String name, String permission) {
    sender.sendPluginTDMessage("§v" + name + "§w has not permission §v" + permission);
  }

}
