package no.nordicsemi.android.swaromesh;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;

import no.nordicsemi.android.swaromesh.viewmodels.SharedViewModel;

public class DevicesFilterFragment extends Fragment {

    private static final String TAG = "DevicesFilter";

    // 🔹 DEFAULT FILTER VALUE
    private static final String DEFAULT_DEVICE_NAME = "SW-RL03-016";

    private TextInputEditText etDeviceName;
    private Button btnApply, btnReset;

    private SharedViewModel sharedViewModel;

    public DevicesFilterFragment() {
        // Required empty public constructor
    }

    public static DevicesFilterFragment newInstance() {
        return new DevicesFilterFragment();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_devices_filter,
                container,
                false
        );

        // ✅ Shared ViewModel (Activity scope)
        sharedViewModel = new ViewModelProvider(requireActivity())
                .get(SharedViewModel.class);

        initUi(view);
        setupDefaultValue();
        setupActions();

        return view;
    }

    private void initUi(View view) {
        etDeviceName = view.findViewById(R.id.etDeviceName);
        btnApply     = view.findViewById(R.id.btnApply);
        btnReset     = view.findViewById(R.id.btnReset);
    }

    // ---------------------------------------------------------------------
    // SET DEFAULT FILTER
    // ---------------------------------------------------------------------
    private void setupDefaultValue() {
        etDeviceName.setText(DEFAULT_DEVICE_NAME);
    }

    private void setupActions() {
        btnApply.setOnClickListener(v -> applyFilter());
        btnReset.setOnClickListener(v -> resetFilter());
    }

    // ---------------------------------------------------------------------
    // APPLY FILTER
    // ---------------------------------------------------------------------
    private void applyFilter() {

        String deviceName =
                etDeviceName.getText() != null
                        ? etDeviceName.getText().toString().trim()
                        : "";

        Log.i(TAG, "----- APPLY FILTER -----");
        Log.i(TAG, "Unprovisioned Device Name Filter: " + deviceName);

        // ✅ SEND FILTER TO VIEWMODEL
        sharedViewModel.setDeviceNameFilter(deviceName);

        // Close filter screen
        requireActivity()
                .getSupportFragmentManager()
                .popBackStack();
    }

    // ---------------------------------------------------------------------
    // RESET FILTER
    // ---------------------------------------------------------------------
    private void resetFilter() {

        etDeviceName.setText("");

        // ✅ CLEAR FILTER IN VIEWMODEL
        sharedViewModel.setDeviceNameFilter(null);

        Log.i(TAG, "Filters reset");
    }
}