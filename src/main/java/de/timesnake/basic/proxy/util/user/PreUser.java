package de.timesnake.basic.proxy.util.user;

import de.timesnake.basic.proxy.core.group.DisplayGroup;
import de.timesnake.basic.proxy.core.group.PermGroup;
import de.timesnake.basic.proxy.core.user.UserNotInDatabaseException;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.group.DbPermGroup;
import de.timesnake.database.util.permission.DbPermission;
import de.timesnake.database.util.user.DataProtectionAgreement;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.extension.util.permission.ExPermission;
import net.kyori.adventure.text.Component;

import java.util.*;

public class PreUser {

    private final UUID uuid;

    private final String name;

    private final DbUser dbUser;

    private final boolean service;

    private final boolean airMode;

    private final DataProtectionAgreement dataProtectionAgreement;

    private final PermGroup permGroup;
    private final SortedSet<DisplayGroup> displayGroups;

    private final Component prefix;
    private final Component suffix;
    private final Component nick;

    private final float coins;

    private final Collection<ExPermission> databasePermissions = new HashSet<>();

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

        this.prefix = Component.text(dbLocalUser.getPrefix());

        this.suffix = Component.text(dbLocalUser.getSuffix());

        this.nick = Component.text(dbLocalUser.getNick());

        DbPermGroup dbGroup = dbLocalUser.getPermGroup();
        String permGroupName;
        if (dbGroup != null) {
            permGroupName = dbGroup.getName();
        } else {
            permGroupName = Network.getGuestGroup().getName();
        }

        this.permGroup = Network.getPermGroup(permGroupName);

        this.displayGroups = new TreeSet<>(Comparator.comparingInt(DisplayGroup::getRank));
        for (String groupName : dbLocalUser.getDisplayGroupNames()) {
            this.displayGroups.add(Network.getDisplayGroup(groupName));
        }

        if (this.displayGroups.isEmpty()) {
            this.displayGroups.add(Network.getGuestDisplayGroup());
        }

        this.service = dbLocalUser.isService();

        this.coins = dbLocalUser.getCoins();

        this.dataProtectionAgreement = dbLocalUser.getDataProtectionAgreement();

        for (DbPermission perm : dbLocalUser.getPermissions()) {
            this.databasePermissions.add(new ExPermission(perm.getName(), perm.getMode(), perm.getServers()));
        }
        this.databasePermissions.addAll(this.permGroup.getPermissions());
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

    public boolean isAirMode() {
        return airMode;
    }

    public DataProtectionAgreement getDataProtectionAgreement() {
        return dataProtectionAgreement;
    }

    public PermGroup getPermGroup() {
        return permGroup;
    }

    public SortedSet<DisplayGroup> getDisplayGroups() {
        return displayGroups;
    }

    public DisplayGroup getMainDisplayGroup() {
        return displayGroups.first();
    }

    public Component getPrefix() {
        return prefix;
    }

    public Component getSuffix() {
        return suffix;
    }

    public Component getNick() {
        return nick;
    }

    public float getCoins() {
        return coins;
    }

    public Collection<ExPermission> getDatabasePermissions() {
        return databasePermissions;
    }
}
