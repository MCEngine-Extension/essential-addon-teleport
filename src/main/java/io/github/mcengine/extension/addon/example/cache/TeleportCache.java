package io.github.mcengine.extension.addon.essential.fly.cache;

import io.github.mcengine.extension.addon.essential.fly.model.TeleportRequest;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Centralized cache for pending teleport requests with timed expiration.
 *
 * Thread-safety: uses ConcurrentHashMap and only schedules tasks on the main thread.
 */
public class TeleportCache {

    /**
     * Callback notified when a request expires: (requesterId, targetId)
     */
    private final BiConsumer<UUID, UUID> onExpire;
    private final Plugin plugin;

    // targetUUID -> request
    private final Map<UUID, TeleportRequest> byTarget = new ConcurrentHashMap<>();

    public TeleportCache(Plugin plugin, BiConsumer<UUID, UUID> onExpire) {
        this.plugin = plugin;
        this.onExpire = onExpire;
    }

    /**
     * Create/overwrite a pending request to target from requester.
     * Any prior request to the same target is cancelled and replaced.
     *
     * @param requesterId requester UUID
     * @param targetId    target UUID
     * @param expireTicks ticks until expiration (e.g., 600L for 30s)
     */
    public void put(UUID requesterId, UUID targetId, long expireTicks) {
        // Cancel previous for this target (if any)
        TeleportRequest old = byTarget.remove(targetId);
        cancelTask(old);

        long now = System.currentTimeMillis();
        long expMs = now + (expireTicks * 50L);
        TeleportRequest req = new TeleportRequest(requesterId, targetId, now, expMs);

        // Schedule expiry
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TeleportRequest current = byTarget.get(targetId);
            if (current != null && Objects.equals(current.getRequesterId(), requesterId)) {
                // still the same pending request → expire it
                byTarget.remove(targetId);
                if (onExpire != null) onExpire.accept(requesterId, targetId);
            }
        }, expireTicks);
        req.setExpiryTask(task);

        byTarget.put(targetId, req);
    }

    /**
     * Accept and remove pending request for target, returning requesterId (or null).
     */
    public UUID accept(UUID targetId) {
        TeleportRequest req = byTarget.remove(targetId);
        cancelTask(req);
        return req != null ? req.getRequesterId() : null;
    }

    /**
     * Remove all requests involving this player (as target or as requester).
     */
    public void clearFor(UUID playerId) {
        // Remove as target
        TeleportRequest tReq = byTarget.remove(playerId);
        cancelTask(tReq);

        // Remove as requester (scan)
        byTarget.entrySet().removeIf(e -> {
            TeleportRequest r = e.getValue();
            if (r != null && Objects.equals(r.getRequesterId(), playerId)) {
                cancelTask(r);
                return true;
            }
            return false;
        });
    }

    /**
     * Return the requester for a given target (or null).
     */
    public UUID getRequesterForTarget(UUID targetId) {
        TeleportRequest req = byTarget.get(targetId);
        return req != null ? req.getRequesterId() : null;
    }

    /**
     * Clear all state (e.g., on addon unload).
     */
    public void clearAll() {
        byTarget.values().forEach(this::cancelTask);
        byTarget.clear();
    }

    private void cancelTask(TeleportRequest req) {
        if (req == null) return;
        BukkitTask t = req.getExpiryTask();
        if (t != null) {
            try { t.cancel(); } catch (Throwable ignored) {}
            req.setExpiryTask(null);
        }
    }
}
