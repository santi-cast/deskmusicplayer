# Setup

*[Leer en español](setup.md)*

## Before you start: which phone this is for

DeskMusicPlayer is meant to **reuse an old phone** that is no longer fit to be a phone (no security updates, worn
battery). That is why:

- **Do not sign in to any Google account** and do not use Google Play: a phone without patches should not hold personal
  accounts. Apps are installed from **F-Droid** or from **Aurora Store** in anonymous mode.
- The phone is dedicated to music and, with kiosk mode, locked to the chosen apps.
- On a modern phone it makes no sense to set it up like this.

Tested on a Motorola Moto G5 Plus (XT1685) with stock Android 8.1. The app needs Android 8.0 or newer; the touch
detection that drives the screensaver relies on behaviour that newer Android versions (12 and up) restrict.

**FM radio is optional:** many phones have no tuner, and if it is missing it simply does not show up. The station name,
logos and favourites scan target Motorola's radio app; other radio apps work like any player if they publish what is
playing.

The interface is in Spanish; the names below are the ones shown in the app.

## Option A: everything from the phone

1. **Install the app**: download the APK with the phone's browser and open it. Android will ask to allow installing apps
   from the browser.
2. **Choose it as the home screen**: when you press Home, Android asks which app to use; choose *Reproductor* and
   *Always*.
3. **🔧 → Configuración inicial**: complete the steps marked with •. Each *Abrir* opens the matching Android screen; back
   in the app, the step shows ✓:
   - *Mostrar el reloj sobre otras apps* — draw over other apps (without it there is no screensaver).
   - *Ver qué está sonando* — notification access.
   - *Botón de apagar* — accessibility service that only opens the power menu.
   - *Pantalla siempre encendida* — screen always on (the first time it asks for "Modify system settings").
   - *Sin PIN ni patrón* — no PIN or pattern on the lock screen, so the player starts straight away.
   - *Sin pantalla de bloqueo* — no swipe lock screen (kiosk mode only). Android does not let an app check it, so its ✓
     appears after pressing *Aplicar*.

   While something essential is missing, the home screen shows an orange notice that leads here.
4. **🔧 → Apps de la pantalla principal**: choose up to 5 apps, reorder them (the first one is the large one) and name
   them. If no store is installed, **Instalar Aurora Store** opens its official F-Droid page: press *Download APK*, open it
   and, if Android asks, allow installing apps from the browser.
5. **🔧 → Logos de emisoras** (optional, Motorola radio only): pick an image on the phone for each station.

Without kiosk mode the app works the same as home screen and player, but it does not lock: you can leave it from the
notification shade.

## Option B: with a computer (needed today for kiosk mode)

**Requirements:** USB debugging enabled and the PC authorised for `adb`; on the PC, JDK 17 or newer and the Android SDK
(platform 36), with its path in `local.properties`: `sdk.dir=/path/to/Android/Sdk`.

```
./gradlew assembleDebug testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Kiosk mode (device owner)

Locks the phone to the chosen apps. Android only allows it on a phone with **no accounts** (Settings → Accounts); a
freshly reset phone is easiest.

```
adb shell dpm set-device-owner com.reproductor.kiosk/.AdminReceiver
adb shell am start -n com.reproductor.kiosk/.MainActivity
```

From then on the phone boots into the app and only the chosen apps, the chosen store and Android's install confirmation
can open. To undo it, open 🔧 and use **Quitar device owner**: the phone becomes a normal phone again.

Turning it on without a computer with a **QR code** after a factory reset (the Android enterprise method) is planned, once
there is a published release for the QR to point to.

### Permissions and settings over `adb` (alternative to the in-app setup)

```
# Overlay windows: touch detector and clock
adb shell appops set com.reproductor.kiosk SYSTEM_ALERT_WINDOW allow
# Read the media session of the app that is playing
adb shell cmd notification allow_listener com.reproductor.kiosk/com.reproductor.kiosk.MediaListener
# Accessibility service used only to open the power menu
adb shell settings put secure enabled_accessibility_services com.reproductor.kiosk/com.reproductor.kiosk.PowerMenuService
adb shell settings put secure accessibility_enabled 1
# No lock screen, screen always on and auto-rotate
adb shell locksettings set-disabled true
adb shell settings put system screen_off_timeout 2147483647
adb shell settings put system accelerometer_rotation 1
```

The accessibility command replaces any other enabled accessibility service; check
`adb shell settings get secure enabled_accessibility_services` first if you use one.

## Apps and stores

- **Home screen apps:** until they are configured, the default apps that are installed are shown (Spotify, Tempus as
  "Servidor", Motorola's FM radio, Auxio as "Música" and AntennaPod as "Podcasts"). The default package names are in
  `app/src/main/java/com/reproductor/kiosk/Apps.kt`.
- **Supported stores:** Aurora Store, F-Droid and Droid-ify. If several are installed, choose one in *Apps de la pantalla
  principal*. Google Play is not offered, on purpose.
- **Uninstall:** from the same screen, for apps that are not on the home screen. Apps that came with the phone, the stores
  and the app itself cannot be uninstalled.
- **Local music:** Auxio needs its music folder chosen once in *Music sources*, and that picker does not open while
  locked: in 🔧 turn off *Bloqueo activado*, choose the folder (internal storage or microSD) and turn it back on.

## FM station logos

The radio publishes no artwork. The easiest way is **🔧 → Logos de emisoras**: *Añadir logo* asks for the frequency
(prefilled with the station playing; "93,9" and "93.9" both work) and opens Android's image picker. The image is stored
at 512 px.

From a computer they can also be copied directly, named after the frequency (`93.9.png`; `.jpg`, `.jpeg` and `.webp` also
work):

```
adb push 93.9.png /sdcard/Android/data/com.reproductor.kiosk/files/emisoras/
```

No logos are included in this repository: they belong to the stations.

To make previous/next go through your favourite stations only (on the Motorola radio), turn on its *favourites scan*
button in the radio's notification (with the lock off, since the notification shade is blocked).

## Handy commands

```
# Show the clock now
adb shell am start-foreground-service -n com.reproductor.kiosk/.ScreensaverService -a com.reproductor.kiosk.SHOW_CLOCK
# Screenshot
adb exec-out screencap -p > screen.png
```
