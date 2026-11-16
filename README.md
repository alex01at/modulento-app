# Modulento-App

Android-App für Websites, die mit dem CMS Modulento betrieben werden. Dieselbe APK
dient Betreibern (Verwaltung der eigenen Website) und deren Kundinnen und Kunden
(Konto, Stöbern) – auch ohne Konto als Gast. Welche Bereiche jemand sieht,
entscheidet allein die Antwort des Servers: Die App kennt keine Rollen.

Stand: Grundgerüst (Phase 1). Ohne weitere Angaben entsteht die neutrale
„Modulento"-App, in der die Server-Adresse eingegeben wird. Für einen Betreiber
lassen sich Adresse, App-Name und Icon zur Bauzeit fest einbauen (siehe
„Betreiber-Build").

## Funktionen

- **Erststart / Einstellungen**: Eingabe der Server-Adresse. Ohne Adresse zeigt die
  App nur dieses Formular. Fehlt das Schema, wird `https://` angenommen. Ist in
  die APK eine Adresse eingebaut, entfällt das Formular beim ersten Start; ändern
  lässt sich die Adresse weiterhin in den Einstellungen.
- **Gast**: die öffentliche Startseite der Website in einer WebView, dazu
  „Anmelden" in der Kopfleiste.
- **Anmelden**: E-Mail, Passwort und ein Gerätename (vorbelegt mit dem
  Gerätemodell). Falsche Zugangsdaten, zu viele Versuche und unbestätigte
  E-Mail-Adresse werden getrennt erklärt.
- **Dashboard**: Name der Website, Hinweise des Servers („Zu erledigen") und – nur
  wenn der Server ein Verwaltungsmenü liefert – der Einstieg in die Verwaltung.
- **Entdecken**: die Website in der App; „Mein Konto auf der Website öffnen"
  wechselt angemeldet in den Kontobereich.
- **Verwaltung** (nur mit Verwaltungsmenü): alle Gruppen und Einträge des
  Admin-Menüs. Ein Eintrag öffnet die jeweilige Seite angemeldet in der
  In-App-WebView; der Zurück-Button geht erst in der Seite zurück, dann aus ihr
  heraus.
- **Konto**: Kontodaten, Server-Adresse, Abmelden.
- **Farben**: Die Primärfarbe folgt `branding.accent` des Servers (gültiger
  Hex-Wert vorausgesetzt, sonst Material-Standard), der Titel `branding.site_name`.

## Bauen

Voraussetzungen: JDK 17 oder neuer (getestet mit Temurin 21) und das Android SDK
(Plattform 37). Der Gradle-Wrapper lädt Gradle selbst.

**Android Studio:** Ordner öffnen, synchronisieren lassen, „Run".

**Kommandozeile:**

```bash
export JAVA_HOME=~/.jdks/temurin-21        # Pfad zum JDK
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # Pfad zum Android SDK
./gradlew assembleDebug
```

Die APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`.

Tests (API-Client, Fehlerzuordnung, Sitzungslogik, Adress- und Farbprüfung):
`./gradlew testDebugUnitTest`

### Signierte Release-APK

Im Projekt-Root eine Datei `keystore.properties` anlegen (steht wie der Keystore in
der `.gitignore`):

```properties
storeFile=modulento-release.jks
storePassword=…
keyAlias=modulento
keyPassword=…
```

```bash
./gradlew assembleRelease
```

Ohne `keystore.properties` entsteht eine unsignierte, nicht installierbare
`app-release-unsigned.apk`.

Bei einem Keystore im Format PKCS12 (Standard von `keytool`) gibt es kein eigenes
Schlüssel-Passwort: `keyPassword` muss denselben Wert haben wie `storePassword`.
Weichen die beiden ab, bricht der Build bei `packageRelease` mit „Failed to read
key … Given final block not properly padded" ab.

### Betreiber-Build

Vier optionale Gradle-Properties bauen die Angaben eines Betreibers fest ein. Jede
kann einzeln entfallen; ohne sie bleibt der Build unverändert.

| Property | Wirkung |
|---|---|
| `appServerUrl` | Server-Adresse, die beim ersten Start übernommen wird (`BuildConfig.DEFAULT_SERVER_URL`). Die Release-Variante spricht nur HTTPS. |
| `appLabel` | Name der App auf dem Gerät (`app_name`), Standard „Modulento" |
| `appIdSuffix` | Zusatz zur App-ID: `kunde1` ergibt `at.modulento.app.kunde1`. Nur damit lassen sich mehrere Builds nebeneinander installieren. Erlaubt sind Kleinbuchstaben, Ziffern und `_`. |
| `appIconPath` | Pfad zu einer PNG-Datei, die den Vordergrund des App-Icons ersetzt |

```bash
./gradlew assembleRelease \
  -PappServerUrl=https://kunde.example.com \
  -PappLabel="Kunde GmbH" \
  -PappIdSuffix=kunde1 \
  -PappIconPath=/pfad/zu/icon.png
```

Zum Icon: Die PNG-Datei sollte quadratisch sein (432 × 432 px) und einen
transparenten Hintergrund haben; das Motiv gehört in die mittleren zwei Drittel,
der Rand wird vom System beschnitten. Die Hintergrundfarbe bleibt
`ic_launcher_background` aus `app/src/main/res/values/colors.xml`. Geprüft ist
nur, dass die Datei im Paket landet, nicht die Darstellung auf einem Gerät.

`app_name` steht bewusst nicht in `strings.xml`, sondern wird allein von
`app/build.gradle.kts` erzeugt.

### Build über GitHub Actions

`.github/workflows/build-customer-app.yml` baut eine signierte Betreiber-APK auf
Anforderung (`workflow_dispatch`, Eingaben `server_url`, `app_name`,
`app_id_suffix`, `client_token`) und legt sie einen Tag lang als Artefakt
`app-<client_token>` ab. Der Lauf heißt „App-Build <client_token>", daran findet
ihn das aufrufende Werkzeug wieder. Ein Icon nimmt der Workflow nicht entgegen.

Der Workflow braucht vier Repository-Secrets (Settings → Secrets and variables →
Actions):

| Secret | Inhalt |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | der Keystore, Base64-kodiert (`base64 -w0 datei.jks`) |
| `ANDROID_KEYSTORE_PASSWORD` | `storePassword` |
| `ANDROID_KEY_ALIAS` | `keyAlias` |
| `ANDROID_KEY_PASSWORD` | `keyPassword` (bei PKCS12 gleich dem Store-Passwort) |

`.github/workflows/ci.yml` führt bei jedem Push und Pull Request die Tests aus.

## Gegen einen lokalen Server testen

Die Release-Variante spricht ausschließlich HTTPS. Nur die **Debug-Variante**
erlaubt zusätzlich unverschlüsseltes HTTP zu `127.0.0.1`, `localhost` und
`10.0.2.2` (`app/src/debug/res/xml/network_security_config.xml`).

Empfohlen ist die Portweiterleitung, weil der Server Links mit genau dem Host
ausliefert, unter dem er angesprochen wird:

```bash
adb reverse tcp:8098 tcp:8098      # Gerät/Emulator: 127.0.0.1:8098 -> Rechner
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Dann in der App als Server-Adresse `http://127.0.0.1:8098` eintragen. Im Emulator
geht ohne Weiterleitung auch `http://10.0.2.2:8098`.

## Technik

Kotlin, Jetpack Compose, Material 3, MVVM (Screen → ViewModel → Repository →
API-Client), minSdk 26 (Android 8.0), target/compileSdk 37.

| Aufgabe | Lösung | Warum |
|---|---|---|
| HTTP/JSON | Retrofit + OkHttp + `kotlinx-serialization-json` | schlank, ohne Reflection-Bibliothek; die Routen stehen lesbar in einem Interface |
| Server-Adresse und Token | `EncryptedSharedPreferences` (`androidx.security:security-crypto`) | verschlüsselt mit einem Schlüssel aus dem Android Keystore |
| Navigation | Navigation Compose mit typisierten Routen | Routen sind Kotlin-Objekte, kein Zusammenbauen von Pfad-Strings |
| Website-Seiten | `android.webkit.WebView` in Compose | Plattform-Bestandteil, keine Zusatzbibliothek |

Aufbau unter `app/src/main/java/at/modulento/app/`:

- `data/api/` – Datenmodelle, Retrofit-Interface, `ApiClient` (Envelope auspacken,
  Fehler vereinheitlichen)
- `data/` – `SessionStore` (verschlüsselter Speicher), `ServerUrl`,
  `ModulentoRepository`
- `ui/` – `AppRoot` (Phasen und Navigation), Bildschirme, ViewModels, `SiteWebView`
- `ui/theme/` – Farbschema aus der Akzentfarbe

Alle Oberflächentexte stehen in `app/src/main/res/values/strings.xml`.

### Ablauf

Die App kennt drei Phasen, jede mit eigenem Navigationsgraphen:

1. **Keine Server-Adresse** → Eingabeformular (entfällt, wenn eine Adresse
   eingebaut ist).
2. **Gast** → Website, Anmelden, Einstellungen.
3. **Angemeldet** → `GET /api/v1/meta` wird geladen und im `AppViewModel` gehalten;
   daraus entstehen Tabs, Dashboard und Farben.

Lehnt der Server das Token ab (401, oder 403 mit `unauthenticated`), wird es
gelöscht und die Anmeldung erscheint mit einem Hinweis. Ist der Server nur nicht
erreichbar, bleibt die Anmeldung erhalten; es gibt „Erneut versuchen" und
„Abmelden".

Seiten, die eine Anmeldung brauchen, öffnet die App über
`POST /api/v1/auth/webview-handoff`: Der Server liefert eine einmalig gültige
Adresse, die in der WebView eine normale Browser-Sitzung startet und zum
gewünschten Pfad weiterleitet. Das Token selbst gelangt nie in die WebView. Beim
Abmelden und beim Wechsel der Server-Adresse werden Token, Cookies und
Website-Speicher der WebView gelöscht; das Token wird zusätzlich am Server
widerrufen (ein Fehler dabei verhindert das lokale Abmelden nicht).

## Bekannte Einschränkungen / offene Punkte

- **Kein Datei-Upload und kein Download in der WebView** (z. B. Mediathek,
  Rechnungs-PDF): Die Dateiauswahl und der Download-Manager sind noch nicht
  angebunden.
- **Gast-Modus ohne Branding**: Farben und Website-Name kommen aus
  `/api/v1/meta`, das eine Anmeldung verlangt. Vor dem Login gelten die
  Standardfarben und der Titel „Modulento".
- Wer sich als Gast **innerhalb der Website** anmeldet, ist nur dort angemeldet,
  nicht in der App (keine nativen Bereiche). Umgekehrt meldet ein Logout auf der
  Website die App nicht ab.
- `meta` wird nur im Arbeitsspeicher gehalten: kein Offline-Betrieb, und nach
  einem App-Neustart wird neu geladen. „Aktualisieren" im Dashboard lädt neu.
- Beim Tab-Wechsel lädt „Entdecken" die Startseite neu (die WebView wird nicht
  zwischengespeichert).
- Hinweise („Zu erledigen") sind nur antippbar, wenn ihr Link ein lokaler Pfad ist.
- Externe Links bleiben in der WebView (nötig, damit Zahlungsanbieter in dieselbe
  Sitzung zurückkehren); nur `mailto:`, `tel:` u. Ä. gehen ans System.
- Release-Build ohne Code-Verkleinerung (R8 aus).
- Nur deutsche Oberfläche; die Texte des Servers (Menü, Hinweise) kommen in der
  Sprache des Kontos.
- Eine eingebaute Server-Adresse wird nur übernommen, solange noch keine
  gespeichert ist. Ein Update mit anderer eingebauter Adresse ändert eine
  bestehende Installation nicht.
- Registrierung, native Verwaltungsbildschirme und In-App-Update sind bewusst
  noch nicht enthalten.
