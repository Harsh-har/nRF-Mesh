package no.nordicsemi.android.swaromesh;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import no.nordicsemi.android.swaromesh.adapter.ExtendedBluetoothDevice;
import no.nordicsemi.android.swaromesh.ble.BleMeshManager;
import no.nordicsemi.android.swaromesh.ble.ScannerActivity;
import no.nordicsemi.android.swaromesh.viewmodels.ScannerViewModel;
import no.nordicsemi.android.swaromesh.viewmodels.SharedViewModel;

public class DevicesFilterFragment extends Fragment {

    private static final String TAG = "DevicesFilterFragment";

    // 🔹 DEFAULT FILTER VALUE
    private static final String DEFAULT_DEVICE_NAME = "";
    private static final String DEFAULT_SELECTED_DEVICE = "Select Device";

    // 🔹 PREDEFINED DEVICE LIST
    private static final List<String> PREDEFINED_DEVICES = Arrays.asList(
            "Select Device",  // Default item
            "SW-RL01-006", "SW-RL02-012", "SW-RL03-016",
            "SW-CLF01-100", "SW-CLE02-050", "SW-CLC03-150",
            "SW-PSU01-30", "SW-PSD02-60", "SW-PSS04-60", "SW-PSR05-60",
            "SW-DND01-03", "SW-DNU02-10", "SW-DNT03-10", "SW-DNR04-60",
            "SW-DM01-004", "SW-CN01-AA", "SW-IR01-AA",
            "SW-UIQP01-AA", "SW-UIQS02-AA", "SW-UIQB03-AA",
            "SW-UIKP04-AA", "SW-UIKS05-AA", "SW-UIKB06-AA",
            "SW-CS07-1N", "SW-PB08-AA", "SW-CS09-6N",
            "SW-URC01-AA", "SW-URT02-AA", "SW-UITC01-10",
            "SW-HUB02-AA", "SW-SSO01-AA", "SW-SHO02-AA",
            "SW-SWO03-AA", "SW-SUVR04-AA", "SW-STH06-AA",
            "SW-SAQ07-AA", "SW-SFG08-AA", "SW-SAP12-AA",
            "SW-SOF09-AA", "SW-SCO218-AA", "SW-STD10-AA",
            "SW-SWT01-AA", "SW-SFR05-AA", "SW-SWP11-AA",
            "SW-STW13-AA", "SW-SRS17-AA", "SW-SGB14-AA",
            "SW-SDS15-AA", "SW-SSM16-AA", "SW-SVL01-AA",
            "SW-SAS01-AA", "SW-HWS01-AA", "SW-MRB01-AA",
            "SW-MCS02-AA", "SW-LR97-10", "SW-LR95-10",
            "SW-LR00-05", "SW-LTW90-15", "SW-LRG00-28",
            "SW-LS97-10", "SW-LGS02-AA", "SW-LGR03-AA"
    );

    private TextInputEditText etDeviceName;
    private Spinner spinnerDevices;
    private Button btnApply, btnReset;

    private ScannerViewModel scannerViewModel;
    private SharedViewModel sharedViewModel;

    // Store all unprovisioned devices for filtering
    private List<ExtendedBluetoothDevice> allUnprovisionedDevices = new ArrayList<>();

    // Current filter text and selected device
    private String currentFilter = "";
    private String selectedDevice = DEFAULT_SELECTED_DEVICE;

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

        // ✅ ViewModels (Activity scope)
        scannerViewModel = new ViewModelProvider(requireActivity())
                .get(ScannerViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity())
                .get(SharedViewModel.class);

        initUi(view);
        setupDeviceSpinner();
        setupDefaultValue();
        setupTextWatcher();
        setupActions();
        observeScanResults();

