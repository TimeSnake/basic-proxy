package de.timesnake.basic.proxy.core.group;

import de.timesnake.basic.proxy.core.permission.Permission;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.api.message.ChannelGroupMessage;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.group.DbPermGroup;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Group {

    private final String name;
    private final DbPermGroup database;
    private final Integer rank;
    private String prefix;
    private ChatColor prefixColor;
    private final String tablistName;
    private final Set<User> users = new HashSet<>();

    private List<Permission> permissions = new ArrayList<>();

    public Group(String name) throws GroupNotInDatabaseException {
        this.name = name;
        DbPermGroup group = Database.getGroups().getPermGroup(name);
        if (group.getRank() == null) {
            throw new GroupNotInDatabaseException(name);
        }
        this.database = group;
        this.prefix = group.getPrefix();

        this.prefixColor = ChatColor.of(group.getChatColorName());
        this.rank = group.getRank();

        int size = String.valueOf(this.rank).length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4 - size; i++) {
            sb.append("0");
        }
        this.tablistName = sb.append(this.rank).append("a").toString();
    }

    public String getName() {
        return this.name;
    }

    public Integer getRank() {
        return this.rank;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        this.database.setPrefix(prefix);
    }

    public ChatColor getPrefixColor() {
        return this.prefixColor;
    }

    public void setPrefixColor(ChatColor chatColor) {
        this.prefixColor = chatColor;
        this.database.setChatColorName(chatColor.getName());
    }

    public String getTablistName() {
        return this.tablistName;
    }

    public void addUser(User user) {
        this.users.add(user);
    }

    public void removeUser(User user) {
        this.users.remove(user);
    }

    public Set<User> getUsers() {
        return this.users;
    }

    public List<Permission> getPermissions() {
        return this.permissions;
    }

    public void updatePermissions() {
        ArrayList<Permission> permissions = new ArrayList<>();

        DbPermGroup group = this.database;
        while (Database.getGroups().containsGroup(group.getRank())) {
            permissions.addAll(Network.getGroup(group.getName()).getPermissions());
            group = group.getInheritance();
            if (group == null) {
                break;
            }
        }
        this.permissions = permissions;

        Network.getChannel().sendMessage(ChannelGroupMessage.getPermissionMessage(this.name));
        System.out.println("[BasicSystem] Updated permissions for group " + this.name + " from database");

        for (DbPermGroup g : this.database.getGroupsInherit()) {
            Network.getGroup(g.getName()).updatePermissions();
        }

        for (User user : this.users) {
            user.updatePermissions();
        }
    }
}
