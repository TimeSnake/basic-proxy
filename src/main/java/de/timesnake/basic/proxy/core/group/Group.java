package de.timesnake.basic.proxy.core.group;

import de.timesnake.basic.proxy.core.permission.Permission;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.util.message.ChannelGroupMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.group.DbPermGroup;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Group {

    private final String name;
    private final DbPermGroup database;
    private final Integer rank;
    private final String tablistName;
    private final Set<User> users = new HashSet<>();
    private String prefix;
    private NamedTextColor prefixColor;
    private List<Permission> permissions = new ArrayList<>();

    public Group(String name) throws GroupNotInDatabaseException {
        this.name = name;
        DbPermGroup group = Database.getGroups().getPermGroup(name);
        if (group.getRank() == null) {
            throw new GroupNotInDatabaseException(name);
        }
        this.database = group;
        this.prefix = group.getPrefix();

        this.prefixColor = NamedTextColor.NAMES.value(group.getChatColorName());
        this.rank = group.getRank();

        int size = String.valueOf(this.rank).length();
        StringBuilder sb = new StringBuilder();
        sb.append("0".repeat(Math.max(0, 4 - size)));
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

    public NamedTextColor getPrefixColor() {
        return this.prefixColor;
    }

    public void setPrefixColor(NamedTextColor chatColor) {
        this.prefixColor = chatColor;
        this.database.setChatColorName(chatColor.toString());
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

        Network.getChannel().sendMessage(new ChannelGroupMessage<>(this.name, MessageType.Group.PERMISSION));
        System.out.println("[BasicSystem] Updated permissions for group " + this.name + " from database");

        for (DbPermGroup g : this.database.getGroupsInherit()) {
            Network.getGroup(g.getName()).updatePermissions();
        }

        for (User user : this.users) {
            user.updatePermissions();
        }
    }
}
