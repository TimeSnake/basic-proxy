package de.timesnake.basic.proxy.util;

import com.velocitypowered.api.proxy.Player;
import de.timesnake.basic.proxy.core.file.ServerConfig;
import de.timesnake.basic.proxy.core.group.Group;
import de.timesnake.basic.proxy.core.permission.PermissionManager;
import de.timesnake.basic.proxy.core.server.BukkitCmdHandler;
import de.timesnake.basic.proxy.util.chat.CommandHandler;
import de.timesnake.basic.proxy.util.server.*;
import de.timesnake.basic.proxy.util.user.PreUser;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.proxy.channel.Channel;
import de.timesnake.database.util.server.DbServer;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.basic.util.chat.Plugin;
import de.timesnake.library.basic.util.server.Task;
import de.timesnake.library.network.NetworkServer;
import de.timesnake.library.network.ServerCreationResult;
import net.kyori.adventure.text.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public interface Network {

    int PORT_BASE = 25100;
    String TMP_SERVER_SUFFIX = "_tmp";
    String TMUX_SESSION_NAME = "mcts";

    static void broadcastMessage(String msg) {
        NetworkManager.getInstance().broadcastMessage(msg);
    }

    static void broadcastMessage(Plugin plugin, String msg) {
        NetworkManager.getInstance().broadcastMessage(plugin, msg);
    }

    static void broadcastMessage(Plugin plugin, Component msg) {
        NetworkManager.getInstance().broadcastMessage(plugin, msg);
    }

    static void sendConsoleMessage(String message) {
        NetworkManager.getInstance().sendConsoleMessage(message);
    }

    static Integer getPort() {
        return NetworkManager.getInstance().getPort();
    }

    static User getUser(UUID uuid) {
        return NetworkManager.getInstance().getUser(uuid);
    }

    static User getUser(Player p) {
        return NetworkManager.getInstance().getUser(p);
    }

    static boolean isUserOnline(UUID uuid) {
        return NetworkManager.getInstance().isUserOnline(uuid);
    }

    static Group getGroup(String group) {
        return NetworkManager.getInstance().getGroup(group);
    }

    static Collection<Group> getGroups() {
        return NetworkManager.getInstance().getGroups();
    }

    static Collection<User> getUsers() {
        return NetworkManager.getInstance().getUsers();
    }

    static Collection<Server> getServers() {
        return NetworkManager.getInstance().getServers();
    }

    static Collection<Integer> getNotOfflineServerPorts() {
        return NetworkManager.getInstance().getNotOfflineServerPorts();
    }

    static Server getServer(Integer port) {
        return NetworkManager.getInstance().getServer(port);
    }

    static Server getServer(String name) {
        return NetworkManager.getInstance().getServer(name);
    }

    static Server getServer(DbServer server) {
        return NetworkManager.getInstance().getServer(server);
    }

    static void updateServerTaskAll() {
        NetworkManager.getInstance().updateServerTaskAll();
    }

    static void updateServerTask(int port) {
        NetworkManager.getInstance().getServer(port);
    }

    static Tuple<ServerCreationResult, Optional<Server>> newServer(NetworkServer server, boolean copyWorlds) {
        return NetworkManager.getInstance().newServer(server, copyWorlds);
    }

    static LobbyServer addLobby(int port, String name, Path folderPath) {
        return NetworkManager.getInstance().addLobby(port, name, folderPath);
    }

    static GameServer addGame(int port, String name, String task, Path folderPath) {
        return NetworkManager.getInstance().addGame(port, name, task, folderPath);
    }

    static LoungeServer addLounge(int port, String name, Path folderPath) {
        return NetworkManager.getInstance().addLounge(port, name, folderPath);
    }

    static TempGameServer addTempGame(int port, String name, String task, Path folderPath) {
        return NetworkManager.getInstance().addTempGame(port, name, task, folderPath);
    }

    static BuildServer addBuild(int port, String name, String task, Path folderPath) {
        return NetworkManager.getInstance().addBuild(port, name, task, folderPath);
    }

    static void sendUserToServer(User user, String server) {
        NetworkManager.getInstance().sendUserToServer(user, server);
    }

    static void sendUserToServer(User user, Integer port) {
        NetworkManager.getInstance().sendUserToServer(user, port);
    }

    static void removeUser(Player p) {
        User user = Network.getUser(p);
        if (user != null) {
            user.getGroup().removeUser(user);
        }
        NetworkManager.getInstance().removeUser(p);
    }

    static User addUser(Player p, PreUser user) {
        return NetworkManager.getInstance().addUser(p, user);
    }

    static boolean isWork() {
        return NetworkManager.getInstance().isWork();
    }

    static void setWork(boolean isWork) {
        NetworkManager.getInstance().setWork(isWork);
    }

    static ArrayList<User> getNetworkMessageListeners() {
        return NetworkManager.getInstance().getNetworkMessageListeners();
    }

    static ArrayList<User> getPrivateMessageListeners() {
        return NetworkManager.getInstance().getPrivateMessageListeners();
    }

    static ArrayList<User> getSupportMessageListeners() {
        return NetworkManager.getInstance().getSupportMessageListeners();
    }

    static void addNetworkMessageListener(User user) {
        NetworkManager.getInstance().addNetworkMessageListener(user);
    }

    static void addPrivateMessageListener(User user) {
        NetworkManager.getInstance().addPrivateMessageListener(user);
    }

    static void addSupportMessageListener(User user) {
        NetworkManager.getInstance().addSupportMessageListener(user);
    }

    static void removeNetworkMessageListener(User user) {
        NetworkManager.getInstance().removeNetworkMessageListener(user);
    }

    static void removePrivateMessageListener(User user) {
        NetworkManager.getInstance().removePrivateMessageListener(user);
    }

    static void removeSupportMessageListener(User user) {
        NetworkManager.getInstance().removeSupportMessageListener(user);
    }

    static Group getGuestGroup() {
        return NetworkManager.getInstance().getGuestGroup();
    }

    static Group getMemberGroup() {
        return NetworkManager.getInstance().getMemberGroup();
    }

    static void runCommand(String command) {
        NetworkManager.getInstance().runCommand(command);
    }

    static void registerListener(Object listener) {
        NetworkManager.getInstance().registerListener(listener);
    }

    static Channel getChannel() {
        return NetworkManager.getInstance().getChannel();
    }

    static Integer getMaxPlayersLobby() {
        return NetworkManager.getInstance().getMaxPlayersLobby();
    }

    static Integer getMaxPlayersBuild() {
        return NetworkManager.getInstance().getMaxPlayersBuild();
    }

    static ServerConfig getServerConfig() {
        return NetworkManager.getInstance().getServerConfig();
    }

    static CommandHandler getCommandHandler() {
        return NetworkManager.getInstance().getCommandHandler();
    }

    static PermissionManager getPermissionHandler() {
        return NetworkManager.getInstance().getPermissionHandler();
    }

    static void printText(Plugin plugin, String text, String... subPlugins) {
        NetworkManager.getInstance().printText(plugin, text, subPlugins);
    }

    static void printText(Plugin plugin, Component text, String... subPlugins) {
        NetworkManager.getInstance().printText(plugin, text, subPlugins);
    }

    static void printWarning(Plugin plugin, String warning, String... subPlugins) {
        NetworkManager.getInstance().printWarning(plugin, warning, subPlugins);
    }

    static void printWarning(Plugin plugin, Component text, String... subPlugins) {
        NetworkManager.getInstance().printText(plugin, text, subPlugins);
    }

    static BukkitCmdHandler getBukkitCmdHandler() {
        return NetworkManager.getInstance().getBukkitCmdHandler();
    }

    static void runTaskLater(Task task, Duration delay) {
        NetworkManager.getInstance().runTaskLater(task, delay);
    }

    static void runTaskAsync(Task task) {
        NetworkManager.getInstance().runTaskAsync(task);
    }

    static int getOnlineLobbys() {
        return NetworkManager.getInstance().getOnlineLobbys();
    }

    static int nextEmptyPort() {
        return NetworkManager.getInstance().nextEmptyPort();
    }

    static String getVelocitySecret() {
        return NetworkManager.getInstance().getVelocitySecret();
    }

    static Map<String, Path> getTmpServerDirs() {
        return NetworkManager.getInstance().getTmpDirsByServerName();
    }

    static boolean deleteServer(String name) {
        return NetworkManager.getInstance().deleteServer(name);
    }

    static Path getNetworkPath() {
        return NetworkManager.getInstance().getNetworkPath();
    }

    static boolean isTmuxEnabled() {
        return NetworkManager.getInstance().isTmuxEnabled();
    }
}
