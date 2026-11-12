# Creation Date in Project View

Een plugin voor PhpStorm (en andere JetBrains-IDE's) die laat zien wanneer een bestand of map is
aangemaakt. Ga in de **Project**-weergave met de muis op een bestand of map staan en een geel blokje
toont de aanmaakdatum en -tijd uit het bestandssysteem.

![Het gele blokje](docs/balloon.png)

## Installeren

1. Download de nieuwste `creation-date-plugin-x.y.z.zip` bij [Releases](https://github.com/erikvandeblaak/creation-date-plugin/releases/latest).
2. In PhpStorm: **Settings → Plugins → ⚙ → Install Plugin from Disk…** en kies de zip.

## Automatisch updaten

Voeg deze bron één keer toe; daarna meldt PhpStorm nieuwe versies zelf:

1. **Settings → Plugins → ⚙ → Manage Plugin Repositories…**
2. Klik op **+** en plak:

   ```
   https://github.com/erikvandeblaak/creation-date-plugin/releases/latest/download/updatePlugins.xml
   ```

3. Klik op **OK**. Nieuwe versies verschijnen onder **Plugins → Installed** en in de updatemeldingen.

## Eisen

- PhpStorm 2026.2 of nieuwer (build 262+). De plugin gebruikt alleen het IntelliJ-platform, dus hij
  hoort ook te werken in IntelliJ IDEA, WebStorm, PyCharm en de andere JetBrains-IDE's; getest is hij in PhpStorm.
- Bestanden op een lokale schijf. Bestanden in archieven of op externe bestandssystemen hebben geen aanmaakdatum.
  Op Linux-bestandssystemen die geen aanmaaktijd bijhouden, geeft Java de laatste wijzigingstijd.
- De taal volgt je systeem: Nederlands op een Nederlands systeem, anders Engels.

## Licentie

Naar keuze [MIT](LICENSE-MIT) of [Apache-2.0](LICENSE-APACHE).
