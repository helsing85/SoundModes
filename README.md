# Sound Modes 🎵

A lightweight Android utility designed for AOSP and Pixel devices that adds the missing **Quick Settings tile** for switching sound profiles.

On modern Pixel phones, users are forced to use physical volume buttons to change ringer modes. **Sound Modes** solves this by adding a toggle directly to your notification shade to cycle through **Normal, Vibrate, and Mute**.

This project was inspired by the [Sound-Toggle](https://github.com/grmasa/Sound-Toggle) and was rebuilt from the ground up in Kotlin as a fun project to learn modern Android development.

---

![Main activity](img/main.png)

## ✨ Features
- **Quick Settings Tile:** Cycle through Sound, Vibrate, and Mute with one tap. The tile always shows the current mode, even when the mode is changed with the volume buttons.

  ![Normal Mode](img/normal.png) ![Vibrate Mode](img/vibrate.png) ![Silent Mode](img/silent.png)
- **One-tap tile setup:** Add the tile to Quick Settings straight from the app, without editing the Quick Settings panel.
- **Widget:** Toggle between modes directly from your home screen. The resizable widget is built with Jetpack Glance and uses adaptive colors.

  ![Light Mode](img/widget_light.png) ![Dark Mode](img/widget_dark.png)
- **Works without the app running:** The tile and the widget work even when the app is closed, including on **Android 17**, which blocks sound mode changes from background apps.
- **Light and dark theme:** The app follows the system theme and Material You dynamic colors.
- **Lightweight and private:** No ads, no tracking and no internet access. The release APK is about 2 MB.
- **Multilingual:** Full support for **English** and **Polish**.

## 🛠️ Requirements
- **Android 15 (API 35) or newer:** Required for the [AutomaticZenRule.Builder](https://developer.android.com/reference/android/app/AutomaticZenRule.Builder) implementation. Fully supports Android 17.
- **Access Notification Policy:** The app requires "Do Not Disturb" access to switch on Silent mode.

## 🔐 Permissions
| Permission | Why it is needed |
|---|---|
| Notification Policy Access | Turning on Silent mode, which is implemented as a "Do Not Disturb" mode. |
| Foreground service | Since Android 17, apps can change the sound mode only while they are visible or running a foreground service. The app runs a short foreground service for a moment after you tap the tile or the widget. |
| Vibrate | A short vibration when switching to Vibrate mode. |
| Run at startup | Refreshing the widget after the phone restarts. |

## 🏗️ Building
- **Tools:** Android Studio with JDK 17 or newer
- **Stack:** Kotlin 2.4, Jetpack Compose, Jetpack Glance, Android Gradle Plugin 9, compile and target SDK 37
- **Build a release APK:** `./gradlew assembleRelease`. R8 code and resource shrinking is enabled for release builds.

---

## 👨‍💻 Credits & Learning
This app was created as an educational exercise. Special thanks to [grmasa](https://github.com/grmasa) for the original [Sound-Toggle](https://github.com/grmasa/Sound-Toggle) concept.

## 📄 License
This project is licensed under the [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.html) - see the [LICENSE](LICENSE) file for details.
