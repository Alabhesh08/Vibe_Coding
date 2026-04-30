package com.phoneboard;

/**
 * HID Keyboard Report Descriptor and USB HID keycodes.
 *
 * This descriptor tells the host (laptop) that we are a standard
 * 101-key keyboard. The report format is 8 bytes:
 *   Byte 0: Modifier keys bitmask
 *   Byte 1: Reserved (always 0)
 *   Bytes 2-7: Up to 6 simultaneous key codes (rollover)
 */
public class HidKeyboard {

    // ── Standard HID Keyboard Report Descriptor ──────────────────────────────
    public static final byte[] DESCRIPTOR = {
        (byte)0x05, (byte)0x01,  // Usage Page (Generic Desktop)
        (byte)0x09, (byte)0x06,  // Usage (Keyboard)
        (byte)0xA1, (byte)0x01,  // Collection (Application)

        // Modifier keys (Shift, Ctrl, Alt, GUI) — 8 bits → 1 byte
        (byte)0x05, (byte)0x07,  //   Usage Page (Key Codes)
        (byte)0x19, (byte)0xE0,  //   Usage Minimum (224) — Left Control
        (byte)0x29, (byte)0xE7,  //   Usage Maximum (231) — Right GUI
        (byte)0x15, (byte)0x00,  //   Logical Minimum (0)
        (byte)0x25, (byte)0x01,  //   Logical Maximum (1)
        (byte)0x75, (byte)0x01,  //   Report Size (1)
        (byte)0x95, (byte)0x08,  //   Report Count (8)
        (byte)0x81, (byte)0x02,  //   Input (Data, Variable, Absolute)

        // Reserved byte
        (byte)0x95, (byte)0x01,  //   Report Count (1)
        (byte)0x75, (byte)0x08,  //   Report Size (8)
        (byte)0x81, (byte)0x01,  //   Input (Constant) — reserved

        // LED output (Caps Lock, Num Lock etc) — we don't use but must declare
        (byte)0x95, (byte)0x05,  //   Report Count (5)
        (byte)0x75, (byte)0x01,  //   Report Size (1)
        (byte)0x05, (byte)0x08,  //   Usage Page (LEDs)
        (byte)0x19, (byte)0x01,  //   Usage Minimum (1)
        (byte)0x29, (byte)0x05,  //   Usage Maximum (5)
        (byte)0x91, (byte)0x02,  //   Output (Data, Variable, Absolute)
        (byte)0x95, (byte)0x01,  //   Report Count (1)
        (byte)0x75, (byte)0x03,  //   Report Size (3)
        (byte)0x91, (byte)0x01,  //   Output (Constant)

        // Key array — 6 keys simultaneously
        (byte)0x95, (byte)0x06,  //   Report Count (6)
        (byte)0x75, (byte)0x08,  //   Report Size (8)
        (byte)0x15, (byte)0x00,  //   Logical Minimum (0)
        (byte)0x25, (byte)0x65,  //   Logical Maximum (101)
        (byte)0x05, (byte)0x07,  //   Usage Page (Key Codes)
        (byte)0x19, (byte)0x00,  //   Usage Minimum (0)
        (byte)0x29, (byte)0x65,  //   Usage Maximum (101)
        (byte)0x81, (byte)0x00,  //   Input (Data, Array)

        (byte)0xC0               // End Collection
    };

    // ── Modifier bitmasks ─────────────────────────────────────────────────────
    public static final byte MOD_NONE       = 0x00;
    public static final byte MOD_LCTRL      = 0x01;
    public static final byte MOD_LSHIFT     = 0x02;
    public static final byte MOD_LALT       = 0x04;
    public static final byte MOD_LGUI       = 0x08;  // Windows/Cmd key
    public static final byte MOD_RCTRL      = 0x10;
    public static final byte MOD_RSHIFT     = 0x20;
    public static final byte MOD_RALT       = 0x40;
    public static final byte MOD_RGUI       = (byte)0x80;

