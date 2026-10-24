# How the project evolved

*[Leer en español](history.md)*

The goal never changed: a cheap, always-ready music player for the desk that can only play music, built from a phone
that was already in a drawer. It had been stuck on Android 8.1 for years with no security updates since February 2019,
and its battery was worn out by years of use, so it was no longer a safe or practical everyday phone. As a desk device
with no accounts that only plays music, plugged in, neither is a real problem. Total cost: €0. The way there changed several times.

## 1. Choosing the phone

The first candidate was a **Samsung Galaxy S3**. It stopped at Android 4.3, and Spotify needs Android 7.0, so it would
have needed an unofficial ROM: LineageOS 14.1 no longer has official builds, and LineageOS 20/21 are slow betas on its
1 GB of RAM.

A **Motorola Moto G5 Plus** (XT1685) meets Spotify's minimum with its stock Android 8.1, is 64-bit and has more RAM.
LineageOS was ruled out for it too: only unofficial builds abandoned since 2021, no security gain over the stock
firmware, and unlocking the bootloader wipes the phone. So the phone kept its stock, locked firmware, without root.

## 2. Stripping it down

With USB debugging and `adb`:

- **63 apps removed** for the current user (`pm uninstall --user 0`), which a factory reset can undo: Google and
  Motorola apps, phone, contacts, camera, telemetry, carrier leftovers…
- **Kept on purpose:** Chrome (it is the WebView on Android 8.1), Google Play services, the keyboard, Settings, Bluetooth,
  the equaliser and the **FM radio**.
- Spotify installed from **Aurora Store** in anonymous mode, with F-Droid's signature checked; no Google account on the
  phone.
- **Tempus**, an open-source Subsonic client, for the music on a home server (Substreamer needs Android 10).

## 3. First version: an open-source kiosk app

**FreeKiosk** (open source, its SHA-256 checked against the release) was made device owner and configured over `adb` to
allow only Spotify, Tempus and the radio. A clock screensaver was an HTML page loaded as a `data:` URL, typed into the
app's settings 150 characters at a time with `adb shell input text`, because its WebView could not read local files.

It worked, with known problems:

- After every boot a "this app can't be unpinned" message kept popping up (its loading screen fighting lock task on
  Android 8.1).
- With its "default launcher" option on, two instances started at boot and crashed, leaving the phone **unlocked**
  in 2 of 4 test boots.
- Changing the clock meant retyping the whole page.

## 4. An app of its own

Replacing FreeKiosk with a small Kotlin app solved all three at once: being the launcher and the device owner itself,
there is no loading screen to fight and nothing to race at boot.

Then the features grew, each one tested on the phone:

1. **Clock screensaver that also works on battery.** Android's `DreamService` only runs while charging on this phone,
   so the app watches touches with a 1×1 overlay and shows its own clock window.
2. **Now playing** from the media session, with working controls.
3. **Night mode** in dim red.
4. **Re-locking after updates**, found when an `adb install -r` dropped the phone to the stock launcher.
5. **Settings icon instead of a PIN**, since the phone never leaves home.
6. **FM radio:** station name from RDS, the radio's own "favourites only" mode for previous/next, and station logos
   looked up by frequency.
7. **Power menu and video-player gestures** for brightness and volume.

## 5. From my phone to any old phone

With the player working, the next step was letting someone else set it up on their own old phone without knowing
anything about `adb`:

1. **Music and podcasts.** Auxio (MP3s on the phone or a microSD card) and AntennaPod were added, both from F-Droid.
2. **Choosable apps.** The buttons stopped being fixed in the code: up to 5 apps, with their order, icon and any name
   (Tempus shows as "Servidor"). Until they are configured, only the default apps that are installed show up, since not
   every phone has an FM radio.
3. **Install and uninstall without leaving the kiosk.** Testing showed the lock also blocked Android's install
   confirmation, so nothing could be installed. The app now always allows that installer and the chosen store (Aurora
   Store, F-Droid or Droid-ify; not Google Play, consistent with having no Google account), and uninstalls as device owner.
4. **Setup without a computer.** Every permission once granted over `adb` has a step with its state and a button that
   opens the matching Android screen. The first version stopped the lock to open those screens and, on Android 8.1, the
   phone was left on a black screen. The redesign never stops the lock: it allows only the needed screen until the app
   shows again.
5. **Station logos from the phone**, with Android's image picker and the frequency typed with a comma or a dot.

## 6. What is still open

- Kiosk mode can now be turned on without a computer with a QR code after a factory reset (version 1.1.0), but it still has
  to be tested on a real reset phone.

- Because of the worn battery the phone is meant to stay plugged in, through a cable with a switch so it is not charging
  around the clock (without root, charging cannot be limited). Battery life unplugged has not been measured; if the
  back starts to bulge, the battery is swelling and must be replaced.
- The phone gets no security updates (last patch: February 2019). It has no accounts and only runs music apps, but it is
  the reason the cheapest upgrade path is another second-hand Android phone, if Spotify ever raises its minimum version.
