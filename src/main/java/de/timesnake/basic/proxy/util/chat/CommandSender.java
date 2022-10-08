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

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import net.kyori.adventure.text.Component;

public class CommandSender implements de.timesnake.library.extension.util.cmd.CommandSender {

    private final CommandSource cmdSender;

    public CommandSender(CommandSource cmdSender) {
        this.cmdSender = cmdSender;
    }

    @Override
    public void sendMessage(String s) {
        this.cmdSender.sendMessage(Component.text(s));
    }

    @Override
    public void sendMessage(String[] messages) {
        for (String s : messages) {
            this.sendMessage(s);
        }
    }

    @Override
    public void sendMessage(Component... components) {
        for (Component component : components) {
            this.cmdSender.sendMessage(component);
        }
    }

    public void sendMessage(Component message) {
        this.cmdSender.sendMessage(message);
    }

    @Override
    public String getName() {
        if (cmdSender instanceof ConsoleCommandSource) {
            return "console";
        } else if (cmdSender instanceof Player) {
            return ((Player) cmdSender).getUsername();
        }
        return "unknown";
    }

    @Override
    public boolean hasPermission(String s) {
        return this.cmdSender.hasPermission(s);
    }

    @Override
    public boolean isConsole() {
        return cmdSender instanceof ConsoleCommandSource;
    }

    @Override
    public Player getPlayer() {
        return (Player) this.cmdSender;
    }

    @Override
    public User getUser() {
        return this.cmdSender instanceof Player ? Network.getUser((Player) this.cmdSender) : null;
    }
}
