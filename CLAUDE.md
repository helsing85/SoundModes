# Sound Modes – project notes

Android app with a Quick Settings tile and a Glance home screen widget that cycle the ringer mode
Normal → Vibrate → Silent. Silent is implemented as an `AutomaticZenRule` (DND "alarms only"),
Normal/Vibrate via `AudioManager.setRingerMode()`. Main device: Pixel 8 on Android 17.

## Architecture (key files in `app/src/main/java/com/helsing/soundmodes/`)
| File | Role |
|---|---|
| `SoundModesManager.kt` | All sound logic. `nextRingerMode()` (pure, unit tested), zen rule create/find, ringer change, icons/labels |
| `SoundModesService.kt` | `TileService`. **Not** an active tile: refreshes in `onStartListening()`, listens for `RINGER_MODE_CHANGED` while listening. Click → starts `SoundChangeForegroundService`; fallback → `SoundChangeActivity` via `startActivityAndCollapse` |
| `SoundChangeForegroundService.kt` | Short-lived `specialUse` FGS. Calls `startForeground()` **before** changing the mode, refreshes widgets, stops. `start()` returns false on `ForegroundServiceStartNotAllowedException` |
| `SoundChangeActivity.kt` | Invisible fallback activity: changes mode in `onResume()` and finishes |
| `widgets/ToggleAllWidgetGlance.kt` | Glance widget + `ToggleModeAction` (routes through the FGS). Ringer mode stored in Glance state (`RINGER_MODE_KEY`) |
| `widgets/ToggleAllWidgetGlanceReceiver.kt` | `refreshAllGlanceWidgets()` (suspend) / `updateAllGlanceWidgets()` (fire and forget) |
| `RingerModeReceiver.kt` | `BOOT_COMPLETED` only → refresh widgets using `goAsync()` |
| `MainActivity.kt` | Compose UI: permission card, "Add tile" card (`StatusBarManager.requestAddTileService`), Material 3 `TopAppBar` |

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
- Stack: Gradle 9.8.2, AGP 9.4.1 (new DSL, built-in Kotlin), Kotlin 2.4.21, Compose BOM 2026.09.00,
  Glance 1.2.0, compileSdk/targetSdk 37, minSdk 35, Java 17.
- APK name comes from `base.archivesName` → `SoundModes_v<versionName>-<buildType>.apk`.
- Release builds: R8 minify + resource shrinking, **signed with the debug key** (owner's choice –
  the app was always installed from debug builds; no release keystore exists).
- Remote: `origin` = GitHub (helsing85/SoundModes) – the only remote to push to; the local Gitea
  mirrors GitHub automatically. On the Ubuntu VM HTTPS has no saved login – push over SSH
  (`git push git@github.com:helsing85/SoundModes.git master`, the SSH key is registered).

## Important knowledge / gotchas
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

### Measured outputs
- Release APK size: 37.0 MB (with ads SDK, no R8) → 26.5 MB (no ads SDK) → ~2.1 MB (R8).
- Emulator (API 37, release build, hardening `enable`, app process killed before first tap):
  Vibrate → Silent (zen=3) → Normal → Vibrate; FGS allowed with WIU; no crashes.

## Unresolved / to do
- [x] Widget tap tested on the emulator (release build, Nov 2026): placed via the launcher widget
      picker (search "Sound", `input draganddrop` to the home screen). Cold start Silent → Normal,
      then Vibrate → Silent, FGS allowed each time, no crash. Note: after `am force-stop` the launcher
      shows a "stopped app" placeholder and the first tap only restores the widget (launcher behaviour).
- [ ] Tile not yet confirmed on the physical Pixel 8 (only emulator). After confirming, README can say "tested on Pixel 8".
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
- [ ] `mipmap-anydpi-v26` folder is obsolete with minSdk 35; unused template colors and
      `ic_launcher_background` drawable (lint warnings).
- [x] `versionCode` 15 / 2.0.3 – keep increasing it on every release.
- [ ] Glance 1.2 generated widget previews (Android 15+ widget picker) – not implemented.
- [ ] Template tests `ExampleUnitTest` / `ExampleInstrumentedTest` can be removed.
- [ ] Merged feature branches (`fix_tile_android17`, `dependency_cleanup`, `agp9_migration`,
      `code_modernization`) can be deleted.

## Risks / thoughts
- Debug-key signing: back up `C:\Users\helsi\.android\debug.keystore`. The Ubuntu VM has its own
  `~/.android/debug.keystore` (created 2026-11-04, SHA-256 `D6:14:31:B1:…:1D:CB:FD`). If it was not
  copied from Windows, APKs built on the VM cannot update apps installed from Windows builds
  (and vice versa) – compare fingerprints with `keytool -list` and copy one keystore to both. Losing it means installed
  apps cannot be updated without uninstalling. Google Play would require a real release keystore
  (and a `specialUse` FGS justification – the manifest `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` text can be reused).
- `strictFullModeForKeepRules=false` may be removed in a future AGP (10?). Then explicit keep rules
  with `<init>()` will be needed for Room/WorkManager and any reflection-created classes.
- `getOrCreateZenRuleID()` deletes duplicate rules; if the deleted duplicate was the active one,
  Silent mode turns off until the next tap (rare: only after a language change while Silent).
- `.idea` was changed by the owner to Java 25 bytecode target (`c6c7e4d`); Gradle builds still use
  `compileOptions` Java 17 – Gradle is authoritative.
- The emulator was left with `set-enable-hardening enable`.
