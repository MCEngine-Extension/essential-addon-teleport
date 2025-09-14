package io.github.mcengine.extension.addon.essential.fly;

import io.github.mcengine.api.core.MCEngineCoreApi;
import io.github.mcengine.api.core.extension.logger.MCEngineExtensionLogger;
import io.github.mcengine.api.essential.extension.addon.IMCEngineEssentialAddOn;

import io.github.mcengine.extension.addon.essential.fly.cache.TeleportCache;
import io.github.mcengine.extension.addon.essential.fly.command.TeleportCommand;
import io.github.mcengine.extension.addon.essential.fly.listener.TeleportListener;
import io.github.mcengine.extension.addon.essential.fly.tabcompleter.TeleportTabCompleter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

public class Teleport implements IMCEngineEssentialAddOn {

    private static final String PERM_TP  = "essential.teleport.tp";
    private static final String PERM_TPA = "essential.teleport.tpa";

    private MCEngineExtensionLogger logger;
    private TeleportCommand handler;
    private TeleportTabCompleter completer;
    private TeleportCache cache;

    private Permission permTp;
    private Permission permTpa;

    @Override
    public void onLoad(Plugin plugin) {
        this.logger = new MCEngineExtensionLogger(plugin, "AddOn", "EssentialTeleport");

        // Build cache with expiration callback that notifies players
        this.cache = new TeleportCache(plugin, (requesterId, targetId) -> {
            Player requester = Bukkit.getPlayer(requesterId);
            Player target    = Bukkit.getPlayer(targetId);
            if (requester != null && requester.isOnline()) {
                requester.sendMessage(col("&cYour teleport request to &e" + (target != null ? target.getName() : "target") + " &cexpired."));
            }
            if (target != null && target.isOnline()) {
                target.sendMessage(col("&7The teleport request from &e" + (requester != null ? requester.getName() : "player") + " &7has expired."));
            }
        });

        this.handler = new TeleportCommand(plugin, this.logger, this.cache);
        this.completer = new TeleportTabCompleter();

        try {
            // Register permissions dynamically
            registerPermissions();

            // Listener
            PluginManager pm = Bukkit.getPluginManager();
            pm.registerEvents(new TeleportListener(cache, this.logger), plugin);

            // CommandMap (reflect)
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            CommandMap commandMap = (CommandMap) f.get(Bukkit.getServer());

            // /tp
            Command tpCmd = new Command("tp") {
                @Override public boolean execute(CommandSender sender, String label, String[] args) {
                    return handler.onTp(sender, this, label, args);
                }
                @Override public List<String> tabComplete(CommandSender s, String a, String[] args) {
                    return completer.onTabComplete(s, this, a, args);
                }
            };
            tpCmd.setDescription("Teleport to a player instantly (OP).");
            tpCmd.setUsage("/tp <player>");

            // /tpa
            Command tpaCmd = new Command("tpa") {
                @Override public boolean execute(CommandSender sender, String label, String[] args) {
                    return handler.onTpa(sender, this, label, args);
                }
                @Override public List<String> tabComplete(CommandSender s, String a, String[] args) {
                    return completer.onTabComplete(s, this, a, args);
                }
            };
            tpaCmd.setDescription("Request to teleport to a player (needs acceptance).");
            tpaCmd.setUsage("/tpa <player>");

            // /tpaccept
            Command tpacceptCmd = new Command("tpaccept") {
                @Override public boolean execute(CommandSender sender, String label, String[] args) {
                    return handler.onTpAccept(sender, this, label, args);
                }
            };
            tpacceptCmd.setDescription("Accept a pending /tpa request.");
            tpacceptCmd.setUsage("/tpaccept");

            // Register commands
            String prefix = plugin.getName().toLowerCase();
            commandMap.register(prefix, tpCmd);
            commandMap.register(prefix, tpaCmd);
            commandMap.register(prefix, tpacceptCmd);

            this.logger.info("Teleport addon enabled successfully.");
        } catch (Exception e) {
            this.logger.warning("Failed to initialize Teleport addon: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisload(Plugin plugin) {
        if (this.cache != null) this.cache.clearAll();
        unregisterPermissions();
        if (this.logger != null) this.logger.info("Teleport addon disabled.");
    }

    @Override
    public void setId(String id) {
        MCEngineCoreApi.setId("mcengine-essential-addon-teleport");
    }

    // ---- Permission registration helpers ----
    private void registerPermissions() {
        PluginManager pm = Bukkit.getPluginManager();

        permTp  = new Permission(PERM_TP,  "Teleport instantly to a target player.", PermissionDefault.OP);
        permTpa = new Permission(PERM_TPA, "Request to teleport to a player.",        PermissionDefault.TRUE);

        if (pm.getPermission(PERM_TP) == null)  pm.addPermission(permTp);
        if (pm.getPermission(PERM_TPA) == null) pm.addPermission(permTpa);

        pm.recalculatePermissionDefaults(permTp);
        pm.recalculatePermissionDefaults(permTpa);
    }

    private void unregisterPermissions() {
        PluginManager pm = Bukkit.getPluginManager();
        if (permTp != null && pm.getPermission(PERM_TP) != null)  pm.removePermission(permTp);
        if (permTpa != null && pm.getPermission(PERM_TPA) != null) pm.removePermission(permTpa);
    }

    private String col(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
}
