
package no.nordicsemi.android.swaromesh.keys;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import no.nordicsemi.android.swaromesh.ApplicationKey;
import no.nordicsemi.android.swaromesh.NetworkKey;
import no.nordicsemi.android.swaromesh.NodeKey;
import no.nordicsemi.android.swaromesh.transport.ConfigAppKeyAdd;
import no.nordicsemi.android.swaromesh.transport.ConfigAppKeyDelete;
import no.nordicsemi.android.swaromesh.transport.ConfigAppKeyGet;
import no.nordicsemi.android.swaromesh.transport.MeshMessage;
import no.nordicsemi.android.swaromesh.transport.ProvisionedMeshNode;
import no.nordicsemi.android.swaromesh.R;
import no.nordicsemi.android.swaromesh.keys.adapter.AddedAppKeyAdapter;
import no.nordicsemi.android.swaromesh.viewmodels.AddKeysViewModel;

@AndroidEntryPoint
public class AddAppKeysActivity extends AddKeysActivity implements
        AddedAppKeyAdapter.OnItemClickListener {
    private AddedAppKeyAdapter adapter;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set ActionBar title
//        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.title_added_app_keys);

        // Initialize adapter with current AppKeys and selected node
        adapter = new AddedAppKeyAdapter(
                this,
                mViewModel.getNetworkLiveData().getMeshNetwork().getAppKeys(),
                mViewModel.getSelectedMeshNode()
        );

        binding.recyclerViewKeys.setAdapter(adapter);
        adapter.setOnItemClickListener(this);
        updateClickableViews();

    }



    @Override
    public void onItemClick(@NonNull final ApplicationKey appKey) {
        if (!checkConnectivity(binding.container))
            return;
        final MeshMessage meshMessage;
        final String message;
        final NetworkKey networkKey = mViewModel.getNetworkLiveData().getMeshNetwork().getNetKey(appKey.getBoundNetKeyIndex());
        if (!((AddKeysViewModel) mViewModel).isAppKeyAdded(appKey.getKeyIndex())) {
            message = getString(R.string.adding_app_key);
            meshMessage = new ConfigAppKeyAdd(networkKey, appKey);
        } else {
            message = getString(R.string.deleting_app_key);
            meshMessage = new ConfigAppKeyDelete(networkKey, appKey);
        }
        mViewModel.displaySnackBar(this, binding.container, message, Snackbar.LENGTH_SHORT);
        sendMessage(meshMessage);
    }



    @Override
    void enableAdapterClickListener(final boolean enable) {

    }
}
