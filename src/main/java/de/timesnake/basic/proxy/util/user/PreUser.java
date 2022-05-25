package de.timesnake.basic.proxy.util.user;

import de.timesnake.basic.proxy.core.group.Group;
import de.timesnake.basic.proxy.core.permission.Permission;
import de.timesnake.basic.proxy.core.user.UserNotInDatabaseException;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.group.DbPermGroup;
import de.timesnake.database.util.permission.DbPermission;
import de.timesnake.database.util.user.DataProtectionAgreement;
import de.timesnake.database.util.user.DbUser;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PreUser {

    private final UUID uuid;

    private final String name;

    private final DbUser dbUser;

    private final boolean service;
    private final String chatName;

    private final boolean airMode;

    private final DataProtectionAgreement dataProtectionAgreement;

    private final Group group;

    private final String prefix;
    private final String suffix;
    private final String nick;

    private final float coins;

    private final Set<Permission> databasePermissions = new HashSet<>();
    private final Set<Permission> permissions = new HashSet<>();

    public PreUser(String name) throws UserNotInDatabaseException {
        if (!Database.getUsers().containsUser(name)) {
            throw new UserNotInDatabaseException();
        }

        this.dbUser = Database.getUsers().getUser(name);
        this.dbUser.checkEntries();

        this.uuid = dbUser.getUniqueId();

        DbUser dbLocalUser = this.dbUser.toLocal();

        this.name = dbLocalUser.getName();

        this.airMode = dbLocalUser.isAirMode();

        this.prefix = dbLocalUser.getPrefix();

        this.suffix = dbLocalUser.getSuffix();

        this.nick = dbLocalUser.getNick();

        DbPermGroup dbGroup = dbLocalUser.getPermGroup();
        String groupName;
        if (dbGroup != null) {
            groupName = dbGroup.getName();
        } else {
            groupName = Network.getGuestGroup().getName();
        }

        this.group = Network.getGroup(groupName);

        this.service = dbLocalUser.isService();

        this.coins = dbLocalUser.getCoins();

        String chatName;

        Group group = this.getGroup();
        if (this.getNick() == null) {
            String chatPrefix = "&1";
            String chatSuffix = "&1";
            if (this.getPrefix() != null) {
                chatPrefix = this.getPrefix();
            }

            if (this.getSuffix() != null) {
                chatSuffix = this.getSuffix();
            }

            chatName =
                    group.getPrefixColor() + group.getPrefix() + ChatColor.RESET + net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', chatPrefix) + ChatColor.RESET + this.getName() + ChatColor.RESET + net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', chatSuffix) + ChatColor.RESET;
        } else {
            group = Network.getMemberGroup();
            chatName =
                    group.getPrefixColor() + group.getPrefix() + ChatColor.RESET + net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', this.getNick());
        }

        this.chatName = chatName;

        this.dataProtectionAgreement = dbLocalUser.getDataProtectionAgreement();

        for (DbPermission perm : dbLocalUser.getPermissions()) {
            this.databasePermissions.add(new Permission(perm.getName(), perm.getMode(), perm.getServers()));
        }

        if (this.group != null) {
            while (Database.getGroups().containsGroup(dbGroup.getName())) {
                for (DbPermission perm : dbGroup.getPermissions()) {
                    this.databasePermissions.add(new Permission(perm.getName(), perm.getMode(), perm.getServers()));
                }
                dbGroup = dbGroup.getInheritance();
                if (dbGroup == null) {
                    break;
                }
            }
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public DbUser getDbUser() {
        return dbUser;
    }

    public boolean isService() {
        return service;
    }

    public String getChatName() {
        return chatName;
    }

    public boolean isAirMode() {
        return airMode;
    }

    public DataProtectionAgreement getDataProtectionAgreement() {
        return dataProtectionAgreement;
    }

    public Group getGroup() {
        return group;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getNick() {
        return nick;
    }

    public float getCoins() {
        return coins;
    }

    public Set<Permission> getDatabasePermissions() {
        return databasePermissions;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}
