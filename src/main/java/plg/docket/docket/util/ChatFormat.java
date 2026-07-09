package plg.docket.docket.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import plg.docket.docket.report.Finding;
import plg.docket.docket.report.HealthScore;
import plg.docket.docket.report.ScanReport;

public final class ChatFormat {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final String BLUE      = "<#4a90d9>";
    private static final String AMBER     = "<#f2c14e>";
    private static final String RED       = "<#e8563f>";
    private static final String GRAY      = "<gray>";
    private static final String DARK_GRAY = "<dark_gray>";
    private static final String WHITE     = "<white>";
    private static final String RESET     = "<reset>";
    private static final String SEP       = DARK_GRAY + " · ";

    private static final String PREFIX  = "<bold>" + BLUE + "[Docket]</bold>" + RESET + "  ";
    private static final String DIVIDER = BLUE + "<strikethrough>" + "  ".repeat(26) + "</strikethrough>";

    private ChatFormat() {}

    private static Component mm(String text) {
        return MM.deserialize(text);
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("<", "\\<").replace(">", "\\>");
    }

    private static String statusColor(String status) {
        return switch (status) {
            case "Mild" -> AMBER;
            case "Bad"  -> RED;
            default     -> BLUE;
        };
    }

    private static String statusIcon(String status) {
        return switch (status) {
            case "Great" -> "✔ ";
            case "Good"  -> "◐ ";
            case "Mild"  -> "⚠ ";
            default      -> "✖ ";
        };
    }

    public static Component divider() {
        return mm(DIVIDER);
    }

    public static Component info(String text) {
        return mm(PREFIX + GRAY + text);
    }

    public static Component success(String text) {
        return mm(PREFIX + BLUE + text);
    }

    public static Component error(String text) {
        return mm(PREFIX + RED + "✖  " + text);
    }

    public static Component finding(Finding f) {
        String icon = switch (f.getTier()) {
            case ERROR   -> RED   + "✖ ";
            case WARNING -> AMBER + "⚠ ";
            case INFO    -> BLUE  + "ℹ ";
        };
        String tier = switch (f.getTier()) {
            case ERROR   -> RED   + "<bold>ERROR</bold>" + RESET;
            case WARNING -> AMBER + "<bold>WARN </bold>" + RESET;
            case INFO    -> BLUE  + "<bold>INFO </bold>" + RESET;
        };
        String body = f.getSecondaryPlugin() != null
            ? WHITE + esc(f.getPrimaryPlugin()) + GRAY + " ↔ " + WHITE + esc(f.getSecondaryPlugin())
              + DARK_GRAY + "  →  " + GRAY + esc(f.getMessage())
            : WHITE + esc(f.getPrimaryPlugin())
              + DARK_GRAY + "  →  " + GRAY + esc(f.getMessage());
        return mm(PREFIX + icon + tier + "  " + body);
    }

    public static Component reportHeader(ScanReport report) {
        String status = HealthScore.status(report);
        String sc     = statusColor(status);
        String icon   = statusIcon(status);
        int e = report.countErrors(), w = report.countWarnings(), n = report.countInfo();
        return mm(PREFIX + sc + icon + "<bold>" + status + "</bold>" + RESET
                + DARK_GRAY + "  ·  "
                + RED   + e + " error"   + (e == 1 ? "" : "s")
                + DARK_GRAY + "  ·  "
                + AMBER + w + " warning" + (w == 1 ? "" : "s")
                + DARK_GRAY + "  ·  "
                + BLUE  + n + " note"    + (n == 1 ? "" : "s"));
    }

    public static Component scanMeta(ScanReport report) {
        int p = report.getPluginsScanned();
        return mm(PREFIX + DARK_GRAY + "Scanned " + WHITE + p + DARK_GRAY
                + " plugin" + (p == 1 ? "" : "s") + " in " + WHITE + report.getScanDurationMs() + "ms");
    }

    public static Component noIssues() {
        return mm(PREFIX + BLUE + "✔  No issues found — all plugins are compatible!");
    }

    public static Component scanStart() {
        return mm(PREFIX + GRAY + "Scanning plugins — this may take a moment...");
    }