    // ── HID keycodes ─────────────────────────────────────────────────────────
    public static final byte KEY_NONE       = 0x00;
    public static final byte KEY_A          = 0x04;
    public static final byte KEY_B          = 0x05;
    public static final byte KEY_C          = 0x06;
    public static final byte KEY_D          = 0x07;
    public static final byte KEY_E          = 0x08;
    public static final byte KEY_F          = 0x09;
    public static final byte KEY_G          = 0x0A;
    public static final byte KEY_H          = 0x0B;
    public static final byte KEY_I          = 0x0C;
    public static final byte KEY_J          = 0x0D;
    public static final byte KEY_K          = 0x0E;
    public static final byte KEY_L          = 0x0F;
    public static final byte KEY_M          = 0x10;
    public static final byte KEY_N          = 0x11;
    public static final byte KEY_O          = 0x12;
    public static final byte KEY_P          = 0x13;
    public static final byte KEY_Q          = 0x14;
    public static final byte KEY_R          = 0x15;
    public static final byte KEY_S          = 0x16;
    public static final byte KEY_T          = 0x17;
    public static final byte KEY_U          = 0x18;
    public static final byte KEY_V          = 0x19;
    public static final byte KEY_W          = 0x1A;
    public static final byte KEY_X          = 0x1B;
    public static final byte KEY_Y          = 0x1C;
    public static final byte KEY_Z          = 0x1D;
    public static final byte KEY_1          = 0x1E;
    public static final byte KEY_2          = 0x1F;
    public static final byte KEY_3          = 0x20;
    public static final byte KEY_4          = 0x21;
    public static final byte KEY_5          = 0x22;
    public static final byte KEY_6          = 0x23;
    public static final byte KEY_7          = 0x24;
    public static final byte KEY_8          = 0x25;
    public static final byte KEY_9          = 0x26;
    public static final byte KEY_0          = 0x27;
    public static final byte KEY_ENTER      = 0x28;
    public static final byte KEY_ESCAPE     = 0x29;
    public static final byte KEY_BACKSPACE  = 0x2A;
    public static final byte KEY_TAB        = 0x2B;
    public static final byte KEY_SPACE      = 0x2C;
    public static final byte KEY_MINUS      = 0x2D;  // - _
    public static final byte KEY_EQUAL      = 0x2E;  // = +
    public static final byte KEY_LBRACKET   = 0x2F;  // [ {
    public static final byte KEY_RBRACKET   = 0x30;  // ] }
    public static final byte KEY_BACKSLASH  = 0x31;  // \ |
    public static final byte KEY_SEMICOLON  = 0x33;  // ; :
    public static final byte KEY_QUOTE      = 0x34;  // ' "
    public static final byte KEY_GRAVE      = 0x35;  // ` ~
    public static final byte KEY_COMMA      = 0x36;  // , <
    public static final byte KEY_PERIOD     = 0x37;  // . >
    public static final byte KEY_SLASH      = 0x38;  // / ?
    public static final byte KEY_CAPS       = 0x39;
    public static final byte KEY_F1         = 0x3A;
    public static final byte KEY_F2         = 0x3B;
    public static final byte KEY_F3         = 0x3C;
    public static final byte KEY_F4         = 0x3D;
    public static final byte KEY_F5         = 0x3E;
    public static final byte KEY_F6         = 0x3F;
    public static final byte KEY_F7         = 0x40;
    public static final byte KEY_F8         = 0x41;
    public static final byte KEY_F9         = 0x42;
    public static final byte KEY_F10        = 0x43;
    public static final byte KEY_F11        = 0x44;
    public static final byte KEY_F12        = 0x45;
    public static final byte KEY_INSERT     = 0x49;
    public static final byte KEY_HOME       = 0x4A;
    public static final byte KEY_PAGEUP     = 0x4B;
    public static final byte KEY_DELETE     = 0x4C;
    public static final byte KEY_END        = 0x4D;
    public static final byte KEY_PAGEDOWN   = 0x4E;
    public static final byte KEY_RIGHT      = 0x4F;
    public static final byte KEY_LEFT       = 0x50;
    public static final byte KEY_DOWN       = 0x51;
    public static final byte KEY_UP         = 0x52;

