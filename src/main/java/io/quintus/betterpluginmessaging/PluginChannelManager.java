package io.quintus.betterpluginmessaging;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public class PluginChannelManager implements PluginMessageListener {

    private BukkitBetterPluginMessaging plugin;
    private HashMap<String, HashMap<String, PluginChannel>> registeredPlugins;
    private HashMap<String, PluginChannel> requestTracker = new HashMap<>();
    private Gson gson;

    public PluginChannelManager(BukkitBetterPluginMessaging plugin) {
        this.plugin = plugin;
        this.gson = new Gson();

        this.registeredPlugins = new HashMap<>();
        this.requestTracker = new HashMap<>();
    }

    public void registerChannel(Plugin plugin, PluginChannel channel) {
        channel.setManager(this);
        HashMap<String, PluginChannel> channelMap;
        if (registeredPlugins.containsKey(plugin.getName())) {
            channelMap = registeredPlugins.get(plugin.getName());
        } else {
            channelMap = new HashMap<>();
            registeredPlugins.put(plugin.getName(), channelMap);
        }

        if (channelMap.containsKey(channel.getName())) {
            return;
        }

        channelMap.put(channel.getName(), channel);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) { return; }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        plugin.getLogger().log(Level.INFO, subchannel);
        String[] components = subchannel.split("::", 2);
        if (components.length == 2) {
            onBungeeMessage(components[0], components[1], in);
        } else {
            onForwardMessage(subchannel, player, in);
        }
    }

    public void onBungeeMessage(String command, String requestId, ByteArrayDataInput in) {
        if (!requestTracker.containsKey(requestId)) { return; }
        PluginChannel listener = requestTracker.get(requestId);
        requestTracker.remove(requestId);

        String serverName;
        UUID uuid;
        String ip;
        short port;

        switch (command) {
            case "IP":
                ip = in.readUTF();
                port = in.readShort();
                listener.onReceiveIP(ip, port);
                break;
            case "PlayerCount":
                serverName = in.readUTF();
                int count = in.readInt();
                listener.onReceivePlayerCount(serverName, count);
                break;
            case "PlayerList":
                serverName = in.readUTF();
                String[] players = in.readUTF().split(", ");
                listener.onReceivePlayerList(serverName, players);
                break;
            case "GetServers":
                String[] serverNames = in.readUTF().split(", ");
                listener.onReceiveGetServers(serverNames);
                break;
            case "GetServer":
                serverName = in.readUTF();
                listener.onReceiveGetServer(serverName);
                break;
            case "UUID":
                uuid = UUID.fromString(in.readUTF());
                listener.onReceiveUUID(uuid);
                break;
            case "UUIDOther":
                String playerName = in.readUTF();
                uuid = UUID.fromString(in.readUTF());
                listener.onReceiveUUIDOther(playerName, uuid);
                break;
            case "ServerIP":
                serverName = in.readUTF();
                ip = in.readUTF();
                port = in.readShort();
                listener.onReceiveServerIP(serverName, ip, port);
                break;
        }
    }

    public void onForwardMessage(String subchannel, Player player, ByteArrayDataInput in) {
        if (!registeredPlugins.containsKey(subchannel)) { return; }
        HashMap<String, PluginChannel> pluginChannels = registeredPlugins.get(subchannel);

        Short len = in.readShort();
        byte[] msgbytes = new byte[len];
        in.readFully(msgbytes);
        ByteArrayDataInput msgin = ByteStreams.newDataInput(msgbytes);

        String messageClass = msgin.readUTF();
        if (!pluginChannels.containsKey(messageClass)) {
            return;
        }

        PluginChannel listener = pluginChannels.get(messageClass);
        Object msg = gson.fromJson(msgin.readUTF(), listener.getMessageClass());
        listener.onReceiveForward(player, msg);
    }

    public void requestConnect(PluginChannel channel, Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void requestConnectOther(PluginChannel channel, String playerName, String server) {
        Player player = Iterables.getFirst(plugin.getServer().getOnlinePlayers(), null);
        if (player == null) { return; }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ConnectOther");
        out.writeUTF(playerName);
        out.writeUTF(server);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void requestIP(PluginChannel channel, Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        String requestId = UUID.randomUUID().toString();
        requestTracker.put(requestId, channel);
        out.writeUTF("IP::" + requestId);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void requestPlayerCount(PluginChannel channel, String server) {
        Player player = Iterables.getFirst(plugin.getServer().getOnlinePlayers(), null);
        if (player == null) { return; }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        String requestId = UUID.randomUUID().toString();
        requestTracker.put(requestId, channel);
        out.writeUTF("PlayerCount::" + requestId);
        out.writeUTF(server);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void requestPlayerList(PluginChannel channel, String server) {
        Player player = Iterables.getFirst(plugin.getServer().getOnlinePlayers(), null);
        if (player == null) { return; }
        String requestId = UUID.randomUUID().toString();
        requestTracker.put(requestId, channel);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerList::" + requestId);
        out.writeUTF(server);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void requestGetServers(PluginChannel channel) {
        Player player = Iterables.getFirst(plugin.getServer().getOnlinePlayers(), null);
        if (player == null) { return; }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        String requestId = UUID.randomUUID().toString();
        requestTracker.put(requestId, channel);
        out.writeUTF("GetServers::" + requestId);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void requestMessage(PluginChannel channel, String playerName, String message) {
        Player player = Iterables.getFirst(plugin.getServer().getOnlinePlayers(), null);
        if (player == null) { return; }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Message");
        out.writeUTF(playerName);
        out.writeUTF(message);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void requestGetServer(PluginChannel channel) {
        Player player = Iterables.getFirst(plugin.getServer().getOnlinePlayers(), null);
        if (player == null) { return; }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        String requestId = UUID.randomUUID().toString();
        requestTracker.put(requestId, channel);
        out.writeUTF("GetServer::" + requestId);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void requestForward(PluginChannel channel, String server, Object message) {
        Player player = Iterables.getFirst(plugin.getServer().getOnlinePlayers(), null);
        if (player == null) { return; }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF(server);
        out.writeUTF(channel.getName());
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void requestForwardToPlayer(PluginChannel channel, String playerName, Object message) {
        Player player = Iterables.getFirst(plugin.getServer().getOnlinePlayers(), null);
        if (player == null) { return; }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ForwardToPlayer");
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void requestUUID(PluginChannel channel) {
        Player player = Iterables.getFirst(plugin.getServer().getOnlinePlayers(), null);
        if (player == null) { return; }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        String requestId = UUID.randomUUID().toString();
        requestTracker.put(requestId, channel);
        out.writeUTF("UUID::" + requestId);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void requestUUIDOther(PluginChannel channel, String playerName) {
        Player player = Iterables.getFirst(plugin.getServer().getOnlinePlayers(), null);
        if (player == null) { return; }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        String requestId = UUID.randomUUID().toString();
        requestTracker.put(requestId, channel);
        out.writeUTF("UUIDOther::" + requestId);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void requestServerIP(PluginChannel channel, String server) {
        Player player = Iterables.getFirst(plugin.getServer().getOnlinePlayers(), null);
        if (player == null) { return; }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        String requestId = UUID.randomUUID().toString();
        requestTracker.put(requestId, channel);
        out.writeUTF("ServerIP::" + requestId);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void requestKickPlayer(PluginChannel channel, String playerName, String reason) {
        Player player = Iterables.getFirst(plugin.getServer().getOnlinePlayers(), null);
        if (player == null) { return; }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("KickPlayer");
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

}
