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

package de.timesnake.basic.proxy.util.chat;

import com.velocitypowered.api.proxy.Player;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.extension.util.chat.Plugin;
import net.kyori.adventure.text.Component;

public class Sender extends de.timesnake.library.extension.util.cmd.Sender {

    public Sender(CommandSender cmdSender, Plugin plugin) {
        super(cmdSender, plugin);
    }

    public Player getPlayer() {
        return BasicProxy.getServer().getPlayer(this.cmdSender.getName()).get();
    }

    public String getChatName() {
        return null;
    }

    public User getUser() {
        return this.cmdSender.getUser();
    }

    @Override
    public void sendConsoleMessage(String message) {
        Network.printText(Plugin.PROXY, message);
    }

    public void sendMessage(Component component) {
        this.cmdSender.sendMessage(component);
    }

    public void sendPluginMessage(Component component) {
        this.cmdSender.sendMessage(Chat.getSenderPlugin(this.plugin).append(component));
    }
}
