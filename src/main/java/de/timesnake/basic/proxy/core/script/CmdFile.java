/*
 * basic-proxy.main
 * Copyright (C) 2022 timesnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

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
