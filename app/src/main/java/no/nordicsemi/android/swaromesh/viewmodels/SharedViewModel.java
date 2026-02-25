package no.nordicsemi.android.swaromesh.viewmodels;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import no.nordicsemi.android.swaromesh.adapter.ExtendedBluetoothDevice;
import no.nordicsemi.android.swaromesh.utils.NetworkExportUtils;


@HiltViewModel
public class SharedViewModel extends BaseViewModel implements NetworkExportUtils.NetworkExportCallbacks {

    private final ScannerRepository mScannerRepository;
    private final SingleLiveEvent<String> networkExportState = new SingleLiveEvent<>();

    private static final String PREFS_NAME = "mesh_prefs";
    private static final String KEY_PROXY_ENABLED = "proxy_enabled";
    private static final String KEY_DEVICE_NAME_FILTER = "device_name_filter";
    private static final String KEY_SELECTED_DEVICE = "selected_device";
    private static final String DEFAULT_SELECTED_DEVICE = "Select Device";

    private final SharedPreferences prefs;

    private final MutableLiveData<Boolean> proxyEnabled = new MutableLiveData<>();
    private final MutableLiveData<String> deviceNameFilter = new MutableLiveData<>("");
    private final MutableLiveData<String> selectedDevice = new MutableLiveData<>(DEFAULT_SELECTED_DEVICE);
    private final MutableLiveData<List<ExtendedBluetoothDevice>> filteredDevices = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ExtendedBluetoothDevice>> allUnprovisionedDevices = new MutableLiveData<>(new ArrayList<>());

    @Inject
    SharedViewModel(
            @NonNull final NrfMeshRepository nrfMeshRepository,
            @NonNull final ScannerRepository scannerRepository,
            @ApplicationContext @NonNull final Context context
    ) {
        super(nrfMeshRepository);

        mScannerRepository = scannerRepository;
        scannerRepository.registerBroadcastReceivers();

        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        boolean savedProxy = prefs.getBoolean(KEY_PROXY_ENABLED, true);
        proxyEnabled.setValue(savedProxy);

        String savedFilter = prefs.getString(KEY_DEVICE_NAME_FILTER, "");
        deviceNameFilter.setValue(savedFilter);

        String savedSelectedDevice = prefs.getString(KEY_SELECTED_DEVICE, DEFAULT_SELECTED_DEVICE);
        selectedDevice.setValue(savedSelectedDevice);
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        // ✅ ROOT FIX: Do NOT disconnect if proxy is currently connected.
        // Previously, onCleared() always called disconnect() when ScannerActivity
        // was destroyed — even during a successful proxy connection transition to
        // NodeConfigurationActivity. This caused the ~1 second auto-disconnect bug.
        //
        // Now: only disconnect if BLE is NOT connected (i.e., genuine cleanup scenario).
        // When proxy is connected and user navigates forward, BLE stays alive.
        if (!mNrfMeshRepository.getBleMeshManager().isConnected()) {
            mNrfMeshRepository.disconnect();
        }

        mScannerRepository.unregisterBroadcastReceivers();
    }

    public LiveData<String> getNetworkLoadState() {
        return mNrfMeshRepository.getNetworkLoadState();
    }

    public LiveData<String> getNetworkExportState() {
        return networkExportState;
    }

    public void setSelectedGroup(final int address) {
        mNrfMeshRepository.setSelectedGroup(address);
    }

    public void exportMeshNetwork(@NonNull final OutputStream stream) {
        NetworkExportUtils.exportMeshNetwork(getMeshManagerApi(), stream, this);
    }

    public void exportMeshNetwork() {
        final String fileName = getNetworkLiveData().getNetworkName() + ".json";
        NetworkExportUtils.exportMeshNetwork(getMeshManagerApi(), NrfMeshRepository.EXPORT_PATH, fileName, this);
    }

    @Override
    public void onNetworkExported() {
        networkExportState.postValue(getNetworkLiveData().getMeshNetwork().getMeshName()
                + " has been successfully exported.");
    }

    @Override
    public void onNetworkExportFailed(@NonNull final String error) {
        networkExportState.postValue(error);
    }

    // ---------------- PROXY BUTTON STATE (PERSISTENT) ----------------

    public LiveData<Boolean> getProxyEnabled() {
        return proxyEnabled;
    }

    public void setProxyEnabled(boolean enabled) {
        proxyEnabled.setValue(enabled);
        prefs.edit().putBoolean(KEY_PROXY_ENABLED, enabled).apply();
    }

    public boolean isProxyEnabled() {
        Boolean v = proxyEnabled.getValue();
        return v != null && v;
    }

    // ---------------- DEVICE NAME FILTER (PERSISTENT) ----------------

    public LiveData<String> getDeviceNameFilter() {
        return deviceNameFilter;
    }

    public void setDeviceNameFilter(String filter) {
        if (filter == null) filter = "";
        deviceNameFilter.setValue(filter);
        prefs.edit().putString(KEY_DEVICE_NAME_FILTER, filter).apply();
    }

