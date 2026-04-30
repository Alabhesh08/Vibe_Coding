package com.phoneboard;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.VH> {

    public interface OnDeviceClick {
        void onClick(BluetoothDevice device);
    }

    private final List<BluetoothDevice> devices;
    private final OnDeviceClick         listener;

    public DeviceAdapter(List<BluetoothDevice> devices, OnDeviceClick listener) {
        this.devices  = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        BluetoothDevice device = devices.get(position);
        holder.bind(device, listener);
    }

    @Override
    public int getItemCount() { return devices.size(); }

    static class VH extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvAddress;

        VH(View itemView) {
            super(itemView);
            tvName    = itemView.findViewById(R.id.tv_device_name);
            tvAddress = itemView.findViewById(R.id.tv_device_address);
        }

        void bind(BluetoothDevice device, OnDeviceClick listener) {
            try {
                String name = device.getName();
                tvName.setText((name != null && !name.isEmpty()) ? name : "Unknown Device");
                tvAddress.setText(device.getAddress());
            } catch (SecurityException e) {
                tvName.setText("Unknown Device");
                tvAddress.setText("Permission error");
            }
            itemView.setOnClickListener(v -> listener.onClick(device));
        }
    }
}
