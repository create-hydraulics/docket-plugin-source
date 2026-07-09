package plg.docket.docket;

import org.bukkit.plugin.java.JavaPlugin;
import plg.docket.docket.command.PluginCheckCommand;
import plg.docket.docket.listener.AdminJoinListener;
import plg.docket.docket.report.ScanReport;
import plg.docket.docket.scanner.PluginScanner;
import plg.docket.docket.util.ConsoleReport;
import plg.docket.docket.webhook.DiscordWebhook;

import java.io.File;

public final class Docket extends JavaPlugin {

    private volatile ScanReport lastReport;
    private DiscordWebhook discordWebhook;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getConfig().getBoolean("discord.enabled", false)) {
            String url = getConfig().getString("discord.webhook-url", "");
            if (!url.isBlank()) {
                discordWebhook = new DiscordWebhook(url, getLogger());
            }
        }

        PluginCheckCommand cmd = new PluginCheckCommand(this);
        var pluginCheckCmd = getCommand("plugincheck");
        if (pluginCheckCmd != null) {
            pluginCheckCmd.setExecutor(cmd);
            pluginCheckCmd.setTabCompleter(cmd);
        }

        getServer().getPluginManager().registerEvents(new AdminJoinListener(this), this);

        printBanner();

        // Delay by 1 tick so all plugins finish their onEnable before we scan
        getServer().getScheduler().runTaskLater(this, this::runScan, 1L);
    }

    private void printBanner() {
        String v       = getDescription().getVersion();
        String discord = (discordWebhook != null) ? "Enabled" : "Disabled";
        String bar     = "════════════════════════════════════════════════════════";
        String thin    = "────────────────────────────────────────────────────────";
        getLogger().info(bar);
        getLogger().info("  DOCKET  v" + v + "  —  Plugin Compatibility Scanner");
        getLogger().info("  Monitors installed plugins for dependency gaps,");
        getLogger().info("  command collisions, and event-listener conflicts.");
        getLogger().info(thin);
        getLogger().info("  Authors  »  Docket Contributors  |  Platform: Paper 1.21+");
        getLogger().info("  Discord  »  " + discord + "  |  Scan: Queued for next tick");
        getLogger().info(bar);
    }

    public void runScan() {
        File pluginsDir = getDataFolder().getParentFile();
        boolean analyzeListeners = getConfig().getBoolean("scan.event-listener-analysis", true);
        boolean consoleInfoListings = getConfig().getBoolean("scan.console-info-listings", false);

        PluginScanner scanner = new PluginScanner(pluginsDir, getName(), analyzeListeners, getLogger());
        ScanReport report = scanner.scan();
        lastReport = report;

        ConsoleReport.print(getLogger(), report, consoleInfoListings);

        if (discordWebhook != null) {
            getServer().getScheduler().runTaskAsynchronously(this,
                    () -> discordWebhook.sendReport(report));
        }
    }

    public ScanReport getLastReport() {
        return lastReport;
    }

    public void reloadPlugin() {
        reloadConfig();
        boolean enabled = getConfig().getBoolean("discord.enabled", false);
        if (enabled) {
            String url = getConfig().getString("discord.webhook-url", "");
            discordWebhook = (!url.isBlank()) ? new DiscordWebhook(url, getLogger()) : null;
        } else {
            discordWebhook = null;
        }
    }

    @Override
    public void onDisable() {}
}
