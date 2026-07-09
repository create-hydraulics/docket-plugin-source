package plg.docket.docket.scanner;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import plg.docket.docket.report.Finding;
import plg.docket.docket.report.ScanReport;
import plg.docket.docket.report.Tier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PluginScanner {

    private final File pluginsDir;
    private final String selfName;
    private final boolean analyzeListeners;
    private final Logger log;

    public PluginScanner(File pluginsDir, String selfName, boolean analyzeListeners, Logger log) {
        this.pluginsDir = pluginsDir;
        this.selfName = selfName;
        this.analyzeListeners = analyzeListeners;
        this.log = log;
    }

    public ScanReport scan() {
        long start = System.currentTimeMillis();
        List<Finding> findings = new ArrayList<>();

        List<PluginData> plugins = loadPluginData(findings);

        findings.addAll(new DependencyValidator().validate(plugins));
        findings.addAll(new CommandCollisionDetector().detect(plugins));

        Map<String, List<EventListenerEntry>> rawListeners = new LinkedHashMap<>();
        if (analyzeListeners) {
            EventListenerAnalyzer analyzer = new EventListenerAnalyzer(log);
            for (PluginData pd : plugins) {
                for (EventListenerEntry e : analyzer.analyzeJar(pd.getName(), pd.getJarFile())) {
                    rawListeners.computeIfAbsent(e.getEventType(), k -> new ArrayList<>()).add(e);
                }
            }
            findings.addAll(analyzer.detectConflicts(rawListeners));
        }

        return new ScanReport(findings, rawListeners, plugins.size(), System.currentTimeMillis() - start);
    }

    private List<PluginData> loadPluginData(List<Finding> findings) {
        File[] jars = pluginsDir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars == null) return List.of();

        List<PluginData> result = new ArrayList<>();
        for (File jar : jars) {
            try {
                PluginData data = parseJar(jar);
                if (data == null) continue;
                if (data.getName().equalsIgnoreCase(selfName)) continue; // skip self
                result.add(data);
            } catch (Exception e) {
                findings.add(new Finding(Tier.INFO, jar.getName(),
                        "Could not parse plugin metadata: " + e.getMessage() + " — bytecode analysis skipped"));
            }
        }
        return result;
    }

    private PluginData parseJar(File jar) throws IOException, InvalidConfigurationException {
        try (ZipFile zip = new ZipFile(jar)) {
            ZipEntry entry = zip.getEntry("plugin.yml");
            boolean isPaper = false;
            if (entry == null) {
                entry = zip.getEntry("paper-plugin.yml");
                isPaper = true;
            }
            if (entry == null) return null; // not a plugin jar

            YamlConfiguration yaml = new YamlConfiguration();
            try (InputStreamReader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                yaml.load(reader);
            }

            String name = yaml.getString("name");
            if (name == null || name.isBlank()) return null;

            String version = yaml.getString("version", "unknown");

            List<String> hardDepends;
            List<String> softDepends;
            List<String> loadBefore;
            List<String> loadAfter;

            if (isPaper) {
                hardDepends = new ArrayList<>();
                softDepends = new ArrayList<>();
                loadBefore = new ArrayList<>();
                loadAfter = new ArrayList<>();
                var serverDeps = yaml.getConfigurationSection("dependencies.server");
                if (serverDeps != null) {
                    for (String depName : serverDeps.getKeys(false)) {
                        var depCfg = serverDeps.getConfigurationSection(depName);
                        boolean required = depCfg != null && depCfg.getBoolean("required", false);
                        String load = depCfg != null ? depCfg.getString("load", "AFTER") : "AFTER";
                        (required ? hardDepends : softDepends).add(depName);
                        ("BEFORE".equals(load) ? loadBefore : loadAfter).add(depName);
                    }
                }
            } else {
                hardDepends = yaml.getStringList("depend");
                softDepends = yaml.getStringList("softdepend");
                loadBefore = yaml.getStringList("loadbefore");
                loadAfter = yaml.getStringList("loadafter");
            }

            Map<String, List<String>> commands = new LinkedHashMap<>();
            var cmdsSection = yaml.getConfigurationSection("commands");
            if (cmdsSection != null) {
                for (String cmdName : cmdsSection.getKeys(false)) {
                    List<String> tokens = new ArrayList<>();
                    tokens.add(cmdName);
                    var cmdCfg = cmdsSection.getConfigurationSection(cmdName);
                    if (cmdCfg != null) {
                        tokens.addAll(cmdCfg.getStringList("aliases"));
                    }
                    commands.put(cmdName, tokens);
                }
            }

            return new PluginData(name, version, hardDepends, softDepends, loadBefore, loadAfter, commands, jar);
        }
    }
}
