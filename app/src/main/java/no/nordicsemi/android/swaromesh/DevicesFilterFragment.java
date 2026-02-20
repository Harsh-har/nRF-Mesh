package no.nordicsemi.android.swaromesh;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

public class DevicesFilterFragment extends Fragment {

    private static final String TAG = "DevicesFilter";

    // UI references (MATCH XML)
    private TextInputEditText etDeviceName;
    private RadioGroup rgSignalStrength;
    private TextView tvSignalPreview;
    private TextView tvRssiValue;
    private Button btnApply, btnReset;

    public DevicesFilterFragment() {}

    public static DevicesFilterFragment newInstance() {
        return new DevicesFilterFragment();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_devices_filter, container, false);

        initUi(view);
        setupActions();

        return view;
    }

    private void initUi(View view) {
        etDeviceName     = view.findViewById(R.id.etDeviceName);
        rgSignalStrength = view.findViewById(R.id.rgSignalStrength);
        tvSignalPreview  = view.findViewById(R.id.tvSignalPreview);
        tvRssiValue      = view.findViewById(R.id.tvRssiValue);
        btnApply         = view.findViewById(R.id.btnApply);
        btnReset         = view.findViewById(R.id.btnReset);

        // Default state
        rgSignalStrength.check(R.id.rbSignalDefault);
        tvSignalPreview.setText("Any signal strength");
        tvRssiValue.setText("RSSI: Any");
    }

    private void setupActions() {

        rgSignalStrength.setOnCheckedChangeListener((group, checkedId) ->
                updateSignalPreview(checkedId)
        );

        btnApply.setOnClickListener(v -> applyFilter());

        btnReset.setOnClickListener(v -> resetFilter());
    }

    private void updateSignalPreview(int checkedId) {

        if (checkedId == R.id.rbSignal3Bars) {
            tvSignalPreview.setText("Weak signal (3 bars)");
            tvRssiValue.setText("RSSI ≥ -80 dBm");

        } else if (checkedId == R.id.rbSignal4Bars) {
            tvSignalPreview.setText("Medium signal (4 bars)");
            tvRssiValue.setText("RSSI ≥ -65 dBm");

        } else if (checkedId == R.id.rbSignal5Bars) {
            tvSignalPreview.setText("Strong signal (5 bars)");
            tvRssiValue.setText("RSSI ≥ -50 dBm");

        } else {
            tvSignalPreview.setText("Any signal strength");
            tvRssiValue.setText("RSSI: Any");
        }
    }

    private void applyFilter() {

        String deviceName = etDeviceName.getText() != null
                ? etDeviceName.getText().toString().trim()
                : "";

        int selectedId = rgSignalStrength.getCheckedRadioButtonId();
        RadioButton rb = requireView().findViewById(selectedId);
        String signalFilter = rb != null ? rb.getText().toString() : "Default";

        Log.i(TAG, "----- APPLY FILTER -----");
        Log.i(TAG, "Device Name     : " + deviceName);
        Log.i(TAG, "Signal Strength : " + signalFilter);

        // TODO: connect to ViewModel / device scan filtering
        // getParentFragmentManager().popBackStack();
    }

    private void resetFilter() {
        etDeviceName.setText("");
        rgSignalStrength.check(R.id.rbSignalDefault);
        tvSignalPreview.setText("Any signal strength");
        tvRssiValue.setText("RSSI: Any");

        Log.i(TAG, "Filters reset");
    }
}