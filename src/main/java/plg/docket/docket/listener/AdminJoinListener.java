package plg.docket.docket.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import plg.docket.docket.Docket;
import plg.docket.docket.report.ScanReport;
import plg.docket.docket.util.ChatFormat;
import plg.docket.docket.util.SoundKit;

public final class AdminJoinListener implements Listener {

    private final Docket plugin;

    public AdminJoinListener(Docket plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        if (!player.hasPermission("plugincheck.joinmessage")) return;

        ScanReport report = plugin.getLastReport();
        if (report == null) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            try {
                player.sendMessage(ChatFormat.joinSummary(report));
                SoundKit.joinAlert(player, report);
            } catch (Exception e) {
                plugin.getLogger().warning("[Docket] Failed to send join summary to " + player.getName() + ": " + e.getMessage());
            }
        }, 10L);
    }
}
