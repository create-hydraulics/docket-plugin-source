package plg.docket.docket.scanner;

import plg.docket.docket.report.Finding;
import plg.docket.docket.report.Tier;

import java.util.*;

public final class DependencyValidator {

    public List<Finding> validate(Collection<PluginData> plugins) {
        List<Finding> findings = new ArrayList<>();
        Set<String> knownNames = new HashSet<>();
        for (PluginData p : plugins) {
            knownNames.add(p.getName());
        }

        for (PluginData plugin : plugins) {
            for (String dep : plugin.getHardDepends()) {
                if (!knownNames.contains(dep)) {
                    findings.add(new Finding(Tier.ERROR, plugin.getName(),
                            "Missing hard dependency: " + dep + " is not installed"));
                }
            }
        }

        detectCycles(plugins, findings);
        return findings;
    }

    private void detectCycles(Collection<PluginData> plugins, List<Finding> findings) {
        // Build dependency graph: plugin -> set of plugins it requires (hard deps)
        Map<String, Set<String>> depGraph = new LinkedHashMap<>();
        // Build load-order graph: plugin -> set of plugins it must load before
        Map<String, Set<String>> loadBeforeGraph = new LinkedHashMap<>();

        Set<String> knownNames = new HashSet<>();
        for (PluginData p : plugins) {
            knownNames.add(p.getName());
        }

        for (PluginData p : plugins) {
            depGraph.put(p.getName(), new LinkedHashSet<>(p.getHardDepends()));
            // loadBefore[A] means A loads before B — so B depends on A loading first
            // For cycle detection: if A says loadBefore B AND B says loadBefore A, that's circular
            for (String lb : p.getLoadBefore()) {
                loadBeforeGraph.computeIfAbsent(p.getName(), k -> new LinkedHashSet<>()).add(lb);
            }
        }

        Set<String> reported = new HashSet<>();

        // Check hard dependency cycles
        for (String start : depGraph.keySet()) {
            List<String> cycle = findCycle(depGraph, start);
            if (cycle != null && cycle.size() > 1) {
                String key = String.join("->", cycle.stream().sorted().toList());
                if (reported.add(key)) {
                    String chain = String.join(" → ", cycle);
                    findings.add(new Finding(Tier.ERROR, cycle.get(0),
                            "Circular hard dependency: " + chain));
                }
            }
        }

        // Check loadbefore cycles
        for (String start : loadBeforeGraph.keySet()) {
            List<String> cycle = findCycle(loadBeforeGraph, start);
            if (cycle != null && cycle.size() > 1) {
                String key = "lb:" + String.join("->", cycle.stream().sorted().toList());
                if (reported.add(key)) {
                    String chain = String.join(" → ", cycle);
                    findings.add(new Finding(Tier.WARNING, cycle.get(0),
                            "Circular load-order request: " + chain + " (loadbefore loop)"));
                }
            }
        }
    }

    private List<String> findCycle(Map<String, Set<String>> graph, String start) {
        Set<String> visited = new HashSet<>();
        Set<String> inStack = new LinkedHashSet<>();
        return dfs(graph, start, visited, inStack);
    }

    private List<String> dfs(Map<String, Set<String>> graph, String node,
                              Set<String> visited, Set<String> inStack) {
        if (inStack.contains(node)) {
            // Build the cycle from where we re-entered
            List<String> cycle = new ArrayList<>(inStack);
            int idx = cycle.indexOf(node);
            cycle = cycle.subList(idx, cycle.size());
            cycle.add(node); // close the loop
            return cycle;
        }
        if (visited.contains(node)) return null;

        visited.add(node);
        inStack.add(node);

        for (String neighbor : graph.getOrDefault(node, Set.of())) {
            List<String> cycle = dfs(graph, neighbor, visited, inStack);
            if (cycle != null) return cycle;
        }

        inStack.remove(node);
        return null;
    }
}
