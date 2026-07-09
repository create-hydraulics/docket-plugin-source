package plg.docket.docket.report;

public final class Finding {
    private final Tier tier;
    private final String primaryPlugin;
    private final String secondaryPlugin; // null when a finding involves only one plugin
    private final String message;

    public Finding(Tier tier, String primaryPlugin, String message) {
        this(tier, primaryPlugin, null, message);
    }

    public Finding(Tier tier, String primaryPlugin, String secondaryPlugin, String message) {
        this.tier = tier;
        this.primaryPlugin = primaryPlugin;
        this.secondaryPlugin = secondaryPlugin;
        this.message = message;
    }

    public Tier getTier() { return tier; }
    public String getPrimaryPlugin() { return primaryPlugin; }
    public String getSecondaryPlugin() { return secondaryPlugin; }
    public String getMessage() { return message; }

    public String formatOneLiner() {
        if (secondaryPlugin != null) {
            return "[" + tier + "] " + primaryPlugin + " <> " + secondaryPlugin + ": " + message;
        }
        return "[" + tier + "] " + primaryPlugin + ": " + message;
    }
}
