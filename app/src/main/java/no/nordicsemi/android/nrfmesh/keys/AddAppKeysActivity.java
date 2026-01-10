
package no.nordicsemi.android.nrfmesh.keys;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import no.nordicsemi.android.mesh.ApplicationKey;
import no.nordicsemi.android.mesh.NetworkKey;
import no.nordicsemi.android.mesh.NodeKey;
import no.nordicsemi.android.mesh.transport.ConfigAppKeyAdd;
import no.nordicsemi.android.mesh.transport.ConfigAppKeyDelete;
import no.nordicsemi.android.mesh.transport.ConfigAppKeyGet;
import no.nordicsemi.android.mesh.transport.MeshMessage;
import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode;
import no.nordicsemi.android.nrfmesh.R;
import no.nordicsemi.android.nrfmesh.keys.adapter.AddedAppKeyAdapter;
import no.nordicsemi.android.nrfmesh.viewmodels.AddKeysViewModel;

@AndroidEntryPoint
public class AddAppKeysActivity extends AddKeysActivity implements
        AddedAppKeyAdapter.OnItemClickListener {
    private AddedAppKeyAdapter adapter;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.title_added_app_keys);
        adapter = new AddedAppKeyAdapter(this,
                mViewModel.getNetworkLiveData().getMeshNetwork().getAppKeys(), mViewModel.getSelectedMeshNode());
        binding.recyclerViewKeys.setAdapter(adapter);
        adapter.setOnItemClickListener(this);
        updateClickableViews();
        setUpObserver();
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
    public void onRefresh() {
        super.onRefresh();
        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
        if (node != null) {
            for (NodeKey key : node.getAddedNetKeys()) {
                final NetworkKey networkKey = mViewModel.getNetworkLiveData().getMeshNetwork().getNetKey(key.getIndex());
                final ConfigAppKeyGet configAppKeyGet = new ConfigAppKeyGet(networkKey);
                mViewModel.getMessageQueue().add(configAppKeyGet);
            }
            sendMessage(mViewModel.getMessageQueue().peek());
        }
    }

    protected void setUpObserver() {
        mViewModel.getNetworkLiveData().observe(this, networkLiveData -> {
            if (networkLiveData != null) {
                final List<ApplicationKey> keys = networkLiveData.getAppKeys();
                if (keys != null) {
                    binding.emptyAppKeys.getRoot().setVisibility(keys.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    @Override
    void enableAdapterClickListener(final boolean enable) {
        adapter.enableDisableKeySelection(enable);
    }
}
