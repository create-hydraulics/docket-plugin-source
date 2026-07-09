package plg.docket.docket.webhook;

import plg.docket.docket.report.Finding;
import plg.docket.docket.report.HealthScore;
import plg.docket.docket.report.ScanReport;
import plg.docket.docket.report.Tier;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

public final class DiscordWebhook {

    private final String webhookUrl;
    private final Logger log;

    public DiscordWebhook(String webhookUrl, Logger log) {
        this.webhookUrl = webhookUrl;
        this.log = log;
    }

    public void sendReport(ScanReport report) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        try {
            String payload = buildPayload(report);
            HttpURLConnection conn = (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                log.warning("[Docket] Discord webhook returned HTTP " + code);
            }
        } catch (Exception e) {
            log.warning("[Docket] Discord webhook failed: " + e.getMessage());
        }
    }

    private String buildPayload(ScanReport report) {
        String status  = HealthScore.status(report);
        int colorInt = report.countErrors() > 0 ? 0xe8563f
                     : report.countWarnings() > 0 ? 0xf2c14e
                     : 0x4a90d9;

        String description = "**" + status + "** | "
                + report.countErrors() + " errors, "
                + report.countWarnings() + " warnings, "
                + report.countInfo() + " notes | "
                + report.getPluginsScanned() + " plugins scanned";

        StringBuilder fields = new StringBuilder();

        appendFindingFields(fields, report.getFindings(Tier.ERROR),   "Errors",   5);
        appendFindingFields(fields, report.getFindings(Tier.WARNING),  "Warnings", 5);

        if (fields.length() > 0) {
            fields.insert(0, ",\"fields\":[");
            fields.append("]");
        }

        return "{\"embeds\":[{\"title\":\"Docket Plugin Health Report\","
                + "\"color\":" + colorInt + ","
                + "\"description\":\"" + escapeJson(description) + "\""
                + fields
                + "}]}";
    }

    private static void appendFindingFields(StringBuilder sb, List<Finding> findings, String label, int max) {
        if (findings.isEmpty()) return;
        StringBuilder value = new StringBuilder();
        int shown = Math.min(findings.size(), max);
        for (int i = 0; i < shown; i++) {
            value.append("• ").append(findings.get(i).formatOneLiner()).append("\\n");
        }
        if (findings.size() > max) {
            value.append("…and ").append(findings.size() - max).append(" more");
        }
        if (sb.length() > 0) sb.append(",");
        sb.append("{\"name\":\"").append(label).append("\","
                + "\"value\":\"").append(escapeJson(value.toString())).append("\","
                + "\"inline\":false}");
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
