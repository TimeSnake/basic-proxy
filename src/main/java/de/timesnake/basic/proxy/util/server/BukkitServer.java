/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import java.io.IOException;
import java.nio.file.Path;

public abstract class BukkitServer {

    protected final String name;
    protected final Path folderPath;

    public BukkitServer(String name, Path folderPath) {
        this.name = name;
        this.folderPath = folderPath;
    }

    public boolean start() {
        if (Network.isTmuxEnabled()) {
            try {
                new ProcessBuilder().command("/bin/bash", "-c", "tmux new-window -n " + this.name + " -t " +
                        Network.TMUX_SESSION_NAME + ": " + this.folderPath.toAbsolutePath() + "/start.sh " +
                        this.getServerTask()).start();
                return true;
            } catch (IOException var2) {
                var2.printStackTrace();
                return false;
            }
        } else {
            try {
                Runtime.getRuntime().exec("konsole --separate --workdir " + this.folderPath + " -e " +
                        this.folderPath + "/start.sh " + this.getServerTask());
                return true;
            } catch (IOException var2) {
                var2.printStackTrace();
                return false;
            }
        }
    }

    public void stop() {
        this.execute("stop");
    }

    public void execute(String cmd) {
        Network.getChannel().sendMessage(new ChannelServerMessage<>(this.getName(), MessageType.Server.COMMAND, cmd));
    }

    public String getName() {
        return name;
    }

    public Path getFolderPath() {
        return this.folderPath;
    }

    public abstract Integer getPort();

    public abstract String getServerTask();
}
