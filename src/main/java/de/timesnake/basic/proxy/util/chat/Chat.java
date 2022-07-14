package de.timesnake.basic.proxy.util.chat;

import de.timesnake.library.basic.util.chat.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.jetbrains.annotations.NotNull;

public interface Chat extends de.timesnake.library.extension.util.chat.Chat {

    static @NotNull Component getSplitter() {
        return Component.text("» ").color(NamedTextColor.DARK_GRAY);
    }

    static Component getOtherSplitter() {
        return Component.text("» ").color(NamedTextColor.DARK_PURPLE);
    }

    static Component getLineSeparator() {
        return Component.text("----------").color(NamedTextColor.WHITE);
    }

    static Component getLongLineSeparator() {
        return Component.text("---------------").color(NamedTextColor.WHITE);
    }

    static Component getDoubleLineSeparator() {
        return Component.text("==========").color(NamedTextColor.WHITE);
    }

    static Component getSenderPlugin(Plugin plugin) {
        return Component.text(plugin.getName()).color(NamedTextColor.DARK_AQUA).append(getSplitter());
    }

    static String componentToString(Component component) {
        StringBuilder sb = new StringBuilder();
        ComponentFlattener.basic().flatten(component, sb::append);
        return sb.toString();
    }


}
