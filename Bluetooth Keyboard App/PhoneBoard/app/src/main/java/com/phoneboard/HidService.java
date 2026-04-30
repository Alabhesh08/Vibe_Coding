package com.phoneboard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Background service that:
 *  1. Registers the phone as a Bluetooth HID keyboard device
 *  2. Accepts connections from paired hosts (laptop)
 *  3. Sends HID key reports when KeyboardActivity calls sendKey()
 */
public class HidService extends Service {

    private static final String TAG = "PhoneBoard/HidService";
    private static final String CHANNEL_ID = "phoneboard_channel";
    private static final int NOTIF_ID = 1;

    // ── Broadcast actions ─────────────────────────────────────────────────────
    public static final String ACTION_CONNECTION_STATE = "com.phoneboard.CONNECTION_STATE";
    public static final String EXTRA_STATE            = "state";
    public static final String EXTRA_DEVICE           = "device";
    public static final int    STATE_DISCONNECTED     = 0;
    public static final int    STATE_CONNECTING       = 1;
    public static final int    STATE_CONNECTED        = 2;
    public static final int    STATE_REGISTERED       = 3;
    public static final int    STATE_ERROR            = -1;

    // ── Binder ────────────────────────────────────────────────────────────────
    public class LocalBinder extends Binder {
        public HidService getService() { return HidService.this; }
    }
    private final IBinder binder = new LocalBinder();

    // ── Bluetooth ─────────────────────────────────────────────────────────────
    private BluetoothAdapter   btAdapter;
    private BluetoothHidDevice hidDevice;
    private BluetoothDevice    connectedHost;
    private boolean            isRegistered = false;

