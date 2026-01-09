package no.nordicsemi.android.nrfmesh;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dagger.hilt.android.AndroidEntryPoint;

import no.nordicsemi.android.mesh.ApplicationKey;
import no.nordicsemi.android.mesh.Group;
import no.nordicsemi.android.mesh.MeshNetwork;
import no.nordicsemi.android.mesh.models.SigModelParser;
import no.nordicsemi.android.mesh.models.VendorModel;
import no.nordicsemi.android.mesh.transport.Element;
import no.nordicsemi.android.mesh.transport.GenericLevelSetUnacknowledged;
import no.nordicsemi.android.mesh.transport.GenericOnOffSetUnacknowledged;
import no.nordicsemi.android.mesh.transport.MeshMessage;
import no.nordicsemi.android.mesh.transport.MeshModel;
import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode;
import no.nordicsemi.android.mesh.transport.VendorModelMessageAcked;
import no.nordicsemi.android.mesh.transport.VendorModelMessageStatus;
import no.nordicsemi.android.mesh.transport.VendorModelMessageUnacked;
import no.nordicsemi.android.mesh.utils.MeshAddress;
import no.nordicsemi.android.mesh.utils.MeshParserUtils;
import no.nordicsemi.android.nrfmesh.adapter.SubGroupAdapter;
import no.nordicsemi.android.nrfmesh.databinding.ActivityConfigGroupsBinding;
import no.nordicsemi.android.nrfmesh.dialog.DialogFragmentError;
import no.nordicsemi.android.nrfmesh.node.dialog.BottomSheetDetailsDialogFragment;
import no.nordicsemi.android.nrfmesh.node.dialog.BottomSheetLevelDialogFragment;
import no.nordicsemi.android.nrfmesh.node.dialog.BottomSheetOnOffDialogFragment;
import no.nordicsemi.android.nrfmesh.node.dialog.BottomSheetVendorDialogFragment;
import no.nordicsemi.android.nrfmesh.viewmodels.GroupControlsViewModel;

@AndroidEntryPoint
public class GroupControlsActivity extends AppCompatActivity implements
        SubGroupAdapter.OnItemClickListener,
        BottomSheetOnOffDialogFragment.BottomSheetOnOffListener,
        BottomSheetLevelDialogFragment.BottomSheetLevelListener,
        BottomSheetVendorDialogFragment.BottomSheetVendorModelControlsListener,
        BottomSheetDetailsDialogFragment.BottomSheetDetailsListener {

    private static final String ON_OFF_FRAGMENT = "ON_OFF_FRAGMENT";
    private static final String LEVEL_FRAGMENT = "LEVEL_FRAGMENT";
    private static final String VENDOR_FRAGMENT = "VENDOR_FRAGMENT";
    private static final String DETAILS_FRAGMENT = "DETAILS_FRAGMENT";

    private GroupControlsViewModel mViewModel;
    private SubGroupAdapter groupAdapter;
    private boolean mIsConnected;

    CoordinatorLayout container;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityConfigGroupsBinding binding =
                ActivityConfigGroupsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mViewModel = new ViewModelProvider(this)
                .get(GroupControlsViewModel.class);

        container = binding.container;
        setSupportActionBar(binding.toolbarInfo);
        Objects.requireNonNull(getSupportActionBar())
                .setDisplayHomeAsUpEnabled(true);

        final RecyclerView recyclerView = binding.recyclerViewGroupedModels;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        groupAdapter = new SubGroupAdapter(
                this,
                mViewModel.getNetworkLiveData().getMeshNetwork(),
                mViewModel.getSelectedGroup()
        );
        groupAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(groupAdapter);

        mViewModel.isConnectedToProxy().observe(this, connected -> {
            mIsConnected = connected;
            invalidateOptionsMenu();
        });
    }

    // ---------------------------------------------------------
    // GENERIC ON OFF (NO TRANSITION)
    // ---------------------------------------------------------
    @Override
    public void toggle(final int appKeyIndex,
                       final int modelId,
                       final boolean isChecked) {

        if (!isConnected()) return;

        final Group group = mViewModel.getSelectedGroup().getValue();
        if (group == null) return;

        final MeshNetwork network =
                mViewModel.getNetworkLiveData().getMeshNetwork();

        final ApplicationKey appKey =
                network.getAppKey(appKeyIndex);

        final int tid = new Random().nextInt(255);

        if (modelId == SigModelParser.GENERIC_ON_OFF_SERVER) {

            int command = 1; // 🔥 CUSTOM COMMAND (UI se bhi le sakte ho)

            MeshMessage message =
                    new GenericOnOffSetUnacknowledged(
                            appKey,
                            isChecked ? 1 : 0,
                            tid,
                            command
                    );

            sendMessage(group.getAddress(), message);
        }
    }

    // ---------------------------------------------------------
    // REQUIRED BY INTERFACE (NOT USED ANYMORE)
    // ---------------------------------------------------------
    @Override
    public void toggle(int keyIndex,
                       boolean state,
                       int transitionSteps,
                       int transitionStepResolution,
                       int delay) {
        // ❌ Transition based ON/OFF not supported anymore
        // Interface satisfy karne ke liye empty rakha
    }

    // ---------------------------------------------------------
    // GENERIC LEVEL (UNCHANGED)
    // ---------------------------------------------------------
    @Override
    public void toggleLevel(int keyIndex,
                            int state,
                            int command,
                            int transitionStepResolution,
                            int delay) {

        if (!isConnected()) return;

        final Group group = mViewModel.getSelectedGroup().getValue();
        if (group == null) return;

        final ApplicationKey appKey =
                mViewModel.getNetworkLiveData()
                        .getMeshNetwork()
                        .getAppKey(keyIndex);

        int tid = new Random().nextInt(255);

        MeshMessage message =
                new GenericLevelSetUnacknowledged(
                        appKey,
                        state,
                        tid,
                        command
                );

        sendMessage(group.getAddress(), message);
    }

    // ---------------------------------------------------------
    private void sendMessage(int address, MeshMessage message) {
        try {
            mViewModel.getMeshManagerApi()
                    .createMeshPdu(address, message);
        } catch (IllegalArgumentException ex) {
            DialogFragmentError
                    .newInstance("Error", ex.getMessage())
                    .show(getSupportFragmentManager(), null);
        }
    }

    private boolean isConnected() {
        if (!mIsConnected) {
            Snackbar.make(
                    container,
                    "Please connect to network",
                    Snackbar.LENGTH_SHORT
            ).show();
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------
    // Remaining interface methods (unchanged)
    // ---------------------------------------------------------
    @Override public void onSubGroupItemClick(int a, int b) {}
    @Override public void onModelItemClicked(@NonNull Element e, @NonNull MeshModel m) {}
    @Override public void onGroupNameChanged(@NonNull Group group) {}
    @Override public void sendVendorModelMessage(int a, int b, int c, byte[] d, boolean e) {}
}
