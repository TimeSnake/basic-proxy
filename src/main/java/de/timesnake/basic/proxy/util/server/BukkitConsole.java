package de.timesnake.basic.proxy.util.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.channel.api.message.ChannelServerMessage;

import java.io.IOException;

public abstract class BukkitConsole {

    public static final String START_FILE = "start.sh";
    private final String folderPath;

    public BukkitConsole(String folderPath) {
        this.folderPath = folderPath;
    }

    public boolean start() {
        try {
            Runtime.getRuntime().exec("konsole --separate --workdir " + this.folderPath + " -e " + this.folderPath + "/start.sh");
            return true;
        } catch (IOException var2) {
            var2.printStackTrace();
            return false;
        }
    }

    public void stop() {
        Network.getChannel().sendMessage(this.getPort(), ChannelServerMessage.getCommandMessage(this.getPort(), "stop"));
    }

    public void execute(String cmd) {
        Network.getChannel().sendMessage(this.getPort(), ChannelServerMessage.getCommandMessage(this.getPort(), cmd));
    }

    public String getFolderPath() {
        return this.folderPath;
    }

    public abstract Integer getPort();
}