    public static Component scanResult(ScanReport report) {
        String status = HealthScore.status(report);
        String sc     = statusColor(status);
        String icon   = statusIcon(status);
        int e = report.countErrors(), w = report.countWarnings();
        return mm(PREFIX + GRAY + "Scan complete  "
                + sc + icon + "<bold>" + status + "</bold>" + RESET
                + "  " + RED   + e + " error"   + (e == 1 ? "" : "s")
                + "  " + AMBER + w + " warning" + (w == 1 ? "" : "s"));
    }

    public static Component reloadDone() {
        return mm(PREFIX + BLUE + "✔  Configuration reloaded successfully.");
    }

    public static Component notReady() {
        return mm(PREFIX + GRAY + "Scan is still in progress — please wait a moment.");
    }

    public static Component joinSummary(ScanReport report) {
        String status  = HealthScore.status(report);
        String sc      = statusColor(status);
        String icon    = statusIcon(status);
        int p          = report.getPluginsScanned();
        int errors     = report.countErrors();
        int warnings   = report.countWarnings();

        String counts;
        if (errors == 0 && warnings == 0) {
            counts = BLUE + "no issues";
        } else if (errors == 0) {
            counts = AMBER + warnings + " warning" + (warnings == 1 ? "" : "s");
        } else {
            counts = RED + errors + " error" + (errors == 1 ? "" : "s")
                   + GRAY + " · " + AMBER + warnings + " warning" + (warnings == 1 ? "" : "s");
        }
        Component line1 = mm(PREFIX + sc + icon + "<bold>" + status + "</bold>" + RESET
                + SEP + DARK_GRAY + p + " plugin" + (p == 1 ? "" : "s") + " checked"
                + SEP + counts);

        String subtitle = switch (status) {
            case "Great" -> BLUE  + "✔  All plugins are compatible — server is healthy.";
            case "Good"  -> BLUE  + "◐  No critical issues, but there are warnings to review.";
            case "Mild"  -> AMBER + "⚠  Plugin conflicts detected — review the report.";
            default      -> RED   + "✖  Critical conflicts detected — action required!";
        };
        String viewBtn = "<click:run_command:'/plugincheck'><hover:show_text:'"
                + GRAY + "Open the full compatibility report'>"
                + BLUE + "[ View Report ]</hover></click>";
        Component line2 = mm(PREFIX + subtitle + "  " + viewBtn);

        return line1.append(Component.newline()).append(line2);
    }

    public static Component pageFooter(int page, int total, String cmd) {
        if (total <= 1) return Component.empty();
        String nav = DARK_GRAY + "Page " + WHITE + page + DARK_GRAY + " / " + WHITE + total;
        if (page > 1) {
            nav += "  " + BLUE + "<click:run_command:'/" + cmd + " " + (page - 1) + "'>[ ← Prev ]</click>";
        }
        if (page < total) {
            nav += "  " + BLUE + "<click:run_command:'/" + cmd + " " + (page + 1) + "'>[ Next → ]</click>";
        }
        return mm(PREFIX + nav);
    }

    public static Component tips(String label) {
        return mm(PREFIX + DARK_GRAY + "Tip: "
                + BLUE + "<click:run_command:'/" + label + " scan'>/" + label + " scan</click>"
                + DARK_GRAY + "  ·  "
                + BLUE + "<click:run_command:'/" + label + " events'>/" + label + " events</click>"
                + DARK_GRAY + "  ·  "
                + BLUE + "<click:run_command:'/" + label + " help'>/" + label + " help</click>");
    }

    public static Component helpTitle(String version) {
        return mm(PREFIX + WHITE + "<bold>Docket</bold>"
                + RESET + "  " + DARK_GRAY + "v" + version
                + "  " + GRAY + "Plugin Compatibility Scanner");
    }

    public static Component helpEntry(String cmd, String description) {
        return mm(PREFIX + "  " + BLUE + "<click:suggest_command:'/" + cmd + "'>▸ /" + cmd + "</click>"
                + DARK_GRAY + "  →  " + GRAY + description);
    }
}
