package com.phoneboard;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

/**
 * Typing screen.
 *
 * Layout:
 *  ┌─────────────────────────────────────┐
 *  │  Status bar (connected device)      │
 *  ├─────────────────────────────────────┤
 *  │  Special keys strip (horizontal     │
 *  │  scroll): Tab Esc F1…F12 ↑↓←→      │
 *  │  Home End PgUp PgDn Del Ins Win     │
 *  ├─────────────────────────────────────┤
 *  │                                     │
 *  │  EditText (phone's default          │
 *  │  keyboard opens here automatically) │
 *  │                                     │
 *  └─────────────────────────────────────┘
 *
 * Strategy for real-time sync:
 *   We track the previous text. On each TextWatcher change:
 *   - If new text is longer → send the added characters
 *   - If new text is shorter → send backspace(s) for deleted chars
 *   This means copy/paste, autocorrect, swipe-type all work correctly.
 */
public class KeyboardActivity extends AppCompatActivity {

    private HidService  hidService;
    private boolean     serviceBound = false;

    private TextView       tvConnStatus;
    private EditText       etInput;
    private MaterialButton btnClearAll;
    private MaterialButton btnDisconnect;

    private String prevText = "";

    // Worker thread for sending HID reports (keeps UI thread snappy)
    private final Handler uiHandler   = new Handler(Looper.getMainLooper());
    private final Handler workerHandler;

    {
        android.os.HandlerThread ht = new android.os.HandlerThread("HidWorker");
        ht.start();
        workerHandler = new Handler(ht.getLooper());
    }

