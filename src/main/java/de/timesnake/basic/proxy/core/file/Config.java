package de.timesnake.basic.proxy.core.file;

import de.timesnake.basic.proxy.util.file.ExFile;

public class Config extends ExFile {

    public Config() {
        super("basic-proxy", "config.toml");
        this.load();
    }

    public String getGuestGroupName() {
        return config.getString("group.guest");
    }

    public String getNetworkPath() {
        return config.getString("network.path");
    }

    public String getVelocitySecret() {
        return config.getString("network.velocity-secret");
    }

    public Boolean isTmuxEnabled() {
        return config.getBoolean("network.tmux");
    }

}
