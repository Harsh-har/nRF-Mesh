package no.nordicsemi.android.nrfmesh.node;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;
import no.nordicsemi.android.mesh.ApplicationKey;
import no.nordicsemi.android.mesh.models.GenericOnOffServerModel;
import no.nordicsemi.android.mesh.transport.Element;
import no.nordicsemi.android.mesh.transport.GenericOnOffGet;
import no.nordicsemi.android.mesh.transport.GenericOnOffSet;
import no.nordicsemi.android.mesh.transport.MeshMessage;
import no.nordicsemi.android.mesh.transport.MeshModel;
import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode;
import no.nordicsemi.android.mesh.utils.MeshAddress;
import no.nordicsemi.android.nrfmesh.R;
import no.nordicsemi.android.nrfmesh.databinding.LayoutGenericOnOffBinding;

@AndroidEntryPoint
public class GenericOnOffServerActivity extends ModelConfigurationActivity {

    private static final String TAG = "GENERIC_ON_OFF";

    private TextInputEditText etCommand;
    private TextInputEditText etParam;
    private Button mActionOnOff;
    private Button mActionRead;

    // Persistent TID for transaction, cycles 0–255
    private int mTid = 0;

    private int nextTid() {
        mTid = (mTid + 1) % 256;
        return mTid;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSwipe.setOnRefreshListener(this);

        final MeshModel model = mViewModel.getSelectedModel().getValue();
        Log.d(TAG, "onCreate → Selected model: " + (model != null ? model.getClass().getSimpleName() : "null"));

        if (!(model instanceof GenericOnOffServerModel)) {
            Log.w(TAG, "Selected model is not GenericOnOffServerModel");
            return;
        }

        final LayoutGenericOnOffBinding bindingControls =
                LayoutGenericOnOffBinding.inflate(getLayoutInflater(), binding.nodeControlsContainer, true);

        // ---------------- UI References ----------------
        etCommand = bindingControls.etCommand;
        etParam = bindingControls.etParam;

        mActionOnOff = bindingControls.actionOn;
        mActionRead = bindingControls.actionRead;

        setupButtons();
        observeModel();
    }

    // ---------------- BUTTON SETUP ----------------
    private void setupButtons() {
        // SEND button → execute command with parameter
        mActionOnOff.setOnClickListener(v -> {
            String command = etCommand.getText() != null ? etCommand.getText().toString().trim() : "";
            String paramStr = etParam.getText() != null ? etParam.getText().toString().trim() : "";
            int param = 0;

            try {
                if (!paramStr.isEmpty()) {
                    param = Integer.parseInt(paramStr);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid parameter: " + paramStr);
            }

            Log.d(TAG, "SEND button clicked → command=" + command + " param=" + param);

            sendGenericCommand(command, param);
        });

        // READ button → same as before
        mActionRead.setOnClickListener(v -> {
            Log.d(TAG, "READ button clicked");
            sendGenericOnOffGet();
        });
    }

    private void observeModel() {
        mViewModel.getSelectedModel().observe(this, meshModel -> {
            if (meshModel != null) {
                Log.d(TAG, "Model updated → updating UI");
                updateAppStatusUi(meshModel);
                updatePublicationUi(meshModel);
                updateSubscriptionUi(meshModel);
            }
        });
    }

    // ---------------- RECEIVE MESH MESSAGE ----------------
    @Override
    protected void updateMeshMessage(MeshMessage meshMessage) {
        super.updateMeshMessage(meshMessage);

        Log.d(TAG, "📥 MeshMessage received → " + meshMessage.getClass().getSimpleName());

        // Optional: handle GenericOnOffStatus if you still want ON/OFF feedback
        hideProgressBar();
    }

    // ---------------- SEND GET ----------------
    public void sendGenericOnOffGet() {
        if (!checkConnectivity(mContainer)) {
            Log.e(TAG, "❌ Connectivity failed → cannot send GET");
            return;
        }

        Element element = mViewModel.getSelectedElement().getValue();
        MeshModel model = mViewModel.getSelectedModel().getValue();

        if (element == null || model == null) {
            Log.e(TAG, "❌ Element or Model is NULL → cannot send GET");
            return;
        }

        if (model.getBoundAppKeyIndexes().isEmpty()) {
            Log.e(TAG, "❌ No AppKey bound → cannot send GET");
            return;
        }

        int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
        ApplicationKey appKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(appKeyIndex);
        int address = element.getElementAddress();

        Log.d(TAG, "📤 Sending GenericOnOffGet → address=" + MeshAddress.formatAddress(address, true));
        sendAcknowledgedMessage(address, new GenericOnOffGet(appKey));
    }

    // ---------------- SEND GENERIC COMMAND ----------------
    private void sendGenericCommand(String command, int param) {
        if (!checkConnectivity(mContainer)) {
            Log.e(TAG, "❌ Connectivity failed → cannot send command");
            return;
        }

        ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
        Element element = mViewModel.getSelectedElement().getValue();
        MeshModel model = mViewModel.getSelectedModel().getValue();

        if (node == null || element == null || model == null) {
            Log.e(TAG, "❌ Node / Element / Model NULL → cannot send command");
            return;
        }

        if (model.getBoundAppKeyIndexes().isEmpty()) {
            Log.e(TAG, "❌ No AppKey bound → cannot send command");
            return;
        }

        int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
        ApplicationKey appKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(appKeyIndex);
        int address = element.getElementAddress();

        int tid = nextTid();

        // Here we just use param as ON/OFF value for GenericOnOffSet as example
        boolean state = param != 0;

        Log.d(TAG, "📤 Sending GenericOnOffSet → address=" + MeshAddress.formatAddress(address, true)
                + " command=" + command
                + " parameter=" + param
                + " tid=" + tid
                + " state=" + (state ? "ON" : "OFF"));

        GenericOnOffSet set = new GenericOnOffSet(
                appKey,
                state,
                tid,
                null,  // no transition steps
                null,  // no resolution
                null   // no delay
        );

        sendAcknowledgedMessage(address, set);
    }
}
