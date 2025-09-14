package io.github.mcengine.extension.addon.teleport.listener;

import io.github.mcengine.api.core.extension.logger.MCEngineExtensionLogger;
import io.github.mcengine.extension.addon.teleport.cache.TeleportCache;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class TeleportListener implements Listener {

    private final TeleportCache cache;
    private final MCEngineExtensionLogger logger;

    public TeleportListener(TeleportCache cache, MCEngineExtensionLogger logger) {
        this.cache = cache;
        this.logger = logger;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        cache.clearFor(id);
        logger.info("Cleared pending teleport requests for " + e.getPlayer().getName());
    }
}
