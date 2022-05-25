package de.timesnake.basic.proxy.core.main;

import de.timesnake.basic.proxy.core.channel.ChannelBroadcastCmd;
import de.timesnake.basic.proxy.core.channel.ChannelCmdHandler;
import de.timesnake.basic.proxy.core.coins.CoinsCmd;
import de.timesnake.basic.proxy.core.infomessage.NetworkMsgCmd;
import de.timesnake.basic.proxy.core.network.MotdManager;
import de.timesnake.basic.proxy.core.network.ServiceWorkCmd;
import de.timesnake.basic.proxy.core.permission.PermissionCheck;
import de.timesnake.basic.proxy.core.permission.PermissionCmd;
import de.timesnake.basic.proxy.core.permission.PermissionTest;
import de.timesnake.basic.proxy.core.punishment.PunishCmd;
import de.timesnake.basic.proxy.core.rule.RuleCmd;
import de.timesnake.basic.proxy.core.server.*;
import de.timesnake.basic.proxy.core.user.AirModeCmd;
import de.timesnake.basic.proxy.core.user.AliasCmd;
import de.timesnake.basic.proxy.core.user.ChatManager;
import de.timesnake.basic.proxy.util.NetworkManager;
import de.timesnake.channel.core.NetworkChannel;
import de.timesnake.database.util.Database;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

import java.util.List;

public class BasicProxy extends Plugin {


    public static final String DATA_PROTECTION_VERSION = "2.0";
    private static BasicProxy plugin;

    public static BasicProxy getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        BasicProxy.plugin = this;

        NetworkManager.getInstance().onEnable();


        PluginManager pm = ProxyServer.getInstance().getPluginManager();

        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "permission", List.of("perm", "perms"),
                new PermissionCmd(), de.timesnake.basic.proxy.util.chat.Plugin.PERMISSION);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "alias", new AliasCmd(),
                de.timesnake.basic.proxy.util.chat.Plugin.ALIAS);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "work", new ServiceWorkCmd(),
                de.timesnake.basic.proxy.util.chat.Plugin.NETWORK);
        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "ban", List.of("netban"),
                new PunishCmd(), de.timesnake.basic.proxy.util.chat.Plugin.PUNISH);
        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "tempban", List.of("nettempban",
                "tmpban", "nettmpmban"), new PunishCmd(), de.timesnake.basic.proxy.util.chat.Plugin.PUNISH);
        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "unban", List.of("netunban", "pardon"),
                new PunishCmd(), de.timesnake.basic.proxy.util.chat.Plugin.PUNISH);
        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "mute", List.of("netmute"),
                new PunishCmd(), de.timesnake.basic.proxy.util.chat.Plugin.PUNISH);
        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "unmute", List.of("netunmute"),
                new PunishCmd(), de.timesnake.basic.proxy.util.chat.Plugin.PUNISH);
        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "kick", List.of("netkick"),
                new PunishCmd(), de.timesnake.basic.proxy.util.chat.Plugin.PUNISH);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "start", new StartCmd(),
                de.timesnake.basic.proxy.util.chat.Plugin.NETWORK);
        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "cmd", new BukkitCmdHandler(),
                de.timesnake.basic.proxy.util.chat.Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "netmessages", List.of("netmsg",
                        "networkmsg", "networkmessages", "networkmessage", "netmsgs"), new NetworkMsgCmd(),
                de.timesnake.basic.proxy.util.chat.Plugin.SUPPORT);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "timecoins", new CoinsCmd(),
                de.timesnake.basic.proxy.util.chat.Plugin.TIME_COINS);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "channelmsg", List.of("channelmsgs",
                        "channelmessage", "channelmessages"), new ChannelBroadcastCmd(),
                de.timesnake.basic.proxy.util.chat.Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "permcheck", new PermissionTest(),
                de.timesnake.library.basic.util.chat.Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "air", List.of("airmode", "am"),
                new AirModeCmd(), de.timesnake.library.basic.util.chat.Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "rule", List.of("rules", "regeln",
                "regel"), new RuleCmd(), de.timesnake.library.basic.util.chat.Plugin.NETWORK);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "pid", new PidCmd(),
                de.timesnake.basic.proxy.util.chat.Plugin.SYSTEM);

        NetworkManager.getInstance().getCommandHandler().addCommand(this, pm, "kickall", new KickAllCmd(),
                de.timesnake.basic.proxy.util.chat.Plugin.NETWORK);

        pm.registerListener(this, new MotdManager());
        pm.registerListener(this, new PermissionCheck());
        pm.registerListener(this, new ChatManager());
        pm.registerListener(this, new FallbackListener());

        NetworkManager.getInstance().getChannel().addListener(new ChannelCmdHandler());
    }

    public void onDisable() {
        NetworkChannel.stop();
        Database.getInstance().closeWithBackups();
    }
}
