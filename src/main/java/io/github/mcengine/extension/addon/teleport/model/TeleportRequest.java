package io.github.mcengine.extension.addon.essential.teleport.model;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Immutable(ish) model of a pending /tpa request.
 * One active request per target at a time.
 */
public class TeleportRequest {
    private final UUID requesterId;
    private final UUID targetId;
    private final long  createdAtMs;
    private final long  expiresAtMs;

    // Managed by cache (set/unset when scheduling/cancelling)
    private BukkitTask expiryTask;

    public TeleportRequest(UUID requesterId, UUID targetId, long createdAtMs, long expiresAtMs) {
        this.requesterId = requesterId;
        this.targetId = targetId;
        this.createdAtMs = createdAtMs;
        this.expiresAtMs = expiresAtMs;
    }

    public UUID getRequesterId() { return requesterId; }
    public UUID getTargetId() { return targetId; }
    public long getCreatedAtMs() { return createdAtMs; }
    public long getExpiresAtMs() { return expiresAtMs; }

    public BukkitTask getExpiryTask() { return expiryTask; }
    public void setExpiryTask(BukkitTask expiryTask) { this.expiryTask = expiryTask; }
}
