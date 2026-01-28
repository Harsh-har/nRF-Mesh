

package no.nordicsemi.android.swaromesh.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.io.OutputStream;

import javax.inject.Inject;


import dagger.hilt.android.lifecycle.HiltViewModel;
import no.nordicsemi.android.swaromesh.GroupsFragment;
import no.nordicsemi.android.swaromesh.NetworkFragment;
import no.nordicsemi.android.swaromesh.ProxyFilterFragment;
import no.nordicsemi.android.swaromesh.SettingsFragment;
import no.nordicsemi.android.swaromesh.utils.NetworkExportUtils;

/**
 * ViewModel for {@link NetworkFragment}, {@link GroupsFragment}, {@link ProxyFilterFragment}, {@link SettingsFragment}
 */
@HiltViewModel
public class SharedViewModel extends BaseViewModel implements NetworkExportUtils.NetworkExportCallbacks {

    private final ScannerRepository mScannerRepository;
    private final SingleLiveEvent<String> networkExportState = new SingleLiveEvent<>();

    @Inject
    SharedViewModel(@NonNull final NrfMeshRepository nrfMeshRepository, @NonNull final ScannerRepository scannerRepository) {
        super(nrfMeshRepository);
        mScannerRepository = scannerRepository;
        scannerRepository.registerBroadcastReceivers();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mNrfMeshRepository.disconnect();
        mScannerRepository.unregisterBroadcastReceivers();
    }

    /**
     * Returns network load state
     */
    public LiveData<String> getNetworkLoadState() {
        return mNrfMeshRepository.getNetworkLoadState();
    }

    public LiveData<String> getNetworkExportState() {
        return networkExportState;
    }

    /**
     * Sets the selected group
     *
     * @param address Address of the group
     */
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
        networkExportState.postValue(getNetworkLiveData().getMeshNetwork().getMeshName() + " has been successfully exported.");
    }

    @Override
    public void onNetworkExportFailed(@NonNull final String error) {
        networkExportState.postValue(error);
    }
}
