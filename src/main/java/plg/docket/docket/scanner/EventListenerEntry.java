package plg.docket.docket.scanner;

public final class EventListenerEntry {
    private final String pluginName;
    private final String eventType;
    private final String priority;
    private final boolean ignoreCancelled;
    private final boolean callsSetCancelled;

    public EventListenerEntry(String pluginName, String eventType, String priority,
                               boolean ignoreCancelled, boolean callsSetCancelled) {
        this.pluginName = pluginName;
        this.eventType = eventType;
        this.priority = priority;
        this.ignoreCancelled = ignoreCancelled;
        this.callsSetCancelled = callsSetCancelled;
    }

    public String getPluginName() { return pluginName; }
    public String getEventType() { return eventType; }
    public String getPriority() { return priority; }
    public boolean isIgnoreCancelled() { return ignoreCancelled; }
    public boolean callsSetCancelled() { return callsSetCancelled; }
}
