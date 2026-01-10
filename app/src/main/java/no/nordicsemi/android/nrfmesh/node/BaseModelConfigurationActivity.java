
package no.nordicsemi.android.nrfmesh.node;

import static no.nordicsemi.android.mesh.data.ScheduleEntry.Hour.Random;
import static no.nordicsemi.android.mesh.utils.MeshAddress.formatAddress;
import static no.nordicsemi.android.mesh.utils.MeshAddress.isValidGroupAddress;
import static no.nordicsemi.android.mesh.utils.MeshAddress.isValidVirtualAddress;
import static no.nordicsemi.android.nrfmesh.utils.Utils.BIND_APP_KEY;
import static no.nordicsemi.android.nrfmesh.utils.Utils.EXTRA_DATA;
import static no.nordicsemi.android.nrfmesh.utils.Utils.MESSAGE_TIME_OUT;
import static no.nordicsemi.android.nrfmesh.utils.Utils.RESULT_KEY;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import no.nordicsemi.android.mesh.ApplicationKey;
import no.nordicsemi.android.mesh.Group;
import no.nordicsemi.android.mesh.MeshNetwork;
import no.nordicsemi.android.mesh.models.ConfigurationClientModel;
import no.nordicsemi.android.mesh.models.ConfigurationServerModel;
import no.nordicsemi.android.mesh.models.SigModel;
import no.nordicsemi.android.mesh.models.SigModelParser;
import no.nordicsemi.android.mesh.transport.ConfigModelAppBind;
import no.nordicsemi.android.mesh.transport.ConfigModelAppUnbind;
import no.nordicsemi.android.mesh.transport.ConfigModelPublicationGet;
import no.nordicsemi.android.mesh.transport.ConfigModelPublicationSet;
import no.nordicsemi.android.mesh.transport.ConfigModelSubscriptionAdd;
import no.nordicsemi.android.mesh.transport.ConfigModelSubscriptionDelete;
import no.nordicsemi.android.mesh.transport.ConfigModelSubscriptionVirtualAddressAdd;
import no.nordicsemi.android.mesh.transport.ConfigModelSubscriptionVirtualAddressDelete;
import no.nordicsemi.android.mesh.transport.ConfigSigModelAppGet;
import no.nordicsemi.android.mesh.transport.ConfigSigModelSubscriptionGet;
import no.nordicsemi.android.mesh.transport.ConfigVendorModelAppGet;
import no.nordicsemi.android.mesh.transport.ConfigVendorModelSubscriptionGet;
import no.nordicsemi.android.mesh.transport.Element;
import no.nordicsemi.android.mesh.transport.GenericOnOffSet;
import no.nordicsemi.android.mesh.transport.MeshMessage;
import no.nordicsemi.android.mesh.transport.MeshModel;
import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode;
import no.nordicsemi.android.mesh.transport.PublicationSettings;
import no.nordicsemi.android.mesh.utils.CompositionDataParser;
import no.nordicsemi.android.nrfmesh.GroupCallbacks;
import no.nordicsemi.android.nrfmesh.R;
import java.util.Random;
import java.util.Random;

import no.nordicsemi.android.nrfmesh.adapter.GroupAddressAdapter;
import no.nordicsemi.android.nrfmesh.databinding.ActivityModelConfigurationBinding;
import no.nordicsemi.android.nrfmesh.dialog.DialogFragmentConfigStatus;
import no.nordicsemi.android.nrfmesh.dialog.DialogFragmentDisconnected;
import no.nordicsemi.android.nrfmesh.dialog.DialogFragmentError;
import no.nordicsemi.android.nrfmesh.dialog.DialogFragmentGroupSubscription;
import no.nordicsemi.android.nrfmesh.dialog.DialogFragmentTransactionStatus;
import no.nordicsemi.android.nrfmesh.keys.AppKeysActivity;
import no.nordicsemi.android.nrfmesh.keys.adapter.BoundAppKeysAdapter;
import no.nordicsemi.android.nrfmesh.viewmodels.BaseActivity;
import no.nordicsemi.android.nrfmesh.viewmodels.ModelConfigurationViewModel;
import no.nordicsemi.android.nrfmesh.widgets.ItemTouchHelperAdapter;
import no.nordicsemi.android.nrfmesh.widgets.RemovableItemTouchHelperCallback;
import no.nordicsemi.android.nrfmesh.widgets.RemovableViewHolder;

