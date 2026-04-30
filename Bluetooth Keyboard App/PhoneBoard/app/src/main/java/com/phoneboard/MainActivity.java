package com.phoneboard;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Main screen:
 *  • Shows all paired Bluetooth devices
 *  • Tap a device to connect as HID keyboard
 *  • Status bar at top
 */
public class MainActivity extends AppCompatActivity {

    private HidService hidService;
    private boolean    serviceBound = false;

    private TextView          tvStatus;
    private RecyclerView      rvDevices;
    private MaterialButton    btnRefresh;
    private View              vConnectedBar;
    private TextView          tvConnectedName;
    private MaterialButton    btnDisconnect;
    private View              vLoadingOverlay;

    private DeviceAdapter deviceAdapter;
    private final List<BluetoothDevice> deviceList = new ArrayList<>();

    private BluetoothAdapter btAdapter;

    // ── Permission launcher ───────────────────────────────────────────────────
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean v : result.values()) if (!v) allGranted = false;
                if (allGranted) {
                    startHidService();
                } else {
                    tvStatus.setText("⚠ Bluetooth permissions required. Please grant in Settings.");
                }
            });

    // ── Service connection ────────────────────────────────────────────────────
    private final ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            hidService  = ((HidService.LocalBinder) service).getService();
            serviceBound = true;
            hidService.registerHidDevice();
            loadPairedDevices();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            hidService = null;
        }
    };

    // ── BroadcastReceiver for state changes ───────────────────────────────────
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(HidService.EXTRA_STATE, -999);
            BluetoothDevice device = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                device = intent.getParcelableExtra(HidService.EXTRA_DEVICE, BluetoothDevice.class);
            } else {
                device = intent.getParcelableExtra(HidService.EXTRA_DEVICE);
            }
            handleStateChange(state, device);
        }
    };

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus        = findViewById(R.id.tv_status);
        rvDevices       = findViewById(R.id.rv_devices);
        btnRefresh      = findViewById(R.id.btn_refresh);
        vConnectedBar   = findViewById(R.id.connected_bar);
        tvConnectedName = findViewById(R.id.tv_connected_name);
        btnDisconnect   = findViewById(R.id.btn_disconnect);
        vLoadingOverlay = findViewById(R.id.loading_overlay);

        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = bm != null ? bm.getAdapter() : null;

        if (btAdapter == null || !btAdapter.isEnabled()) {
            tvStatus.setText("⚠ Bluetooth is off. Please enable Bluetooth and reopen the app.");
            return;
        }

        // Device list
        deviceAdapter = new DeviceAdapter(deviceList, this::onDeviceTapped);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(deviceAdapter);

        btnRefresh.setOnClickListener(v -> loadPairedDevices());
        btnDisconnect.setOnClickListener(v -> {
            if (serviceBound && hidService != null) {
                hidService.disconnect();
            }
        });

        checkPermissionsAndStart();
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

    // ── Permissions ───────────────────────────────────────────────────────────

    private void checkPermissionsAndStart() {
        List<String> needed = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT))
                needed.add(Manifest.permission.BLUETOOTH_CONNECT);
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN))
                needed.add(Manifest.permission.BLUETOOTH_SCAN);
        } else {
            // Android 9–11
            if (!hasPermission(Manifest.permission.BLUETOOTH))
                needed.add(Manifest.permission.BLUETOOTH);
            if (!hasPermission(Manifest.permission.BLUETOOTH_ADMIN))
                needed.add(Manifest.permission.BLUETOOTH_ADMIN);
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION))
                needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (needed.isEmpty()) {
            startHidService();
        } else {
            permissionLauncher.launch(needed.toArray(new String[0]));
        }
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    // ── Service ───────────────────────────────────────────────────────────────

    private void startHidService() {
        Intent serviceIntent = new Intent(this, HidService.class);
        startForegroundService(serviceIntent);
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
    }

    // ── Device list ───────────────────────────────────────────────────────────

    private void loadPairedDevices() {
        deviceList.clear();
        if (btAdapter == null) return;
        try {
            Set<BluetoothDevice> paired = btAdapter.getBondedDevices();
            if (paired != null) deviceList.addAll(paired);
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission error reading paired devices", Toast.LENGTH_SHORT).show();
        }
        deviceAdapter.notifyDataSetChanged();

        if (deviceList.isEmpty()) {
            tvStatus.setText("No paired devices found.\nPair your laptop via phone's Bluetooth Settings first.");
        } else {
            tvStatus.setText("Tap your laptop to connect as keyboard");
        }
    }

    private void onDeviceTapped(BluetoothDevice device) {
        if (!serviceBound || hidService == null) return;
        if (!hidService.isRegistered()) {
            Toast.makeText(this, "HID not ready yet, please wait…", Toast.LENGTH_SHORT).show();
            return;
        }
        vLoadingOverlay.setVisibility(View.VISIBLE);
        tvStatus.setText("Connecting to " + safeDeviceName(device) + "…");
        hidService.connectToHost(device);
    }

    // ── State handling ────────────────────────────────────────────────────────

    private void handleStateChange(int state, BluetoothDevice device) {
        vLoadingOverlay.setVisibility(View.GONE);
        switch (state) {
            case HidService.STATE_REGISTERED:
                tvStatus.setText("Ready — tap your laptop to connect");
                break;

            case HidService.STATE_CONNECTING:
                vLoadingOverlay.setVisibility(View.VISIBLE);
                tvStatus.setText("Connecting…");
                break;

            case HidService.STATE_CONNECTED:
                vConnectedBar.setVisibility(View.VISIBLE);
                tvConnectedName.setText("● " + safeDeviceName(device));
                tvStatus.setText("Connected! Opening keyboard…");
                // Auto-open keyboard after brief delay
                rvDevices.postDelayed(() -> openKeyboard(device), 500);
                break;

            case HidService.STATE_DISCONNECTED:
                vConnectedBar.setVisibility(View.GONE);
                tvStatus.setText("Disconnected — tap a device to reconnect");
                break;

            case HidService.STATE_ERROR:
                tvStatus.setText("⚠ Error initializing Bluetooth HID.\nMake sure Bluetooth is enabled.");
                break;
        }
    }

    private void openKeyboard(BluetoothDevice device) {
        Intent intent = new Intent(this, KeyboardActivity.class);
        try {
            intent.putExtra("device_name", device.getName());
            intent.putExtra("device_address", device.getAddress());
        } catch (SecurityException ignored) {}
        startActivity(intent);
    }

    private String safeDeviceName(BluetoothDevice device) {
        if (device == null) return "Unknown";
        try {
            String n = device.getName();
            return (n != null && !n.isEmpty()) ? n : device.getAddress();
        } catch (SecurityException e) {
            return device.getAddress();
        }
    }
}
