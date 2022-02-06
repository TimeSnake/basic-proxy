package de.timesnake.basic.proxy.core.script;

import de.timesnake.basic.proxy.util.NetworkManager;
import de.timesnake.library.basic.util.chat.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CmdFile {

    private final File configFile = new File("plugins/basic-proxy/commands.yml");
    private Configuration config;
    private List<String> startCommands;

    private Integer delay;

    public void onEnable() {

        //ConfigFile
        File dir = new File("plugins/basic-proxy");
        if (!dir.exists()) {
            dir.mkdir();
        }


        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            this.load();

            config.set("start", List.of("help"));

            this.save();
        }

        this.loadStartCommands();
    }

    private void loadStartCommands() {
        this.startCommands = this.getCommandList();
    }

    public List<String> getStartCommands() {
        return this.startCommands;
    }

    public void executeStartCommands() {
        if (this.startCommands != null) {
            this.delay = 3;
            for (String cmd : this.startCommands) {
                new Thread(() -> {
                    try {
                        Thread.sleep(delay * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    NetworkManager.getInstance().printText(Plugin.NETWORK, "Executing command: " + cmd, "Commands");
                    NetworkManager.getInstance().runCommand(cmd);
                }).start();
                delay += 3;
            }
        }
    }

    public void load() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> getCommandList() {
        this.load();
        return config.getStringList("start");
    }

}
