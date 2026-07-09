package plg.docket.docket.scanner;

import java.io.File;
import java.util.List;
import java.util.Map;

public final class PluginData {
    private final String name;
    private final String version;
    private final List<String> hardDepends;
    private final List<String> softDepends;
    private final List<String> loadBefore;
    private final List<String> loadAfter;
    private final Map<String, List<String>> commands; // command name -> [name, alias1, alias2, ...]
    private final File jarFile;

    public PluginData(String name, String version,
                      List<String> hardDepends, List<String> softDepends,
                      List<String> loadBefore, List<String> loadAfter,
                      Map<String, List<String>> commands, File jarFile) {
        this.name = name;
        this.version = version;
        this.hardDepends = List.copyOf(hardDepends);
        this.softDepends = List.copyOf(softDepends);
        this.loadBefore = List.copyOf(loadBefore);
        this.loadAfter = List.copyOf(loadAfter);
        this.commands = Map.copyOf(commands);
        this.jarFile = jarFile;
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public List<String> getHardDepends() { return hardDepends; }
    public List<String> getSoftDepends() { return softDepends; }
    public List<String> getLoadBefore() { return loadBefore; }
    public List<String> getLoadAfter() { return loadAfter; }
    public Map<String, List<String>> getCommands() { return commands; }
    public File getJarFile() { return jarFile; }
}
