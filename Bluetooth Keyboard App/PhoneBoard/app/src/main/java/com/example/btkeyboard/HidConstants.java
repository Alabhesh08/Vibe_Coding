package com.example.btkeyboard;

/**
 * Constants for the Bluetooth HID keyboard implementation:
 * - Standard boot-protocol keyboard HID descriptor
 * - ASCII -> HID Usage code mapping (US QWERTY)
 * - Modifier bit definitions
 */
public final class HidConstants {
    private HidConstants() {}

    // Modifier bits (HID 1.11, Keyboard/Keypad page 0x07)
    public static final byte MOD_LCTRL  = 0x01;
    public static final byte MOD_LSHIFT = 0x02;
    public static final byte MOD_LALT   = 0x04;
    public static final byte MOD_LGUI   = 0x08;

    // Common usage codes
    public static final byte KEY_ENTER     = 0x28;
    public static final byte KEY_ESC       = 0x29;
    public static final byte KEY_BACKSPACE = 0x2A;
    public static final byte KEY_TAB       = 0x2B;
    public static final byte KEY_SPACE     = 0x2C;

    /** Internal flag (NOT part of HID): set when shift is required for the character. */
    public static final int SHIFT_FLAG = 0x100;

    /**
     * Standard boot-protocol keyboard descriptor:
     *   8-byte input report  : modifier, reserved, key1..key6
     *   1-byte output report : LED states (NumLock, CapsLock, ScrollLock, ...)
     */
    public static final byte[] HID_DESCRIPTOR = new byte[] {
            0x05, 0x01,        // Usage Page (Generic Desktop)
            0x09, 0x06,        // Usage (Keyboard)
            (byte) 0xA1, 0x01, // Collection (Application)
            0x05, 0x07,        //   Usage Page (Key Codes)
            0x19, (byte) 0xE0, //   Usage Minimum (224)
            0x29, (byte) 0xE7, //   Usage Maximum (231)
            0x15, 0x00,        //   Logical Minimum (0)
            0x25, 0x01,        //   Logical Maximum (1)
            0x75, 0x01,        //   Report Size (1)
            (byte) 0x95, 0x08, //   Report Count (8)
            (byte) 0x81, 0x02, //   Input (Data, Var, Abs)  -- modifiers
            (byte) 0x95, 0x01, //   Report Count (1)
            0x75, 0x08,        //   Report Size (8)
            (byte) 0x81, 0x01, //   Input (Const)           -- reserved
            (byte) 0x95, 0x05, //   Report Count (5)
            0x75, 0x01,        //   Report Size (1)
            0x05, 0x08,        //   Usage Page (LEDs)
            0x19, 0x01,        //   Usage Minimum (1)
            0x29, 0x05,        //   Usage Maximum (5)
            (byte) 0x91, 0x02, //   Output (Data, Var, Abs) -- LEDs
            (byte) 0x95, 0x01, //   Report Count (1)
            0x75, 0x03,        //   Report Size (3)
            (byte) 0x91, 0x01, //   Output (Const)          -- LED padding
            (byte) 0x95, 0x06, //   Report Count (6)
            0x75, 0x08,        //   Report Size (8)
            0x15, 0x00,        //   Logical Minimum (0)
            0x25, 0x65,        //   Logical Maximum (101)
            0x05, 0x07,        //   Usage Page (Key Codes)
            0x19, 0x00,        //   Usage Minimum (0)
            0x29, 0x65,        //   Usage Maximum (101)
            (byte) 0x81, 0x00, //   Input (Data, Array)     -- 6 keys
            (byte) 0xC0        // End Collection
    };

    /**
     * Convert an ASCII character to an HID usage code (US layout).
     * Returns 0 if the character cannot be mapped.
     * If SHIFT_FLAG is set in the result, the caller must add MOD_LSHIFT to the
     * modifier byte and mask SHIFT_FLAG off the usage code before sending.
     */
    public static int charToHid(char c) {
        if (c >= 'a' && c <= 'z') return 0x04 + (c - 'a');
        if (c >= 'A' && c <= 'Z') return (0x04 + (c - 'A')) | SHIFT_FLAG;
        if (c >= '1' && c <= '9') return 0x1E + (c - '1');
        if (c == '0') return 0x27;

        switch (c) {
            case '\n': return 0x28;          // Enter
            case '\b': return 0x2A;          // Backspace
            case '\t': return 0x2B;          // Tab
            case ' ':  return 0x2C;          // Space
            case '-':  return 0x2D;
            case '=':  return 0x2E;
            case '[':  return 0x2F;
            case ']':  return 0x30;
            case '\\': return 0x31;
            case ';':  return 0x33;
            case '\'': return 0x34;
            case '`':  return 0x35;
            case ',':  return 0x36;
            case '.':  return 0x37;
            case '/':  return 0x38;
            // shifted symbols
            case '!':  return 0x1E | SHIFT_FLAG;
            case '@':  return 0x1F | SHIFT_FLAG;
            case '#':  return 0x20 | SHIFT_FLAG;
            case '$':  return 0x21 | SHIFT_FLAG;
            case '%':  return 0x22 | SHIFT_FLAG;
            case '^':  return 0x23 | SHIFT_FLAG;
            case '&':  return 0x24 | SHIFT_FLAG;
            case '*':  return 0x25 | SHIFT_FLAG;
            case '(':  return 0x26 | SHIFT_FLAG;
            case ')':  return 0x27 | SHIFT_FLAG;
            case '_':  return 0x2D | SHIFT_FLAG;
            case '+':  return 0x2E | SHIFT_FLAG;
            case '{':  return 0x2F | SHIFT_FLAG;
            case '}':  return 0x30 | SHIFT_FLAG;
            case '|':  return 0x31 | SHIFT_FLAG;
            case ':':  return 0x33 | SHIFT_FLAG;
            case '"':  return 0x34 | SHIFT_FLAG;
            case '~':  return 0x35 | SHIFT_FLAG;
            case '<':  return 0x36 | SHIFT_FLAG;
            case '>':  return 0x37 | SHIFT_FLAG;
            case '?':  return 0x38 | SHIFT_FLAG;
        }
        return 0;
    }
}
