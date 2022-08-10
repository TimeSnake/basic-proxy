package de.timesnake.basic.proxy.core.group;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.group.DbDisplayGroup;
import net.kyori.adventure.text.format.NamedTextColor;

public class DisplayGroup extends de.timesnake.library.extension.util.chat.DisplayGroup<NamedTextColor, User> {

    public DisplayGroup(DbDisplayGroup database) {
        super(database);
        Network.printText(Plugin.PROXY, "Loaded display-group " + this.name, "Group");
    }

    @Override
    public NamedTextColor loadPrefixColor(String chatColorName) {
        if (chatColorName != null) {
            return NamedTextColor.NAMES.value(chatColorName);
        } else {
            return NamedTextColor.WHITE;
        }
    }
}