    // ── Empty (key-release) report ────────────────────────────────────────────
    public static final byte[] REPORT_EMPTY = new byte[8];

    /**
     * Build an 8-byte HID report for a single character.
     * Returns null if the character is not mappable.
     */
    public static byte[] charToReport(char c) {
        byte keyCode = KEY_NONE;
        byte modifier = MOD_NONE;

        switch (c) {
            // ── Lower case letters ────────────────────────────────────────────
            case 'a': keyCode = KEY_A; break;
            case 'b': keyCode = KEY_B; break;
            case 'c': keyCode = KEY_C; break;
            case 'd': keyCode = KEY_D; break;
            case 'e': keyCode = KEY_E; break;
            case 'f': keyCode = KEY_F; break;
            case 'g': keyCode = KEY_G; break;
            case 'h': keyCode = KEY_H; break;
            case 'i': keyCode = KEY_I; break;
            case 'j': keyCode = KEY_J; break;
            case 'k': keyCode = KEY_K; break;
            case 'l': keyCode = KEY_L; break;
            case 'm': keyCode = KEY_M; break;
            case 'n': keyCode = KEY_N; break;
            case 'o': keyCode = KEY_O; break;
            case 'p': keyCode = KEY_P; break;
            case 'q': keyCode = KEY_Q; break;
            case 'r': keyCode = KEY_R; break;
            case 's': keyCode = KEY_S; break;
            case 't': keyCode = KEY_T; break;
            case 'u': keyCode = KEY_U; break;
            case 'v': keyCode = KEY_V; break;
            case 'w': keyCode = KEY_W; break;
            case 'x': keyCode = KEY_X; break;
            case 'y': keyCode = KEY_Y; break;
            case 'z': keyCode = KEY_Z; break;
            // ── Upper case letters ────────────────────────────────────────────
            case 'A': keyCode = KEY_A; modifier = MOD_LSHIFT; break;
            case 'B': keyCode = KEY_B; modifier = MOD_LSHIFT; break;
            case 'C': keyCode = KEY_C; modifier = MOD_LSHIFT; break;
            case 'D': keyCode = KEY_D; modifier = MOD_LSHIFT; break;
            case 'E': keyCode = KEY_E; modifier = MOD_LSHIFT; break;
            case 'F': keyCode = KEY_F; modifier = MOD_LSHIFT; break;
            case 'G': keyCode = KEY_G; modifier = MOD_LSHIFT; break;
            case 'H': keyCode = KEY_H; modifier = MOD_LSHIFT; break;
            case 'I': keyCode = KEY_I; modifier = MOD_LSHIFT; break;
            case 'J': keyCode = KEY_J; modifier = MOD_LSHIFT; break;
            case 'K': keyCode = KEY_K; modifier = MOD_LSHIFT; break;
            case 'L': keyCode = KEY_L; modifier = MOD_LSHIFT; break;
            case 'M': keyCode = KEY_M; modifier = MOD_LSHIFT; break;
            case 'N': keyCode = KEY_N; modifier = MOD_LSHIFT; break;
            case 'O': keyCode = KEY_O; modifier = MOD_LSHIFT; break;
            case 'P': keyCode = KEY_P; modifier = MOD_LSHIFT; break;
            case 'Q': keyCode = KEY_Q; modifier = MOD_LSHIFT; break;
            case 'R': keyCode = KEY_R; modifier = MOD_LSHIFT; break;
            case 'S': keyCode = KEY_S; modifier = MOD_LSHIFT; break;
            case 'T': keyCode = KEY_T; modifier = MOD_LSHIFT; break;
            case 'U': keyCode = KEY_U; modifier = MOD_LSHIFT; break;
            case 'V': keyCode = KEY_V; modifier = MOD_LSHIFT; break;
            case 'W': keyCode = KEY_W; modifier = MOD_LSHIFT; break;
            case 'X': keyCode = KEY_X; modifier = MOD_LSHIFT; break;
            case 'Y': keyCode = KEY_Y; modifier = MOD_LSHIFT; break;
            case 'Z': keyCode = KEY_Z; modifier = MOD_LSHIFT; break;
            // ── Numbers ───────────────────────────────────────────────────────
            case '1': keyCode = KEY_1; break;
            case '2': keyCode = KEY_2; break;
            case '3': keyCode = KEY_3; break;
            case '4': keyCode = KEY_4; break;
            case '5': keyCode = KEY_5; break;
            case '6': keyCode = KEY_6; break;
            case '7': keyCode = KEY_7; break;
            case '8': keyCode = KEY_8; break;
            case '9': keyCode = KEY_9; break;
            case '0': keyCode = KEY_0; break;
            // ── Shifted numbers (symbols) ─────────────────────────────────────
            case '!': keyCode = KEY_1; modifier = MOD_LSHIFT; break;
            case '@': keyCode = KEY_2; modifier = MOD_LSHIFT; break;
            case '#': keyCode = KEY_3; modifier = MOD_LSHIFT; break;
            case '$': keyCode = KEY_4; modifier = MOD_LSHIFT; break;
            case '%': keyCode = KEY_5; modifier = MOD_LSHIFT; break;
            case '^': keyCode = KEY_6; modifier = MOD_LSHIFT; break;
            case '&': keyCode = KEY_7; modifier = MOD_LSHIFT; break;
            case '*': keyCode = KEY_8; modifier = MOD_LSHIFT; break;
            case '(': keyCode = KEY_9; modifier = MOD_LSHIFT; break;
            case ')': keyCode = KEY_0; modifier = MOD_LSHIFT; break;
            // ── Punctuation ───────────────────────────────────────────────────
            case ' ':  keyCode = KEY_SPACE;    break;
            case '\n': keyCode = KEY_ENTER;    break;
            case '\t': keyCode = KEY_TAB;      break;
            case '-':  keyCode = KEY_MINUS;    break;
            case '_':  keyCode = KEY_MINUS;    modifier = MOD_LSHIFT; break;
            case '=':  keyCode = KEY_EQUAL;    break;
            case '+':  keyCode = KEY_EQUAL;    modifier = MOD_LSHIFT; break;
            case '[':  keyCode = KEY_LBRACKET; break;
            case '{':  keyCode = KEY_LBRACKET; modifier = MOD_LSHIFT; break;
            case ']':  keyCode = KEY_RBRACKET; break;
            case '}':  keyCode = KEY_RBRACKET; modifier = MOD_LSHIFT; break;
            case '\\': keyCode = KEY_BACKSLASH; break;
            case '|':  keyCode = KEY_BACKSLASH; modifier = MOD_LSHIFT; break;
            case ';':  keyCode = KEY_SEMICOLON; break;
            case ':':  keyCode = KEY_SEMICOLON; modifier = MOD_LSHIFT; break;
            case '\'': keyCode = KEY_QUOTE;    break;
            case '"':  keyCode = KEY_QUOTE;    modifier = MOD_LSHIFT; break;
            case '`':  keyCode = KEY_GRAVE;    break;
            case '~':  keyCode = KEY_GRAVE;    modifier = MOD_LSHIFT; break;
            case ',':  keyCode = KEY_COMMA;    break;
            case '<':  keyCode = KEY_COMMA;    modifier = MOD_LSHIFT; break;
            case '.':  keyCode = KEY_PERIOD;   break;
            case '>':  keyCode = KEY_PERIOD;   modifier = MOD_LSHIFT; break;
            case '/':  keyCode = KEY_SLASH;    break;
            case '?':  keyCode = KEY_SLASH;    modifier = MOD_LSHIFT; break;
            default:
                return null; // unmappable character
        }

        byte[] report = new byte[8];
        report[0] = modifier;
        report[1] = 0; // reserved
        report[2] = keyCode;
        // bytes 3-7 stay 0 (no additional simultaneous keys)
        return report;
    }

    /**
     * Build a report for a special key with optional modifiers.
     */
    public static byte[] specialKeyReport(byte keyCode, byte modifier) {
        byte[] report = new byte[8];
        report[0] = modifier;
        report[1] = 0;
        report[2] = keyCode;
        return report;
    }
}
