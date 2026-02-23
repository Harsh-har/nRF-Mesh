package no.nordicsemi.android.swaromesh.viewmodels;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.OutputStream;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import no.nordicsemi.android.swaromesh.utils.NetworkExportUtils;

@HiltViewModel
public class SharedViewModel extends BaseViewModel
        implements NetworkExportUtils.NetworkExportCallbacks {

    private final ScannerRepository mScannerRepository;
    private final SingleLiveEvent<String> networkExportState = new SingleLiveEvent<>();

    // ---------------------------------------------------------------------
    // PREFS
    // ---------------------------------------------------------------------
    private static final String PREFS_NAME = "mesh_prefs";
    private static final String KEY_PROXY_ENABLED = "proxy_enabled";

    private final SharedPreferences prefs;

    // ---------------------------------------------------------------------
    // PROXY STATE
    // ---------------------------------------------------------------------
    private final MutableLiveData<Boolean> proxyEnabled = new MutableLiveData<>();

    // ---------------------------------------------------------------------
    // DEVICE NAME FILTER
    // ---------------------------------------------------------------------
    private static final String DEFAULT_DEVICE_NAME_FILTER = "SW-RL02-012";
    private final MutableLiveData<String> deviceNameFilter = new MutableLiveData<>();

    // ---------------------------------------------------------------------
    // CONSTRUCTOR
    // ---------------------------------------------------------------------
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

        // ✅ Load proxy state
        boolean saved = prefs.getBoolean(KEY_PROXY_ENABLED, true);
        proxyEnabled.setValue(saved);

        // ✅ Default device name filter
        deviceNameFilter.setValue(DEFAULT_DEVICE_NAME_FILTER);

        // 🔥 APPLY DEFAULT FILTER TO SCANNER
        mScannerRepository.setDeviceNameFilter(DEFAULT_DEVICE_NAME_FILTER);
    }

    // ---------------------------------------------------------------------
    // LIFECYCLE
    // ---------------------------------------------------------------------
    @Override
    protected void onCleared() {
        super.onCleared();
        mNrfMeshRepository.disconnect();
        mScannerRepository.unregisterBroadcastReceivers();
    }

    // ---------------------------------------------------------------------
    // NETWORK
    // ---------------------------------------------------------------------
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
        final String fileName =
                getNetworkLiveData().getNetworkName() + ".json";
        NetworkExportUtils.exportMeshNetwork(
                getMeshManagerApi(),
                NrfMeshRepository.EXPORT_PATH,
                fileName,
                this
        );
    }

    @Override
    public void onNetworkExported() {
        networkExportState.postValue(
                getNetworkLiveData().getMeshNetwork().getMeshName()
                        + " has been successfully exported."
        );
    }

    @Override
    public void onNetworkExportFailed(@NonNull final String error) {
        networkExportState.postValue(error);
    }

    // ---------------------------------------------------------------------
    // PROXY BUTTON STATE
    // ---------------------------------------------------------------------
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

    // ---------------------------------------------------------------------
    // DEVICE NAME FILTER (FINAL + WORKING)
    // ---------------------------------------------------------------------
    public LiveData<String> getDeviceNameFilter() {
        return deviceNameFilter;
    }

    public void setDeviceNameFilter(String name) {
        deviceNameFilter.setValue(name);

        // 🔥 THIS IS THE MISSING LINK
        mScannerRepository.setDeviceNameFilter(name);
    }

    public String getCurrentDeviceNameFilter() {
        return deviceNameFilter.getValue();
    }
}