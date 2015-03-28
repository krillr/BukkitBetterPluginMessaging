package io.quintus.betterpluginmessaging;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.mcstats.Metrics;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BukkitBetterPluginMessaging extends JavaPlugin {

    private Server server;
    private Logger logger;
    private Messenger messenger;
    private PluginChannelManager channelManager;

    @Override
    public void onEnable() {
        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {}

        server = getServer();
        logger = server.getLogger();
        messenger = server.getMessenger();

        if (!isBungeeEnabled()) {
            logger.log(Level.WARNING, "Either not Spigot or not configured in BungeeCord mode. I'm useless.");
            server.getPluginManager().disablePlugin(this);
            return;
        }

        channelManager = new PluginChannelManager(this);

        messenger.registerOutgoingPluginChannel(this, "BungeeCord");
        messenger.registerIncomingPluginChannel(this, "BungeeCord", channelManager);
    }

    public boolean isSpigot() {
        try {
            server.getClass().getMethod("spigot");
            return true;
        } catch (NoSuchMethodException ex) {
            logger.log(Level.WARNING, "Not a Spigot server, QuintusCore has no power here.");
            return false;
        }
    }

    public boolean isBungeeEnabled() {
        return isSpigot() && server.spigot().getConfig().getBoolean("settings.bungeecord");
    }

    public PluginChannelManager getChannelManager() {
        return channelManager;
    }

    public void setChannelManager(PluginChannelManager channelManager) {
        this.channelManager = channelManager;
    }

}
