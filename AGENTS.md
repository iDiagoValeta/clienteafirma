# AutoFirma (fork)

Fork of `ctt-gob-es/clienteafirma` to modernise the Swing desktop UI (AutoFirma) on Linux.

## Build

Maven is installed per-user via mise (`maven@3.9.16`, runs on Temurin 17). The system `~/.m2` is root-owned (from a Docker image), so builds use a user-owned local repo:

```bash
~/.local/share/mise/shims/mvn -Dmaven.repo.local=$HOME/.m2-afirma -Denv=install -pl afirma-simple -am -DskipTests package
```

The `env=install` profile produces the shaded fat jar `afirma-simple/target/autofirma.jar` (Main-Class `es.gob.afirma.standalone.SimpleAfirma`). Run it with `java -jar afirma-simple/target/autofirma.jar`.

## Design

Palette: grafito. Chosen 2026-11-07: monochrome, identity from typography and spacing, follows the system light/dark theme.

Implemented with FlatLaf (`com.formdev:flatlaf`, Apache-2.0). `LookAndFeelManager` installs `FlatLightLaf`/`FlatDarkLaf` by the system theme (`GNOME_DARK_MODE`), keeps the system L&F only for Windows high-contrast, and recomputes `DEFAULT_COLOR`/`SECUNDARY_COLOR` from the active theme. `SignPanel` no longer hardcodes the welcome colour or the dark-grey drop area; the drop zone uses a dashed themed border and muted hint text. `PreferencesPanel` keeps all tabs in one row (`SCROLL_TAB_LAYOUT`) and widens the dialog to the measured width of the tabs, capped at 90% of the screen.
