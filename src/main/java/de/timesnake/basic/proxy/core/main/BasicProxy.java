/*
 * workspace.basic-proxy.main
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

package de.timesnake.basic.proxy.core.main;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import de.timesnake.basic.proxy.cmd.*;
import de.timesnake.basic.proxy.core.channel.ChannelCmdHandler;
import de.timesnake.basic.proxy.core.group.DisplayGroupCmd;
import de.timesnake.basic.proxy.core.network.MotdManager;
import de.timesnake.basic.proxy.core.network.ServiceWorkCmd;
import de.timesnake.basic.proxy.core.permission.PermissionCheck;
import de.timesnake.basic.proxy.core.permission.PermissionCmd;
import de.timesnake.basic.proxy.core.permission.PermissionTestCmd;
import de.timesnake.basic.proxy.core.punishment.KickAllCmd;
import de.timesnake.basic.proxy.core.punishment.PunishCmd;
import de.timesnake.basic.proxy.core.server.*;
import de.timesnake.basic.proxy.core.user.ChatManager;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.NetworkManager;
import de.timesnake.channel.core.NetworkChannel;
import de.timesnake.database.util.Database;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

@Plugin(id = "basic-proxy", name = "BasicProxy", version = "1.0-SNAPSHOT", url = "https://git.timesnake.de", authors = {"MarkusNils"}, dependencies = {@Dependency(id = "database-proxy"), @Dependency(id = "channel-proxy")})
public class BasicProxy {


    public static final String DATA_PROTECTION_VERSION = "2.0";

    public static BasicProxy getPlugin() {
        return plugin;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static ProxyServer getServer() {
        return server;
    }

    public static EventManager getEventManager() {
        return server.getEventManager();
    }

    public static PluginManager getPluginManager() {
        return server.getPluginManager();
    }

    public static CommandManager getCommandManager() {
        return server.getCommandManager();
    }

    private static BasicProxy plugin;
    private static ProxyServer server;
    private static Logger logger;

    @Inject
    public BasicProxy(ProxyServer server, Logger logger) {
        BasicProxy.server = server;
        BasicProxy.logger = logger;

        BasicProxy.plugin = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        NetworkManager.getInstance().onEnable();

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "permission", List.of("perm", "perms"), new PermissionCmd(), de.timesnake.basic.proxy.util.chat.Plugin.PERMISSION);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "alias", new AliasCmd(), de.timesnake.basic.proxy.util.chat.Plugin.ALIAS);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "work", new ServiceWorkCmd(), de.timesnake.basic.proxy.util.chat.Plugin.NETWORK);
        NetworkManager.getInstance().getCommandHandler().addCommand(this, "ban", List.of("netban"), new PunishCmd(), de.timesnake.basic.proxy.util.chat.Plugin.PUNISH);
        NetworkManager.getInstance().getCommandHandler().addCommand(this, "tempban", List.of("nettempban", "tmpban", "nettmpmban"), new PunishCmd(), de.timesnake.basic.proxy.util.chat.Plugin.PUNISH);
        NetworkManager.getInstance().getCommandHandler().addCommand(this, "unban", List.of("netunban", "pardon"), new PunishCmd(), de.timesnake.basic.proxy.util.chat.Plugin.PUNISH);
        NetworkManager.getInstance().getCommandHandler().addCommand(this, "mute", List.of("netmute"), new PunishCmd(), de.timesnake.basic.proxy.util.chat.Plugin.PUNISH);
        NetworkManager.getInstance().getCommandHandler().addCommand(this, "unmute", List.of("netunmute"), new PunishCmd(), de.timesnake.basic.proxy.util.chat.Plugin.PUNISH);
        NetworkManager.getInstance().getCommandHandler().addCommand(this, "kick", List.of("netkick"), new PunishCmd(), de.timesnake.basic.proxy.util.chat.Plugin.PUNISH);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "start", new StartCmd(), de.timesnake.basic.proxy.util.chat.Plugin.NETWORK);
        NetworkManager.getInstance().getCommandHandler().addCommand(this, "cmd", new ServerCmd(), de.timesnake.basic.proxy.util.chat.Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "netmessages", List.of("netmsg", "networkmsg", "networkmessages", "networkmessage", "netmsgs"), new NetworkMsgCmd(), de.timesnake.basic.proxy.util.chat.Plugin.SUPPORT);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "timecoins", new CoinsCmd(), de.timesnake.basic.proxy.util.chat.Plugin.TIME_COINS);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "permcheck", new PermissionTestCmd(), de.timesnake.library.extension.util.chat.Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "air", List.of("airmode", "am"), new AirModeCmd(), de.timesnake.library.extension.util.chat.Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "rule", List.of("rules", "regeln", "regel"), new RuleCmd(), de.timesnake.library.extension.util.chat.Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "pid", new PidCmd(), de.timesnake.basic.proxy.util.chat.Plugin.SYSTEM);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "kickall", new KickAllCmd(), de.timesnake.basic.proxy.util.chat.Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "dtmp", List.of("delete_tmp"), new DeleteTmpServerCmd(), de.timesnake.library.extension.util.chat.Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "dgroup", List.of("displaygroup", "dg"), new DisplayGroupCmd(), de.timesnake.basic.proxy.util.chat.Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "build", new MapBuildCmd(), de.timesnake.library.extension.util.chat.Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, "network", new NetworkCmd(), de.timesnake.library.extension.util.chat.Plugin.NETWORK);

        EventManager em = server.getEventManager();

        em.register(this, new MotdManager());
        em.register(this, new PermissionCheck());
        em.register(this, new ChatManager());
        em.register(this, new FallbackManager());

        NetworkManager.getInstance().getChannel().addListener(new ChannelCmdHandler());

    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent e) {
        for (Path tmpServerPath : Network.getTmpServerDirs().values()) {
            try {
                FileUtils.deleteDirectory(tmpServerPath.toFile());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        getLogger().info("Deleted tmp servers");
        NetworkChannel.stop();
        Database.getInstance().closeWithBackups();
    }
}
