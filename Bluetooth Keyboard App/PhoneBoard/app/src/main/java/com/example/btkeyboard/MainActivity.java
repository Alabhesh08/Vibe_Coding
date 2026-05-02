package com.example.btkeyboard;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_PERMISSIONS = 1001;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothHidService hidService;

    private TextView statusText;
    private EditText inputField;
    private Spinner deviceSpinner;
    private Button refreshBtn, registerBtn, connectBtn;
    private Button enterBtn, backspaceBtn, tabBtn, escBtn, spaceBtn;
    private Button ctrlBtn, altBtn, shiftBtn, winBtn;

    private final List<BluetoothDevice> pairedDevices = new ArrayList<>();
    private boolean ctrlActive, altActive, shiftActive, winActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText    = findViewById(R.id.statusText);
        inputField    = findViewById(R.id.inputField);
        deviceSpinner = findViewById(R.id.deviceSpinner);
        refreshBtn    = findViewById(R.id.refreshBtn);
        registerBtn   = findViewById(R.id.registerBtn);
        connectBtn    = findViewById(R.id.connectBtn);
        enterBtn      = findViewById(R.id.enterBtn);
        backspaceBtn  = findViewById(R.id.backspaceBtn);
        tabBtn        = findViewById(R.id.tabBtn);
        escBtn        = findViewById(R.id.escBtn);
        spaceBtn      = findViewById(R.id.spaceBtn);
        ctrlBtn       = findViewById(R.id.ctrlBtn);
        altBtn        = findViewById(R.id.altBtn);
        shiftBtn      = findViewById(R.id.shiftBtn);
        winBtn        = findViewById(R.id.winBtn);

        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = (bm != null) ? bm.getAdapter() : null;
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        hidService = new BluetoothHidService(this, bluetoothAdapter, this::updateStatus);
        requestNeededPermissions();

        refreshBtn.setOnClickListener(v -> loadPairedDevices());
        registerBtn.setOnClickListener(v -> hidService.registerHidApp());
        connectBtn.setOnClickListener(v -> {
            int pos = deviceSpinner.getSelectedItemPosition();
            if (pos >= 0 && pos < pairedDevices.size()) {
                hidService.connectTo(pairedDevices.get(pos));
            } else {
                Toast.makeText(this, "Pick a paired device first",
                        Toast.LENGTH_SHORT).show();
            }
        });

        enterBtn.setOnClickListener(v ->
                hidService.sendKey(HidConstants.KEY_ENTER, currentModifiers()));
        backspaceBtn.setOnClickListener(v ->
                hidService.sendKey(HidConstants.KEY_BACKSPACE, currentModifiers()));
        tabBtn.setOnClickListener(v ->
                hidService.sendKey(HidConstants.KEY_TAB, currentModifiers()));
        escBtn.setOnClickListener(v ->
                hidService.sendKey(HidConstants.KEY_ESC, currentModifiers()));
        spaceBtn.setOnClickListener(v ->
                hidService.sendKey(HidConstants.KEY_SPACE, currentModifiers()));

        ctrlBtn.setOnClickListener(v ->  { ctrlActive  = !ctrlActive;  ctrlBtn.setSelected(ctrlActive);   });
        altBtn.setOnClickListener(v ->   { altActive   = !altActive;   altBtn.setSelected(altActive);     });
        shiftBtn.setOnClickListener(v -> { shiftActive = !shiftActive; shiftBtn.setSelected(shiftActive); });
        winBtn.setOnClickListener(v ->   { winActive   = !winActive;   winBtn.setSelected(winActive);     });

        // Real-time keystroke streaming: as the user types in the system
        // keyboard, fire HID reports for each new character.
        inputField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > before) {
                    String added = s.subSequence(start + before, start + count).toString();
                    for (int i = 0; i < added.length(); i++) {
                        hidService.sendChar(added.charAt(i), currentModifiers());
                    }
                    // sticky modifiers (Ctrl/Alt/Win) auto-clear after a keypress
                    if (ctrlActive || altActive || winActive) {
                        ctrlActive = altActive = winActive = false;
                        ctrlBtn.setSelected(false);
                        altBtn.setSelected(false);
                        winBtn.setSelected(false);
                    }
                } else if (count < before) {
                    int removed = before - count;
                    for (int i = 0; i < removed; i++) {
                        hidService.sendKey(HidConstants.KEY_BACKSPACE, (byte) 0);
                    }
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadPairedDevices();
    }

    private byte currentModifiers() {
        byte m = 0;
        if (ctrlActive)  m |= HidConstants.MOD_LCTRL;
        if (shiftActive) m |= HidConstants.MOD_LSHIFT;
        if (altActive)   m |= HidConstants.MOD_LALT;
        if (winActive)   m |= HidConstants.MOD_LGUI;
        return m;
    }

    private void requestNeededPermissions() {
        List<String> needed = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.BLUETOOTH_CONNECT);
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.BLUETOOTH_SCAN);
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    needed.toArray(new String[0]), REQ_PERMISSIONS);
        }
    }

    private void loadPairedDevices() {
        pairedDevices.clear();
        try {
            for (BluetoothDevice d : bluetoothAdapter.getBondedDevices()) {
                pairedDevices.add(d);
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth permission not granted",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> names = new ArrayList<>();
        for (BluetoothDevice d : pairedDevices) {
            String name;
            try { name = d.getName(); }
            catch (SecurityException e) { name = null; }
            names.add((name != null ? name : "Unknown") + "  \u00b7  " + d.getAddress());
        }
        if (names.isEmpty()) names.add("(no paired devices \u2014 pair laptop in Settings)");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceSpinner.setAdapter(adapter);
    }

    private void updateStatus(String s) {
        runOnUiThread(() -> statusText.setText(s));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) loadPairedDevices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hidService != null) hidService.cleanup();
    }
}
