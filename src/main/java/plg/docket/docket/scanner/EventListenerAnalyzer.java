package plg.docket.docket.scanner;

import plg.docket.docket.report.Finding;
import plg.docket.docket.report.Tier;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class EventListenerAnalyzer {

    private static final String EVENT_HANDLER_DESC = "Lorg/bukkit/event/EventHandler;";

    private static final Map<String, Integer> PRIORITY_ORDER = Map.of(
            "LOWEST", 0, "LOW", 1, "NORMAL", 2, "HIGH", 3, "HIGHEST", 4, "MONITOR", 5
    );

    private final Logger log;

    public EventListenerAnalyzer(Logger log) {
        this.log = log;
    }

    public List<EventListenerEntry> analyzeJar(String pluginName, File jarFile) {
        List<EventListenerEntry> result = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jarFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                try (InputStream is = zip.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(is);
                    EventHandlerClassVisitor visitor = new EventHandlerClassVisitor(pluginName, result);
                    reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                } catch (Exception ignored) {
                    // Unreadable class (encrypted, unsupported version, etc.) — skip silently
                }
            }
        } catch (Exception e) {
            log.warning("[Docket] Could not read JAR for " + pluginName + ": " + e.getMessage());
        }
        return result;
    }

    public List<Finding> detectConflicts(Map<String, List<EventListenerEntry>> listenersByEvent) {
        List<Finding> findings = new ArrayList<>();
        Set<String> reported = new HashSet<>();

        for (Map.Entry<String, List<EventListenerEntry>> entry : listenersByEvent.entrySet()) {
            String eventType = entry.getKey();
            List<EventListenerEntry> listeners = entry.getValue();

            if (listeners.size() < 2) continue;

            List<EventListenerEntry> cancellers = listeners.stream()
                    .filter(EventListenerEntry::callsSetCancelled)
                    .toList();

            List<EventListenerEntry> ignorers = listeners.stream()
                    .filter(EventListenerEntry::isIgnoreCancelled)
                    .toList();

            // Rule 1: canceller at priority X, ignoreCancelled listener at higher priority
            for (EventListenerEntry canceller : cancellers) {
                int cancelPri = PRIORITY_ORDER.getOrDefault(canceller.getPriority(), 2);
                for (EventListenerEntry ignorer : ignorers) {
                    if (ignorer.getPluginName().equals(canceller.getPluginName())) continue;
                    int ignorePri = PRIORITY_ORDER.getOrDefault(ignorer.getPriority(), 2);
                    if (ignorePri > cancelPri) {
                        String key = eventType + ":" + canceller.getPluginName() + ":" + ignorer.getPluginName();
                        if (reported.add(key)) {
                            findings.add(new Finding(Tier.WARNING,
                                    canceller.getPluginName(), ignorer.getPluginName(),
                                    "Possible conflict on " + eventType + ": "
                                    + canceller.getPluginName() + " may cancel at " + canceller.getPriority()
                                    + ", but " + ignorer.getPluginName() + " runs later with ignoreCancelled=true"));
                        }
                    }
                }
            }

            // Rule 2: multiple distinct plugins at HIGHEST or MONITOR on the same event
            List<String> highPlugins = listeners.stream()
                    .filter(l -> "HIGHEST".equals(l.getPriority()) || "MONITOR".equals(l.getPriority()))
                    .map(EventListenerEntry::getPluginName)
                    .distinct()
                    .toList();
            if (highPlugins.size() >= 2) {
                String key = "hm:" + eventType + ":" + String.join(":", highPlugins.stream().sorted().toList());
                if (reported.add(key)) {
                    findings.add(new Finding(Tier.WARNING,
                            highPlugins.get(0), highPlugins.get(1),
                            "Possible conflict on " + eventType + ": "
                            + String.join(", ", highPlugins)
                            + " all run at HIGHEST/MONITOR — last-write wins, may produce unexpected state (possible conflict, unverified)"));
                }
            }
        }

        return findings;
    }

    // ── ASM visitors ──────────────────────────────────────────────────────────

    private static final class EventHandlerClassVisitor extends ClassVisitor {
        private final String pluginName;
        private final List<EventListenerEntry> output;

        EventHandlerClassVisitor(String pluginName, List<EventListenerEntry> output) {
            super(Opcodes.ASM9);
            this.pluginName = pluginName;
            this.output = output;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            return new EventHandlerMethodVisitor(pluginName, descriptor, output);
        }
    }

    private static final class EventHandlerMethodVisitor extends MethodVisitor {
        private final String pluginName;
        private final String descriptor;
        private final List<EventListenerEntry> output;

        private boolean isEventHandler = false;
        private String priority = "NORMAL";
        private boolean ignoreCancelled = false;
        private boolean callsSetCancelled = false;

        EventHandlerMethodVisitor(String pluginName, String descriptor, List<EventListenerEntry> output) {
            super(Opcodes.ASM9);
            this.pluginName = pluginName;
            this.descriptor = descriptor;
            this.output = output;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (!EVENT_HANDLER_DESC.equals(desc)) return null;
            isEventHandler = true;
            return new AnnotationVisitor(Opcodes.ASM9) {
                @Override
                public void visitEnum(String name, String desc, String value) {
                    if ("priority".equals(name)) priority = value;
                }

                @Override
                public void visit(String name, Object value) {
                    if ("ignoreCancelled".equals(name) && value instanceof Boolean b) {
                        ignoreCancelled = b;
                    }
                }
            };
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                                     String desc, boolean isInterface) {
            if ("setCancelled".equals(name)) callsSetCancelled = true;
        }

        @Override
        public void visitEnd() {
            if (!isEventHandler) return;
            String eventType = parseFirstParam(descriptor);
            if (eventType == null) return;
            output.add(new EventListenerEntry(pluginName, eventType, priority, ignoreCancelled, callsSetCancelled));
        }

        private static String parseFirstParam(String desc) {
            int i = desc.indexOf('(');
            if (i < 0) return null;
            i++;
            while (i < desc.length() && desc.charAt(i) == '[') i++; // skip arrays
            if (i >= desc.length() || desc.charAt(i) != 'L') return null;
            i++;
            int end = desc.indexOf(';', i);
            if (end < 0) return null;
            String internal = desc.substring(i, end);
            int slash = internal.lastIndexOf('/');
            return slash >= 0 ? internal.substring(slash + 1) : internal;
        }
    }
}
