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
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import de.timesnake.basic.proxy.core.channel.ChannelCmdHandler;
import de.timesnake.basic.proxy.core.cmd.*;
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
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.channel.proxy.main.ChannelProxy;
import de.timesnake.database.util.Database;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

@com.velocitypowered.api.plugin.Plugin(id = "basic-proxy", name = "BasicProxy", version = "1.0-SNAPSHOT",
        url = "https://git.timesnake.de", authors = {"MarkusNils"},
        dependencies = {@Dependency(id = "database-proxy"), @Dependency(id = "channel-proxy")})
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

        Network.getCommandHandler().addCommand(this, "permission", List.of("perm", "perms"), new PermissionCmd(), Plugin.PERMISSION);

        Network.getCommandHandler().addCommand(this, "alias", new AliasCmd(), Plugin.ALIAS);

        Network.getCommandHandler().addCommand(this, "work", new ServiceWorkCmd(), Plugin.NETWORK);
        Network.getCommandHandler().addCommand(this, "ban", List.of("netban"),
                new PunishCmd(), Plugin.PUNISH);
        Network.getCommandHandler().addCommand(this, "tempban", List.of("nettempban", "tmpban", "nettmpmban"),
                new PunishCmd(), Plugin.PUNISH);
        Network.getCommandHandler().addCommand(this, "unban", List.of("netunban", "pardon"),
                new PunishCmd(), Plugin.PUNISH);
        Network.getCommandHandler().addCommand(this, "mute", List.of("netmute"),
                new PunishCmd(), Plugin.PUNISH);
        Network.getCommandHandler().addCommand(this, "unmute", List.of("netunmute"),
                new PunishCmd(), Plugin.PUNISH);
        Network.getCommandHandler().addCommand(this, "kick", List.of("netkick"),
                new PunishCmd(), Plugin.PUNISH);

        Network.getCommandHandler().addCommand(this, "start", new StartCmd(), Plugin.NETWORK);
        Network.getCommandHandler().addCommand(this, "cmd", new ServerCmd(), Plugin.NETWORK);

        Network.getCommandHandler().addCommand(this, "netmessages",
                List.of("netmsg", "networkmsg", "networkmessages", "networkmessage", "netmsgs"),
                new NetworkMsgCmd(), Plugin.SUPPORT);

        Network.getCommandHandler().addCommand(this, "timecoins", new CoinsCmd(), Plugin.TIME_COINS);

        Network.getCommandHandler().addCommand(this, "permcheck", new PermissionTestCmd(), Plugin.NETWORK);

        Network.getCommandHandler().addCommand(this, "air", List.of("airmode", "am"),
                new AirModeCmd(), Plugin.NETWORK);

        Network.getCommandHandler().addCommand(this, "rule", List.of("rules", "regeln", "regel"),
                new RuleCmd(), Plugin.NETWORK);

        Network.getCommandHandler().addCommand(this, "pid", new PidCmd(), Plugin.SYSTEM);

        Network.getCommandHandler().addCommand(this, "kickall", new KickAllCmd(), Plugin.NETWORK);

        Network.getCommandHandler().addCommand(this, "dtmp", List.of("delete_tmp"),
                new DeleteTmpServerCmd(), Plugin.NETWORK);
        Network.getCommandHandler().addCommand(this, "cleanup_servers",
                new CleanupServersCmd(), Plugin.SYSTEM);

        Network.getCommandHandler().addCommand(this, "dgroup", List.of("displaygroup", "dg"),
                new DisplayGroupCmd(), Plugin.NETWORK);

        Network.getCommandHandler().addCommand(this, "build", new MapBuildCmd(), Plugin.NETWORK);

        Network.getCommandHandler().addCommand(this, "network", new NetworkCmd(), Plugin.NETWORK);

        Network.getCommandHandler().addCommand(this, "logger", List.of("log"),
                new LoggerCmd(), Plugin.SYSTEM);


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
        ChannelProxy.stop();
        Database.getInstance().closeWithBackups();
    }
}
