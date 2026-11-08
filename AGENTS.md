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

Typography is Inter (`flatlaf-fonts-inter`) at base size +1; the FlatLaf accent is set to grey (`@accentColor`) so primary buttons, focus and selection stay monochrome. Primary actions use `SignPanel.PRIMARY_BUTTON_STYLE`. The main screen shows a one-line summary with the full legal notice folded behind "More information", and the file selection button lives inside the drop zone.

## Compose UI

The desktop signing flow runs on Compose Desktop (Kotlin) by default; `-Dafirma.ui=swing` restores the classic Swing UI. Sources live in `afirma-simple/src/main/kotlin/es/gob/afirma/standalone/compose/`; `SimpleAfirma.main` hands over to `ComposeLauncher.launch` just before the Swing `initGUI`.

- Build: `kotlin-maven-plugin` compiles Kotlin (reading the Java sources) before javac, with the Compose compiler passed as `-Xplugin` (Maven has no Compose Gradle plugin). Maven cannot read Gradle module metadata, so the `*-desktop` artifacts and `skiko-awt-runtime-linux-x64` are declared explicitly. Kotlin targets JVM 17, so the jar needs Java 17+.
- Rendering: Skia's OpenGL backend fails under XWayland here, so the launcher defaults `skiko.renderApi` to `SOFTWARE_FAST`.
- Logic: `Engine.kt` calls the existing signing code (`ComposeBridge` exposes `SignPanel.prepareSignConfig` and the certificate filters). `ComposeUIManager` routes core prompts (keystore and PDF passwords, confirmations) to Compose dialogs; calls on the event thread fall back to Swing.
- Screens: home, document, visible signature (draw the box on the page, live preview), certificate, signing, result. UI text is Spanish only for now.
- Testing: `-Dafirma.ui.open=<file>` opens a document at startup (any program argument switches AutoFirma to command line mode); `-Dafirma.ui.theme=light|dark` overrides the system theme. A PKCS#12 store can be preselected with the `defaultKeystore=PKCS12` and `defaultLocalKeystorePath` preferences.
