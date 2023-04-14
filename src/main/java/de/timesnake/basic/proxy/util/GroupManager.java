/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util;

import de.timesnake.basic.proxy.core.group.DisplayGroup;
import de.timesnake.basic.proxy.core.group.PermGroup;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.group.DbDisplayGroup;
import de.timesnake.database.util.group.DbPermGroup;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class GroupManager {

    private final HashMap<String, PermGroup> permGroups = new HashMap<>();
    private final HashMap<String, DisplayGroup> displayGroups = new HashMap<>();

    public GroupManager() {

    }

    public void loadGroups() {
        for (DbPermGroup dbGroup : Database.getGroups().getPermGroups()) {
            PermGroup group = new PermGroup(dbGroup);
            this.permGroups.put(group.getName(), group);
        }

        ArrayList<PermGroup> groups = new ArrayList<>(this.getPermGroups().values());
        groups.sort(PermGroup::compareTo);
        groups.sort(Comparator.reverseOrder());
        for (PermGroup group : groups) {
            group.updatePermissions(false);
        }

        for (DbDisplayGroup dbGroup : Database.getGroups().getDisplayGroups()) {
            DisplayGroup group = new DisplayGroup(dbGroup);
            this.displayGroups.put(group.getName(), group);
        }
    }

    public HashMap<String, DisplayGroup> getDisplayGroups() {
        return displayGroups;
    }

    public DisplayGroup getDisplayGroup(String name) {
        return this.displayGroups.get(name);
    }

    public HashMap<String, PermGroup> getPermGroups() {
        return permGroups;
    }

    public PermGroup getPermGroup(String name) {
        return this.permGroups.get(name);
    }

    public PermGroup getGuestPermGroup() {
        return this.permGroups.get(Network.GUEST_PERM_GROUP_NAME);
    }

    public DisplayGroup getGuestDisplayGroup() {
        return this.displayGroups.get(Network.GUEST_DISPLAY_GROUP_NAME);
    }

    public PermGroup getMemberPermGroup() {
        return this.permGroups.get(Network.MEMBER_PERM_GROUP_NAME);
    }

    public DisplayGroup getMemberDisplayGroup() {
        return this.displayGroups.get(Network.MEMBER_DISPLAY_GROUP_NAME);
    }
}