    // ── Service connection ────────────────────────────────────────────────────
    private final ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            hidService   = ((HidService.LocalBinder) service).getService();
            serviceBound = true;
            updateStatus();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            hidService = null;
        }
    };

    // ── BroadcastReceiver ─────────────────────────────────────────────────────
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(HidService.EXTRA_STATE, -1);
            if (state == HidService.STATE_DISCONNECTED) {
                tvConnStatus.setText("⚠ Disconnected");
                tvConnStatus.setBackgroundColor(0xFFB71C1C);
                Toast.makeText(KeyboardActivity.this,
                        "Disconnected from laptop", Toast.LENGTH_SHORT).show();
            } else if (state == HidService.STATE_CONNECTED) {
                updateStatus();
            }
        }
    };

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyboard);

        tvConnStatus = findViewById(R.id.tv_conn_status);
        etInput      = findViewById(R.id.et_input);
        btnClearAll  = findViewById(R.id.btn_clear_all);
        btnDisconnect= findViewById(R.id.btn_disconnect);

        String deviceName = getIntent().getStringExtra("device_name");
        if (deviceName != null && !deviceName.isEmpty()) {
            tvConnStatus.setText("● Connected to " + deviceName);
        }

        setupSpecialKeys();
        setupTextWatcher();
        setupButtons();

        // Bind to service
        Intent svc = new Intent(this, HidService.class);
        bindService(svc, serviceConn, Context.BIND_AUTO_CREATE);

        // Auto-open keyboard
        etInput.requestFocus();
        uiHandler.postDelayed(() -> {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT);
        }, 300);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(stateReceiver,
                new IntentFilter(HidService.ACTION_CONNECTION_STATE),
                Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(stateReceiver);
    }

    @Override
    protected void onDestroy() {
        if (serviceBound) {
            unbindService(serviceConn);
            serviceBound = false;
        }
        super.onDestroy();
    }

    // ── TextWatcher — core real-time sync logic ───────────────────────────────

    private void setupTextWatcher() {
        etInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String newText = s.toString();
                final String prev = prevText;
                prevText = newText;

                // Diff: find the longest common prefix
                int commonLen = 0;
                int minLen = Math.min(prev.length(), newText.length());
                while (commonLen < minLen && prev.charAt(commonLen) == newText.charAt(commonLen)) {
                    commonLen++;
                }

                // Characters removed from the end of common prefix
                int deletions = prev.length() - commonLen;
                // Characters added after the common prefix
                String additions = newText.substring(commonLen);

                // Send on worker thread to avoid blocking UI
                final int del = deletions;
                final String add = additions;
                workerHandler.post(() -> {
                    if (!serviceBound || hidService == null) return;

                    // Send backspaces for deletions
                    for (int i = 0; i < del; i++) {
                        hidService.sendBackspace();
                        sleepMs(10); // short gap between rapid backspaces
                    }

                    // Send new characters
                    for (int i = 0; i < add.length(); i++) {
                        char c = add.charAt(i);
                        boolean sent = hidService.sendChar(c);
                        if (!sent) {
                            // Character not in HID map (emoji, Unicode etc) — skip
                            // We could show a toast but that'd be too noisy
                        }
                        sleepMs(8); // ~125 chars/sec max — well above typing speed
                    }
                });
            }
        });
    }

    // ── Special keys strip ────────────────────────────────────────────────────

    private void setupSpecialKeys() {
        // Define all special keys: label → (keyCode, modifier)
        SpecialKey[] keys = {
            // Navigation cluster
            new SpecialKey("Tab",    HidKeyboard.KEY_TAB,      HidKeyboard.MOD_NONE),
            new SpecialKey("Esc",    HidKeyboard.KEY_ESCAPE,   HidKeyboard.MOD_NONE),
            new SpecialKey("↑",      HidKeyboard.KEY_UP,       HidKeyboard.MOD_NONE),
            new SpecialKey("↓",      HidKeyboard.KEY_DOWN,     HidKeyboard.MOD_NONE),
            new SpecialKey("←",      HidKeyboard.KEY_LEFT,     HidKeyboard.MOD_NONE),
            new SpecialKey("→",      HidKeyboard.KEY_RIGHT,    HidKeyboard.MOD_NONE),
            new SpecialKey("Home",   HidKeyboard.KEY_HOME,     HidKeyboard.MOD_NONE),
            new SpecialKey("End",    HidKeyboard.KEY_END,      HidKeyboard.MOD_NONE),
            new SpecialKey("PgUp",   HidKeyboard.KEY_PAGEUP,   HidKeyboard.MOD_NONE),
            new SpecialKey("PgDn",   HidKeyboard.KEY_PAGEDOWN, HidKeyboard.MOD_NONE),
            new SpecialKey("Ins",    HidKeyboard.KEY_INSERT,   HidKeyboard.MOD_NONE),
            new SpecialKey("Del",    HidKeyboard.KEY_DELETE,   HidKeyboard.MOD_NONE),
            // Function keys
            new SpecialKey("F1",     HidKeyboard.KEY_F1,  HidKeyboard.MOD_NONE),
            new SpecialKey("F2",     HidKeyboard.KEY_F2,  HidKeyboard.MOD_NONE),
            new SpecialKey("F3",     HidKeyboard.KEY_F3,  HidKeyboard.MOD_NONE),
            new SpecialKey("F4",     HidKeyboard.KEY_F4,  HidKeyboard.MOD_NONE),
            new SpecialKey("F5",     HidKeyboard.KEY_F5,  HidKeyboard.MOD_NONE),
            new SpecialKey("F6",     HidKeyboard.KEY_F6,  HidKeyboard.MOD_NONE),
            new SpecialKey("F7",     HidKeyboard.KEY_F7,  HidKeyboard.MOD_NONE),
            new SpecialKey("F8",     HidKeyboard.KEY_F8,  HidKeyboard.MOD_NONE),
            new SpecialKey("F9",     HidKeyboard.KEY_F9,  HidKeyboard.MOD_NONE),
            new SpecialKey("F10",    HidKeyboard.KEY_F10, HidKeyboard.MOD_NONE),
            new SpecialKey("F11",    HidKeyboard.KEY_F11, HidKeyboard.MOD_NONE),
            new SpecialKey("F12",    HidKeyboard.KEY_F12, HidKeyboard.MOD_NONE),
            // Modifier combos
            new SpecialKey("Win",    HidKeyboard.KEY_NONE, HidKeyboard.MOD_LGUI),
            new SpecialKey("CapsLk", HidKeyboard.KEY_CAPS, HidKeyboard.MOD_NONE),
            // Common shortcuts — Ctrl+C/V/X/Z/A
            new SpecialKey("Ctrl+C", HidKeyboard.KEY_C, HidKeyboard.MOD_LCTRL),
            new SpecialKey("Ctrl+V", HidKeyboard.KEY_V, HidKeyboard.MOD_LCTRL),
            new SpecialKey("Ctrl+X", HidKeyboard.KEY_X, HidKeyboard.MOD_LCTRL),
            new SpecialKey("Ctrl+Z", HidKeyboard.KEY_Z, HidKeyboard.MOD_LCTRL),
            new SpecialKey("Ctrl+A", HidKeyboard.KEY_A, HidKeyboard.MOD_LCTRL),
            new SpecialKey("Ctrl+S", HidKeyboard.KEY_S, HidKeyboard.MOD_LCTRL),
            new SpecialKey("Alt+F4", HidKeyboard.KEY_F4, HidKeyboard.MOD_LALT),
            new SpecialKey("Alt+Tab",HidKeyboard.KEY_TAB,HidKeyboard.MOD_LALT),
        };

        HorizontalScrollView scrollView = findViewById(R.id.special_keys_scroll);
        // The scroll view contains a LinearLayout (R.id.special_keys_container)
        android.widget.LinearLayout container = findViewById(R.id.special_keys_container);

        int dp8  = dpToPx(8);
        int dp6  = dpToPx(6);

        for (SpecialKey sk : keys) {
            Chip chip = new Chip(this);
            chip.setText(sk.label);
            chip.setChipBackgroundColorResource(R.color.chip_bg);
            chip.setTextColor(getResources().getColor(R.color.chip_text, null));
            chip.setTextSize(12f);
            chip.setEnsureMinTouchTargetSize(false);

            android.widget.LinearLayout.LayoutParams lp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp6, 0, 0, 0);
            chip.setLayoutParams(lp);

            final byte kc = sk.keyCode;
            final byte mod = sk.modifier;
            chip.setOnClickListener(v -> {
                if (!serviceBound || hidService == null) return;
                workerHandler.post(() -> hidService.sendSpecialKey(kc, mod));
            });

            container.addView(chip);
        }
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private void setupButtons() {
        btnClearAll.setOnClickListener(v -> {
            etInput.setText("");
            prevText = "";
        });

        btnDisconnect.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Disconnect?")
                .setMessage("Disconnect from laptop keyboard?")
                .setPositiveButton("Disconnect", (d, w) -> {
                    if (serviceBound && hidService != null) hidService.disconnect();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateStatus() {
        if (serviceBound && hidService != null && hidService.isConnected()) {
            tvConnStatus.setText("● Connected");
            tvConnStatus.setBackgroundColor(0xFF1B5E20);
        } else {
            tvConnStatus.setText("⚠ Not connected");
            tvConnStatus.setBackgroundColor(0xFFB71C1C);
        }
    }

    private static void sleepMs(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    private static class SpecialKey {
        final String label;
        final byte   keyCode;
        final byte   modifier;
        SpecialKey(String label, byte keyCode, byte modifier) {
            this.label    = label;
            this.keyCode  = keyCode;
            this.modifier = modifier;
        }
    }
}
