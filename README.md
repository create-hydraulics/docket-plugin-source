<div align="center">

<p align="center">
  <img src="https://raw.githubusercontent.com/create-hydraulics/docket-plugin-source/main/media/logo.png" alt="Create: Hydraulics" width="360px">
</p>

# Docket: Plugin Compatibility Scanner

*Plugin compatibility auditing for Paper and Spigot servers.*

<br/>

<a href="https://papermc.io/downloads">
  <img src="https://img.shields.io/badge/PaperMC-1.21%2B-00AAFF?style=for-the-badge" height="35" alt="PaperMC"/>
</a>
&nbsp;
<a href="https://www.spigotmc.org">
  <img src="https://img.shields.io/badge/SpigotMC-1.21%2B-ED8106?style=for-the-badge" height="35" alt="SpigotMC"/>
</a>
&nbsp;
<img src="https://img.shields.io/badge/Version-1.0--RELEASE-4a90d9?style=for-the-badge" height="35" alt="Version"/>
&nbsp;
<img src="https://img.shields.io/badge/Java-21%2B-f89820?style=for-the-badge" height="35" alt="Java 21+"/>
&nbsp;
<img src="https://img.shields.io/badge/License-Apache%202.0-lightgrey?style=for-the-badge" height="35" alt="Apache 2.0"/>

</div>

---

Docket runs a compatibility scan on every startup and prints the results directly in your console. It checks hard dependency gaps, command collisions, and event-listener priority conflicts across all installed plugins, grouped by severity with a server health score.

No configuration required. Drop the jar in and it works.

---

## What's Included

- **Bytecode event analysis:** reads plugin JARs using [ASM 9.7](https://asm.ow2.io/) to map event handlers and detect listener priority conflicts
- **Dependency auditing:** flags missing hard dependencies and circular dependency chains
- **Command collision detection:** identifies overlapping command registrations across plugins
- **Health score:** rates your server as Great, Good, Mild, or Bad with a full findings breakdown
- **Discord webhook:** posts scan reports to a channel on startup or rescan, no bot required
- **In-game report:** paginated `/plugincheck` output with colour-coded findings and clickable navigation
- **Large server support:** bytecode analysis can be disabled in config if startup performance is a concern

---

## Requirements

| | |
|-|-|
| Server | Paper 1.21+ or Spigot 1.21+ |
| Java | 21 or newer |

---

## Installation

Drop `docket-plugin-compatibility-1.0-RELEASE.jar` into your server's `plugins/` folder and restart. The scan runs automatically with no additional setup. See [USAGE.md](https://github.com/create-hydraulics/docket-plugin-source/blob/main/USAGE.md) for commands, configuration, and Discord setup.


---

## Credits

| | |
|-|-|
| Development | Docket Contributors |
| Bytecode Analysis | [ASM Library](https://asm.ow2.io/) by OW2 Consortium |
| Server API | [PaperMC](https://papermc.io/) Team |

---

## License

Released under the **Apache License 2.0**. See [LICENSE](LICENSE) for full terms.

---

<div align="center">

Paper 1.21+ &nbsp;·&nbsp; Spigot-compatible &nbsp;·&nbsp; Java 21+

</div>
