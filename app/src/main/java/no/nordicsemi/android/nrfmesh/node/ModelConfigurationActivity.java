package no.nordicsemi.android.nrfmesh.node;

import android.os.Bundle;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;
import no.nordicsemi.android.mesh.transport.*;
import no.nordicsemi.android.nrfmesh.R;

@AndroidEntryPoint
public abstract class ModelConfigurationActivity
        extends BaseModelConfigurationActivity {

    private static final String TAG = "MESH_FLOW";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "SCREEN → ModelConfigurationActivity CREATED");
    }

    @Override
    protected void updateMeshMessage(final MeshMessage meshMessage) {

        Log.d(TAG, "CONFIG RECEIVE → " +
                meshMessage.getClass().getSimpleName());

        if (meshMessage instanceof ConfigModelAppStatus) {
            ConfigModelAppStatus status = (ConfigModelAppStatus) meshMessage;

            Log.d(TAG, "CONFIG → AppKey status = " +
                    status.getStatusCodeName());

            if (status.isSuccessful()) {
                Snackbar.make(mContainer,
                        R.string.operation_success,
                        Snackbar.LENGTH_SHORT).show();
            }

        } else if (meshMessage instanceof ConfigModelPublicationStatus) {

            Log.d(TAG, "CONFIG → Publication updated");

        } else if (meshMessage instanceof ConfigModelSubscriptionStatus) {

            Log.d(TAG, "CONFIG → Subscription updated");

        } else if (meshMessage instanceof ConfigSigModelAppList ||
                meshMessage instanceof ConfigSigModelSubscriptionList) {

            Log.d(TAG, "CONFIG → List received");
            handleStatuses();
        }
    }
}
