package io.github.mcengine.extension.addon.teleport.command;

import io.github.mcengine.api.core.extension.logger.MCEngineExtensionLogger;
import io.github.mcengine.extension.addon.teleport.cache.TeleportCache;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class TeleportCommand {

    private static final String PERM_TP  = "essential.teleport.tp";   // default: OP
    private static final String PERM_TPA = "essential.teleport.tpa";  // default: everyone
    private static final long   EXPIRE_TICKS = 600L; // 30 seconds
    private static final long   ACCEPT_DELAY_TICKS = 80L; // 4s

    private final Plugin plugin;
    private final MCEngineExtensionLogger logger;
    private final TeleportCache cache;

    public TeleportCommand(Plugin plugin, MCEngineExtensionLogger logger, TeleportCache cache) {
        this.plugin = plugin;
        this.logger = logger;
        this.cache = cache;
    }

    // ---------- /tp <player> ----------
    public boolean onTp(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(col("&cOnly players can use this command."));
            return true;
        }
        Player p = (Player) sender;

        if (!p.hasPermission(PERM_TP) && !p.isOp()) {
            p.sendMessage(col("&cYou lack permission: &7" + PERM_TP));
            return true;
        }

        if (args.length < 1) {
            p.sendMessage(col("&eUsage: &7/tp <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            p.sendMessage(col("&cPlayer not found: &7" + args[0]));
            return true;
        }
        if (target.equals(p)) {
            p.sendMessage(col("&cYou are already here. That’s very efficient, but also very unnecessary."));
            return true;
        }

        Location to = target.getLocation();
        boolean ok = p.teleport(to);
        if (ok) {
            p.sendMessage(col("&aTeleported to &e" + target.getName() + "&a."));
        } else {
            p.sendMessage(col("&cTeleport failed."));
        }
        return true;
    }

    // ---------- /tpa <player> ----------
    public boolean onTpa(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(col("&cOnly players can use this command."));
            return true;
        }
        Player requester = (Player) sender;

        if (!(requester.hasPermission(PERM_TPA) || requester.isOp())) {
            requester.sendMessage(col("&cYou lack permission: &7" + PERM_TPA));
            return true;
        }

        if (args.length < 1) {
            requester.sendMessage(col("&eUsage: &7/tpa <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(col("&cPlayer not found: &7" + args[0]));
            return true;
        }
        if (target.equals(requester)) {
            requester.sendMessage(col("&cRequesting to teleport to yourself collapses space, time, and common sense."));
            return true;
        }

        // Put the pending request (overwrites previous for that target) with 30s expiry
        cache.put(requester.getUniqueId(), target.getUniqueId(), EXPIRE_TICKS);

        requester.sendMessage(col("&aRequest sent to &e" + target.getName() + "&a. Ask them to type &e/tpaccept&a."));
        target.sendMessage(col("&e" + requester.getName() + " &7wants to teleport to you. Type &a/tpaccept &7to allow. &8(30s)"));

        return true;
    }

    // ---------- /tpaccept ----------
    public boolean onTpAccept(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(col("&cOnly players can use this command."));
            return true;
        }
        Player target = (Player) sender;

        UUID requesterId = cache.accept(target.getUniqueId());
        if (requesterId == null) {
            target.sendMessage(col("&cYou have no pending teleport requests."));
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(col("&cThe requester is no longer online."));
            return true;
        }

        target.sendMessage(col("&aAccepted. Teleporting &e" + requester.getName() + " &ain &e4s&a..."));
        requester.sendMessage(col("&a" + target.getName() + " &eaccepted& a your request. Teleporting in &e4s&a..."));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!requester.isOnline() || !target.isOnline()) return;
            boolean ok = requester.teleport(target.getLocation());
            if (ok) {
                requester.sendMessage(col("&aTeleported to &e" + target.getName() + "&a."));
                target.sendMessage(col("&a" + requester.getName() + " teleported to you."));
            } else {
                requester.sendMessage(col("&cTeleport failed."));
            }
        }, ACCEPT_DELAY_TICKS);

        return true;
    }

    private String col(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
}
