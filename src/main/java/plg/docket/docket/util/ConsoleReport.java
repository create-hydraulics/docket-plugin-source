package plg.docket.docket.util;

import plg.docket.docket.report.Finding;
import plg.docket.docket.report.HealthScore;
import plg.docket.docket.report.ScanReport;
import plg.docket.docket.report.Tier;
import plg.docket.docket.scanner.EventListenerEntry;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class ConsoleReport {

    private static final String TOP = "═".repeat(62);
    private static final String MID = "─".repeat(62);

    private ConsoleReport() {}

    private static String statusMessage(String status) {
        return switch (status) {
            case "Great" -> "GREAT  —  All plugins are fully compatible";
            case "Good"  -> "GOOD   —  Minor warnings found, no critical issues";
            case "Mild"  -> "MILD   —  Conflicts detected, attention recommended";
            default      -> "BAD    —  Critical compatibility issues — action required!";
        };
    }

    public static void print(Logger log, ScanReport report, boolean includeInfoListings) {
        String status = HealthScore.status(report);
        int errors    = report.countErrors();
        int warnings  = report.countWarnings();
        int notes     = report.countInfo();
        int plugins   = report.getPluginsScanned();
        long ms       = report.getScanDurationMs();

        log.info(TOP);
        log.info("  DOCKET  SCAN  REPORT");
        log.info(TOP);
        log.info("  Status    »  " + statusMessage(status));
        log.info("  Scanned   »  " + plugins + " plugin" + (plugins == 1 ? "" : "s") + " in " + ms + "ms");
        log.info("  Findings  »  " + errors   + " error"   + (errors   == 1 ? "" : "s")
                + "  ·  " + warnings + " warning" + (warnings == 1 ? "" : "s")
                + "  ·  " + notes    + " note"    + (notes    == 1 ? "" : "s"));
        log.info(MID);

        List<Finding> errorList = report.getFindings(Tier.ERROR);
        if (!errorList.isEmpty()) {
            log.severe("  ERRORS  (" + errors + ")");
            for (Finding f : errorList) {
                log.severe("    " + f.formatOneLiner());
            }
        } else {
            log.info("  ERRORS  (0)  —  None found");
        }

        List<Finding> warnList = report.getFindings(Tier.WARNING);
        if (!warnList.isEmpty()) {
            log.warning("  WARNINGS  (" + warnings + ")");
            for (Finding f : warnList) {
                log.warning("    " + f.formatOneLiner());
            }
        } else {
            log.info("  WARNINGS  (0)  —  None found");
        }

        List<Finding> infoList = report.getFindings(Tier.INFO);
        if (!infoList.isEmpty()) {
            log.info("  NOTES  (" + notes + ")");
            for (Finding f : infoList) {
                log.info("    " + f.formatOneLiner());
            }
        } else {
            log.info("  NOTES  (0)  —  None");
        }

        if (includeInfoListings && !report.getRawListeners().isEmpty()) {
            log.info(MID);
            log.info("  EVENT LISTENER MAP  (multi-plugin shared events only)");
            for (Map.Entry<String, List<EventListenerEntry>> entry : report.getRawListeners().entrySet()) {
                long distinct = entry.getValue().stream()
                        .map(EventListenerEntry::getPluginName).distinct().count();
                if (distinct < 2) continue;
                StringBuilder sb = new StringBuilder("    ").append(entry.getKey());
                int pad = Math.max(1, 38 - entry.getKey().length());
                sb.append(" ".repeat(pad));
                for (EventListenerEntry e : entry.getValue()) {
                    sb.append(e.getPluginName()).append("@").append(e.getPriority());
                    if (e.isIgnoreCancelled()) sb.append("(ic)");
                    sb.append("  ");
                }
                log.info(sb.toString().stripTrailing());
            }
        }

        log.info(MID);
        log.info("  Use /plugincheck in-game for the interactive report");
        log.info(TOP);
    }
}
