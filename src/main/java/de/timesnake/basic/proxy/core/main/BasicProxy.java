/*
 * Copyright (C) 2023 timesnake
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
import de.timesnake.library.basic.util.logger.LogConfig;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

@com.velocitypowered.api.plugin.Plugin(
    id = "basic-proxy",
    name = "BasicProxy",
    version = "1.3",
    url = "https://git.timesnake.de/timesnake/minecraft/basic-proxy",
    authors = {"timesnake"},
    dependencies = {
        @Dependency(id = "database-proxy"),
        @Dependency(id = "channel-proxy")
    })
public class BasicProxy {

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

    new LogConfig(new File("log.cfg")).apply();
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    NetworkManager.getInstance().onEnable();

    Network.getCommandManager().addCommand(this, "permission", List.of("perm", "perms"),
        new PermissionCmd(), Plugin.PERMISSION);

    Network.getCommandManager().addCommand(this, "alias", new AliasCmd(), Plugin.ALIAS);

    Network.getCommandManager().addCommand(this, "work", new ServiceWorkCmd(), Plugin.NETWORK);
    Network.getCommandManager().addCommand(this, "ban", List.of("netban"),
        new PunishCmd(), Plugin.PUNISH);
    Network.getCommandManager().addCommand(this, "tempban",
        List.of("nettempban", "tmpban", "nettmpmban"), new PunishCmd(), Plugin.PUNISH);
    Network.getCommandManager().addCommand(this, "unban", List.of("netunban", "pardon"),
        new PunishCmd(), Plugin.PUNISH);
    Network.getCommandManager().addCommand(this, "mute", List.of("netmute"),
        new PunishCmd(), Plugin.PUNISH);
    Network.getCommandManager().addCommand(this, "unmute", List.of("netunmute"),
        new PunishCmd(), Plugin.PUNISH);
    Network.getCommandManager().addCommand(this, "kick", List.of("netkick"),
        new PunishCmd(), Plugin.PUNISH);

    Network.getCommandManager().addCommand(this, "jail", List.of("netjail"),
        new PunishCmd(), Plugin.PUNISH);

    Network.getCommandManager().addCommand(this, "start", new StartCmd(), Plugin.NETWORK);
    Network.getCommandManager().addCommand(this, "cmd", new ServerCmd(), Plugin.NETWORK);

    Network.getCommandManager().addCommand(this, "netmsg",
        List.of("netmessages", "networkmsg", "networkmessages", "networkmessage", "netmsgs"),
        new NetworkMsgCmd(), Plugin.SUPPORT);

    Network.getCommandManager().addCommand(this, "timecoins", new CoinsCmd(), Plugin.TIME_COINS);

    Network.getCommandManager().addCommand(this, "permcheck", new PermissionTestCmd(), Plugin.NETWORK);

    Network.getCommandManager().addCommand(this, "air", List.of("airmode", "am"),
        new AirModeCmd(), Plugin.NETWORK);

    Network.getCommandManager().addCommand(this, "rule", List.of("rules", "regeln", "regel"),
        new RuleCmd(), Plugin.NETWORK);

    Network.getCommandManager().addCommand(this, "pid", new PidCmd(), Plugin.SYSTEM);

    Network.getCommandManager().addCommand(this, "kickall", new KickAllCmd(), Plugin.NETWORK);

    Network.getCommandManager().addCommand(this, "dtmp", List.of("delete_tmp"),
        new DeleteTmpServerCmd(), Plugin.NETWORK);
    Network.getCommandManager().addCommand(this, "cleanup_servers", new CleanupServersCmd(), Plugin.SYSTEM);

    Network.getCommandManager().addCommand(this, "dg", List.of("displaygroup", "dgroup"),
        new DisplayGroupCmd(), Plugin.NETWORK);

    Network.getCommandManager().addCommand(this, "build", new MapBuildCmd(), Plugin.NETWORK);

    Network.getCommandManager().addCommand(this, "network", new NetworkCmd(), Plugin.NETWORK);

    Network.getCommandManager().addCommand(this, "logger", List.of("log"),
        new LoggerCmd(), Plugin.SYSTEM);

    Network.getCommandManager().addCommand(this, "uuid", new UuidCmd(), Plugin.NETWORK);

    Network.getCommandManager().addCommand(this, "code", new CodeCmd(), Plugin.SYSTEM);

    Network.getCommandManager().addCommand(this, "game", new GameCmd(), Plugin.NETWORK);

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
