# Modulento-App

Android-App für Websites, die mit dem CMS Modulento betrieben werden. Dieselbe APK
dient Betreibern (Verwaltung der eigenen Website) und deren Kundinnen und Kunden
(Konto, Stöbern) – auch ohne Konto als Gast. Welche Bereiche jemand sieht,
entscheidet allein die Antwort des Servers: Die App kennt keine Rollen.

Stand: Grundgerüst (Phase 1). Die Server-Adresse wird in der App eingegeben; fest
eingebaute Adresse und Betreiber-Branding zur Bauzeit folgen später.

## Funktionen

- **Erststart / Einstellungen**: Eingabe der Server-Adresse. Ohne Adresse zeigt die
  App nur dieses Formular. Fehlt das Schema, wird `https://` angenommen.
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

1. **Keine Server-Adresse** → Eingabeformular.
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
- Registrierung, native Verwaltungsbildschirme, In-App-Update sowie fest
  eingebaute Server-Adresse und Branding zur Bauzeit sind bewusst noch nicht
  enthalten.
