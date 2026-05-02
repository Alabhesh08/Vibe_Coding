# Bluetooth HID Keyboard (Android)

Turns an Android phone (API 28+) into a Bluetooth keyboard for a paired laptop,
using the `BluetoothHidDevice` profile. Uses the **system keyboard** for input
— no custom IME.

## Building

### Option A — GitHub Actions (zero local setup)
1. Push this repo to GitHub.
2. The workflow at `.github/workflows/build.yml` builds the APK automatically.
3. Download the `app-debug` artifact from the Actions run.

### Option B — Android Studio
1. `File → Open` this folder.
2. `Build → Build Bundle(s)/APK(s) → Build APK(s)`.

### Option C — Local CLI
You need Java 17 and Android SDK with platform-34 installed (set `ANDROID_HOME`).
```bash
chmod +x gradlew
./gradlew assembleDebug
```
APK ends up at `app/build/outputs/apk/debug/app-debug.apk`.

## Using the app
1. In Android Settings, pair the phone with the laptop normally (Bluetooth
   pairing). Some hosts will refuse a phone as a HID device until the app
   registers — that's fine, do it anyway.
2. Open the app. Grant Bluetooth permissions when prompted.
3. Tap **Register** — phone advertises itself as a HID keyboard.
4. Pick the laptop in the spinner, tap **Connect**.
5. Tap inside the text field. Whatever you type with the system keyboard is
   sent live to the laptop. Special-key buttons send Enter, Backspace, etc.
   Modifier toggles (Ctrl/Alt/Shift/Win) apply to the next keystroke.

## Notes
- Some laptops require re-pairing after `registerApp()` so they re-read the
  HID descriptor. If the host doesn't accept input, unpair on both sides and
  pair again with the app already running and registered.
- `minSdk = 28` because `BluetoothHidDevice` was added in API 28 (Android 9).
- The included `gradle-wrapper.jar` is the official Gradle 8.5 wrapper jar
  taken from the `v8.5.0` tag of `github.com/gradle/gradle`.
