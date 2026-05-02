package com.example.btkeyboard;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppQosSettings;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Executor;

/**
 * Wraps the BluetoothHidDevice profile (API 28+) so MainActivity can:
 *   1. Acquire the HID profile proxy
 *   2. Register this app as a HID device with a keyboard descriptor
 *   3. Connect to a paired host (laptop)
 *   4. Send key press / release reports
 */
public class BluetoothHidService {
    private static final String TAG = "BTHidService";

    public interface StatusCallback {
        void onStatus(String status);
    }

    private final Context context;
    private final BluetoothAdapter adapter;
    private final StatusCallback callback;

    private BluetoothHidDevice hidProfile;
    private BluetoothDevice connectedDevice;
    private boolean appRegistered = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor executor = mainHandler::post;

    public BluetoothHidService(Context ctx, BluetoothAdapter adapter, StatusCallback cb) {
        this.context = ctx.getApplicationContext();
        this.adapter = adapter;
        this.callback = cb;
        getProxy();
    }

    private void status(String s) {
        Log.d(TAG, s);
        if (callback != null) callback.onStatus(s);
    }

    @SuppressLint("MissingPermission")
    private void getProxy() {
        boolean ok = adapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidProfile = (BluetoothHidDevice) proxy;
                    status("HID profile ready \u2014 tap REGISTER");
                }
            }
            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidProfile = null;
                    appRegistered = false;
                    status("HID profile disconnected");
                }
            }
        }, BluetoothProfile.HID_DEVICE);
        if (!ok) status("Failed to obtain HID profile");
    }

    @SuppressLint("MissingPermission")
    public void registerHidApp() {
        if (hidProfile == null) { status("HID profile not ready"); return; }
        if (appRegistered)      { status("Already registered");   return; }

        BluetoothHidDeviceAppSdpSettings sdp = new BluetoothHidDeviceAppSdpSettings(
                "BT Keyboard",
                "Android Bluetooth HID Keyboard",
                "Android",
                BluetoothHidDevice.SUBCLASS1_KEYBOARD,
                HidConstants.HID_DESCRIPTOR
        );

        BluetoothHidDeviceAppQosSettings inQos = new BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800, 9, 0,
                11250, BluetoothHidDeviceAppQosSettings.MAX);
        BluetoothHidDeviceAppQosSettings outQos = new BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800, 9, 0,
                11250, BluetoothHidDeviceAppQosSettings.MAX);

        boolean ok = hidProfile.registerApp(sdp, inQos, outQos, executor,
                new BluetoothHidDevice.Callback() {
                    @Override
                    public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
                        appRegistered = registered;
                        status(registered
                                ? "App registered \u2014 pick a device and CONNECT"
                                : "App unregistered");
                    }
                    @Override
                    public void onConnectionStateChanged(BluetoothDevice device, int state) {
                        String s;
                        switch (state) {
                            case BluetoothProfile.STATE_CONNECTED:
                                s = "CONNECTED"; connectedDevice = device; break;
                            case BluetoothProfile.STATE_CONNECTING:
                                s = "CONNECTING"; break;
                            case BluetoothProfile.STATE_DISCONNECTED:
                                s = "DISCONNECTED"; connectedDevice = null; break;
                            case BluetoothProfile.STATE_DISCONNECTING:
                                s = "DISCONNECTING"; break;
                            default: s = "STATE_" + state;
                        }
                        String name = "?";
                        try { name = device != null ? device.getName() : "?"; }
                        catch (SecurityException ignored) {}
                        status("Connection: " + s + " (" + name + ")");
                    }
                    @Override
                    public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
                        // Reply with empty report so hosts don't time out.
                        if (hidProfile != null) {
                            hidProfile.replyReport(device, type, id,
                                    new byte[]{0,0,0,0,0,0,0,0});
                        }
                    }
                });
        if (!ok) status("registerApp() returned false");
    }

    @SuppressLint("MissingPermission")
    public void connectTo(BluetoothDevice device) {
        if (hidProfile == null || !appRegistered) {
            status("Register the app first");
            return;
        }
        boolean ok = hidProfile.connect(device);
        status(ok ? "Connecting\u2026" : "connect() returned false");
    }

    /**
     * Send a single character: looks up the HID code, applies SHIFT if the
     * character requires it, and emits a press+release report pair.
     */
    @SuppressLint("MissingPermission")
    public void sendChar(char c, byte modifierBase) {
        if (hidProfile == null || connectedDevice == null) return;
        int hid = HidConstants.charToHid(c);
        if (hid == 0) return;

        byte modifier = modifierBase;
        if ((hid & HidConstants.SHIFT_FLAG) != 0) {
            modifier |= HidConstants.MOD_LSHIFT;
            hid &= ~HidConstants.SHIFT_FLAG;
        }
        sendKey((byte) hid, modifier);
    }

    /** Press + release a single HID usage code with given modifier byte. */
    @SuppressLint("MissingPermission")
    public void sendKey(byte hidCode, byte modifier) {
        if (hidProfile == null || connectedDevice == null) return;
        byte[] press   = new byte[]{ modifier, 0, hidCode, 0, 0, 0, 0, 0 };
        byte[] release = new byte[]{        0, 0,       0, 0, 0, 0, 0, 0 };
        hidProfile.sendReport(connectedDevice, 0, press);
        hidProfile.sendReport(connectedDevice, 0, release);
    }

    @SuppressLint("MissingPermission")
    public void cleanup() {
        if (hidProfile != null) {
            try {
                if (connectedDevice != null) hidProfile.disconnect(connectedDevice);
                if (appRegistered)           hidProfile.unregisterApp();
            } catch (Exception ignored) {}
            adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidProfile);
            hidProfile = null;
        }
    }
}
