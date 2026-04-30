# PhoneBoard — Build & Install Guide
## Turn your Android phone into a Bluetooth keyboard for any laptop

---

## How It Works

PhoneBoard registers your phone as a **Bluetooth HID (Human Interface Device) Keyboard**
— the exact same protocol that real wireless keyboards use. Your laptop needs **zero drivers
or apps**. You pair once, then your phone's keyboard types directly on your laptop.

**Architecture:**
```
[Your Phone]                    [Laptop]
  - Opens PhoneBoard app          - Sees a Bluetooth keyboard
  - Your default keyboard shows   - Receives keystrokes natively
  - Special keys strip (Fn/Ctrl)  - No app, no driver needed
  - Keystrokes → BT HID →        → Text appears in whatever is focused
```

---

## Requirements

| Item | Requirement |
|------|-------------|
| Android version | 9.0 (Pie) or newer |
| Bluetooth | Classic BT (not BLE-only) — all modern phones have this |
| Laptop OS | Windows 10/11, macOS 10.15+, or Linux (any with BT stack) |
| Laptop app | **None needed** |

---

## Part 1 — Build the APK

### Step 1: Install prerequisites

**On Windows:**
```
1. Install Android Studio: https://developer.android.com/studio
   (This installs Java + Android SDK automatically)

2. Or manually:
   - Java JDK 17+: https://adoptium.net/
   - Android SDK command-line tools: https://developer.android.com/studio#command-tools
```

**On Mac:**
```bash
brew install --cask android-studio
# Or:
brew install openjdk@17
```

**On Linux (Ubuntu/Debian):**
```bash
sudo apt install openjdk-17-jdk
# Then download Android command-line tools from:
# https://developer.android.com/studio#command-tools
```

---

### Step 2: Set up Android SDK

```bash
# After installing Android Studio, SDK is at:
# Windows: C:\Users\<you>\AppData\Local\Android\Sdk
# Mac:     ~/Library/Android/sdk
# Linux:   ~/Android/Sdk

# Set environment variable (add to ~/.bashrc or ~/.zshrc):
export ANDROID_HOME=~/Android/Sdk          # Linux/Mac
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# Install required SDK components:
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

---

### Step 3: Build the APK

```bash
# Navigate to the project folder (where this guide is)
cd PhoneBoard

# Make gradlew executable (Mac/Linux only):
chmod +x gradlew

# Build debug APK (fastest, no signing required):
./gradlew assembleDebug

# Windows:
gradlew.bat assembleDebug

# The APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

**That's it!** The build takes 2–5 minutes on first run (downloads dependencies).

---

### Step 4 (Optional): Build a release APK

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk

# Sign it (required to install on some devices):
# Generate a key (one time):
keytool -genkeypair -v -keystore phoneboard.jks -keyalg RSA \
        -keysize 2048 -validity 10000 -alias phoneboard

# Sign the APK:
apksigner sign --ks phoneboard.jks --out phoneboard-signed.apk \
               app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## Part 2 — Install on Your Phone

### Enable "Install from Unknown Sources"

**Android 8+:**
```
Settings → Apps → Special app access → Install unknown apps
→ Find your file manager or Chrome → Allow
```

**Transfer the APK to your phone:**
- Email it to yourself, or
- Copy via USB cable, or
- Upload to Google Drive and download on phone

**Then tap the APK file** → Install.

---

### Alternative: Install via ADB (USB cable, fastest)

```bash
# Enable Developer Options on phone:
# Settings → About Phone → tap "Build Number" 7 times

# Enable USB Debugging:
# Settings → Developer Options → USB Debugging → ON

# Connect phone via USB, then:
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Part 3 — Pair & Use

### First Time Setup (do this once)

**Step 1: Make your laptop discoverable**
- **Windows:** Settings → Bluetooth → turn ON → "Add a device"
- **Mac:** System Settings → Bluetooth → make sure it's ON
- **Linux:** `bluetoothctl` → `discoverable on`

**Step 2: Pair from your phone**
```
Normal Bluetooth pairing (not through PhoneBoard):
Settings → Bluetooth → Search → tap your laptop name → Pair
Confirm the pairing code on both devices
```

**Step 3: Open PhoneBoard app**
- App opens → shows your paired devices
- Tap your laptop name
- The app registers as a HID keyboard (~2 seconds)
- Connection confirmed → keyboard screen opens automatically

**Step 4: Type!**
- Your phone's default keyboard appears
- Type anything — it appears on your laptop in real time
- Use the special keys strip at the top for Fn keys, arrows, Ctrl shortcuts

---

### Daily Use (after first setup)

```
1. Open PhoneBoard
2. Tap your laptop → connects in ~1-2 seconds
3. Type!
```

---

## Special Keys Strip

The top strip (scroll left/right) contains keys not on phone keyboards:

| Key | What it does |
|-----|-------------|
| Tab | Tab / indent |
| Esc | Escape |
| ↑ ↓ ← → | Arrow keys |
| Home / End | Jump to start/end of line |
| PgUp / PgDn | Scroll pages |
| Del | Forward delete |
| Ins | Insert key |
| F1–F12 | All function keys |
| Win | Windows key (opens Start menu) |
| CapsLk | Caps Lock toggle |
| Ctrl+C/V/X/Z/A/S | Copy, paste, cut, undo, select all, save |
| Alt+F4 | Close window |
| Alt+Tab | Switch windows |

---

## Troubleshooting

### "HID not supported" or app crashes on start
- Your phone must be **Android 9.0+**
- Some very cheap phones remove BT HID support — rare but possible
- Check: your phone needs `BluetoothHidDevice` API support

### Laptop doesn't accept typing
- Make sure you tapped a text box on the laptop before typing on phone
- Try disconnecting and reconnecting
- On Windows: sometimes you need to remove the pairing and re-pair

### Connection drops frequently
- Keep phone within 5 metres of laptop
- Avoid WiFi 2.4GHz interference (BT shares the 2.4GHz band)

### Characters appear doubled or wrong
- This is a HID keycode timing issue — very rare
- The 8ms inter-key delay in the app handles this

### Special characters / emoji don't appear
- HID keyboards can only send standard USB keycodes
- Emoji and Unicode outside ASCII won't transmit via HID
- Type emoji on laptop directly, or use Win+. shortcut on Windows

### Pairing fails
- Make sure laptop Bluetooth is ON and in pairing mode
- Forget the device on both sides and pair fresh

---

## How the Real-Time Sync Works (Technical)

The app uses a **diff-based text watcher**:

```
Previous text: "Hello"
New text:      "Hello world"
Diff:          common prefix = "Hello", addition = " world"
Action:        send HID reports for ' ', 'w', 'o', 'r', 'l', 'd'

Previous text: "Hello world"  
New text:      "Hello"
Diff:          common prefix = "Hello", deletions = 6
Action:        send 6 × Backspace HID reports
```

This means **autocorrect, swipe-type, and paste all work correctly** —
the app doesn't care how the text changed, only what changed.

HID reports are sent on a background thread at up to ~125 keys/second,
which is faster than any human typist and introduces effectively zero lag.

---

## No Laptop App Needed

Because we use the **Bluetooth HID profile** (the same one used by every
wireless keyboard ever made), your laptop's OS handles everything natively.
Windows, macOS, and Linux all have built-in HID keyboard support.

---

## Privacy

- All communication is **local Bluetooth only** — no internet, no servers, no cloud
- Nothing you type is logged or stored beyond the text box on screen
- Clearing the text box clears all local text

---

*PhoneBoard — built with Claude*
