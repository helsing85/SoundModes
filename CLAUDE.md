# Sound Modes – project notes

Android app with a Quick Settings tile and a Glance home screen widget that cycle the ringer mode
Normal → Vibrate → Silent. Silent is implemented as an `AutomaticZenRule` (DND "alarms only"),
Normal/Vibrate via `AudioManager.setRingerMode()`. Main device: Pixel 8 on Android 17.

## Architecture (key files in `app/src/main/java/com/helsing/soundmodes/`)
| File | Role |
|---|---|
| `SoundModesApp.kt` | `Application`: app-wide `RINGER_MODE_CHANGED` receiver → widget refresh (`onlyIfChanged`), widget refresh on process start, publishes the generated widget preview once per versionCode |
| `SoundModesManager.kt` | All sound logic. `nextRingerMode()` (pure, unit tested), zen rule create/find, `suspend changeSoundMode()` (waits for Silent exit, verified ringer set), icons/labels, prefs flags (`tile_added`, `widget_preview_version`) |
| `SoundModesService.kt` | `TileService`. **Not** an active tile: refreshes in `onStartListening()`, listens for `RINGER_MODE_CHANGED` while listening. Click → starts `SoundChangeForegroundService`; fallback → `SoundChangeActivity` via `startActivityAndCollapse` |
| `SoundChangeForegroundService.kt` | Short-lived `specialUse` FGS. Calls `startForeground()` **before** changing the mode, refreshes widgets, stops. `start()` returns false on `ForegroundServiceStartNotAllowedException` |
| `SoundChangeActivity.kt` | Invisible fallback activity: changes mode in a coroutine started in `onResume()`, finishes when done |
| `widgets/ToggleAllWidgetGlance.kt` | Glance widget (rounded `widgetBackground`) + `providePreview()` + `ToggleModeAction` (routes through the FGS). Ringer mode stored in Glance state (`RINGER_MODE_KEY`) |
| `widgets/ToggleAllWidgetGlanceReceiver.kt` | `refreshAllGlanceWidgets(onlyIfChanged)` (suspend) / `updateAllGlanceWidgets()` (fire and forget) / `publishWidgetPreviews()` |
| `RingerModeReceiver.kt` | `BOOT_COMPLETED` only → refresh widgets using `goAsync()` |
| `MainActivity.kt` | Compose UI: permission card, "Add tile" card (`StatusBarManager.requestAddTileService`, disabled "Tile added" state), Material 3 `TopAppBar`, inverted status bar style |
| Icon resources | `drawable/ic_launcher_foreground.xml` (108 grid, also the monochrome layer), `values/ic_launcher_background.xml` (`ic_launcher_background` #2F3D6E, `ic_launcher_glyph` #DCE3FF), `drawable/app_icon.xml` (same glyph cropped to 28..80, top bar + notification), `ic_launcher-playstore.png` (512 px, not packaged) |

## Build environment
### Current: Ubuntu VM (since Nov 2026)
- JDK: snap Android Studio JBR – `JAVA_HOME=/snap/android-studio/250/jbr` (path contains the snap
  revision, check `ls /snap/android-studio/` after Studio updates). `java` is on PATH.
- adb: `/usr/bin/adb` (on PATH). SDK: `~/Android/Sdk`. `gh` CLI is not installed.
- Emulator: `~/Android/Sdk/emulator/emulator -avd Medium_Phone_API_37.0 -no-snapshot-load -no-audio`
  (API 37 Google Play image, 1080x2400, 420 dpi). Needs ~10 GB free disk (data partition is 10 GB);
  it refuses to start otherwise.
- Python 3 + Pillow available (used for cropping README screenshots, run with `python3 -I`).
- Commands:
  ```bash
  JAVA_HOME=/snap/android-studio/250/jbr ./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug
  ```

### Optional: Windows (previous environment – keep, the owner may move back if the VM is too slow)
- JDK: Android Studio bundled JBR – `C:\Program Files\Android\Android Studio\jbr`
  (`java` is not on PATH, set `JAVA_HOME`). During a Studio update the folder was temporarily
  incomplete and `C:\Program Files\Android\Android Studio1\jbr` had to be used.
- adb: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` (not on PATH). Python is not installed.
- Commands (Git Bash):
  ```bash
  JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug
  ```

### Common
- Stack: Gradle 9.9.1, AGP 9.5.1 (new DSL, built-in Kotlin), Kotlin 2.4.21, Compose BOM 2026.09.00,
  Glance 1.2.0, compileSdk/targetSdk 37, minSdk 35, Java 17.
- APK name comes from `base.archivesName` → `SoundModes_v<versionName>-<buildType>.apk`.
- Release builds: R8 minify + resource shrinking, **signed with the debug key** (owner's choice –
  the app was always installed from debug builds; no release keystore exists).
- Remote: `origin` = GitHub (helsing85/SoundModes) – the only remote to push to; the local Gitea
  mirrors GitHub automatically. On the Ubuntu VM `origin` fetches over HTTPS and pushes over SSH
  (`git remote set-url --push origin git@github.com:helsing85/SoundModes.git`, the SSH key is
  registered; HTTPS has no saved login), so plain `git push` works. All local branches track
  `origin/<branch>`. Tags are lightweight (`vX.Y.Z` on the version bump commit).

## Important knowledge / gotchas
- **Do not delete branches by default** – merged feature branches are kept (local and remote).
  Delete one only when the owner explicitly asks for it.
- **Silent → Normal race** (fixed in 2.0.4): turning the zen rule off is asynchronous and the system
  then restores the ringer mode from before Silent (Vibrate). `changeSoundMode()` is `suspend`, waits
  (max 1 s) for DND off + ringer not silent, then sets the mode and re-applies it if overwritten.
  Callers must keep the FGS/activity alive until it returns. Test: 30+ tile clicks, count 0 → 2.
- **Android 17 background audio hardening**: `setRingerMode()` is *silently ignored* unless the app
  has a visible activity or a non-`shortService` foreground service. For targetSdk 37 the FGS must
  also have while-in-use (WIU) capability. Zen rule changes (Silent) are not affected.
  Docs: https://developer.android.com/about/versions/17/changes/bg-audio
  Test: `adb shell cmd audio set-enable-hardening enable|disable|throw`, logcat tag `AudioHardening`.
- Verified on emulator: a tile click gets a temp allowlist with WIU
  (`Background started FGS: Allowed … tempAllowListReason:<tile onclick …> allowWiu:329 … targetSdkVersion:37`).
- **Do not remove `android.r8.strictFullModeForKeepRules=false`** from `gradle.properties`.
  Without it R8 strips implicitly kept no-arg constructors; Room's rule
  (`-keep class * extends androidx.room.RoomDatabase`) breaks → WorkManager (used by Glance) crashes
  the release app at startup (`Failed to create an instance of androidx.work.impl.WorkDatabase`).
- `proguard-rules.pro` keeps `ActionCallback` constructors – Glance creates `ToggleModeAction`
  by reflection. R8 inlines `onAction` into Glance's receiver, so an "empty" class in dexdump is OK
  as long as `<init>` exists.
- After R8-related changes, always launch the **release** APK (cold start) on the emulator/device.
- Active tiles (`ACTIVE_TILE` meta-data) only refresh via `requestListeningState()`, so they showed
  stale state when the process was dead. `requestListeningState()` is meaningless for non-active tiles.
- Zen rule is found by its condition URI (`zen_rule_condition_uri`), not by the translated name.
- `adb shell cmd statusbar click-tile` is only reliable after `cmd statusbar expand-settings`
  (otherwise SystemUI logs "click on non-listening state").

## Session log (Oct 2026)
1. **Tile fix for Android 17** (`367820a`, committed by owner) – FGS-based ringer change, non-active
   tile with ringer listener, fallback activity, widget routed through FGS, localized FGS strings.
2. **Dependency cleanup** (`afb4e30`) – removed unused `ads-mobile-sdk` beta (dropped INTERNET, AD_ID,
   ADSERVICES, READ_BASIC_PHONE_STATE permissions), removed duplicate material3/ui-tooling,
   single Glance version, library bumps, Gradle wrapper 9.8.2, compileSdk 37.
3. **AGP 9 migration** (`68dd0cd`) – AGP 9.4.1, Kotlin 2.4.21, built-in Kotlin, new DSL,
   `archivesName`, removed AGP 9 compat flags, Java 17, R8 enabled, ActionCallback keep rule.
4. **Code modernization** (`e3fbf89`) – zen rule by URI + duplicate cleanup, Modes API
   (`TYPE_OTHER`, manual invocation), `nextRingerMode()` + 4 unit tests, Compose fixes
   (`remember`, `LifecycleEventEffect`, `TopAppBar`), "Add tile" card, widget state stores ringer
   mode, `goAsync()` boot receiver, removed `LOCKED_BOOT_COMPLETED`, DayNight theme, Polish typos.
5. **targetSdk 37** (`1fc0613`) – separate commit so it can be reverted easily.
6. Version 2.0.0 (`1b6e149`), README update (`95305ab`).
7. **Release crash fix** (`5c638bd`, owner) – restored `strictFullModeForKeepRules=false`
   (I wrongly removed it in step 3), debug-key signing, version 2.0.2 (tag `v2.0.2`).

## Session log (11 Nov 2026, Ubuntu VM)
Development moved from Windows to an Ubuntu VM. The emulator first refused to start (disk 99 % full,
the AVD needs ~10 GB free); the owner enlarged the disk. Workflow used and accepted by the owner:
one branch per change set, one commit per fix, verify each fix on the emulator (release build),
then merge to `master` (fast-forward), bump the version, lightweight tag, push. Branches are kept.

8. **README rebuild** (`77ec828`, branch `readme_screenshots`) – all screenshots retaken on the
   API 37 emulator (main screen light/dark, wide tile in each mode, widget, picker), Installation
   section, "Silent Mode" in system Modes, build notes. CLAUDE.md: Ubuntu environment (`d7f0cef`).
9. **UI fixes → 2.0.3** (branch `ui_fixes`, tag `v2.0.3` = `13e4b3b`):
   - `4a51ffc` widget follows ringer changes made outside the app (app-level receiver, refresh on
     process start and in tile `onStartListening()`, `onlyIfChanged`)
   - `5b1925c` rounded widget background · `314a134` status bar icon contrast (inverted
     `SystemBarStyle.auto`) · `6b6de84` widget name "Sound mode toggle" · `b64ab33` monochrome icon
   - `854e506` "Add tile" button disabled as "Tile added" (flag in prefs, set by `SoundModesService`
     `onTileAdded/onTileRemoved/onStartListening` and the add-tile request result; toast removed)
   - `d7a6c74` static preview colors = Glance colors (icon `system_accent1_600/200`, background
     `system_accent2_50/800`) · `0801ddd` README widget screenshots
10. **Cleanup** (branch `cleanup`) – `2ed6724` `mipmap-anydpi`, `3b6c662` unused resources,
    `bdd94f4` template tests. `ac0c904` rule "do not delete branches".
11. **Gradle 9.9.1 / AGP 9.5.1** (branch `gradle_update`, `353032c`, `a5f7c54`) – lint clean,
    release cold start + tile/widget verified (R8 setting still required).
12. **Silent → Normal fix → 2.0.4** (branch `fix_silent_to_normal`, `b1c774b`, tag `v2.0.4` =
    `23f12fc`) – found while testing step 11; see "Silent → Normal race" above.
13. **Generated widget preview → 2.0.5** (branch `widget_preview`, `251e841`, tag `v2.0.5` =
    `d16a189`; the tag was first created on `9783846` and then force-moved to the bump commit).
14. Git setup: `origin` push URL set to SSH, all local branches track `origin/<branch>` (`153a2b9`).
15. **New app icon → 2.0.6** (branch `new_icon`, tag `v2.0.6` = `df843b2`) – four concepts were
    compared on an artifact page (https://claude.ai/artifact/PuKa3iqPLHsj6Fvijp2Qh2, private to the
    owner); the owner picked **A "Cycle ring"**: a speaker with one wave inside a circular arrow
    (= one tap moves to the next mode). `e64cfe9` icon, `ce3b673` removed the unused legacy
    `mipmap-*dpi` .webp bitmaps (minSdk 35 always uses `mipmap-anydpi`), `0594bc7` README main
    screenshots. Geometry (108 grid): ring centre 54,54 r 22, from 40° to 320° clockwise from the
    top, stroke 4.4 round caps, arrowhead at the 320° end; speaker at 55,54 scale 0.62 with one
    wave r ≈ 5. The vector path numbers were generated by a throw-away Python script; edit the XML
    directly for small changes. The Play Store PNG was drawn from the same numbers with Pillow
    (arcs drawn as polygons – Pillow's `arc()` breaks thick, small-radius arcs).

### Measured outputs
- Release APK size: 37.0 MB (with ads SDK, no R8) → 26.5 MB (no ads SDK) → ~2.1 MB (R8)
  → 2.7 MB (2.0.3–2.0.6, R8).
- Emulator (API 37, release build, hardening `enable`, app process killed before first tap):
  Vibrate → Silent (zen=3) → Normal → Vibrate; FGS allowed with WIU; no crashes.
- Silent → Normal before the 2.0.4 fix: 1 of 11 ended in Vibrate. After the fix: 32/32 correct
  (20 tile + 12 widget taps), the re-apply fallback was never needed.
- Final state: unit tests 4/4 (`NextRingerModeTest`), lint 0 findings.

### Emulator tips (learned this session)
- `adb exec-out screencap` sometimes returns the previous frame – take one throw-away capture first.
- Widget picker: long-press empty home space (e.g. 840,1380) → find "Widgets" with
  `uiautomator dump` → search "Sound" → expand → `input draganddrop` the preview to the home screen.
- Wide tile: QS edit mode (pencil), select the tile, drag its resize handle to the right.
- Themed icons: Wallpaper & style → Icons → "Minimal" → Apply.
- `am force-stop` puts the widget into a launcher "stopped app" placeholder; the first tap only
  restores it. `am kill` does not kill a process that is still bound by SystemUI/the launcher.

## Unresolved / to do
- [x] Widget tap tested on the emulator (release build, Nov 2026): placed via the launcher widget
      picker (search "Sound", `input draganddrop` to the home screen). Cold start Silent → Normal,
      then Vibrate → Silent, FGS allowed each time, no crash. Note: after `am force-stop` the launcher
      shows a "stopped app" placeholder and the first tap only restores the widget (launcher behaviour).
- [ ] **Not yet tested on the physical Pixel 8** (only emulator). Install the 2.0.6 release APK over
      the installed version and check tile (incl. Silent → Normal several times), widget
      (tap + volume keys), "Tile added" button, picker preview, new icon (also themed). Then README can say
      "tested on Pixel 8".
- [ ] **Debug keystore check** (owner, on Windows) – see Risks; installing over a Windows-built APK
      on the Pixel 8 also answers it (signature error = different keys).
- [ ] Fallback `SoundChangeActivity` path not tested (the FGS never fails to start on the emulator).
- [x] README screenshots regenerated on the emulator (Nov 2026): `main.png`, `main_dark.png`,
      tile `normal/vibrate/silent.png` (wide tile, resized in QS edit mode), `widget_modes.png`,
      `widget_light/dark.png` (picker).
- [x] Light theme status bar contrast – fixed with an inverted `SystemBarStyle.auto` in `MainActivity` (2.0.3).
- [x] Widget background – `widgetBackground` + system corner radius, same colors in the static preview layout (2.0.3).
- [x] Widget name – receiver `android:label="@string/widget_name"` (2.0.3).
- [x] Widget follows external ringer changes (2.0.3): `SoundModesApp` dynamic receiver while the
      process is alive, plus refresh on process start and in the tile's `onStartListening()`
      (`onlyIfChanged = true`). Changes made while the process is dead show up the next time the
      process starts or Quick Settings is opened.
- [x] Monochrome launcher icon layer added (2.0.3).
- [x] `mipmap-anydpi-v26` moved to `mipmap-anydpi`; unused template colors and
      `ic_launcher_background` drawable removed. Lint is clean.
- [x] `versionCode` 18 / 2.0.6 – keep increasing it on every release.
- [x] New app icon "Cycle ring" (2.0.6) – checked on the emulator: home screen, predicted-apps
      slot, top bar in light/dark, themed icons. The notification small icon was not seen on
      screen (the FGS notification is only visible for a moment).
- [x] Generated widget picker preview (2.0.5): `ToggleAllWidgetGlance.providePreview()`, published by
      `publishWidgetPreviews()` from `SoundModesApp` once per versionCode (stored in prefs).
      `setWidgetPreviews()` is rate limited (result 1 = `RATE_LIMITED`, then retried on the next
      process start). Static `previewLayout` stays as the fallback. To test it, `pm clear` the app
      (resets the stored version, re-grant DND with `cmd notification allow_dnd`).
- [x] Template tests removed (`androidTest` dependencies kept for future device tests).

## Risks / thoughts
- Debug-key signing: back up `C:\Users\helsi\.android\debug.keystore`. The Ubuntu VM has its own
  `~/.android/debug.keystore` (created 2026-11-04), SHA-256
  `D6:14:31:B1:8C:D6:AC:AD:2A:8C:12:39:BE:AA:48:7A:D3:B5:17:87:35:A4:5D:C8:D8:63:5B:A7:49:1D:CB:FD`.
  If it was not copied from Windows, APKs built on the VM cannot update apps installed from Windows
  builds (and vice versa) – on Windows run
  `keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -storepass android`, compare, and
  copy one keystore to both machines. Losing it means installed
  apps cannot be updated without uninstalling. Google Play would require a real release keystore
  (and a `specialUse` FGS justification – the manifest `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` text can be reused).
- `strictFullModeForKeepRules=false` may be removed in a future AGP (10?). Then explicit keep rules
  with `<init>()` will be needed for Room/WorkManager and any reflection-created classes.
- `getOrCreateZenRuleID()` deletes duplicate rules; if the deleted duplicate was the active one,
  Silent mode turns off until the next tap (rare: only after a language change while Silent).
- `.idea` was changed by the owner to Java 25 bytecode target (`c6c7e4d`); Gradle builds still use
  `compileOptions` Java 17 – Gradle is authoritative.
- Emulator state at the end of the 11 Nov session: 2.0.6 release installed, hardening `enable`,
  dark theme, default (non-themed) icons – the owner asked to keep themed icons off; switch them on
  only for a check and back off afterwards. Sound Modes widget on the home screen (tap at
  157,1386), a Sound Modes shortcut at the top of the home screen (added by a reinstall), wide
  tile in QS. App data was cleared during the preview test, so the "Tile added" flag is reset
  until Quick Settings is opened, and the generated widget preview may still be the Vibrate test
  preview until the next successful (rate limited) publish.
