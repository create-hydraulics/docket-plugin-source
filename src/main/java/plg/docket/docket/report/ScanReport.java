package plg.docket.docket.report;

import plg.docket.docket.scanner.EventListenerEntry;

import java.util.List;
import java.util.Map;

public final class ScanReport {
    private final List<Finding> findings;
    private final Map<String, List<EventListenerEntry>> rawListeners;
    private final int pluginsScanned;
    private final long scanDurationMs;

    public ScanReport(List<Finding> findings,
                      Map<String, List<EventListenerEntry>> rawListeners,
                      int pluginsScanned,
                      long scanDurationMs) {
        this.findings = List.copyOf(findings);
        this.rawListeners = Map.copyOf(rawListeners);
        this.pluginsScanned = pluginsScanned;
        this.scanDurationMs = scanDurationMs;
    }

    public List<Finding> getFindings() { return findings; }

    public List<Finding> getFindings(Tier tier) {
        return findings.stream().filter(f -> f.getTier() == tier).toList();
    }

    public Map<String, List<EventListenerEntry>> getRawListeners() { return rawListeners; }

    public int getPluginsScanned() { return pluginsScanned; }
    public long getScanDurationMs() { return scanDurationMs; }

    public int countErrors() {
        return (int) findings.stream().filter(f -> f.getTier() == Tier.ERROR).count();
    }

    public int countWarnings() {
        return (int) findings.stream().filter(f -> f.getTier() == Tier.WARNING).count();
    }

    public int countInfo() {
        return (int) findings.stream().filter(f -> f.getTier() == Tier.INFO).count();
    }
}
