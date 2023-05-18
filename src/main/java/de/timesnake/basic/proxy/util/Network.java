/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.timesnake.basic.proxy.core.permission.PermissionManager;
import de.timesnake.basic.proxy.core.punishment.PunishmentManager;
import de.timesnake.basic.proxy.core.server.ServerCmd;
import de.timesnake.basic.proxy.util.chat.CommandManager;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.basic.proxy.util.user.PreUser;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.proxy.channel.ProxyChannel;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.server.DbServer;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.basic.util.server.Task;
import de.timesnake.library.chat.TimeDownParser;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.network.NetworkServer;
import de.timesnake.library.network.NetworkUtils;
import de.timesnake.library.network.ServerCreationResult;
import de.timesnake.library.network.ServerInitResult;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;

public class Network implements de.timesnake.library.basic.util.server.Server {

    public static final int PORT_BASE = 25100;
    public static final String TMP_SERVER_SUFFIX = "_tmp";
    public static final String TMUX_SESSION_NAME = "mcts";
    public static final String GUEST_PERM_GROUP_NAME = "guest";
    public static final int GUEST_PERM_GROUP_RANK = 20;
    public static final String GUEST_DISPLAY_GROUP_NAME = "guest";
    public static final int GUEST_DISPLAY_GROUP_RANK = 30;
    public static final String MEMBER_PERM_GROUP_NAME = "member";
    public static final int MEMBER_PERM_GROUP_RANK = 17;
    public static final String MEMBER_DISPLAY_GROUP_NAME = "member";
    public static final int MEMBER_DISPLAY_GROUP_RANK = 25;

    public static final int WORLDS_PER_BUILD_SERVER = 10;

    @Deprecated
    public static void broadcastMessage(Plugin plugin, String msg) {
        network.broadcastMessage(plugin, msg);
    }

    public static void broadcastTDMessage(Plugin plugin, String msg) {
        network.broadcastTDMessage(plugin, msg);
    }

    public static void broadcastMessage(Plugin plugin, Component msg) {
        network.broadcastMessage(plugin, msg);
    }

    public static void sendConsoleMessage(String message) {
        network.sendConsoleMessage(message);
    }

    public static Integer getPort() {
        return network.getPort();
    }

    public static String getName() {
        return network.getName();
    }

    public static User getUser(UUID uuid) {
        return network.getUser(uuid);
    }

    public static User getUser(Player p) {
        return network.getUser(p);
    }

    public static boolean isUserOnline(UUID uuid) {
        return network.isUserOnline(uuid);
    }

    public static Map<String, Path> getTmpDirsByServerName() {
        return network.getTmpDirsByServerName();
    }

    public static Collection<User> getUsers() {
        return network.getUsers();
    }

    public static Collection<Server> getServers() {
        return network.getServers();
    }

    @Deprecated
    public static Collection<Integer> getNotOfflineServerPorts() {
        return network.getNotOfflineServerPorts();
    }

    public static Collection<String> getNotOfflineServerNames() {
        return network.getNotOfflineServerNames();
    }

    public static Server getServer(Integer port) {
        return network.getServer(port);
    }

    public static Server getServer(String name) {
        return network.getServer(name);
    }

    public static Server getServer(DbServer server) {
        return network.getServer(server);
    }

    public static void updateServerTaskAll() {
        network.updateServerTaskAll();
    }

    public static void updateServerTask(int port) {
        network.getServer(port);
    }

    public static Tuple<ServerCreationResult, Optional<Server>> createTmpServer(
            NetworkServer server) {
        return network.createTmpServer(server);
    }

    public static Tuple<ServerCreationResult, Optional<Server>> createTmpServer(
            NetworkServer server,
            boolean registerServer) {
        return network.createTmpServer(server, registerServer);
    }

    public static void sendUserToServer(User user, String server) {
        network.sendUserToServer(user, server);
    }

    public static void sendUserToServer(User user, Integer port) {
        network.sendUserToServer(user, port);
    }

    public static void removeUser(Player p) {
        User user = Network.getUser(p);
        if (user != null) {
            user.getGroup().removeUser(user);
        }
        network.removeUser(p);
    }

    public static User addUser(Player p, PreUser user) {
        return network.addUser(p, user);
    }

    public static boolean isWork() {
        return network.isWork();
    }

    public static void setWork(boolean isWork) {
        network.setWork(isWork);
    }