        return view;
    }

    private void initUi(View view) {
        etDeviceName = view.findViewById(R.id.etDeviceName);
        spinnerDevices = view.findViewById(R.id.spinnerDevices);
        btnApply = view.findViewById(R.id.btnApply);
        btnReset = view.findViewById(R.id.btnReset);
    }

    // ---------------------------------------------------------------------
    // SETUP DEVICE SPINNER
    // ---------------------------------------------------------------------
    private void setupDeviceSpinner() {
        // Create adapter for spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                PREDEFINED_DEVICES
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDevices.setAdapter(adapter);

        // Set spinner selection listener
        spinnerDevices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDevice = PREDEFINED_DEVICES.get(position);

                // Show toast when device is selected
                if (!selectedDevice.equals(DEFAULT_SELECTED_DEVICE)) {
                    Toast.makeText(requireContext(),
                            "Selected: " + selectedDevice,
                            Toast.LENGTH_SHORT).show();

                    // Auto-fill the device name field with selected device
                    etDeviceName.setText(selectedDevice);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDevice = DEFAULT_SELECTED_DEVICE;
            }
        });
    }

    // ---------------------------------------------------------------------
    // SET DEFAULT FILTER
    // ---------------------------------------------------------------------
    private void setupDefaultValue() {
        etDeviceName.setText(DEFAULT_DEVICE_NAME);
        currentFilter = DEFAULT_DEVICE_NAME;

        // Set spinner to default position
        spinnerDevices.setSelection(0);
    }

    // ---------------------------------------------------------------------
    // TEXT WATCHER FOR REAL-TIME FILTERING
    // ---------------------------------------------------------------------
    private void setupTextWatcher() {
        etDeviceName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentFilter = s.toString().trim();
                // Apply filter in real-time
                filterDevicesAndUpdateScanner();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupActions() {
        btnApply.setOnClickListener(v -> {
            applyFilter();

            // Show toast with applied filter info
            String filterInfo = selectedDevice.equals(DEFAULT_SELECTED_DEVICE)
                    ? "Filter: " + currentFilter
                    : "Filter: " + selectedDevice;

            Toast.makeText(requireContext(),
                    "Applied - " + filterInfo,
                    Toast.LENGTH_LONG).show();

            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();
        });

        btnReset.setOnClickListener(v -> resetFilter());
    }

    // ---------------------------------------------------------------------
    // OBSERVE SCAN RESULTS
    // ---------------------------------------------------------------------
    private void observeScanResults() {
        scannerViewModel.getScannerRepository().getScannerResults()
                .observe(getViewLifecycleOwner(), scannerLiveData -> {

                    if (scannerLiveData != null && scannerLiveData.getDevices() != null) {

                        // Store only unprovisioned devices (those with provisioning UUID)
                        allUnprovisionedDevices.clear();

                        for (ExtendedBluetoothDevice device : scannerLiveData.getDevices()) {
                            if (isUnprovisionedDevice(device)) {
                                allUnprovisionedDevices.add(device);
                            }
                        }

                        // Apply current filter to update the scanner display
                        filterDevicesAndUpdateScanner();

                        Log.d(TAG, "Found " + allUnprovisionedDevices.size() + " unprovisioned devices");

                        // Show toast with device count
                        if (isAdded()) {
                            Toast.makeText(requireContext(),
                                    "Found " + allUnprovisionedDevices.size() + " devices",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // ---------------------------------------------------------------------
    // CHECK IF DEVICE IS UNPROVISIONED
    // ---------------------------------------------------------------------
    private boolean isUnprovisionedDevice(ExtendedBluetoothDevice device) {
        // Check if device is advertising with provisioning UUID
        if (device.getScanResult() != null &&
                device.getScanResult().getScanRecord() != null &&
                device.getScanResult().getScanRecord().getServiceUuids() != null) {

            return device.getScanResult().getScanRecord().getServiceUuids()
                    .contains(BleMeshManager.MESH_PROVISIONING_UUID);
        }
        return false;
    }

    // ---------------------------------------------------------------------
    // FILTER DEVICES AND UPDATE SCANNER
    // ---------------------------------------------------------------------
    private void filterDevicesAndUpdateScanner() {
        // Create filtered list based on current filter and selected device
        List<ExtendedBluetoothDevice> filteredList = new ArrayList<>();

        // Determine filter text to use (from spinner or manual entry)
        String filterToUse;
        if (!selectedDevice.equals(DEFAULT_SELECTED_DEVICE)) {
            filterToUse = selectedDevice;
        } else {
            filterToUse = currentFilter;
        }

        if (filterToUse.isEmpty()) {
            // No filter - show all unprovisioned devices
            filteredList.addAll(allUnprovisionedDevices);
        } else {
            // Apply name filter
            String lowerCaseFilter = filterToUse.toLowerCase();
            for (ExtendedBluetoothDevice device : allUnprovisionedDevices) {
                if (device.getName() != null &&
                        device.getName().toLowerCase().contains(lowerCaseFilter)) {
                    filteredList.add(device);

                    // Show toast when matching device is found (optional)
                    if (device.getName().equalsIgnoreCase(filterToUse)) {
                        showDeviceFoundToast(device.getName());
                    }
                }
            }
        }

        // Update the scanner display
        updateScannerDisplay(filteredList);

        Log.d(TAG, "Filter applied: '" + filterToUse + "' - showing " +
                filteredList.size() + " devices");
    }

    // ---------------------------------------------------------------------
    // SHOW DEVICE FOUND TOAST
    // ---------------------------------------------------------------------
    private void showDeviceFoundToast(String deviceName) {
        if (isAdded()) {
            Toast.makeText(requireContext(),
                    "✓ Device found: " + deviceName,
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------------
    // UPDATE SCANNER DISPLAY
    // ---------------------------------------------------------------------
    private void updateScannerDisplay(List<ExtendedBluetoothDevice> filteredDevices) {
        // Store the filter in SharedViewModel for the activity to observe
        sharedViewModel.setDeviceNameFilter(currentFilter);
        sharedViewModel.setSelectedDevice(selectedDevice);

        // Also try to directly update the activity if it's available
        if (getActivity() instanceof ScannerActivity) {
            // This will work once we add the method to ScannerActivity
            // ((ScannerActivity) getActivity()).updateFilteredDevices(filteredDevices);
        }
    }

    // ---------------------------------------------------------------------
    // APPLY FILTER (called when Apply button is clicked)
    // ---------------------------------------------------------------------
    private void applyFilter() {
        String deviceName = etDeviceName.getText() != null
                ? etDeviceName.getText().toString().trim()
                : "";

        Log.i(TAG, "----- APPLY FILTER -----");
        Log.i(TAG, "Unprovisioned Device Name Filter: " + deviceName);
        Log.i(TAG, "Selected Device from Spinner: " + selectedDevice);

        // Store the filter in SharedViewModel for persistence
        sharedViewModel.setDeviceNameFilter(deviceName);
        sharedViewModel.setSelectedDevice(selectedDevice);

        // Apply the filter one last time
        filterDevicesAndUpdateScanner();

        // Show summary toast
        showFilterSummaryToast(deviceName);
    }

    // ---------------------------------------------------------------------
    // SHOW FILTER SUMMARY TOAST
    // ---------------------------------------------------------------------
    private void showFilterSummaryToast(String deviceName) {
        if (!isAdded()) return;

        String message;
        if (!selectedDevice.equals(DEFAULT_SELECTED_DEVICE)) {
            message = "Filtering by: " + selectedDevice;
        } else if (!deviceName.isEmpty()) {
            message = "Filtering by: " + deviceName;
        } else {
            message = "Showing all devices";
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    // ---------------------------------------------------------------------
    // RESET FILTER
    // ---------------------------------------------------------------------
    private void resetFilter() {
        etDeviceName.setText("");
        spinnerDevices.setSelection(0);  // Reset spinner to "Select Device"
        currentFilter = "";
        selectedDevice = DEFAULT_SELECTED_DEVICE;

        // Reset filter in SharedViewModel
        sharedViewModel.setDeviceNameFilter("");
        sharedViewModel.setSelectedDevice(DEFAULT_SELECTED_DEVICE);

        // Show all devices again
        filterDevicesAndUpdateScanner();

        Log.i(TAG, "Filters reset - showing all unprovisioned devices");

        Toast.makeText(requireContext(),
                "Filters reset - showing all devices",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Optional: Clear filter when fragment is destroyed
        // Uncomment if you want to clear filter when closing
        // sharedViewModel.setDeviceNameFilter("");
    }
}