public abstract class BaseModelConfigurationActivity extends BaseActivity implements
        GroupCallbacks,
        ItemTouchHelperAdapter,
        DialogFragmentDisconnected.DialogFragmentDisconnectedListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String DIALOG_FRAGMENT_CONFIGURATION_STATUS = "DIALOG_FRAGMENT_CONFIGURATION_STATUS";
    private static final String PROGRESS_BAR_STATE = "PROGRESS_BAR_STATE";
    protected ActivityModelConfigurationBinding binding;

    CoordinatorLayout mContainer;
    View mContainerAppKeyBinding;
    Button mActionBindAppKey;
    TextView mAppKeyView;
    TextView mUnbindHint;
    View mContainerPublication;
    Button mActionSetPublication;
    Button mActionClearPublication;
    TextView mPublishAddressView;
    View mContainerSubscribe;
    Button mActionSubscribe;
    TextView mSubscribeAddressView;
    TextView mSubscribeHint;
    ProgressBar mProgressbar;
    SwipeRefreshLayout mSwipe;

    protected List<Integer> mGroupAddress = new ArrayList<>();
    protected List<Integer> mKeyIndexes = new ArrayList<>();
    protected GroupAddressAdapter mSubscriptionAdapter;
    protected BoundAppKeysAdapter mBoundAppKeyAdapter;
    protected Button mActionRead;
    protected Button mActionSetRelayState;
    protected Button mSendButton;
    protected TextInputEditText mCommandEditText;
    protected TextInputEditText mStateEditText;

    protected Button mSetNetworkTransmitStateButton;

    private RecyclerView recyclerViewBoundKeys, recyclerViewSubscriptions;

    private final ActivityResultLauncher<Intent> appKeySelector = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    final ApplicationKey appKey = result.getData().getParcelableExtra(RESULT_KEY);
                    if (appKey != null) {
                        bindAppKey(appKey.getKeyIndex());
                    }
                }
            });

    private final ActivityResultLauncher<Intent> publicationSettings = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    final ApplicationKey appKey = result.getData().getParcelableExtra(RESULT_KEY);
                    if (appKey != null) {
                        bindAppKey(appKey.getKeyIndex());
                    }
                }
            });

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityModelConfigurationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mContainer = binding.container;
        mContainerAppKeyBinding = binding.appKeyCard;
        mActionBindAppKey = binding.actionBindAppKey;
        mAppKeyView = binding.boundKeys;
        mUnbindHint = binding.unbindHint;
        mContainerPublication = binding.publishAddressCard;
        mActionSetPublication = binding.actionSetPublication;
        mActionClearPublication = binding.actionClearPublication;
        mPublishAddressView = binding.publishAddress;
        mContainerSubscribe = binding.subscriptionAddressCard;
        mActionSubscribe = binding.actionSubscribeAddress;
        mSubscribeAddressView = binding.subscribeAddresses;
        mSubscribeHint = binding.subscribeHint;
        mProgressbar = binding.configurationProgressBar;
        mSwipe = binding.swipeRefresh;

        // Node controls references
        mSendButton = binding.actionOn; // SEND button
        mCommandEditText = binding.etCommand; // Command input
        mStateEditText = binding.etState;    // State input


        mViewModel = new ViewModelProvider(this).get(ModelConfigurationViewModel.class);
        initialize();
        final MeshModel meshModel = mViewModel.getSelectedModel().getValue();
        if (meshModel != null) {
            setSupportActionBar(binding.toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(meshModel.getModelName());
            }

            final int modelId = meshModel.getModelId();
            getSupportActionBar().setSubtitle(getString(R.string.model_id, CompositionDataParser.formatModelIdentifier(modelId, true)));

            recyclerViewSubscriptions = findViewById(R.id.recycler_view_subscriptions);
            recyclerViewSubscriptions.setLayoutManager(new LinearLayoutManager(this));
            final ItemTouchHelper.Callback itemTouchHelperCallback = new RemovableItemTouchHelperCallback(this);
            final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
            itemTouchHelper.attachToRecyclerView(recyclerViewSubscriptions);
            mSubscriptionAdapter = new GroupAddressAdapter(this, mViewModel.getNetworkLiveData().getMeshNetwork(), mViewModel.getSelectedModel());
            recyclerViewSubscriptions.setAdapter(mSubscriptionAdapter);

            recyclerViewBoundKeys = findViewById(R.id.recycler_view_bound_keys);
            recyclerViewBoundKeys.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewBoundKeys.setItemAnimator(null);
            final ItemTouchHelper.Callback itemTouchHelperCallbackKeys = new RemovableItemTouchHelperCallback(this);
            final ItemTouchHelper itemTouchHelperKeys = new ItemTouchHelper(itemTouchHelperCallbackKeys);
            itemTouchHelperKeys.attachToRecyclerView(recyclerViewBoundKeys);
            mBoundAppKeyAdapter = new BoundAppKeysAdapter(this, mViewModel.getNetworkLiveData().getAppKeys(), mViewModel.getSelectedModel());
            recyclerViewBoundKeys.setAdapter(mBoundAppKeyAdapter);

            mActionBindAppKey.setOnClickListener(v -> {
                final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
                if (node != null && !node.isExist(SigModelParser.CONFIGURATION_SERVER)) {
                    return;
                }
                if (!checkConnectivity(mContainer)) return;
                final Intent bindAppKeysIntent = new Intent(BaseModelConfigurationActivity.this, AppKeysActivity.class);
                bindAppKeysIntent.putExtra(EXTRA_DATA, BIND_APP_KEY);
                appKeySelector.launch(bindAppKeysIntent);
            });
            mSendButton.setOnClickListener(v -> { sendGenericOnOffCommand(); });

            mPublishAddressView.setText(R.string.none);
            mActionSetPublication.setOnClickListener(v -> navigateToPublication());

            mActionClearPublication.setOnClickListener(v -> clearPublication());

            mActionSubscribe.setOnClickListener(v -> {
                if (!checkConnectivity(mContainer)) return;
                final ArrayList<Group> groups = new ArrayList<>(mViewModel.getNetworkLiveData().getMeshNetwork().getGroups());
                final DialogFragmentGroupSubscription fragmentSubscriptionAddress = DialogFragmentGroupSubscription.newInstance(groups);
                fragmentSubscriptionAddress.show(getSupportFragmentManager(), null);
            });

            mViewModel.getTransactionStatus().observe(this, transactionStatus -> {
                if (transactionStatus != null) {
                    hideProgressBar();
                    final String message = getString(R.string.operation_timed_out);
                    DialogFragmentTransactionStatus fragmentMessage = DialogFragmentTransactionStatus.newInstance("Transaction Failed", message);
                    fragmentMessage.show(getSupportFragmentManager(), null);
                }
            });
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        mViewModel.setActivityVisible(true);
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PROGRESS_BAR_STATE, mProgressbar.getVisibility() == View.VISIBLE);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.getBoolean(PROGRESS_BAR_STATE)) {
            mProgressbar.setVisibility(View.VISIBLE);
            disableClickableViews();
        } else {
            mProgressbar.setVisibility(View.INVISIBLE);
            enableClickableViews();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mViewModel.setActivityVisible(false);
        if (isFinishing()) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public Group createGroup(@NonNull final String name) {
        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        return network.createGroup(network.getSelectedProvisioner(), name);
    }

    @Override
    public Group createGroup(@NonNull final UUID uuid, final String name) {
        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        return network.createGroup(uuid, null, name);
    }

    @Override
    public boolean onGroupAdded(@NonNull final String name, final int address) {
        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        final Group group = network.createGroup(network.getSelectedProvisioner(), address, name);
        if (network.addGroup(group)) {
            subscribe(group);
            return true;
        }
        return false;
    }

    @Override
    public boolean onGroupAdded(@NonNull final Group group) {
        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        if (network.addGroup(group)) {
            subscribe(group);
            return true;
        }
        return false;
    }

    @Override
    public void subscribe(final Group group) {
        final ProvisionedMeshNode meshNode = mViewModel.getSelectedMeshNode().getValue();
        if (meshNode != null) {
            final Element element = mViewModel.getSelectedElement().getValue();
            if (element != null) {
                final int elementAddress = element.getElementAddress();
                final MeshModel model = mViewModel.getSelectedModel().getValue();
                if (model != null) {
                    final int modelIdentifier = model.getModelId();
                    final MeshMessage configModelSubscriptionAdd;
                    if (group.getAddressLabel() == null) {
                        configModelSubscriptionAdd = new ConfigModelSubscriptionAdd(elementAddress, group.getAddress(), modelIdentifier);
                    } else {
                        configModelSubscriptionAdd = new ConfigModelSubscriptionVirtualAddressAdd(elementAddress, group.getAddressLabel(), modelIdentifier);
                    }
                    sendAcknowledgedMessage(meshNode.getUnicastAddress(), configModelSubscriptionAdd);
                }
            }
        }
    }

    @Override
    public void subscribe(final int address) {
        final ProvisionedMeshNode meshNode = mViewModel.getSelectedMeshNode().getValue();
        if (meshNode != null) {
            final Element element = mViewModel.getSelectedElement().getValue();
            if (element != null) {
                final int elementAddress = element.getElementAddress();
                final MeshModel model = mViewModel.getSelectedModel().getValue();
                if (model != null) {
                    final int modelIdentifier = model.getModelId();
                    sendAcknowledgedMessage(meshNode.getUnicastAddress(), new ConfigModelSubscriptionAdd(elementAddress, address, modelIdentifier));
                }
            }
        }
    }

    @Override
    public void onItemDismiss(final RemovableViewHolder viewHolder) {
        final int position = viewHolder.getAbsoluteAdapterPosition();
        if (viewHolder instanceof BoundAppKeysAdapter.ViewHolder) {
            unbindAppKey(position);
        } else if (viewHolder instanceof GroupAddressAdapter.ViewHolder) {
            deleteSubscription(position);
        }
    }

    @Override
    public void onItemDismissFailed(final RemovableViewHolder viewHolder) {
    }

    @Override
    public void onDisconnected() {
        finish();
    }

    @Override
    public void onRefresh() {
        final MeshModel model = mViewModel.getSelectedModel().getValue();
        if (!checkConnectivity(mContainer) || model == null) {
            mSwipe.setRefreshing(false);
        }
        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
        final Element element = mViewModel.getSelectedElement().getValue();
        if (node != null && element != null && model != null) {
            if (model instanceof SigModel) {
                if (!(model instanceof ConfigurationServerModel) && !(model instanceof ConfigurationClientModel)) {
                    mViewModel.displaySnackBar(this, mContainer, getString(R.string.listing_model_configuration), Snackbar.LENGTH_LONG);
                    mViewModel.getMessageQueue().add(new ConfigSigModelAppGet(element.getElementAddress(), model.getModelId()));
                    if (model.getModelId() != SigModelParser.SCENE_SETUP_SERVER) {
                        mViewModel.getMessageQueue().add(new ConfigSigModelSubscriptionGet(element.getElementAddress(), model.getModelId()));
                        queuePublicationGetMessage(element.getElementAddress(), model.getModelId());
                    }
                    sendQueuedMessage(node.getUnicastAddress());
                } else {
                    mSwipe.setRefreshing(false);
                }

            } else {
                mViewModel.displaySnackBar(this, mContainer, getString(R.string.listing_model_configuration), Snackbar.LENGTH_LONG);
                final ConfigVendorModelAppGet appGet = new ConfigVendorModelAppGet(element.getElementAddress(), model.getModelId());
                final ConfigVendorModelSubscriptionGet subscriptionGet = new ConfigVendorModelSubscriptionGet(element.getElementAddress(), model.getModelId());
                mViewModel.getMessageQueue().add(appGet);
                mViewModel.getMessageQueue().add(subscriptionGet);
                queuePublicationGetMessage(element.getElementAddress(), model.getModelId());
                sendQueuedMessage(node.getUnicastAddress());
            }
        }
    }

    protected final void sendQueuedMessage(final int address) {
        final MeshMessage message = mViewModel.getMessageQueue().peek();
        if (message != null)
            sendAcknowledgedMessage(address, message);
    }

    protected void navigateToPublication() {
        final MeshModel model = mViewModel.getSelectedModel().getValue();
        if (model != null && !model.getBoundAppKeyIndexes().isEmpty()) {
            publicationSettings.launch(new Intent(this, PublicationSettingsActivity.class));
        } else {
            mViewModel.displaySnackBar(this, mContainer, getString(R.string.error_no_app_keys_bound), Snackbar.LENGTH_LONG);
        }
    }

    private void bindAppKey(final int appKeyIndex) {
        final ProvisionedMeshNode meshNode = mViewModel.getSelectedMeshNode().getValue();
        if (meshNode != null) {
            final Element element = mViewModel.getSelectedElement().getValue();
            if (element != null) {
                final MeshModel model = mViewModel.getSelectedModel().getValue();
                if (model != null) {
                    final ConfigModelAppBind configModelAppUnbind = new ConfigModelAppBind(element.getElementAddress(), model.getModelId(), appKeyIndex);
                    sendAcknowledgedMessage(meshNode.getUnicastAddress(), configModelAppUnbind);
                }
            }
        }
    }

    private void unbindAppKey(final int position) {
        if (mBoundAppKeyAdapter.getItemCount() != 0) {
            if (!checkConnectivity(mContainer)) {
                mBoundAppKeyAdapter.notifyItemChanged(position);
                return;
            }
            final ApplicationKey appKey = mBoundAppKeyAdapter.getAppKey(position);
            final int keyIndex = appKey.getKeyIndex();
            final ProvisionedMeshNode meshNode = mViewModel.getSelectedMeshNode().getValue();
            if (meshNode != null) {
                final Element element = mViewModel.getSelectedElement().getValue();
                if (element != null) {
                    final MeshModel model = mViewModel.getSelectedModel().getValue();
                    if (model != null) {
                        final ConfigModelAppUnbind configModelAppUnbind = new ConfigModelAppUnbind(element.getElementAddress(), model.getModelId(), keyIndex);
                        sendAcknowledgedMessage(meshNode.getUnicastAddress(), configModelAppUnbind);
                    }
                }
            }
        }
    }

    private void clearPublication() {
        final ProvisionedMeshNode meshNode = mViewModel.getSelectedMeshNode().getValue();
        if (meshNode != null) {
            final Element element = mViewModel.getSelectedElement().getValue();
            if (element != null) {
                final MeshModel model = mViewModel.getSelectedModel().getValue();
                if (model != null) {
                    sendAcknowledgedMessage(meshNode.getUnicastAddress(), new ConfigModelPublicationSet(element.getElementAddress(), model.getModelId()));
                }
            }
        }
    }

    private void deleteSubscription(final int position) {
        if (mSubscriptionAdapter.getItemCount() != 0) {
            if (!checkConnectivity(mContainer)) {
                mSubscriptionAdapter.notifyItemChanged(position);
                return;
            }
            final int address = mGroupAddress.get(position);
            final ProvisionedMeshNode meshNode = mViewModel.getSelectedMeshNode().getValue();
            if (meshNode != null) {
                final Element element = mViewModel.getSelectedElement().getValue();
                if (element != null) {
                    final MeshModel model = mViewModel.getSelectedModel().getValue();
                    if (model != null) {
                        MeshMessage subscriptionDelete = null;
                        if (isValidGroupAddress(address)) {
                            subscriptionDelete = new ConfigModelSubscriptionDelete(element.getElementAddress(), address, model.getModelId());
                        } else {
                            final UUID uuid = model.getLabelUUID(address);
                            if (uuid != null)
                                subscriptionDelete = new ConfigModelSubscriptionVirtualAddressDelete(element.getElementAddress(), uuid, model.getModelId());
                        }

                        if (subscriptionDelete != null) {
                            sendAcknowledgedMessage(meshNode.getUnicastAddress(), subscriptionDelete);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected final void showProgressBar() {
        mHandler.postDelayed(mRunnableOperationTimeout, MESSAGE_TIME_OUT);
        disableClickableViews();
        mProgressbar.setVisibility(View.VISIBLE);
    }

    @Override
    protected final void hideProgressBar() {
        mSwipe.setRefreshing(false);
        enableClickableViews();
        mProgressbar.setVisibility(View.INVISIBLE);
        mHandler.removeCallbacks(mRunnableOperationTimeout);
    }

    @Override
    protected void enableClickableViews() {
        mActionBindAppKey.setEnabled(true);
        mActionSetPublication.setEnabled(true);
        mActionClearPublication.setEnabled(true);
        mActionSubscribe.setEnabled(true);

        if (mActionSetRelayState != null)
            mActionSetRelayState.setEnabled(true);
        if (mSetNetworkTransmitStateButton != null)
            mSetNetworkTransmitStateButton.setEnabled(true);

        if (mActionRead != null && !mActionRead.isEnabled())
            mActionRead.setEnabled(true);
    }

    @Override
    protected void disableClickableViews() {
        mActionBindAppKey.setEnabled(false);
        mActionSetPublication.setEnabled(false);
        mActionClearPublication.setEnabled(false);
        mActionSubscribe.setEnabled(false);

        if (mActionSetRelayState != null)
            mActionSetRelayState.setEnabled(false);
        if (mSetNetworkTransmitStateButton != null)
            mSetNetworkTransmitStateButton.setEnabled(false);
        if (mActionRead != null)
            mActionRead.setEnabled(false);
    }

    protected void updateAppStatusUi(final MeshModel meshModel) {
        final List<Integer> keys = meshModel.getBoundAppKeyIndexes();
        mKeyIndexes.clear();
        mKeyIndexes.addAll(keys);
        if (!keys.isEmpty()) {
            mUnbindHint.setVisibility(View.VISIBLE);
            mAppKeyView.setVisibility(View.GONE);
            recyclerViewBoundKeys.setVisibility(View.VISIBLE);
        } else {
            mUnbindHint.setVisibility(View.GONE);
            mAppKeyView.setVisibility(View.VISIBLE);
            recyclerViewBoundKeys.setVisibility(View.GONE);
        }
    }

    protected void updatePublicationUi(final MeshModel meshModel) {
        final PublicationSettings publicationSettings = meshModel.getPublicationSettings();
        if (publicationSettings != null) {
            final int publishAddress = publicationSettings.getPublishAddress();
            if (isValidVirtualAddress(publishAddress)) {
                final UUID uuid = publicationSettings.getLabelUUID();
                if (uuid != null) {
                    mPublishAddressView.setText(uuid.toString().toUpperCase(Locale.US));
                } else {
                    mPublishAddressView.setText(formatAddress(publishAddress, true));
                }
            } else {
                mPublishAddressView.setText(formatAddress(publishAddress, true));
            }
            mActionClearPublication.setVisibility(View.VISIBLE);
        } else {
            mPublishAddressView.setText(R.string.none);
            mActionClearPublication.setVisibility(View.GONE);
        }
    }

    protected void updateSubscriptionUi(final MeshModel meshModel) {
        final List<Integer> subscriptionAddresses = meshModel.getSubscribedAddresses();
        mGroupAddress.clear();
        mGroupAddress.addAll(subscriptionAddresses);
        if (!subscriptionAddresses.isEmpty()) {
            mSubscribeHint.setVisibility(View.VISIBLE);
            mSubscribeAddressView.setVisibility(View.GONE);
            recyclerViewSubscriptions.setVisibility(View.VISIBLE);
        } else {
            mSubscribeHint.setVisibility(View.GONE);
            mSubscribeAddressView.setVisibility(View.VISIBLE);
            recyclerViewSubscriptions.setVisibility(View.GONE);
        }
    }

    protected void sendMessage(@NonNull final MeshMessage meshMessage) {
        try {
            if (!checkConnectivity(mContainer))
                return;
            final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
            if (node != null) {
                mViewModel.getMeshManagerApi().createMeshPdu(node.getUnicastAddress(), meshMessage);
                showProgressBar();
            }
        } catch (IllegalArgumentException ex) {
            hideProgressBar();
            final DialogFragmentError message = DialogFragmentError.
                    newInstance(getString(R.string.title_error), ex.getMessage() == null ? getString(R.string.unknwon_error) : ex.getMessage());
            message.show(getSupportFragmentManager(), null);
        }
    }

    protected boolean handleStatuses() {
        final MeshMessage message = mViewModel.getMessageQueue().peek();
        if (message != null) {
            sendMessage(message);
            return true;
        } else {
            mViewModel.displaySnackBar(this, mContainer, getString(R.string.operation_success), Snackbar.LENGTH_SHORT);
        }
        return false;
    }

    protected void sendAcknowledgedMessage(final int address, @NonNull final MeshMessage meshMessage) {
        try {
            if (!checkConnectivity(mContainer))
                return;
            mViewModel.getMeshManagerApi().createMeshPdu(address, meshMessage);
            showProgressBar();
        } catch (IllegalArgumentException ex) {
            hideProgressBar();
            DialogFragmentError
                    .newInstance(getString(R.string.title_error), ex.getMessage() == null ? getString(R.string.unknwon_error) : ex.getMessage())
                    .show(getSupportFragmentManager(), null);
        }
    }


    protected void sendUnacknowledgedMessage(final int address, @NonNull final MeshMessage meshMessage) {
        try {
            if (!checkConnectivity(mContainer))
                return;
            mViewModel.getMeshManagerApi().createMeshPdu(address, meshMessage);
        } catch (IllegalArgumentException ex) {
            DialogFragmentError
                    .newInstance(getString(R.string.title_error), ex.getMessage() == null ? getString(R.string.unknwon_error) : ex.getMessage())
                    .show(getSupportFragmentManager(), null);
        }
    }

    protected void updateClickableViews() {
        final MeshModel model = mViewModel.getSelectedModel().getValue();
        if (model != null && model.getModelId() == SigModelParser.CONFIGURATION_CLIENT)
            disableClickableViews();
    }

    protected void queuePublicationGetMessage(final int address, final int modelId) {
        final ConfigModelPublicationGet publicationGet = new ConfigModelPublicationGet(address, modelId);
        mViewModel.getMessageQueue().add(publicationGet);
    }

    protected void displayStatusDialogFragment(@NonNull final String title, @NonNull final String message) {
        if (mViewModel.isActivityVisible()) {
            DialogFragmentConfigStatus fragmentAppKeyBindStatus = DialogFragmentConfigStatus.
                    newInstance(title, message);
            fragmentAppKeyBindStatus.show(getSupportFragmentManager(), DIALOG_FRAGMENT_CONFIGURATION_STATUS);
        }
    }






    private void sendGenericOnOffCommand() {
        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
        final MeshModel model = mViewModel.getSelectedModel().getValue();

        if (node == null || model == null) {
            mViewModel.displaySnackBar(this, mContainer, "Node/Element/Model not selected", Snackbar.LENGTH_SHORT);
            return;
        }

        final String commandStr = mCommandEditText.getText() != null ? mCommandEditText.getText().toString().trim() : "";
        final String stateStr = mStateEditText.getText() != null ? mStateEditText.getText().toString().trim() : "";

        if (commandStr.isEmpty() || stateStr.isEmpty()) {
            mViewModel.displaySnackBar(this, mContainer, "Please enter command and state", Snackbar.LENGTH_SHORT);
            return;
        }

        try {
            final int command = Integer.parseInt(commandStr);
            final int state = Integer.parseInt(stateStr);

            // Validate state range (0-255)
            if (state < 0 || state > 255) {
                mViewModel.displaySnackBar(this, mContainer, "State must be between 0 and 255", Snackbar.LENGTH_SHORT);
                return;
            }

            // Validate command range (0-255)
            if (command < 0 || command > 255) {
                mViewModel.displaySnackBar(this, mContainer, "Command must be between 0 and 255", Snackbar.LENGTH_SHORT);
                return;
            }

            List<Integer> boundAppKeys = model.getBoundAppKeyIndexes();
            if (boundAppKeys.isEmpty()) {
                mViewModel.displaySnackBar(this, mContainer, "Bind an App Key first", Snackbar.LENGTH_SHORT);
                return;
            }

            final int appKeyIndex = boundAppKeys.get(0);

            ApplicationKey appKey = null;
            for (ApplicationKey key : mViewModel.getNetworkLiveData().getAppKeys()) {
                if (key.getKeyIndex() == appKeyIndex) {
                    appKey = key;
                    break;
                }
            }

            if (appKey == null) {
                mViewModel.displaySnackBar(this, mContainer, "App Key not found", Snackbar.LENGTH_SHORT);
                return;
            }

            // Generate random TID (0-255)
            final int tId = new Random().nextInt(256);

            // Create the message using the updated constructor: appKey, command, tid, state
            final GenericOnOffSet onOffSetMessage = new GenericOnOffSet(appKey, command, tId, state);

            sendAcknowledgedMessage(node.getUnicastAddress(), onOffSetMessage);

        } catch (NumberFormatException e) {
            mViewModel.displaySnackBar(this, mContainer, "Invalid command or state value. Please enter numbers only.", Snackbar.LENGTH_SHORT);
        } catch (IllegalArgumentException e) {
            mViewModel.displaySnackBar(this, mContainer, e.getMessage(), Snackbar.LENGTH_SHORT);
        }
    }





}