    public String getDeviceNameFilterValue() {
        String value = deviceNameFilter.getValue();
        return value != null ? value : "";
    }

    public void clearDeviceNameFilter() {
        setDeviceNameFilter("");
    }

    // ---------------- SELECTED DEVICE (PERSISTENT) ----------------

    public LiveData<String> getSelectedDevice() {
        return selectedDevice;
    }

    public void setSelectedDevice(String device) {
        if (device == null) device = DEFAULT_SELECTED_DEVICE;
        selectedDevice.setValue(device);
        prefs.edit().putString(KEY_SELECTED_DEVICE, device).apply();
    }

    public String getSelectedDeviceValue() {
        String value = selectedDevice.getValue();
        return value != null ? value : DEFAULT_SELECTED_DEVICE;
    }

    public boolean isDeviceSelected(String deviceName) {
        return deviceName != null && deviceName.equals(getSelectedDeviceValue());
    }

    public void clearSelectedDevice() {
        setSelectedDevice(DEFAULT_SELECTED_DEVICE);
    }

    // ---------------- FILTERED DEVICES ----------------

    public LiveData<List<ExtendedBluetoothDevice>> getFilteredDevices() {
        return filteredDevices;
    }

    public void setFilteredDevices(List<ExtendedBluetoothDevice> devices) {
        if (devices == null) devices = new ArrayList<>();
        filteredDevices.setValue(devices);
    }

    public List<ExtendedBluetoothDevice> getFilteredDevicesValue() {
        List<ExtendedBluetoothDevice> value = filteredDevices.getValue();
        return value != null ? value : new ArrayList<>();
    }

    public void clearFilteredDevices() {
        filteredDevices.setValue(new ArrayList<>());
    }

    // ---------------- ALL UNPROVISIONED DEVICES ----------------

    public LiveData<List<ExtendedBluetoothDevice>> getAllUnprovisionedDevices() {
        return allUnprovisionedDevices;
    }

    public void setAllUnprovisionedDevices(List<ExtendedBluetoothDevice> devices) {
        if (devices == null) devices = new ArrayList<>();
        allUnprovisionedDevices.setValue(devices);
    }

    public List<ExtendedBluetoothDevice> getAllUnprovisionedDevicesValue() {
        List<ExtendedBluetoothDevice> value = allUnprovisionedDevices.getValue();
        return value != null ? value : new ArrayList<>();
    }

    public void addUnprovisionedDevice(ExtendedBluetoothDevice device) {
        if (device == null) return;
        List<ExtendedBluetoothDevice> currentList = getAllUnprovisionedDevicesValue();
        if (!currentList.contains(device)) {
            currentList.add(device);
            allUnprovisionedDevices.setValue(currentList);
        }
    }

    public void clearAllUnprovisionedDevices() {
        allUnprovisionedDevices.setValue(new ArrayList<>());
    }

    // ---------------- FILTER UTILITY METHODS ----------------

    public boolean isFilterActive() {
        return !getDeviceNameFilterValue().isEmpty() ||
                !getSelectedDeviceValue().equals(DEFAULT_SELECTED_DEVICE);
    }

    public String getActiveFilterDescription() {
        if (!getSelectedDeviceValue().equals(DEFAULT_SELECTED_DEVICE)) {
            return "Filter: " + getSelectedDeviceValue();
        } else if (!getDeviceNameFilterValue().isEmpty()) {
            return "Filter: " + getDeviceNameFilterValue();
        } else {
            return "No filter active";
        }
    }

    public void resetAllFilters() {
        clearDeviceNameFilter();
        clearSelectedDevice();
        clearFilteredDevices();
    }

    public List<ExtendedBluetoothDevice> applyFilter(List<ExtendedBluetoothDevice> devices) {
        if (devices == null) return new ArrayList<>();

        String filterToUse;
        if (!getSelectedDeviceValue().equals(DEFAULT_SELECTED_DEVICE)) {
            filterToUse = getSelectedDeviceValue();
        } else {
            filterToUse = getDeviceNameFilterValue();
        }

        if (filterToUse.isEmpty()) {
            return new ArrayList<>(devices);
        }

        List<ExtendedBluetoothDevice> filtered = new ArrayList<>();
        String lowerCaseFilter = filterToUse.toLowerCase();

        for (ExtendedBluetoothDevice device : devices) {
            if (device.getName() != null &&
                    device.getName().toLowerCase().contains(lowerCaseFilter)) {
                filtered.add(device);
            }
        }

        return filtered;
    }

    public void applyCurrentFilter() {
        List<ExtendedBluetoothDevice> filtered = applyFilter(getAllUnprovisionedDevicesValue());
        setFilteredDevices(filtered);
    }

    // ---------------- SCANNER REPOSITORY ACCESS ----------------

    public ScannerRepository getScannerRepository() {
        return mScannerRepository;
    }

    public LiveData<ScannerLiveData> getScannerResults() {
        return mScannerRepository.getScannerResults();
    }
}