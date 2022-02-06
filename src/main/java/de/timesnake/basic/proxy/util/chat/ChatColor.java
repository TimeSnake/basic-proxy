package de.timesnake.basic.proxy.util.chat;

public interface ChatColor {

    net.md_5.bungee.api.ChatColor PUBLIC = net.md_5.bungee.api.ChatColor.WHITE;
    net.md_5.bungee.api.ChatColor PERSONAL = net.md_5.bungee.api.ChatColor.YELLOW;
    net.md_5.bungee.api.ChatColor VALUE = net.md_5.bungee.api.ChatColor.GRAY;
    net.md_5.bungee.api.ChatColor WARNING = net.md_5.bungee.api.ChatColor.RED;
    net.md_5.bungee.api.ChatColor QUICK_INFO = net.md_5.bungee.api.ChatColor.BLACK;

    net.md_5.bungee.api.ChatColor BLACK = net.md_5.bungee.api.ChatColor.BLACK;
    net.md_5.bungee.api.ChatColor DARK_BLUE = net.md_5.bungee.api.ChatColor.DARK_BLUE;
    net.md_5.bungee.api.ChatColor DARK_GREEN = net.md_5.bungee.api.ChatColor.DARK_GREEN;
    net.md_5.bungee.api.ChatColor DARK_AQUA = net.md_5.bungee.api.ChatColor.DARK_AQUA;
    net.md_5.bungee.api.ChatColor DARK_RED = net.md_5.bungee.api.ChatColor.DARK_RED;
    net.md_5.bungee.api.ChatColor DARK_PURPLE = net.md_5.bungee.api.ChatColor.DARK_PURPLE;
    net.md_5.bungee.api.ChatColor GOLD = net.md_5.bungee.api.ChatColor.GOLD;
    net.md_5.bungee.api.ChatColor GRAY = net.md_5.bungee.api.ChatColor.GRAY;
    net.md_5.bungee.api.ChatColor DARK_GRAY = net.md_5.bungee.api.ChatColor.DARK_GRAY;
    net.md_5.bungee.api.ChatColor BLUE = net.md_5.bungee.api.ChatColor.BLUE;
    net.md_5.bungee.api.ChatColor GREEN = net.md_5.bungee.api.ChatColor.GREEN;
    net.md_5.bungee.api.ChatColor AQUA = net.md_5.bungee.api.ChatColor.AQUA;
    net.md_5.bungee.api.ChatColor RED = net.md_5.bungee.api.ChatColor.RED;
    net.md_5.bungee.api.ChatColor LIGHT_PURPLE = net.md_5.bungee.api.ChatColor.LIGHT_PURPLE;
    net.md_5.bungee.api.ChatColor YELLOW = net.md_5.bungee.api.ChatColor.YELLOW;
    net.md_5.bungee.api.ChatColor WHITE = net.md_5.bungee.api.ChatColor.WHITE;
    net.md_5.bungee.api.ChatColor MAGIC = net.md_5.bungee.api.ChatColor.MAGIC;
    net.md_5.bungee.api.ChatColor BOLD = net.md_5.bungee.api.ChatColor.BOLD;
    net.md_5.bungee.api.ChatColor STRIKETHROUGH = net.md_5.bungee.api.ChatColor.STRIKETHROUGH;
    net.md_5.bungee.api.ChatColor UNDERLINE = net.md_5.bungee.api.ChatColor.UNDERLINE;
    net.md_5.bungee.api.ChatColor ITALIC = net.md_5.bungee.api.ChatColor.ITALIC;
    net.md_5.bungee.api.ChatColor RESET = net.md_5.bungee.api.ChatColor.RESET;

    static String translateAlternateColorCodes(char c, String prefix) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes(c, prefix);
    }

}