    public static List<User> getNetworkMessageListeners() {
        return network.getNetworkMessageListeners();
    }

    public static List<User> getPrivateMessageListeners() {
        return network.getPrivateMessageListeners();
    }

    public static List<User> getSupportMessageListeners() {
        return network.getSupportMessageListeners();
    }

    public static void addNetworkMessageListener(User user) {
        network.addNetworkMessageListener(user);
    }

    public static void addPrivateMessageListener(User user) {
        network.addPrivateMessageListener(user);
    }

    public static void addSupportMessageListener(User user) {
        network.addSupportMessageListener(user);
    }

    public static void removeNetworkMessageListener(User user) {
        network.removeNetworkMessageListener(user);
    }

    public static void removePrivateMessageListener(User user) {
        network.removePrivateMessageListener(user);
    }

    public static void removeSupportMessageListener(User user) {
        network.removeSupportMessageListener(user);
    }

    public static GroupManager getGroupManager() {
        return network.getGroupManager();
    }

    public static void runCommand(String command) {
        network.runCommand(command);
    }

    public static void registerListener(Object listener) {
        network.registerListener(listener);
    }

    public static ProxyChannel getChannel() {
        return network.getChannel();
    }

    public static Integer getMaxPlayersLobby() {
        return network.getMaxPlayersLobby();
    }

    public static Integer getMaxPlayersBuild() {
        return network.getMaxPlayersBuild();
    }

    public static CommandManager getCommandManager() {
        return network.getCommandManager();
    }

    public static PermissionManager getPermissionHandler() {
        return network.getPermissionHandler();
    }

    @Deprecated
    public static void printText(Plugin plugin, String text, String... subPlugins) {
        network.printText(plugin, text, subPlugins);
    }

    @Deprecated
    public static void printText(Plugin plugin, Component text, String... subPlugins) {
        network.printText(plugin, text, subPlugins);
    }

    @Deprecated
    public static void printWarning(Plugin plugin, String warning, String... subPlugins) {
        network.printWarning(plugin, warning, subPlugins);
    }

    @Deprecated
    public static void printWarning(Plugin plugin, Component text, String... subPlugins) {
        network.printText(plugin, text, subPlugins);
    }

    public static ServerCmd getBukkitCmdHandler() {
        return network.getBukkitCmdHandler();
    }

    public static ScheduledTask runTaskLater(Task task, Duration delay) {
        return network.runTaskLater(task, delay);
    }

    public static ScheduledTask runTaskAsync(Task task) {
        return network.runTaskAsync(task);
    }

    public static int getOnlineLobbys() {
        return network.getOnlineLobbys();
    }

    public static int nextEmptyPort() {
        return network.nextEmptyPort();
    }

    public static String getVelocitySecret() {
        return network.getVelocitySecret();
    }

    public static Map<String, Path> getTmpServerDirs() {
        return network.getTmpDirsByServerName();
    }

    public static boolean deleteServer(String name, boolean force) {
        return network.deleteServer(name, force);
    }

    public static Path getNetworkPath() {
        return network.getNetworkPath();
    }

    public static boolean isTmuxEnabled() {
        return network.isTmuxEnabled();
    }

    public static NetworkUtils getNetworkUtils() {
        return network.getNetworkUtils();
    }

    public static ServerInitResult createPublicPlayerServer(Type.Server<?> type, String task,
            String name) {
        return network.createPublicPlayerServer(type, task, name);
    }

    public static ServerInitResult createPlayerServer(UUID uuid, Type.Server<?> type, String task,
            String name) {
        return network.createPlayerServer(uuid, type, task, name);
    }

    public static Tuple<ServerCreationResult, Optional<Server>> loadPlayerServer(UUID uuid,
            NetworkServer server) {
        return network.loadPlayerServer(uuid, server);
    }

    public static Tuple<ServerCreationResult, Optional<Server>> loadPublicPlayerServer(
            NetworkServer server) {
        return network.loadPublicPlayerServer(server);
    }

    public static CompletableFuture<Boolean> killAndDeleteServer(String serverName, Long pid) {
        return network.killAndDeleteServer(serverName, pid);
    }

    public static TimeDownParser getTimeDownParser() {
        return network.getTimeDownParser();
    }

    public static PunishmentManager getPunishmentManager() {
        return network.getPunishmentManager();
    }

    private static final NetworkManager network = NetworkManager.getInstance();
}
