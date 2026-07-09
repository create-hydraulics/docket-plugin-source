package plg.docket.docket.report;

public final class HealthScore {
    private HealthScore() {}

    public static String status(ScanReport report) {
        int errors = report.countErrors();
        if (errors == 0 && report.countWarnings() == 0) return "Great";
        if (errors == 0) return "Good";
        if (errors <= 2) return "Mild";
        return "Bad";
    }
}
