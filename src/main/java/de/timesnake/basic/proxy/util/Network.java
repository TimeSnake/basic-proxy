package de.timesnake.basic.proxy.util;

import de.timesnake.basic.proxy.core.file.ServerConfig;
import de.timesnake.basic.proxy.core.group.Group;
import de.timesnake.basic.proxy.core.permission.PermissionManager;
import de.timesnake.basic.proxy.core.rule.RuleManager;
import de.timesnake.basic.proxy.core.server.BukkitCmdHandler;
import de.timesnake.basic.proxy.util.chat.Chat;
import de.timesnake.basic.proxy.util.chat.CommandHandler;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.basic.proxy.util.user.PreUser;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.proxy.channel.Channel;
import de.timesnake.database.util.server.DbServer;
import de.timesnake.library.basic.util.chat.Plugin;
import de.timesnake.library.basic.util.server.Task;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public interface Network {

    static void broadcastMessage(String msg) {
        NetworkManager.getInstance().broadcastMessage(msg);
    }

    static void broadcastMessage(Plugin plugin, String msg) {
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

    static User getUser(ProxiedPlayer p) {
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

    static void addLobby(int port, String name, String folderPath) {
        NetworkManager.getInstance().addLobby(port, name, folderPath);
    }

    static void addGame(int port, String name, String task, String folderPath) {
        NetworkManager.getInstance().addGame(port, name, task, folderPath);
    }

    static void addLounge(int port, String name, String folderPath) {
        NetworkManager.getInstance().addLounge(port, name, folderPath);
    }

    static void addTempGame(int port, String name, String task, String folderPath) {
        NetworkManager.getInstance().addTempGame(port, name, task, folderPath);
    }

    static void addBuild(int port, String name, String folderPath) {
        NetworkManager.getInstance().addBuild(port, name, folderPath);
    }

    static void sendUserToServer(User user, String server, String message) {
        NetworkManager.getInstance().sendUserToServer(user, server, message);
    }

    static void sendUserToServer(User user, String server) {
        NetworkManager.getInstance().sendUserToServer(user, server);
    }

    static void sendUserToServer(User user, Integer port) {
        NetworkManager.getInstance().sendUserToServer(user, port);
    }

    static void removeUser(ProxiedPlayer p) {
        User user = Network.getUser(p);
        user.getGroup().removeUser(user);
        NetworkManager.getInstance().removeUser(p);
    }

    static User addUser(ProxiedPlayer p, PreUser user) {
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

    static void registerListener(Listener listener) {
        NetworkManager.getInstance().registerListener(listener);
    }

    static Chat getChat() {
        return NetworkManager.getInstance().getChat();
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

    static void printWarning(Plugin plugin, String warning, String... subPlugins) {
        NetworkManager.getInstance().printWarning(plugin, warning, subPlugins);
    }

    static void printError(Plugin plugin, String error, String... subPlugins) {
        NetworkManager.getInstance().printError(plugin, error, subPlugins);
    }

    static BukkitCmdHandler getBukkitCmdHandler() {
        return NetworkManager.getInstance().getBukkitCmdHandler();
    }

    static RuleManager getRuleManager() {
        return NetworkManager.getInstance().getRuleManager();
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
}
