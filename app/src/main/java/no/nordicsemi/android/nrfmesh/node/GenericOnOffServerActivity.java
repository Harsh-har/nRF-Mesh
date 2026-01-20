////package no.nordicsemi.android.nrfmesh.node;
////
////import android.os.Bundle;
////import android.util.Log;
////import android.view.View;
////import android.widget.Button;
////import android.widget.TextView;
////
////import com.google.android.material.slider.Slider;
////import com.google.android.material.snackbar.Snackbar;
////
////import java.util.Random;
////
////import androidx.annotation.NonNull;
////import dagger.hilt.android.AndroidEntryPoint;
////import no.nordicsemi.android.mesh.ApplicationKey;
////import no.nordicsemi.android.mesh.models.GenericOnOffServerModel;
////import no.nordicsemi.android.mesh.transport.Element;
////import no.nordicsemi.android.mesh.transport.GenericOnOffGet;
////import no.nordicsemi.android.mesh.transport.GenericOnOffSet;
////import no.nordicsemi.android.mesh.transport.GenericOnOffStatus;
////import no.nordicsemi.android.mesh.transport.MeshMessage;
////import no.nordicsemi.android.mesh.transport.MeshModel;
////import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode;
////import no.nordicsemi.android.mesh.utils.MeshAddress;
////import no.nordicsemi.android.mesh.utils.MeshParserUtils;
////import no.nordicsemi.android.nrfmesh.R;
////import no.nordicsemi.android.nrfmesh.databinding.LayoutGenericOnOffBinding;
////
////@AndroidEntryPoint
////public class GenericOnOffServerActivity extends ModelConfigurationActivity {
////
////    private static final String TAG = GenericOnOffServerActivity.class.getSimpleName();
////
////    private TextView onOffState;
////    private TextView remainingTime;
////    private Button mActionOnOff;
////    protected int mTransitionStepResolution;
////    protected int mTransitionSteps;
////
////    @Override
////    protected void onCreate(Bundle savedInstanceState) {
////        super.onCreate(savedInstanceState);
////        mSwipe.setOnRefreshListener(this);
////        final MeshModel model = mViewModel.getSelectedModel().getValue();
////        if (model instanceof GenericOnOffServerModel) {
////            final LayoutGenericOnOffBinding nodeControlsContainer = LayoutGenericOnOffBinding.inflate(getLayoutInflater(), binding.nodeControlsContainer, true);
////            final TextView time = nodeControlsContainer.transitionTime;
////            onOffState = nodeControlsContainer.onOffState;
////            remainingTime = nodeControlsContainer.transitionState;
////            final Slider transitionTimeSlider = nodeControlsContainer.transitionSlider;
////            transitionTimeSlider.setValueFrom(0);
////            transitionTimeSlider.setValueTo(230);
////            transitionTimeSlider.setValue(0);
////            transitionTimeSlider.setStepSize(1);
////
////            final Slider delaySlider = nodeControlsContainer.delaySlider;
////            delaySlider.setValueFrom(0);
////            delaySlider.setValueTo(255);
////            delaySlider.setValue(0);
////            delaySlider.setStepSize(1);
////            final TextView delayTime = nodeControlsContainer.delayTime;
////
////            mActionOnOff = nodeControlsContainer.actionOn;
////            mActionOnOff.setOnClickListener(v -> {
////                try {
////                    // Updated: Removed delay parameter since it's ignored in new implementation
////                    sendGenericOnOff(mActionOnOff.getText().toString().equals(getString(R.string.action_generic_on)));
////                } catch (IllegalArgumentException ex) {
////                    mViewModel.displaySnackBar(this, mContainer, ex.getMessage(), Snackbar.LENGTH_LONG);
////                }
////            });
////
////            mActionRead = nodeControlsContainer.actionRead;
////            mActionRead.setOnClickListener(v -> sendGenericOnOffGet());
////
////            transitionTimeSlider.addOnChangeListener(new Slider.OnChangeListener() {
////                int lastValue = 0;
////                double res = 0.0;
////
////                @Override
////                public void onValueChange(@NonNull final Slider slider, final float value, final boolean fromUser) {
////                    final int progress = (int) value;
////                    if (progress >= 0 && progress <= 62) {
////                        lastValue = progress;
////                        mTransitionStepResolution = 0;
////                        mTransitionSteps = progress;
////                        res = progress / 10.0;
////                        time.setText(getString(R.string.transition_time_interval, String.valueOf(res), "s"));
////                    } else if (progress >= 63 && progress <= 118) {
////                        if (progress > lastValue) {
////                            mTransitionSteps = progress - 56;
////                            lastValue = progress;
////                        } else if (progress < lastValue) {
////                            mTransitionSteps = -(56 - progress);
////                        }
////                        mTransitionStepResolution = 1;
////                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps), "s"));
////
////                    } else if (progress >= 119 && progress <= 174) {
////                        if (progress > lastValue) {
////                            mTransitionSteps = progress - 112;
////                            lastValue = progress;
////                        } else if (progress < lastValue) {
////                            mTransitionSteps = -(112 - progress);
////                        }
////                        mTransitionStepResolution = 2;
////                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps * 10), "s"));
////                    } else if (progress >= 175 && progress <= 230) {
////                        if (progress >= lastValue) {
////                            mTransitionSteps = progress - 168;
////                            lastValue = progress;
////                        } else {
////                            mTransitionSteps = -(168 - progress);
////                        }
////                        mTransitionStepResolution = 3;
////                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps * 10), "min"));
////                    }
////                }
////            });
////            delaySlider.addOnChangeListener((slider, value, fromUser) -> delayTime.setText(getString(R.string.transition_time_interval, String.valueOf((int) value * MeshParserUtils.GENERIC_ON_OFF_5_MS), "ms")));
////
////            mViewModel.getSelectedModel().observe(this, meshModel -> {
////                if (meshModel != null) {
////                    updateAppStatusUi(meshModel);
////                    updatePublicationUi(meshModel);
////                    updateSubscriptionUi(meshModel);
////                }
////            });
////        }
////    }
////
////    @Override
////    protected void enableClickableViews() {
////        super.enableClickableViews();
////        if (mActionOnOff != null && !mActionOnOff.isEnabled())
////            mActionOnOff.setEnabled(true);
////    }
////
////    @Override
////    protected void disableClickableViews() {
////        super.disableClickableViews();
////        if (mActionOnOff != null)
////            mActionOnOff.setEnabled(false);
////    }
////
////    @Override
////    public void onRefresh() {
////        super.onRefresh();
////    }
////
////    @Override
////    protected void updateMeshMessage(final MeshMessage meshMessage) {
////        super.updateMeshMessage(meshMessage);
////        mSwipe.setOnRefreshListener(this);
////        if (meshMessage instanceof GenericOnOffStatus) {
////            final GenericOnOffStatus status = (GenericOnOffStatus) meshMessage;
////            final boolean presentState = status.getPresentState();
////            final Boolean targetOnOff = status.getTargetState();
////            final int steps = status.getTransitionSteps();
////            final int resolution = status.getTransitionResolution();
////            if (targetOnOff == null) {
////                if (presentState) {
////                    onOffState.setText(R.string.generic_state_on);
////                    mActionOnOff.setText(R.string.action_generic_off);
////                } else {
////                    onOffState.setText(R.string.generic_state_off);
////                    mActionOnOff.setText(R.string.action_generic_on);
////                }
////                remainingTime.setVisibility(View.GONE);
////            } else {
////                if (!targetOnOff) {
////                    onOffState.setText(R.string.generic_state_on);
////                    mActionOnOff.setText(R.string.action_generic_off);
////                } else {
////                    onOffState.setText(R.string.generic_state_off);
////                    mActionOnOff.setText(R.string.action_generic_on);
////                }
////                remainingTime.setText(getString(R.string.remaining_time, MeshParserUtils.getRemainingTransitionTime(resolution, steps)));
////                remainingTime.setVisibility(View.VISIBLE);
////            }
////        }
////        hideProgressBar();
////    }
////
////    /**
////     * Send generic on off get to mesh node
////     */
////    public void sendGenericOnOffGet() {
////        if (!checkConnectivity(mContainer)) return;
////        final Element element = mViewModel.getSelectedElement().getValue();
////        if (element != null) {
////            final MeshModel model = mViewModel.getSelectedModel().getValue();
////            if (model != null) {
////                if (!model.getBoundAppKeyIndexes().isEmpty()) {
////                    final int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
////                    final ApplicationKey appKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(appKeyIndex);
////
////                    final int address = element.getElementAddress();
////                    Log.v(TAG, "Sending message to element's unicast address: " + MeshAddress.formatAddress(address, true));
////
////                    final GenericOnOffGet genericOnOffSet = new GenericOnOffGet(appKey);
////                    sendAcknowledgedMessage(address, genericOnOffSet);
////                } else {
////                    mViewModel.displaySnackBar(this, mContainer, getString(R.string.error_no_app_keys_bound), Snackbar.LENGTH_LONG);
////                }
////            }
////        }
////    }
////
////    /**
////     * Send generic on off set to mesh node (UPDATED for new constructor)
////     *
////     * @param state true to turn on and false to turn off
////     */
////    public void sendGenericOnOff(final boolean state) {
////        if (!checkConnectivity(mContainer)) return;
////        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
////        if (node != null) {
////            final Element element = mViewModel.getSelectedElement().getValue();
////            if (element != null) {
////                final MeshModel model = mViewModel.getSelectedModel().getValue();
////                if (model != null) {
////                    if (!model.getBoundAppKeyIndexes().isEmpty()) {
////                        final int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
////                        final ApplicationKey appKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(appKeyIndex);
////                        final int address = element.getElementAddress();
////
////                        // Updated: Using new constructor with 4 parameters
////                        // GenericOnOffSet(ApplicationKey appKey, int command, int tid, int state)
////                        final int tid = new Random().nextInt(256); // TID must be 0-255
////                        final int command = 1; // Default command for on/off
////                        final int stateValue = state ? 1 : 0; // Convert boolean to 1/0
////
////                        final GenericOnOffSet genericOnOffSet = new GenericOnOffSet(appKey, command, tid, stateValue);
////
////                        // Alternative using Builder pattern:
////                        // final GenericOnOffSet genericOnOffSet = new GenericOnOffSet.Builder(appKey)
////                        //         .withCommand(command)
////                        //         .withTid(tid)
////                        //         .withState(stateValue)
////                        //         .build();
////
////                        sendAcknowledgedMessage(address, genericOnOffSet);
////
////                        // Log warning about ignored transition parameters
////                        if (mTransitionSteps != 0 || mTransitionStepResolution != 0) {
////                            Log.w(TAG, "Transition parameters are ignored in new implementation");
////                        }
////
////                    } else {
////                        mViewModel.displaySnackBar(this, mContainer, getString(R.string.error_no_app_keys_bound), Snackbar.LENGTH_LONG);
////                    }
////                }
////            }
////        }
////    }
////}
//
//
//package no.nordicsemi.android.nrfmesh.node;
//
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//
//import com.google.android.material.slider.Slider;
//import com.google.android.material.snackbar.Snackbar;
//
//import java.util.Random;
//
//import androidx.annotation.NonNull;
//import dagger.hilt.android.AndroidEntryPoint;
//import no.nordicsemi.android.mesh.ApplicationKey;
//import no.nordicsemi.android.mesh.models.GenericOnOffServerModel;
//import no.nordicsemi.android.mesh.transport.Element;
//import no.nordicsemi.android.mesh.transport.GenericOnOffGet;
//import no.nordicsemi.android.mesh.transport.GenericOnOffSet;
//import no.nordicsemi.android.mesh.transport.GenericOnOffStatus;
//import no.nordicsemi.android.mesh.transport.MeshMessage;
//import no.nordicsemi.android.mesh.transport.MeshModel;
//import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode;
//import no.nordicsemi.android.mesh.utils.MeshAddress;
//import no.nordicsemi.android.mesh.utils.MeshParserUtils;
//import no.nordicsemi.android.nrfmesh.R;
//import no.nordicsemi.android.nrfmesh.databinding.LayoutGenericOnOffBinding;
//
//@AndroidEntryPoint
//public class GenericOnOffServerActivity extends ModelConfigurationActivity {
//
//    private static final String TAG = GenericOnOffServerActivity.class.getSimpleName();
//
//    private TextView onOffState;
//    private TextView remainingTime;
//    private Button mActionOnOff;
//
//    // Consider removing these since they're not used in new implementation
//    protected int mTransitionStepResolution;
//    protected int mTransitionSteps;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        mSwipe.setOnRefreshListener(this);
//        final MeshModel model = mViewModel.getSelectedModel().getValue();
//        if (model instanceof GenericOnOffServerModel) {
//            final LayoutGenericOnOffBinding nodeControlsContainer = LayoutGenericOnOffBinding.inflate(getLayoutInflater(), binding.nodeControlsContainer, true);
//            final TextView time = nodeControlsContainer.transitionTime;
//            onOffState = nodeControlsContainer.onOffState;
//            remainingTime = nodeControlsContainer.transitionState;
//
//            // Consider removing or disabling these sliders since they're not used
//            final Slider transitionTimeSlider = nodeControlsContainer.transitionSlider;
//            transitionTimeSlider.setValueFrom(0);
//            transitionTimeSlider.setValueTo(230);
//            transitionTimeSlider.setValue(0);
//            transitionTimeSlider.setStepSize(1);
//            // Disable transition slider since it's not used
//            transitionTimeSlider.setEnabled(false);
//
//            final Slider delaySlider = nodeControlsContainer.delaySlider;
//            delaySlider.setValueFrom(0);
//            delaySlider.setValueTo(255);
//            delaySlider.setValue(0);
//            delaySlider.setStepSize(1);
//            final TextView delayTime = nodeControlsContainer.delayTime;
//            // Disable delay slider since it's not used
//            delaySlider.setEnabled(false);
//
//            mActionOnOff = nodeControlsContainer.actionOn;
//            mActionOnOff.setOnClickListener(v -> {
//                try {
//                    // Determine action based on current button text
//                    boolean turnOn = mActionOnOff.getText().toString().equals(getString(R.string.action_generic_on));
//                    sendGenericOnOff(turnOn);
//                } catch (IllegalArgumentException ex) {
//                    mViewModel.displaySnackBar(this, mContainer, ex.getMessage(), Snackbar.LENGTH_LONG);
//                }
//            });
//
//            mActionRead = nodeControlsContainer.actionRead;
//            mActionRead.setOnClickListener(v -> sendGenericOnOffGet());
//
//            // Remove or comment out transition slider listener since it's not used
//            /*
//            transitionTimeSlider.addOnChangeListener(new Slider.OnChangeListener() {
//                int lastValue = 0;
//                double res = 0.0;
//
//                @Override
//                public void onValueChange(@NonNull final Slider slider, final float value, final boolean fromUser) {
//                    final int progress = (int) value;
//                    // ... removed for brevity ...
//                }
//            });
//            */
//
//            // Remove or comment out delay slider listener since it's not used
//            /*
//            delaySlider.addOnChangeListener((slider, value, fromUser) ->
//                delayTime.setText(getString(R.string.transition_time_interval,
//                    String.valueOf((int) value * MeshParserUtils.GENERIC_ON_OFF_5_MS), "ms"))
//            );
//            */
//
//            mViewModel.getSelectedModel().observe(this, meshModel -> {
//                if (meshModel != null) {
//                    updateAppStatusUi(meshModel);
//                    updatePublicationUi(meshModel);
//                    updateSubscriptionUi(meshModel);
//                }
//            });
//        }
//    }
//
//    @Override
//    protected void enableClickableViews() {
//        super.enableClickableViews();
//        if (mActionOnOff != null && !mActionOnOff.isEnabled())
//            mActionOnOff.setEnabled(true);
//    }
//
//    @Override
//    protected void disableClickableViews() {
//        super.disableClickableViews();
//        if (mActionOnOff != null)
//            mActionOnOff.setEnabled(false);
//    }
//
//    @Override
//    public void onRefresh() {
//        super.onRefresh();
//    }
//
//    @Override
//    protected void updateMeshMessage(final MeshMessage meshMessage) {
//        super.updateMeshMessage(meshMessage);
//        mSwipe.setOnRefreshListener(this);
//        if (meshMessage instanceof GenericOnOffStatus) {
//            final GenericOnOffStatus status = (GenericOnOffStatus) meshMessage;
//            final boolean presentState = status.getPresentState();
//            final Boolean targetOnOff = status.getTargetState();
//            final int steps = status.getTransitionSteps();
//            final int resolution = status.getTransitionResolution();
//
//            if (targetOnOff == null) {
//                if (presentState) {
//                    onOffState.setText(R.string.generic_state_on);
//                    mActionOnOff.setText(R.string.action_generic_off); // Show "OFF" when device is ON
//                } else {
//                    onOffState.setText(R.string.generic_state_off);
//                    mActionOnOff.setText(R.string.action_generic_on); // Show "ON" when device is OFF
//                }
//                remainingTime.setVisibility(View.GONE);
//            } else {
//                if (!targetOnOff) {
//                    onOffState.setText(R.string.generic_state_on);
//                    mActionOnOff.setText(R.string.action_generic_off);
//                } else {
//                    onOffState.setText(R.string.generic_state_off);
//                    mActionOnOff.setText(R.string.action_generic_on);
//                }
//                remainingTime.setText(getString(R.string.remaining_time,
//                        MeshParserUtils.getRemainingTransitionTime(resolution, steps)));
//                remainingTime.setVisibility(View.VISIBLE);
//            }
//        }
//        hideProgressBar();
//    }
//
//    /**
//     * Send generic on off get to mesh node
//     */
//    public void sendGenericOnOffGet() {
//        if (!checkConnectivity(mContainer)) return;
//        final Element element = mViewModel.getSelectedElement().getValue();
//        if (element != null) {
//            final MeshModel model = mViewModel.getSelectedModel().getValue();
//            if (model != null) {
//                if (!model.getBoundAppKeyIndexes().isEmpty()) {
//                    final int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
//                    final ApplicationKey appKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(appKeyIndex);
//
//                    final int address = element.getElementAddress();
//                    Log.v(TAG, "Sending message to element's unicast address: " +
//                            MeshAddress.formatAddress(address, true));
//
//                    final GenericOnOffGet genericOnOffSet = new GenericOnOffGet(appKey);
//                    sendAcknowledgedMessage(address, genericOnOffSet);
//                } else {
//                    mViewModel.displaySnackBar(this, mContainer,
//                            getString(R.string.error_no_app_keys_bound), Snackbar.LENGTH_LONG);
//                }
//            }
//        }
//    }
//
//    /**
//     * Send generic on off set to mesh node (UPDATED for new constructor)
//     *
//     * @param turnOn true to turn on, false to turn off
//     */
//    public void sendGenericOnOff(final boolean turnOn) {
//        if (!checkConnectivity(mContainer)) return;
//        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
//        if (node != null) {
//            final Element element = mViewModel.getSelectedElement().getValue();
//            if (element != null) {
//                final MeshModel model = mViewModel.getSelectedModel().getValue();
//                if (model != null) {
//                    if (!model.getBoundAppKeyIndexes().isEmpty()) {
//                        final int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
//                        final ApplicationKey appKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(appKeyIndex);
//                        final int address = element.getElementAddress();
//
//                        // Updated: Using new constructor with 4 parameters
//                        // GenericOnOffSet(ApplicationKey appKey, int command, int tid, int state)
//                        final int tid = new Random().nextInt(256); // TID must be 0-255
//                        final int command = 1; // Default command for on/off
//                        final int stateValue = turnOn ? 1 : 0; // Convert boolean to 1/0
//
////                        final GenericOnOffSet genericOnOffSet = new GenericOnOffSet(appKey, command, tid, stateValue);
//
//                        // Alternative using Builder pattern (recommended for clarity):
//                        // final GenericOnOffSet genericOnOffSet = new GenericOnOffSet.Builder(appKey)
//                        //         .withCommand(command)
//                        //         .withTid(tid)
//                        //         .withState(stateValue)
//                        //         .build();
//
////                        sendAcknowledgedMessage(address, genericOnOffSet);
//
//                        // Log warning about ignored transition parameters (only once to avoid spam)
//                        if (mTransitionSteps != 0 || mTransitionStepResolution != 0) {
//                            Log.w(TAG, "Transition parameters are ignored in new implementation");
//                            // Reset to avoid repeated warnings
//                            mTransitionSteps = 0;
//                            mTransitionStepResolution = 0;
//                        }
//
//                    } else {
//                        mViewModel.displaySnackBar(this, mContainer,
//                                getString(R.string.error_no_app_keys_bound), Snackbar.LENGTH_LONG);
//                    }
//                }
//            }
//        }
//    }
//}