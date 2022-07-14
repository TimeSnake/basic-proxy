package de.timesnake.basic.proxy.util.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;

import java.io.IOException;
import java.nio.file.Path;

public abstract class BukkitConsole {

    private final Path folderPath;

    public BukkitConsole(Path folderPath) {
        this.folderPath = folderPath;
    }

    public boolean start() {
        try {
            Runtime.getRuntime().exec("konsole --separate --workdir " + this.folderPath + " -e " +
                    this.folderPath + "/start.sh " + this.getServerTask());
            return true;
        } catch (IOException var2) {
            var2.printStackTrace();
            return false;
        }
    }

    public void stop() {
        Network.getChannel().sendMessage(new ChannelServerMessage<>(this.getPort(), MessageType.Server.COMMAND, "stop"
        ));
    }

    public void execute(String cmd) {
        Network.getChannel().sendMessage(new ChannelServerMessage<>(this.getPort(), MessageType.Server.COMMAND, cmd));
    }

    public Path getFolderPath() {
        return this.folderPath;
    }

    public abstract Integer getPort();

    public abstract String getServerTask();
}
