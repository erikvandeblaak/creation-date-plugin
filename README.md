# Creation Date in Project View

[![CI](https://github.com/erikvandeblaak/creation-date-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/erikvandeblaak/creation-date-plugin/actions/workflows/ci.yml)

A plugin for PhpStorm (and other JetBrains IDEs) that shows when a file or folder was created.
Rest the mouse on a file or folder in the **Project** tool window and a yellow balloon shows its
creation date and time, read from the file system.

![The balloon](docs/balloon.png)

*Nederlands: zie [README.nl.md](README.nl.md).*

## Install

1. Download the newest `creation-date-plugin-x.y.z.zip` from [Releases](https://github.com/erikvandeblaak/creation-date-plugin/releases/latest).
2. In PhpStorm: **Settings → Plugins → ⚙ → Install Plugin from Disk…** and choose the zip.

## Automatic updates

Add this repository once, then PhpStorm reports new versions by itself:

1. **Settings → Plugins → ⚙ → Manage Plugin Repositories…**
2. Click **+** and paste:

   ```
   https://github.com/erikvandeblaak/creation-date-plugin/releases/latest/download/updatePlugins.xml
   ```

3. Click **OK**. New versions now appear under **Plugins → Installed** and in the update notifications.

You can also install the plugin this way: after adding the repository, search for
"Creation Date" under **Plugins → Marketplace**.

## Requirements

- PhpStorm 2026.2 or newer (build 262+). The plugin only uses the IntelliJ Platform, so it should
  also run in IntelliJ IDEA, WebStorm, PyCharm and the other JetBrains IDEs; it is tested in PhpStorm.
- Files on a local disk. Files inside archives or on remote/virtual file systems have no creation date.
- The language follows your system: Dutch on a Dutch system, English everywhere else.

## Build it yourself

You need JDK 25 (the IntelliJ Platform 2026.2 is built with Java 25).

```
./gradlew test          # tests, against PhpStorm 2026.2.3 (downloaded on first run)
./gradlew buildPlugin   # zip in build/distributions/
./gradlew verifyPlugin  # compatibility check against PhpStorm 2026.2.3 and 2026.3
./gradlew runIde        # try it in a sandbox PhpStorm
```

To skip the PhpStorm download, point `phpstormPath` in `~/.gradle/gradle.properties` at a local installation.

## Release (for the maintainer)

1. Raise `pluginVersion` in `gradle.properties` and add a section to `CHANGELOG.md`.
2. Commit, then tag and push: `git tag v1.2.1 && git push origin main v1.2.1`.
3. The release workflow tests, verifies and builds the plugin, and publishes a GitHub release with the
   zip and a new `updatePlugins.xml`. Every PhpStorm with the repository above then sees the update.

## License

Licensed under either of [MIT](LICENSE-MIT) or [Apache-2.0](LICENSE-APACHE), at your option.
