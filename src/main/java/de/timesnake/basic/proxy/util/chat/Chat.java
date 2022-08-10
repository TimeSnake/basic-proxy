package de.timesnake.basic.proxy.util.chat;

import de.timesnake.library.basic.util.chat.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.format.TextDecoration;
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

    static String parseComponentToString(Component component) {
        StringBuilder sb = new StringBuilder();
        ComponentFlattener.basic().flatten(component, sb::append);
        return sb.toString();
    }

    static Component parseStringToComponent(String string) {
        Component component = Component.text("");
        for (String part : string.split("§")) {
            part = part.substring(1);
            net.kyori.adventure.text.format.NamedTextColor color = parseChatColor(part.charAt(0));
            TextDecoration decoration = parseChatColorDecoration(part.charAt(0));
            if (color != null) {
                component = component.append(Component.text(part).color(color));
            } else if (decoration != null) {
                component = component.append(Component.text(part).decorate(decoration));
            } else {
                component = component.append(Component.text("§" + part));
            }
        }
        return component;
    }

    static net.kyori.adventure.text.format.NamedTextColor parseChatColor(char colorCode) {
        return switch (colorCode) {
            case '0' -> net.kyori.adventure.text.format.NamedTextColor.BLACK;
            case '1' -> net.kyori.adventure.text.format.NamedTextColor.DARK_BLUE;
            case '2' -> net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN;
            case '3' -> net.kyori.adventure.text.format.NamedTextColor.DARK_AQUA;
            case '4' -> net.kyori.adventure.text.format.NamedTextColor.DARK_RED;
            case '5' -> net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE;
            case '6' -> net.kyori.adventure.text.format.NamedTextColor.GOLD;
            case '7' -> net.kyori.adventure.text.format.NamedTextColor.GRAY;
            case '8' -> net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
            case '9' -> net.kyori.adventure.text.format.NamedTextColor.BLUE;
            case 'a' -> net.kyori.adventure.text.format.NamedTextColor.GREEN;
            case 'b' -> net.kyori.adventure.text.format.NamedTextColor.AQUA;
            case 'c' -> net.kyori.adventure.text.format.NamedTextColor.RED;
            case 'd' -> net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
            case 'e' -> net.kyori.adventure.text.format.NamedTextColor.YELLOW;
            case 'f' -> net.kyori.adventure.text.format.NamedTextColor.WHITE;
            default -> null;
        };
    }

    static TextDecoration parseChatColorDecoration(char actionCode) {
        return switch (actionCode) {
            case 'k' -> TextDecoration.OBFUSCATED;
            case 'l' -> TextDecoration.BOLD;
            case 'm' -> TextDecoration.STRIKETHROUGH;
            case 'n' -> TextDecoration.UNDERLINED;
            case 'o' -> TextDecoration.ITALIC;
            default -> null;
        };
    }
}
