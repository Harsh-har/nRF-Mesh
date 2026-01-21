package no.nordicsemi.android.nrfmesh.ble.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import no.nordicsemi.android.nrfmesh.R;
import no.nordicsemi.android.nrfmesh.adapter.ExtendedBluetoothDevice;
import no.nordicsemi.android.nrfmesh.databinding.DeviceItemBinding;
import no.nordicsemi.android.nrfmesh.viewmodels.ScannerLiveData;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {
    private final List<ExtendedBluetoothDevice> mDevices;
    private OnItemClickListener mOnItemClickListener;

    public DevicesAdapter(@NonNull final LifecycleOwner owner, @NonNull final ScannerLiveData scannerLiveData) {
        mDevices = scannerLiveData.getDevices();
        scannerLiveData.observe(owner, devices -> {
            final Integer i = devices.getUpdatedDeviceIndex();
            if (i != null)
                notifyItemChanged(i);
            else
                notifyDataSetChanged();
        });
    }

    public void setOnItemClickListener(@NonNull final OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        return new ViewHolder(DeviceItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        final ExtendedBluetoothDevice device = mDevices.get(position);
        final String deviceName = device.getName();

        holder.deviceName.setText(
                TextUtils.isEmpty(deviceName)
                        ? holder.itemView.getContext().getString(R.string.unknown_device)
                        : deviceName
        );

        holder.deviceAddress.setText(device.getAddress());

        int rssi = device.getRssi();

        // Convert RSSI to percentage
        int rssiPercent =
                (int) (100.0f * (127.0f + rssi) / (127.0f + 20.0f));

        rssiPercent = Math.max(0, Math.min(100, rssiPercent));


        if (rssiPercent >= 46) {

            holder.itemView.setVisibility(View.VISIBLE);
            holder.itemView.setLayoutParams(
                    new RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            holder.rssi.setImageLevel(rssiPercent);

        } else {
            //  HIDE out-of-range devices
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(
                    new RecyclerView.LayoutParams(0, 0)
            );
        }
    }


    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    @FunctionalInterface
    public interface OnItemClickListener {
        void onItemClick(final ExtendedBluetoothDevice device);
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceAddress;
        TextView deviceName;
        ImageView rssi;

        private ViewHolder(final @NonNull DeviceItemBinding binding) {
            super(binding.getRoot());
            deviceAddress = binding.deviceAddress;
            deviceName = binding.deviceName;
            rssi = binding.rssi;

            binding.deviceContainer.setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    if(getAdapterPosition() > -1 && mDevices.size() > 0) {
                        mOnItemClickListener.onItemClick(mDevices.get(getAdapterPosition()));
                    }
                }
            });
        }
    }
}