    private final Executor executor = Executors.newSingleThreadExecutor();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification("PhoneBoard running — not connected"));

        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bm != null) {
            btAdapter = bm.getAdapter();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        unregisterApp();
        super.onDestroy();
    }

    // ── Public API (called from activities) ───────────────────────────────────

    /**
     * Register this device as a BT HID keyboard and start accepting connections.
     * Must be called once after BT is enabled.
     */
    public void registerHidDevice() {
        if (btAdapter == null || !btAdapter.isEnabled()) {
            broadcastState(STATE_ERROR, null);
            return;
        }
        btAdapter.getProfileProxy(this, profileListener, BluetoothProfile.HID_DEVICE);
    }

    /**
     * Connect to a specific paired host device.
     */
    public void connectToHost(BluetoothDevice device) {
        if (hidDevice == null || !isRegistered) {
            Log.w(TAG, "connectToHost: HID not registered yet");
            return;
        }
        broadcastState(STATE_CONNECTING, device);
        try {
            hidDevice.connect(device);
        } catch (SecurityException e) {
            Log.e(TAG, "connectToHost permission denied", e);
            broadcastState(STATE_ERROR, device);
        }
    }

    /**
     * Disconnect from current host.
     */
    public void disconnect() {
        if (hidDevice != null && connectedHost != null) {
            try {
                hidDevice.disconnect(connectedHost);
            } catch (SecurityException e) {
                Log.e(TAG, "disconnect error", e);
            }
        }
        connectedHost = null;
        broadcastState(STATE_DISCONNECTED, null);
    }

    /**
     * Send a character as HID key press + release.
     * Called rapidly from KeyboardActivity on each text change.
     */
    public boolean sendChar(char c) {
        byte[] pressReport   = HidKeyboard.charToReport(c);
        byte[] releaseReport = HidKeyboard.REPORT_EMPTY;

        if (pressReport == null) {
            Log.d(TAG, "sendChar: no HID mapping for '" + c + "'");
            return false;
        }
        return sendReport(pressReport, releaseReport);
    }

    /**
     * Send a special key (Fn keys, arrows, etc.) with modifier.
     */
    public boolean sendSpecialKey(byte keyCode, byte modifier) {
        byte[] pressReport   = HidKeyboard.specialKeyReport(keyCode, modifier);
        byte[] releaseReport = HidKeyboard.REPORT_EMPTY;
        return sendReport(pressReport, releaseReport);
    }

    /**
     * Send backspace key.
     */
    public boolean sendBackspace() {
        return sendSpecialKey(HidKeyboard.KEY_BACKSPACE, HidKeyboard.MOD_NONE);
    }

    public boolean isConnected() {
        return connectedHost != null;
    }

    public BluetoothDevice getConnectedHost() {
        return connectedHost;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private boolean sendReport(byte[] press, byte[] release) {
        if (hidDevice == null || connectedHost == null) return false;
        try {
            boolean ok = hidDevice.sendReport(connectedHost, 0, press);
            if (ok) {
                // Key release — short delay not required for HID but ensures clean state
                hidDevice.sendReport(connectedHost, 0, release);
            }
            return ok;
        } catch (SecurityException e) {
            Log.e(TAG, "sendReport permission denied", e);
            return false;
        }
    }

    private final BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile != BluetoothProfile.HID_DEVICE) return;
            hidDevice = (BluetoothHidDevice) proxy;

            // SDP record — what the laptop sees when it queries our device
            BluetoothHidDeviceAppSdpSettings sdp = new BluetoothHidDeviceAppSdpSettings(
                    "PhoneBoard Keyboard",          // name
                    "PhoneBoard by Claude",         // description
                    "Anthropic",                    // provider
                    BluetoothHidDevice.SUBCLASS1_KEYBOARD,
                    HidKeyboard.DESCRIPTOR
            );

            try {
                hidDevice.registerApp(sdp, null, null, executor, hidCallback);
            } catch (SecurityException e) {
                Log.e(TAG, "registerApp permission denied", e);
                broadcastState(STATE_ERROR, null);
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            hidDevice = null;
            isRegistered = false;
            broadcastState(STATE_DISCONNECTED, null);
        }
    };

    private final BluetoothHidDevice.Callback hidCallback = new BluetoothHidDevice.Callback() {
        @Override
        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
            isRegistered = registered;
            if (registered) {
                Log.i(TAG, "HID app registered — phone is now a BT keyboard");
                broadcastState(STATE_REGISTERED, null);
                updateNotification("PhoneBoard ready — tap a device to connect");
            } else {
                Log.w(TAG, "HID app unregistered");
                broadcastState(STATE_DISCONNECTED, null);
            }
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            Log.i(TAG, "Connection state: " + state + " device: " + device.getName());
            switch (state) {
                case BluetoothProfile.STATE_CONNECTED:
                    connectedHost = device;
                    broadcastState(STATE_CONNECTED, device);
                    updateNotification("Connected to " + safeDeviceName(device));
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    if (device.equals(connectedHost)) connectedHost = null;
                    broadcastState(STATE_DISCONNECTED, device);
                    updateNotification("PhoneBoard — disconnected");
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    broadcastState(STATE_CONNECTING, device);
                    break;
            }
        }

        @Override
        public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
            // Host is requesting our current report — send empty (no keys held)
            if (hidDevice != null) {
                try {
                    hidDevice.replyReport(device, type, id, HidKeyboard.REPORT_EMPTY);
                } catch (SecurityException ignored) {}
            }
        }

        @Override
        public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {}

        @Override
        public void onSetProtocol(BluetoothDevice device, byte protocol) {}

        @Override
        public void onInterruptData(BluetoothDevice device, byte reportId, byte[] data) {}

        @Override
        public void onVirtualCableUnplug(BluetoothDevice device) {
            if (device.equals(connectedHost)) {
                connectedHost = null;
                broadcastState(STATE_DISCONNECTED, device);
            }
        }
    };

    private void unregisterApp() {
        if (hidDevice != null && isRegistered) {
            try {
                hidDevice.unregisterApp();
            } catch (SecurityException ignored) {}
        }
        if (btAdapter != null) {
            btAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice);
        }
    }

    private void broadcastState(int state, BluetoothDevice device) {
        Intent intent = new Intent(ACTION_CONNECTION_STATE);
        intent.putExtra(EXTRA_STATE, state);
        if (device != null) intent.putExtra(EXTRA_DEVICE, device);
        sendBroadcast(intent);
    }

    private String safeDeviceName(BluetoothDevice device) {
        try {
            String name = device.getName();
            return (name != null && !name.isEmpty()) ? name : device.getAddress();
        } catch (SecurityException e) {
            return device.getAddress();
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "PhoneBoard", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("PhoneBoard Bluetooth keyboard service");
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    private Notification buildNotification(String text) {
        Intent tapIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("PhoneBoard")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_send)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification(text));
    }
}
