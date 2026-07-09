package plg.docket.docket.scanner;

import plg.docket.docket.report.Finding;
import plg.docket.docket.report.Tier;

import java.util.*;

public final class CommandCollisionDetector {

    public List<Finding> detect(Collection<PluginData> plugins) {
        // Map: token (command name or alias, lowercased) -> first plugin that registered it
        Map<String, String> firstSeen = new LinkedHashMap<>();
        List<Finding> findings = new ArrayList<>();
        Set<String> reported = new HashSet<>();

        for (PluginData plugin : plugins) {
            for (Map.Entry<String, List<String>> cmdEntry : plugin.getCommands().entrySet()) {
                for (String token : cmdEntry.getValue()) {
                    String key = token.toLowerCase(Locale.ROOT);
                    String existing = firstSeen.get(key);
                    if (existing == null) {
                        firstSeen.put(key, plugin.getName());
                    } else if (!existing.equals(plugin.getName())) {
                        String collisionKey = key + ":" + existing + ":" + plugin.getName();
                        if (reported.add(collisionKey)) {
                            findings.add(new Finding(Tier.WARNING, existing, plugin.getName(),
                                    "Command collision on /" + key + " — Bukkit will resolve by load order, but this is almost certainly unintended"));
                        }
                    }
                }
            }
        }

        return findings;
    }
}
