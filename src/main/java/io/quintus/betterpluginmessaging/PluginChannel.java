package io.quintus.betterpluginmessaging;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class PluginChannel {

    private Plugin plugin;
    private PluginChannelManager manager;
    private Class messageClass;
    private String name;

    public Plugin getPlugin() {
        return plugin;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public PluginChannelManager getManager() {
        return manager;
    }

    public void setManager(PluginChannelManager manager) {
        this.manager = manager;
    }

    public Class getMessageClass() {
        return messageClass;
    }

    public void setMessageClass(Class messageClass) {
        this.messageClass = messageClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void onReceiveIP(String ip, short port) {}
    public void onReceivePlayerCount(String server, int count) {}
    public void onReceivePlayerList(String server, String[] playerNames) {}
    public void onReceiveGetServers(String[] servers) {}
    public void onReceiveGetServer(String server) {}
    public void onReceiveForward(Player player, Object message) {}
    public void onReceiveUUID(UUID uuid) {}
    public void onReceiveUUIDOther(String playerName, UUID uuid) {}
    public void onReceiveServerIP(String server, String ip, short port) {}

    public void requestConnect(Player player, String server) { manager.requestConnect(this, player, server); }
    public void requestConnectOther(String playerName, String server) { manager.requestConnectOther(this, playerName, server); }
    public void requestIP(String server) { manager.requestIP(this, server); }
    public void requestPlayerCount(String server) { manager.requestPlayerCount(this, server); }
    public void requestPlayerList(String server) { manager.requestPlayerList(this, server); }
    public void requestGetServers() { manager.requestGetServers(this); }
    public void requestMessage(String playerName, String message) { manager.requestMessage(this, playerName, message); }
    public void requestGetServer() { manager.requestGetServer(this); }
    public void requestForward(String server, Object message) { manager.requestForward(this, server, message); }
    public void requestForwardToPlayer(String playerName, Object message) { manager.requestForwardToPlayer(this, playerName, message); }
    public void requestUUID() { manager.requestUUID(this); }
    public void requestUUIDOther(String playerName) { manager.requestUUIDOther(this, playerName); }
    public void requestServerIP(String server) { manager.requestServerIP(this, server); }
    public void requestKickPlayer(String playerName, String reason) { manager.requestKickPlayer(this, playerName, reason); }
}