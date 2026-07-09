package plg.docket.docket.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import plg.docket.docket.Docket;
import plg.docket.docket.report.Finding;
import plg.docket.docket.report.ScanReport;
import plg.docket.docket.scanner.EventListenerEntry;
import plg.docket.docket.util.ChatFormat;
import plg.docket.docket.util.SoundKit;

import java.util.List;
import java.util.Map;

public final class PluginCheckCommand implements CommandExecutor, TabCompleter {

    private static final int PAGE_SIZE = 8;

    private final Docket plugin;

    public PluginCheckCommand(Docket plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("plugincheck.use")) {
            if (sender instanceof Player p) SoundKit.denied(p);
            sender.sendMessage(ChatFormat.error("You don't have permission to use this command."));
            return true;
        }

        String sub = args.length > 0 ? args[0].toLowerCase() : "";

        if (sub.equals("help") || sub.equals("?")) {
            handleHelp(sender, label);
        } else if (sub.equals("scan") || sub.equals("rescan")) {
            handleScan(sender);
        } else if (sub.equals("reload")) {
            handleReload(sender);
        } else if (sub.equals("events")) {
            handleEvents(sender, parsePage(args, 1), label);
        } else {
            handleReport(sender, parsePage(args, 0), label);
        }
        return true;
    }

    private void handleHelp(CommandSender sender, String label) {
        if (sender instanceof Player p) SoundKit.pageTurn(p);
        sender.sendMessage(ChatFormat.divider());
        sender.sendMessage(ChatFormat.helpTitle(plugin.getDescription().getVersion()));
        sender.sendMessage(ChatFormat.divider());
        sender.sendMessage(ChatFormat.helpEntry(label,             "View the compatibility report"));
        sender.sendMessage(ChatFormat.helpEntry(label + " events", "Event listener map"));
        sender.sendMessage(ChatFormat.helpEntry(label + " scan",   "Re-run analysis without restart"));
        sender.sendMessage(ChatFormat.helpEntry(label + " reload", "Reload configuration"));
        sender.sendMessage(ChatFormat.helpEntry(label + " help",   "Show this menu"));
        sender.sendMessage(ChatFormat.divider());
    }

    private void handleScan(CommandSender sender) {
        if (sender instanceof Player p) SoundKit.scanStart(p);
        sender.sendMessage(ChatFormat.scanStart());
        plugin.runScan();
        ScanReport report = plugin.getLastReport();
        if (sender instanceof Player p) SoundKit.scanComplete(p, report);
        sender.sendMessage(ChatFormat.scanResult(report));
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadPlugin();
        if (sender instanceof Player p) SoundKit.reload(p);
        sender.sendMessage(ChatFormat.reloadDone());
    }

    private void handleEvents(CommandSender sender, int page, String label) {
        ScanReport report = plugin.getLastReport();
        if (report == null) {
            if (sender instanceof Player p) SoundKit.denied(p);
            sender.sendMessage(ChatFormat.notReady());
            return;
        }
        if (sender instanceof Player p) SoundKit.pageTurn(p);
        showEventListings(sender, report, page, label);
    }

    private void handleReport(CommandSender sender, int page, String label) {
        ScanReport report = plugin.getLastReport();
        if (report == null) {
            if (sender instanceof Player p) SoundKit.denied(p);
            sender.sendMessage(ChatFormat.notReady());
            return;
        }
        if (sender instanceof Player p) SoundKit.reportOpen(p, report);
        showFindingsPage(sender, report, page, label);
    }

    private void showFindingsPage(CommandSender sender, ScanReport report, int page, String label) {
        List<Finding> all = report.getFindings();

        sender.sendMessage(ChatFormat.divider());
        sender.sendMessage(ChatFormat.reportHeader(report));
        sender.sendMessage(ChatFormat.scanMeta(report));
        sender.sendMessage(ChatFormat.divider());

        if (all.isEmpty()) {
            sender.sendMessage(ChatFormat.noIssues());
        } else {
            int totalPages = (int) Math.ceil((double) all.size() / PAGE_SIZE);
            page = Math.max(1, Math.min(page, totalPages));
            int start = (page - 1) * PAGE_SIZE;
            int end   = Math.min(start + PAGE_SIZE, all.size());

            for (int i = start; i < end; i++) {
                sender.sendMessage(ChatFormat.finding(all.get(i)));
            }

            Component footer = ChatFormat.pageFooter(page, totalPages, label);
            if (!footer.equals(Component.empty())) {
                sender.sendMessage(ChatFormat.divider());
                sender.sendMessage(footer);
            }
        }

        sender.sendMessage(ChatFormat.divider());
        sender.sendMessage(ChatFormat.tips(label));
    }

    private void showEventListings(CommandSender sender, ScanReport report, int page, String label) {
        List<Map.Entry<String, List<EventListenerEntry>>> multiPlugin = report.getRawListeners()
                .entrySet().stream()
                .filter(e -> e.getValue().stream().map(EventListenerEntry::getPluginName).distinct().count() > 1)
                .toList();

        sender.sendMessage(ChatFormat.divider());
        sender.sendMessage(ChatFormat.info("Event Listener Map  (" + multiPlugin.size() + " shared events)"));
        sender.sendMessage(ChatFormat.divider());

        if (multiPlugin.isEmpty()) {
            sender.sendMessage(ChatFormat.info("No events are handled by more than one plugin."));
        } else {
            int totalPages = (int) Math.ceil((double) multiPlugin.size() / PAGE_SIZE);
            page = Math.max(1, Math.min(page, totalPages));
            int start = (page - 1) * PAGE_SIZE;
            int end   = Math.min(start + PAGE_SIZE, multiPlugin.size());

            for (int i = start; i < end; i++) {
                Map.Entry<String, List<EventListenerEntry>> entry = multiPlugin.get(i);
                StringBuilder sb = new StringBuilder(entry.getKey()).append(" : ");
                for (EventListenerEntry e : entry.getValue()) {
                    sb.append(e.getPluginName()).append("@").append(e.getPriority());
                    if (e.isIgnoreCancelled()) sb.append("(ic)");
                    sb.append("  ");
                }
                sender.sendMessage(ChatFormat.info(sb.toString().stripTrailing()));
            }

            Component footer = ChatFormat.pageFooter(page, totalPages, label + " events");
            if (!footer.equals(Component.empty())) {
                sender.sendMessage(ChatFormat.divider());
                sender.sendMessage(footer);
            }
        }

        sender.sendMessage(ChatFormat.divider());
    }

    private static int parsePage(String[] args, int argIndex) {
        if (args.length > argIndex) {
            try {
                return Integer.parseInt(args[argIndex]);
            } catch (NumberFormatException ignored) {}
        }
        return 1;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("plugincheck.use")) return List.of();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return List.of("help", "scan", "reload", "events")
                    .stream().filter(s -> s.startsWith(partial)).toList();
        }
        return List.of();
    }
}
