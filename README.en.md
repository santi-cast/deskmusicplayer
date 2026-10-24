# DeskMusicPlayer

*[Leer en español](README.md)*

An old phone turned into a **music player that lives on the desk**: it boots locked into a screen with the music apps
you choose (Spotify, a home music server, the FM radio, MP3s, podcasts…) and, when nobody touches it, it shows a clock
with what is playing.

![Kotlin](https://img.shields.io/badge/Kotlin-Android_SDK-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Device owner](https://img.shields.io/badge/Android-device_owner_%2B_lock_task-3DDC84)
![Tests](https://img.shields.io/badge/tests-54_JUnit-success)
![AI agents](https://img.shields.io/badge/built_with-AI_agents-8A2BE2)
![License](https://img.shields.io/badge/license-MIT-blue)

| Home screen | Clock with the FM radio |
|---|---|
| ![Home screen with Spotify, Servidor, Radio FM, Música and Podcasts](docs/screenshots/home.png) | ![Clock with the station logo and name](docs/screenshots/clock-fm-radio.png) |

| Now playing | Night mode |
|---|---|
| ![Clock with the Spotify track, cover and controls](docs/screenshots/clock-now-playing.png) | ![Night mode in dim red](docs/screenshots/clock-night-mode.png) |

## The problem

I had a Motorola Moto G5 Plus in a drawer that was no longer fit for daily use: it is stuck on Android 8.1, has had no
security updates since February 2019, and its battery is worn out by years of use. As a personal phone it was neither
practical nor safe, but plugged in on a desk, with no accounts and used only for music, none of that matters much.

That is why the project is meant **for old phones**: **no Google account is signed in and Google Play is not used** (a
phone without patches should not hold personal accounts), and apps come from F-Droid or from Aurora Store in anonymous
mode. On a modern phone, with updates and its own stores, none of this makes sense.

The first version used FreeKiosk, an open-source kiosk app. It had bugs on Android 8.1 that could leave the phone
unlocked after a boot, and changing the clock meant typing a whole HTML page over `adb`. So I replaced it with an app of
my own.

## What it does

A native Android app in Kotlin that acts as the home screen and, as **device owner**, locks the phone to the chosen apps,
with no root and no system changes.

- **Configurable home screen:** up to 5 apps, one large on top and the rest in a grid, each with its icon and any name
  ("Servidor" instead of "Tempus"). They are reordered with ↑ ↓. Only those apps can open; Back and Home always return to
  the home screen.
- **Clock screensaver:** after a while without touches, also inside other apps and on battery, a clock appears with the
  date and battery level, drifting a little every minute.
- **Now playing:** title, artist, cover art and progress of the active app, with previous / pause / next. For the radio
  it shows the frequency, the RDS name and the station logo.
- **Video-player gestures:** swiping on the left half changes the brightness, on the right half the volume.
- **Install, update and uninstall apps** without leaving the lock, through the chosen store (Aurora Store, F-Droid or
  Droid-ify; Google Play is not offered). If none is installed, the app leads to Aurora Store's official page.
- **Setup without a computer:** a screen listing every permission or setting needed, its state (✓) and a button that
  opens the Android screen that turns it on.
- **Station logos from the phone**, picked with Android's image picker.
- **Night mode** in dim red within a schedule and a **power menu** icon.

| Home screen apps | Setup |
|---|---|
| ![App list with up, down, name and remove, and the store button](docs/screenshots/apps-screen.png) | ![Setup steps marked with ✓](docs/screenshots/setup-assistant.png) |

| Volume while swiping | Power menu |
|---|---|
| ![Volume level shown while swiping](docs/screenshots/clock-volume-swipe.png) | ![Android power menu opened from the app](docs/screenshots/power-menu.png) |

| Station logos |
|---|
| ![Station logo list with change and remove](docs/screenshots/station-logos.png) |

## Engineering highlights

- **A screensaver that works on battery:** Android's `DreamService` only runs while charging on this phone
  (`config_dreamsEnabledOnBatteryConfig=false`), and an app cannot see touches in other apps. A 1×1 overlay with
  `FLAG_WATCH_OUTSIDE_TOUCH` is told about every touch (without coordinates) and resets the timer; the clock is another
  overlay window, so closing it reveals the app underneath.
- **Maintenance without leaving the lock:** to open an Android screen (a permission, the browser, the image picker) the
  first version stopped lock task, and on Android 8.1 that left the phone on a black screen. Now the app never stops the
  lock: it adds only the needed app to the allow list and removes it as soon as one of its own screens shows again.
- **Installing while locked:** lock task also blocked Android's install confirmation, so nothing could be installed. The
  app finds out on the phone which package shows that confirmation (it varies by maker) and always allows it, together
  with the chosen store. As device owner, it uninstalls with no system dialog.
- **The lock survives updates:** installing a new version ends lock task; a `MY_PACKAGE_REPLACED` receiver reopens the
  home screen, which locks again.
- **Radio logos without an API:** the stock radio only publishes the frequency and the RDS name, so the logo is looked up
  by frequency (`93.9.png`) in the app's folder; frequencies are accepted with a comma or a dot and normalised.
- **Respects the safe volume limit:** above the recommended headphone level Android silently caps `setStreamVolume`; the
  app shows the real level and, when the finger lifts, the system's own warning.
- **Power off without system permissions:** no app can shut the phone down, not even a device owner; a minimal
  accessibility service only opens Android's power menu.
- **Logic kept away from Android:** the app selection (order, limit, storage), the stores, the uninstall rules, clock
  formatting, the night schedule, gestures and frequencies are plain classes with 54 JUnit tests.

## How I built it: AI agents with a method

As with [MyCarDisplay](https://github.com/santi-cast/mycardisplay), I built it with AI agents on **Pi** +
[**Gentle Shell**](https://github.com/Gentleman-Programming/gentle-shell) and
[**Gentle AI**](https://github.com/Gentleman-Programming/gentle-ai). The agents execute; I decide the scope, approve each
step and test it on the real phone.

| Practice | What it meant in this project |
|---|---|
| **Organic Driven Development (ODD)** | A document with the specs and tasks for each feature, and small commits, each with its tests and docs. |
| **Test-first** | Every logic rule started with a failing test (RED) before it was implemented. |
| **Receipt-Driven Development (RDD)** | The two riskiest changes (device owner and lock) went through an independent review, approved. |
| **On-device testing** | Every feature was checked on the phone over `adb`: lock state, screenshots, media sessions and logs. That is how bugs the tests could not see turned up, like the black screen when stopping the lock. |
| **Persistent memory** | [Engram](https://github.com/Gentleman-Programming/engram) keeps decisions and context across sessions. |

## Try it

Requirements: an Android 8.x phone and, for kiosk mode, **no accounts** on it.

**From the phone**, with no computer: download the APK from the [releases page](https://github.com/santi-cast/deskmusicplayer/releases),
choose the app as the home screen and complete **🔧 → Configuración inicial**. **Kiosk mode** is turned on after a factory
reset by scanning the [QR code in the guide](docs/setup.en.md#kiosk-mode-with-a-qr-code-no-computer) (not yet tested on a
real phone) or with a computer. Building it needs JDK 17+ and the Android SDK:

```bash
./gradlew assembleDebug testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell dpm set-device-owner com.reproductor.kiosk/.AdminReceiver
```

Both paths, the recommended settings and how to undo kiosk mode are in the [setup guide](docs/setup.en.md).

## Known limitations

- The screensaver's touch detection relies on Android 8.x behaviour that newer versions restrict; Android 12+ would need
  another approach.
- The QR code to turn on kiosk mode without a computer is generated and checked (link and signature), but **it has not yet
  been tested by resetting a real phone**; the computer (`adb`) path is tested.
- The FM radio is optional: many phones have no tuner. The station name, logos and favourites scan target Motorola's
  radio app.
- Google Play is not offered as a store, on purpose: the project targets a phone with no Google account.
- No station logos are included: they belong to their owners and everyone adds their own.
- Battery life unplugged has not been measured; it is meant to stay plugged in.
- The interface is in Spanish.

## Documentation

| Document | Contents |
|---|---|
| [`docs/setup.en.md`](docs/setup.en.md) | Setup from the phone or with a computer, kiosk mode, stores and station logos |
| [`docs/history.en.md`](docs/history.en.md) | Project history: from a Galaxy S3 to an app of its own |

## Repository layout

```text
deskmusicplayer/
├── app/src/main/java/com/reproductor/kiosk/
│   ├── MainActivity.kt         # home screen (launcher) built from the selection
│   ├── AppSelection.kt         # chosen apps: order, names, limit and lock allow list
│   ├── AppsActivity.kt         # app settings: add, reorder, rename, store, uninstall
│   ├── Stores.kt               # supported stores (no Google Play)
│   ├── Uninstaller.kt          # uninstalling as device owner
│   ├── SetupActivity.kt        # setup without a computer
│   ├── Kiosk.kt                # device-owner policies, lock and temporary permissions
│   ├── ScreensaverService.kt   # idle detection with the touch-watching overlay
│   ├── ClockOverlay.kt         # clock, now playing, gestures, night mode
│   ├── NowPlaying.kt           # active media session
│   ├── StationLogos.kt         # FM logos by frequency
│   ├── LogosActivity.kt        # station logos from the phone
│   ├── PowerMenuService.kt     # system power menu
│   └── SettingsActivity.kt     # settings
├── app/src/test/               # JUnit tests
└── docs/                       # setup, history and screenshots
```

## Where it came from

It started as a setup with existing apps to make use of an old phone, became an app of its own when those apps fell
short, and then grew so it can be set up on any old phone without a computer. The [full history](docs/history.en.md)
covers every decision: why this phone and not the Galaxy S3, why no LineageOS, the debloat, FreeKiosk's problems and what
was added afterwards.

## License

This project is released under the MIT license. See [LICENSE](./LICENSE) for the full text. Spotify, Tempus, Aurora Store
and the station names belong to their owners.

## Author

**Santiago Castillo** · Multiplatform Application Development (DAM) · Madrid
[GitHub](https://github.com/santi-cast)
