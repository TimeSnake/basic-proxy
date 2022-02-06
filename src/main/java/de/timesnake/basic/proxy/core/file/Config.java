package de.timesnake.basic.proxy.core.file;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Config {

    private static final File CONFIG_FILE = new File("plugins/basic-proxy/config.yml");
    private static Configuration config;

    public static void onEnable() {
        File dir = new File("plugins/basic-proxy");
        if (!dir.exists()) {
            dir.mkdir();
        }

        if (!CONFIG_FILE.exists()) {
            try {
                CONFIG_FILE.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            load();

            config.set("group.guest", "guest");

            save();
        }
    }

    public static void load() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(CONFIG_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void save() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, CONFIG_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String getGuestGroupName() {
        load();
        return config.getString("group.guest");
    }
}
