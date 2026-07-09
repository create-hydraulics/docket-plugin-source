# Docket: Usage Guide

## Quick Start

1. Place `docket-plugin-compatibility-1.0-RELEASE.jar` in your server's `plugins/` folder
2. Start or restart your server
3. Docket runs a compatibility scan one tick after all plugins have loaded
4. Check your console for the scan report, printed between two `=` border lines

No configuration is required for the basic scan to work.

---

## Reading the Console Report

After startup (and after every `/plugincheck scan`), Docket prints a structured report to the console:

```
══════════════════════════════════════════════════════════════
  DOCKET  SCAN  REPORT
══════════════════════════════════════════════════════════════
  Status    »  GREAT  -  All plugins are fully compatible
  Scanned   »  24 plugins in 48ms
  Findings  »  0 errors  ·  1 warning  ·  2 notes
──────────────────────────────────────────────────────────────
  ERRORS  (0)  -  None found
  WARNINGS  (1)
    [WARNING] PluginA <> PluginB: Command /home is registered by both plugins
  NOTES  (2)
    [INFO] PluginC: Hard dependency 'WorldEdit' is not installed
──────────────────────────────────────────────────────────────
  Use /plugincheck in-game for the interactive report
══════════════════════════════════════════════════════════════
```

This report is also written to `logs/latest.log`. Search for `DOCKET  SCAN  REPORT` to locate it.

---

## Health Score and Severity

Every scan produces one of four health statuses:

| Status | Condition |
|--------|-----------|
| **Great** | Zero errors and zero warnings |
| **Good** | No errors, at least one warning |
| **Mild** | 1-2 errors |
| **Bad** | 3 or more errors |

### Finding Severity Levels

| Level | Icon | Description |
|-------|------|-------------|
| **ERROR** | ✖ | A definite conflict that is causing or will cause problems |
| **WARN** | ⚠ | A potential conflict depending on your plugin configuration |
| **INFO** | ℹ | Informational, typically soft-dependency gaps or minor observations |

---

## Commands

All commands require the `plugincheck.use` permission (OP by default).

### `/plugincheck`

Opens the paginated compatibility report in chat. Findings are sorted by severity, errors first.

- Aliases: `/docket`, `/pcheck`
- Use `[ <- Prev ]` and `[ Next -> ]` to navigate pages

### `/plugincheck scan`

Triggers a fresh compatibility scan without restarting the server. The full report prints to console and the in-game view updates.

- Alias: `/plugincheck rescan`
- Useful after adding or removing a plugin without a full restart

### `/plugincheck events [page]`

Lists all events handled by more than one plugin. Each entry shows the event class, every plugin listening to it, the priority level, and `(ic)` if the listener has `ignoreCancelled = true`.

```
[Docket]  BlockBreakEvent  ->  WorldGuard@HIGH  CoreProtect@MONITOR
```

Look for cases where a plugin cancels at a lower priority while another at a higher priority has `(ic)` set. That higher listener will fire even after the cancel.

### `/plugincheck reload`

Reloads `plugins/Docket/config.yml`. Changes to Discord settings and scan options apply immediately.

### `/plugincheck help`

Lists all subcommands. Entries are clickable and auto-fill in the chat box.

---

## Admin Join Notification

Players with the `plugincheck.joinmessage` permission receive a health summary 10 ticks after joining. The summary shows the current status, number of plugins checked, issue count, and a clickable button to open the full report.

This permission defaults to OP. To grant it via LuckPerms:

```
/lp user <name> permission set plugincheck.joinmessage true
```

---

## Discord Setup

1. In Discord, open the target channel and go to **Settings > Integrations > Webhooks**
2. Create a webhook and copy the URL
3. In `plugins/Docket/config.yml`, set `discord.enabled: true` and paste the URL
4. Run `/plugincheck reload` then `/plugincheck scan` to test

```yaml
discord:
  enabled: true
  webhook-url: "https://discord.com/api/webhooks/..."
  post-on-rescan: true
```

---

## Configuration Reference

```yaml
discord:
  enabled: false          # Enable Discord webhook posting
  webhook-url: ""         # Webhook URL
  post-on-rescan: true    # Post when /plugincheck scan is run, not just on startup

scan:
  event-listener-analysis: true   # Bytecode scan for event conflicts
  console-info-listings: false    # Print the full event listener map to console on scan
```

### Large Server Tuning

On servers with 50+ plugins, the bytecode scan is the most resource-intensive step. If startup lag is noticeable, disable it:

```yaml
scan:
  event-listener-analysis: false
```

Dependency checking and command collision detection still run. The event map view (`/plugincheck events`) will not populate until event analysis is re-enabled.

---

## Understanding Findings

### Errors

| Finding | Cause | Fix |
|---------|-------|-----|
| Missing hard dependency | A plugin requires another plugin that is not installed | Install the required plugin |
| Circular dependency | Two or more plugins require each other to load first | Contact the plugin authors |
| Event priority conflict | A listener cancels an event, but a higher-priority listener from another plugin has `ignoreCancelled = true` | Adjust priorities or contact the plugin author |

### Warnings

| Finding | Cause | Notes |
|---------|-------|-------|
| Command collision | Two plugins register the same command or alias | Bukkit resolves by load order; use `commands.yml` to override if needed |
| Multiple HIGHEST/MONITOR listeners | Two plugins share top-priority on the same event | Usually harmless, but execution order between them is not guaranteed |

### Notes

Informational findings that require no action, included for debugging context.

---

## FAQ

**Does Docket slow down startup?**
The scan runs one tick after all plugins load and does not block startup. It typically completes in 20-200ms depending on plugin count. Bytecode analysis is the slowest step and can be disabled if needed.

**Can I scan without restarting?**
Yes. Run `/plugincheck scan` at any time to re-scan all currently loaded plugins.

**Does Docket work on Spigot?**
Yes. Docket targets the Paper API but is compatible with Spigot 1.21+.

**Can I turn off the join notification?**
Do not grant the `plugincheck.joinmessage` permission. It defaults to OP-only, so regular players never receive it.
