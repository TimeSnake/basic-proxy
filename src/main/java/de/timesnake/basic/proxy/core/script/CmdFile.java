package de.timesnake.basic.proxy.core.script;

import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.file.ExFile;
import de.timesnake.library.extension.util.chat.Plugin;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CmdFile extends ExFile {

    private List<String> startCommands;

    public CmdFile() {
        super("basic-proxy", "commands.toml");
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
            int delay = 3;
            for (String cmd : this.startCommands) {
                BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), () -> {
                    Network.printText(Plugin.NETWORK, "Executing command: " + cmd, "Commands");
                    Network.runCommand(cmd);
                }).delay(delay, TimeUnit.SECONDS).schedule();
                delay += 3;
            }
        }
    }

    private List<String> getCommandList() {
        this.load();
        return super.getList("start");
    }

}
