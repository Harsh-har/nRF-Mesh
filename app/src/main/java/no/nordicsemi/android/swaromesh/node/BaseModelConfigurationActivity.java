package no.nordicsemi.android.swaromesh.node;

import static no.nordicsemi.android.swaromesh.utils.MeshAddress.formatAddress;
import static no.nordicsemi.android.swaromesh.utils.MeshAddress.isValidGroupAddress;
import static no.nordicsemi.android.swaromesh.utils.MeshAddress.isValidVirtualAddress;
import static no.nordicsemi.android.swaromesh.utils.Utils.BIND_APP_KEY;
import static no.nordicsemi.android.swaromesh.utils.Utils.EXTRA_DATA;
import static no.nordicsemi.android.swaromesh.utils.Utils.MESSAGE_TIME_OUT;
import static no.nordicsemi.android.swaromesh.utils.Utils.RESULT_KEY;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
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
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import no.nordicsemi.android.swaromesh.ApplicationKey;
import no.nordicsemi.android.swaromesh.Group;
import no.nordicsemi.android.swaromesh.MeshNetwork;
import no.nordicsemi.android.swaromesh.models.ConfigurationClientModel;
import no.nordicsemi.android.swaromesh.models.ConfigurationServerModel;
import no.nordicsemi.android.swaromesh.models.SigModel;
import no.nordicsemi.android.swaromesh.models.SigModelParser;
import no.nordicsemi.android.swaromesh.transport.ConfigModelAppBind;
import no.nordicsemi.android.swaromesh.transport.ConfigModelAppUnbind;
import no.nordicsemi.android.swaromesh.transport.ConfigModelPublicationGet;
import no.nordicsemi.android.swaromesh.transport.ConfigModelPublicationSet;
import no.nordicsemi.android.swaromesh.transport.ConfigModelSubscriptionAdd;
import no.nordicsemi.android.swaromesh.transport.ConfigModelSubscriptionDelete;
import no.nordicsemi.android.swaromesh.transport.ConfigModelSubscriptionVirtualAddressAdd;
import no.nordicsemi.android.swaromesh.transport.ConfigModelSubscriptionVirtualAddressDelete;
import no.nordicsemi.android.swaromesh.transport.ConfigSigModelAppGet;
import no.nordicsemi.android.swaromesh.transport.ConfigSigModelSubscriptionGet;
import no.nordicsemi.android.swaromesh.transport.ConfigVendorModelAppGet;
import no.nordicsemi.android.swaromesh.transport.ConfigVendorModelSubscriptionGet;
import no.nordicsemi.android.swaromesh.transport.Element;
import no.nordicsemi.android.swaromesh.transport.GenericLightSet;
import no.nordicsemi.android.swaromesh.transport.GenericOnOffSet;
import no.nordicsemi.android.swaromesh.transport.GenericSceneSet;
import no.nordicsemi.android.swaromesh.transport.MeshMessage;
import no.nordicsemi.android.swaromesh.transport.MeshModel;
import no.nordicsemi.android.swaromesh.transport.ProvisionedMeshNode;
import no.nordicsemi.android.swaromesh.transport.PublicationSettings;
import no.nordicsemi.android.swaromesh.utils.CompositionDataParser;
import no.nordicsemi.android.swaromesh.GroupCallbacks;
import no.nordicsemi.android.swaromesh.R;
import no.nordicsemi.android.swaromesh.adapter.GroupAddressAdapter;
import no.nordicsemi.android.swaromesh.databinding.ActivityModelConfigurationBinding;
import no.nordicsemi.android.swaromesh.dialog.DialogFragmentConfigStatus;
import no.nordicsemi.android.swaromesh.dialog.DialogFragmentDisconnected;
import no.nordicsemi.android.swaromesh.dialog.DialogFragmentError;
import no.nordicsemi.android.swaromesh.dialog.DialogFragmentGroupSubscription;
import no.nordicsemi.android.swaromesh.dialog.DialogFragmentTransactionStatus;
import no.nordicsemi.android.swaromesh.keys.AppKeysActivity;
import no.nordicsemi.android.swaromesh.keys.adapter.BoundAppKeysAdapter;
import no.nordicsemi.android.swaromesh.viewmodels.BaseActivity;
import no.nordicsemi.android.swaromesh.viewmodels.ModelConfigurationViewModel;
import no.nordicsemi.android.swaromesh.widgets.ItemTouchHelperAdapter;
import no.nordicsemi.android.swaromesh.widgets.RemovableItemTouchHelperCallback;
import no.nordicsemi.android.swaromesh.widgets.RemovableViewHolder;

public abstract class BaseModelConfigurationActivity extends BaseActivity implements
        GroupCallbacks,
        ItemTouchHelperAdapter,
        DialogFragmentDisconnected.DialogFragmentDisconnectedListener,
        SwipeRefreshLayout.OnRefreshListener {

    // Model ID Constants
    private static final int GENERIC_ONOFF_SERVER = 0x1000;
    private static final int GENERIC_ONOFF_CLIENT = 0x1001;

    private static final String DIALOG_FRAGMENT_CONFIGURATION_STATUS = "DIALOG_FRAGMENT_CONFIGURATION_STATUS";
    private static final String PROGRESS_BAR_STATE = "PROGRESS_BAR_STATE";

    private static final int DEFAULT_BRIGHTNESS_VALUE = 30;
    private static final int MIN_BRIGHTNESS = 0;
    private static final int MAX_BRIGHTNESS = 255;
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 8;
    private static final int MAX_TID = 255; // 8-bit TID (0-255)

    // Press type constants
    private static final String PRESS_TYPE_SINGLE = "Single";
    private static final String PRESS_TYPE_DOUBLE = "Double";
    private static final String PRESS_TYPE_LONG = "Long";

    // TID counters for different models/elements
    private final AtomicInteger genericOnOffTidCounter = new AtomicInteger(0);
    private final AtomicInteger genericLightTidCounter = new AtomicInteger(0);
    private final AtomicInteger sceneTidCounter = new AtomicInteger(0);

    protected ActivityModelConfigurationBinding binding;

    // Command containers
    private LinearLayout mContainerShortLongCommands;
    private LinearLayout mContainerSceneCommands;

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

    // Long Command Controls
    protected Button mLongSendButton;
    protected Button mLongReadButton;
    protected TextInputEditText mLengthEditText;
    protected TextInputEditText mLongAddressEditText;
    protected List<TextInputLayout> mLongDataFields = new ArrayList<>();
    protected List<TextInputEditText> mLongDataEditTexts = new ArrayList<>();

    // Scene Command Controls
    protected TextInputEditText mSceneIdEditText;
    protected TextInputEditText mTypeEditText;
    protected TextInputEditText mPressEditText;
    protected TextInputEditText mModeEditText;
    protected TextInputEditText mDeviceEditText;
    protected TextInputEditText mSceneStateEditText;
    protected Button mBtnPressSingle;
    protected Button mBtnPressDouble;
    protected Button mBtnPressLong;
    protected Button mSceneSendButton;
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

        // Initialize command containers
        mContainerShortLongCommands = findViewById(R.id.container_short_long_commands);
        mContainerSceneCommands = findViewById(R.id.container_scene_commands);

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
        mCommandEditText = binding.etCommand;
        mStateEditText = binding.etState;
        mSendButton = binding.actionOn;

        // Long Command Controls references
        mLongSendButton = binding.actionLongSend;
        mLongReadButton = binding.actionLongReadState;
        mLengthEditText = binding.etElementAddress;
        mLongAddressEditText = binding.etLongCommand;
        mLengthEditText.setText(String.valueOf(MAX_LENGTH)); // default = 8

        // Scene Command Controls references
        mSceneIdEditText = binding.etSceneId;
        mTypeEditText = binding.etType;
        mPressEditText = binding.etPress;
        mModeEditText = binding.etMode;
        mDeviceEditText = binding.etDevice;
        mSceneStateEditText = binding.etSceneState;
        mBtnPressSingle = binding.btnPressSingle;
        mBtnPressDouble = binding.btnPressDouble;
        mBtnPressLong = binding.btnPressLong;
        mSceneSendButton = binding.btnSend;

        // Initialize long data fields with brightness values
        initializeLongDataFields();

        // Initialize scene controls
        initializeSceneControls();

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
            getSupportActionBar().setSubtitle(getString(R.string.model_id,
                    CompositionDataParser.formatModelIdentifier(modelId, true)));

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
            mBoundAppKeyAdapter = new BoundAppKeysAdapter(
                    this,
                    mViewModel.getNetworkLiveData().getAppKeys(),
                    mViewModel.getSelectedModel()
            );
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

            // Send button
            mSendButton.setOnClickListener(v -> sendGenericOnOffCommand());

            // Long command buttons
            mLongSendButton.setOnClickListener(v -> sendLongBrightnessCommand());
            mLongReadButton.setOnClickListener(v -> readLongCommand());

            // Scene command button
            mSceneSendButton.setOnClickListener(v -> sendSceneCommand());

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

        // ✅ AUTO BIND TRIGGER USING OBSERVERS
        mViewModel.getSelectedMeshNode().observe(this, node -> tryAutoBind());
        mViewModel.getSelectedElement().observe(this, element -> tryAutoBind());

        // ✅ OBSERVE MODEL CHANGES AND UPDATE UI
        mViewModel.getSelectedModel().observe(this, model -> {
            tryAutoBind();
            updateCommandCardsBasedOnModel(model);

            // Update toolbar
            if (model != null && getSupportActionBar() != null) {
                getSupportActionBar().setTitle(model.getModelName());
                getSupportActionBar().setSubtitle(getString(R.string.model_id,
                        CompositionDataParser.formatModelIdentifier(model.getModelId(), true)));
            }
        });

        // Initial UI update
        MeshModel initialModel = mViewModel.getSelectedModel().getValue();
        if (initialModel != null) {
            updateCommandCardsBasedOnModel(initialModel);
        }
    }

    /**
     * Updates which command cards are visible based on the model type
     */
    private void updateCommandCardsBasedOnModel(MeshModel model) {
        if (model == null) {
            mContainerShortLongCommands.setVisibility(View.GONE);
            mContainerSceneCommands.setVisibility(View.GONE);
            return;
        }

        int modelId = model.getModelId();

        // Generic OnOff Client (0x1001) -> Show Scene commands only
        if (modelId == GENERIC_ONOFF_CLIENT) {
            mContainerShortLongCommands.setVisibility(View.GONE);
            mContainerSceneCommands.setVisibility(View.VISIBLE);
            Log.d("MODEL_UI", "Showing Scene commands for Generic OnOff Client");
        }
        // Generic OnOff Server (0x1000) -> Show Short/Long commands only
        else if (modelId == GENERIC_ONOFF_SERVER) {
            mContainerShortLongCommands.setVisibility(View.VISIBLE);
            mContainerSceneCommands.setVisibility(View.GONE);
            Log.d("MODEL_UI", "Showing Short/Long commands for Generic OnOff Server");
        }
        // For other models, hide both
        else {
            mContainerShortLongCommands.setVisibility(View.GONE);
            mContainerSceneCommands.setVisibility(View.GONE);
            Log.d("MODEL_UI", "No commands available for model ID: " + modelId);
        }
    }

    private void initializeSceneControls() {
        // Set default values
        mSceneIdEditText.setText("1");
        mTypeEditText.setText("1");
        mPressEditText.setText(PRESS_TYPE_SINGLE);
        mModeEditText.setText("2");
        mDeviceEditText.setText("1");
        mSceneStateEditText.setText("0");

        // Press type button listeners
        mBtnPressSingle.setOnClickListener(v -> {
            mPressEditText.setText(PRESS_TYPE_SINGLE);
            Toast.makeText(this, "Single Press Selected", Toast.LENGTH_SHORT).show();
        });

        mBtnPressDouble.setOnClickListener(v -> {
            mPressEditText.setText(PRESS_TYPE_DOUBLE);
            Toast.makeText(this, "Double Press Selected", Toast.LENGTH_SHORT).show();
        });

        mBtnPressLong.setOnClickListener(v -> {
            mPressEditText.setText(PRESS_TYPE_LONG);
            Toast.makeText(this, "Long Press Selected", Toast.LENGTH_SHORT).show();
        });

        // Add validation listeners
        addSceneValidationListeners();
    }

    private void addSceneValidationListeners() {
        // Scene ID validation (1-240)
        mSceneIdEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateSceneId();
            }
        });

        // Type validation (1-39)
        mTypeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateType();
            }
        });

        // Mode validation (1-8)
        mModeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateMode();
            }
        });

        // Device validation (1-8)
        mDeviceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateDevice();
            }
        });

        // Scene State validation (0-3)
        mSceneStateEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateSceneState();
            }
        });
    }

    private void validateSceneId() {
        try {
            String text = mSceneIdEditText.getText().toString().trim();
            if (!text.isEmpty()) {
                int sceneId = Integer.parseInt(text);
                if (sceneId < 1 || sceneId > 240) {
                    mSceneIdEditText.setError("Scene ID must be between 1 and 240");
                } else {
                    mSceneIdEditText.setError(null);
                }
            }
        } catch (NumberFormatException e) {
            mSceneIdEditText.setError("Invalid Scene ID");
        }
    }

    private void validateType() {
        try {
            String text = mTypeEditText.getText().toString().trim();
            if (!text.isEmpty()) {
                int type = Integer.parseInt(text);
                if (type < 1 || type > 39) {
                    mTypeEditText.setError("Type must be between 1 and 39");
                } else {
                    mTypeEditText.setError(null);
                }
            }
        } catch (NumberFormatException e) {
            mTypeEditText.setError("Invalid Type");
        }
    }

    private void validateMode() {
        try {
            String text = mModeEditText.getText().toString().trim();
            if (!text.isEmpty()) {
                int mode = Integer.parseInt(text);
                if (mode < 1 || mode > 8) {
                    mModeEditText.setError("Mode must be between 1 and 8");
                } else {
                    mModeEditText.setError(null);
                }
            }
        } catch (NumberFormatException e) {
            mModeEditText.setError("Invalid Mode");
        }
    }

    private void validateDevice() {
        try {
            String text = mDeviceEditText.getText().toString().trim();
            if (!text.isEmpty()) {
                int device = Integer.parseInt(text);
                if (device < 1 || device > 8) {
                    mDeviceEditText.setError("Device must be between 1 and 8");
                } else {
                    mDeviceEditText.setError(null);
                }
            }
        } catch (NumberFormatException e) {
            mDeviceEditText.setError("Invalid Device");
        }
    }

    private void validateSceneState() {
        try {
            String text = mSceneStateEditText.getText().toString().trim();
            if (!text.isEmpty()) {
                int state = Integer.parseInt(text);
                if (state < 0 || state > 3) {
                    mSceneStateEditText.setError("State must be between 0 and 3");
                } else {
                    mSceneStateEditText.setError(null);
                }
            }
        } catch (NumberFormatException e) {
            mSceneStateEditText.setError("Invalid State");
        }
    }

    private boolean isAutoBindTriggered = false;

    private void tryAutoBind() {
        if (isAutoBindTriggered) return;

        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
        final Element element = mViewModel.getSelectedElement().getValue();
        final MeshModel model = mViewModel.getSelectedModel().getValue();

        if (node == null || element == null || model == null) return;

        // Config server required
        if (!node.isExist(SigModelParser.CONFIGURATION_SERVER)) return;

        // Connectivity required
        if (!checkConnectivity(mContainer)) return;

        // Already bound -> skip
        if (model.getBoundAppKeyIndexes() != null && !model.getBoundAppKeyIndexes().isEmpty()) {
            return;
        }

        // Default AppKey index (first one)
        final List<ApplicationKey> appKeys = mViewModel.getNetworkLiveData().getAppKeys();
        if (appKeys == null || appKeys.isEmpty()) return;

        final int defaultAppKeyIndex = appKeys.get(0).getKeyIndex();

        isAutoBindTriggered = true;

        final ConfigModelAppBind bindMessage = new ConfigModelAppBind(
                element.getElementAddress(),
                model.getModelId(),
                defaultAppKeyIndex
        );

        sendAcknowledgedMessage(node.getUnicastAddress(), bindMessage);
    }

    private void initializeLongDataFields() {
        mLongDataFields.add(binding.layoutLongData1);
        mLongDataFields.add(binding.layoutLongData2);
        mLongDataFields.add(binding.layoutLongData3);
        mLongDataFields.add(binding.layoutLongData4);
        mLongDataFields.add(binding.layoutLongData5);
        mLongDataFields.add(binding.layoutLongData6);
        mLongDataFields.add(binding.layoutLongData7);
        mLongDataFields.add(binding.layoutLongData8);

        mLongDataEditTexts.add(binding.etLongData1);
        mLongDataEditTexts.add(binding.etLongData2);
        mLongDataEditTexts.add(binding.etLongData3);
        mLongDataEditTexts.add(binding.etLongData4);
        mLongDataEditTexts.add(binding.etLongData5);
        mLongDataEditTexts.add(binding.etLongData6);
        mLongDataEditTexts.add(binding.etLongData7);
        mLongDataEditTexts.add(binding.etLongData8);

        // Set default brightness values for all 8 fields
        for (int i = 0; i < MAX_LENGTH; i++) {
            // Set default value
            mLongDataEditTexts.get(i).setText(String.valueOf(DEFAULT_BRIGHTNESS_VALUE));

            // Add brightness validation
            final int index = i;
            mLongDataEditTexts.get(i).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    validateBrightnessField(index);
                }
            });

            mLongDataEditTexts.get(i).setImeOptions(EditorInfo.IME_ACTION_NEXT);
        }

        // Last field DONE
        mLongDataEditTexts.get(MAX_LENGTH - 1)
                .setImeOptions(EditorInfo.IME_ACTION_DONE);

        for (int i = 0; i < MAX_LENGTH; i++) {
            mLongDataFields.get(i).setVisibility(View.VISIBLE);
        }
    }

    private void setupLengthTextWatcher() {
        mLengthEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateLengthField();
            }
        });
    }

    private void validateLengthField() {
        try {
            String lengthText = mLengthEditText.getText().toString().trim();
            if (lengthText.isEmpty()) {
                mLengthEditText.setError("Please enter length (1-8)");
                return;
            }

            int length = Integer.parseInt(lengthText);
            if (length < MIN_LENGTH || length > MAX_LENGTH) {
                mLengthEditText.setError("Length must be between " + MIN_LENGTH + " and " + MAX_LENGTH);
                return;
            }
            mLengthEditText.setError(null);

            updateFieldValidationsBasedOnLength(length);

        } catch (NumberFormatException e) {
            mLengthEditText.setError("Invalid length value");
        }
    }

    private void updateFieldValidationsBasedOnLength(int length) {
        for (int i = 0; i < MAX_LENGTH; i++) {
            if (i < length) {
                String text = mLongDataEditTexts.get(i).getText().toString();
                if (text.isEmpty()) {
                    mLongDataFields.get(i).setError("Required for length " + length);
                } else {
                    validateBrightnessField(i);
                }
            } else {
                // Ye field optional hai (length se bahar), error clear karo
                mLongDataFields.get(i).setError(null);
            }
        }
    }

    private void validateBrightnessField(int index) {
        try {
            String text = mLongDataEditTexts.get(index).getText().toString().trim();

            String lengthText = mLengthEditText.getText().toString().trim();
            if (!lengthText.isEmpty()) {
                int length = Integer.parseInt(lengthText);
                if (index >= length) {
                    // Agar field length se bahar hai, to validation nahi karna
                    mLongDataFields.get(index).setError(null);
                    return;
                }
            }

            if (!text.isEmpty()) {
                int brightness = Integer.parseInt(text);
                if (brightness < MIN_BRIGHTNESS || brightness > MAX_BRIGHTNESS) {
                    mLongDataFields.get(index).setError(
                            String.format("Brightness must be between %d and %d", MIN_BRIGHTNESS, MAX_BRIGHTNESS)
                    );
                } else {
                    mLongDataFields.get(index).setError(null);
                }
            } else {
                // Length check karo agar field required hai
                if (!lengthText.isEmpty()) {
                    int length = Integer.parseInt(lengthText);
                    if (index < length) {
                        mLongDataFields.get(index).setError("Enter brightness value");
                    } else {
                        mLongDataFields.get(index).setError(null);
                    }
                } else {
                    mLongDataFields.get(index).setError("Enter brightness value");
                }
            }
        } catch (NumberFormatException e) {
            mLongDataFields.get(index).setError("Invalid brightness value");
        }
    }

    private void readLongCommand() {
        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
        final MeshModel model = mViewModel.getSelectedModel().getValue();

        if (node == null || model == null) {
            mViewModel.displaySnackBar(this, mContainer,
                    "Node or model not selected", Snackbar.LENGTH_SHORT);
            return;
        }

        mViewModel.displaySnackBar(this, mContainer,
                "Reading brightness values from device...", Snackbar.LENGTH_SHORT);
        // TODO: Implement actual read functionality for brightness values
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

    /**
     * Gets the next TID for GenericOnOff model
     * Increments from 0 to 255 and wraps around
     */
    private int getNextGenericOnOffTid() {
        int current = genericOnOffTidCounter.getAndIncrement();
        if (current > MAX_TID) {
            genericOnOffTidCounter.set(0);
            current = 0;
        }
        Log.d("TID", "GenericOnOff TID: " + current);
        return current;
    }

    /**
     * Gets the next TID for GenericLight model
     * Increments from 0 to 255 and wraps around
     */
    private int getNextGenericLightTid() {
        int current = genericLightTidCounter.getAndIncrement();
        if (current > MAX_TID) {
            genericLightTidCounter.set(0);
            current = 0;
        }
        Log.d("TID", "GenericLight TID: " + current);
        return current;
    }

    /**
     * Gets the next TID for Scene model
     * Increments from 0 to 255 and wraps around
     */
    private int getNextSceneTid() {
        int current = sceneTidCounter.getAndIncrement();
        if (current > MAX_TID) {
            sceneTidCounter.set(0);
            current = 0;
        }
        Log.d("TID", "Scene TID: " + current);
        return current;
    }

    /**
     * Reset TID counters (optional, can be called when needed)
     */
    public void resetTidCounters() {
        genericOnOffTidCounter.set(0);
        genericLightTidCounter.set(0);
        sceneTidCounter.set(0);
        Log.d("TID", "TID counters reset to 0");
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

            if (state < 0 || state > 255) {
                mViewModel.displaySnackBar(this, mContainer, "State must be between 0 and 255", Snackbar.LENGTH_SHORT);
                return;
            }

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

            // Use sequential TID instead of random
            final int tId = getNextGenericOnOffTid();

            final GenericOnOffSet onOffSetMessage = new GenericOnOffSet(appKey, command, state, tId);

            Log.d("CMD", "========== GenericOnOffSet ==========");
            Log.d("CMD", "Command: " + command + " (0x" + String.format("%02X", command) + ")");
            Log.d("CMD", "State: " + state + " (0x" + String.format("%02X", state) + ")");
            Log.d("CMD", "TID: " + tId + " (0x" + String.format("%02X", tId) + ")");
            Log.d("CMD", "====================================");

            sendAcknowledgedMessage(node.getUnicastAddress(), onOffSetMessage);

        } catch (NumberFormatException e) {
            mViewModel.displaySnackBar(this, mContainer, "Invalid command or state value. Please enter numbers only.", Snackbar.LENGTH_SHORT);
        } catch (IllegalArgumentException e) {
            mViewModel.displaySnackBar(this, mContainer, e.getMessage(), Snackbar.LENGTH_SHORT);
        }
    }

    private void sendLongBrightnessCommand() {
        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
        final Element element = mViewModel.getSelectedElement().getValue();
        final MeshModel model = mViewModel.getSelectedModel().getValue();

        if (node == null || element == null || model == null) {
            mViewModel.displaySnackBar(
                    this, mContainer,
                    "Node / Element / Model not selected",
                    Snackbar.LENGTH_SHORT
            );
            return;
        }

        try {
            /* ---------- LENGTH ---------- */
            final String lengthStr = mLengthEditText.getText().toString().trim();
            if (lengthStr.isEmpty()) {
                mViewModel.displaySnackBar(this, mContainer,
                        "Please enter length (1–8)", Snackbar.LENGTH_SHORT);
                return;
            }

            final int length = Integer.parseInt(lengthStr);
            if (length < MIN_LENGTH || length > MAX_LENGTH) {
                mViewModel.displaySnackBar(this, mContainer,
                        "Length must be between 1 and 8",
                        Snackbar.LENGTH_SHORT);
                return;
            }

            /* ---------- COMMAND ---------- */
            final String commandStr = mLongAddressEditText.getText().toString().trim();
            if (commandStr.isEmpty()) {
                mViewModel.displaySnackBar(this, mContainer,
                        "Please enter command", Snackbar.LENGTH_SHORT);
                return;
            }

            final int command = Integer.parseInt(commandStr);
            if (command < 0 || command > 255) {
                mViewModel.displaySnackBar(this, mContainer,
                        "Command must be 0–255",
                        Snackbar.LENGTH_SHORT);
                return;
            }

            /* ---------- BRIGHTNESS  ---------- */
            final int[] brightness = new int[length];
            for (int i = 0; i < length; i++) {
                final String valueStr =
                                 mLongDataEditTexts.get(i).getText().toString().trim();

                if (valueStr.isEmpty()) {
                    mViewModel.displaySnackBar(this, mContainer,
                            "Please enter brightness " + (i + 1) + " (required for length " + length + ")",
                            Snackbar.LENGTH_SHORT);
                    return;
                }

                brightness[i] = Integer.parseInt(valueStr);
                if (brightness[i] < MIN_BRIGHTNESS || brightness[i] > MAX_BRIGHTNESS) {
                    mViewModel.displaySnackBar(this, mContainer,
                            "Brightness " + (i + 1) + " must be 0–255",
                            Snackbar.LENGTH_SHORT);
                    return;
                }
            }

            /* ---------- APP KEY ---------- */
            final List<Integer> boundKeys = model.getBoundAppKeyIndexes();
            if (boundKeys.isEmpty()) {
                mViewModel.displaySnackBar(this, mContainer,
                        "No AppKey bound to model",
                        Snackbar.LENGTH_SHORT);
                return;
            }

            final MeshNetwork network =
                    mViewModel.getNetworkLiveData().getMeshNetwork();
            final ApplicationKey appKey =
                    network.getAppKey(boundKeys.get(0));

            if (appKey == null) {
                mViewModel.displaySnackBar(this, mContainer,
                        "AppKey not found",
                        Snackbar.LENGTH_SHORT);
                return;
            }

            /* ---------- TID (SEQUENTIAL) ---------- */
            final int tid = getNextGenericLightTid();

            /* ---------- CREATE MESSAGE (LENGTH FIRST) ---------- */
            final GenericLightSet message =
                    new GenericLightSet(appKey, length, command, brightness, tid);

            /* ---------- LOG ---------- */
            Log.d("LONG_CMD", "========== GenericLightSet ==========");
            Log.d("LONG_CMD", "Element Addr : " + element.getElementAddress());
            Log.d("LONG_CMD", "Length       : " + length);
            Log.d("LONG_CMD", "Command      : " + command + " (0x" + String.format("%02X", command) + ")");
            Log.d("LONG_CMD", "Brightness   : " + Arrays.toString(brightness));
            Log.d("LONG_CMD", "TID          : " + tid + " (0x" + String.format("%02X", tid) + ")");
            Log.d("LONG_CMD", "Payload Size : " + message.getMessageSize() + " bytes");
            Log.d("LONG_CMD", "Raw Payload  : " + Arrays.toString(message.toByteArray()));
            Log.d("LONG_CMD", "====================================");

            mViewModel.displaySnackBar(
                    this, mContainer,
                    String.format("Sending LEN=%d CMD=0x%02X TID=%d", length, command, tid),
                    Snackbar.LENGTH_LONG
            );

            /* ---------- SEND ---------- */
            sendAcknowledgedMessage(node.getUnicastAddress(), message);

        } catch (Exception e) {
            mViewModel.displaySnackBar(this, mContainer,
                    "Failed to send command",
                    Snackbar.LENGTH_SHORT);
            Log.e("LONG_CMD", "Error", e);
        }
    }

    private void sendSceneCommand() {
        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
        final Element element = mViewModel.getSelectedElement().getValue();
        final MeshModel model = mViewModel.getSelectedModel().getValue();

        if (node == null || element == null || model == null) {
            mViewModel.displaySnackBar(this, mContainer,
                    "Node / Element / Model not selected", Snackbar.LENGTH_SHORT);
            return;
        }

        try {
            /* ---------------- INPUT VALIDATION ---------------- */

            // Parse and validate Scene ID (0-255)
            final int sceneId = parseAndValidateInt(
                    mSceneIdEditText, "Scene ID", 0, 255);

            // Parse and validate Type (0-63 for 6 bits)
            final int type = parseAndValidateInt(
                    mTypeEditText, "Type", 0, 63);

            // Parse and validate Mode (0-7 for 3 bits)
            final int mode = parseAndValidateInt(
                    mModeEditText, "Mode", 0, 7);

            // Parse and validate Device (0-7 for 3 bits)
            final int device = parseAndValidateInt(
                    mDeviceEditText, "Device", 0, 7);

            // Parse and validate Scene State (0-3 for 2 bits)
            final int sceneState = parseAndValidateInt(
                    mSceneStateEditText, "State", 0, 3);

            // Validate Press Type
            final String pressTypeStr = mPressEditText.getText().toString().trim();
            final int pressCode = GenericSceneSet.getPressTypeCode(pressTypeStr);
            if (pressCode < 0 || pressCode > 3) {
                mViewModel.displaySnackBar(this, mContainer,
                        "Invalid press type. Use Single, Double, or Long",
                        Snackbar.LENGTH_SHORT);
                return;
            }

            /* ---------------- TID ---------------- */
            final int tid = getNextSceneTid() & 0xFF;

            /* ---------------- APP KEY ---------------- */
            final List<Integer> boundKeys = model.getBoundAppKeyIndexes();
            if (boundKeys == null || boundKeys.isEmpty()) {
                mViewModel.displaySnackBar(this, mContainer,
                        "No AppKey bound to model", Snackbar.LENGTH_SHORT);
                return;
            }

            final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
            final ApplicationKey appKey = network.getAppKey(boundKeys.get(0));
            if (appKey == null) {
                mViewModel.displaySnackBar(this, mContainer,
                        "AppKey not found", Snackbar.LENGTH_SHORT);
                return;
            }

            /* ---------------- CREATE MESSAGE ---------------- */
            final GenericSceneSet sceneSetMessage = new GenericSceneSet(
                    appKey, sceneId, type, pressCode, mode, device, sceneState, tid
            );

            /* ---------------- VERIFY MESSAGE (Optional) ---------------- */
            if (!verifyMessageStructure(sceneSetMessage, sceneId, type, pressCode,
                    mode, device, sceneState, tid)) {
                mViewModel.displaySnackBar(this, mContainer,
                        "Message verification failed", Snackbar.LENGTH_SHORT);
                return;
            }

            /* ---------------- SEND ---------------- */
            final int elementAddress = element.getElementAddress();

            // Log message details
            logSceneCommand(sceneSetMessage, elementAddress, network);

            // Send message
            sendUnacknowledgedMessage(elementAddress, sceneSetMessage);

            mViewModel.displaySnackBar(this, mContainer,
                    String.format("Scene Command sent to Element 0x%04X", elementAddress),
                    Snackbar.LENGTH_LONG);

        } catch (NumberFormatException e) {
            mViewModel.displaySnackBar(this, mContainer,
                    "Invalid numeric input: " + e.getMessage(), Snackbar.LENGTH_SHORT);
        } catch (IllegalArgumentException e) {
            mViewModel.displaySnackBar(this, mContainer,
                    "Validation error: " + e.getMessage(), Snackbar.LENGTH_SHORT);
        } catch (Exception e) {
            Log.e("SCENE_CMD", "Send failed", e);
            mViewModel.displaySnackBar(this, mContainer,
                    "Failed: " + e.getMessage(), Snackbar.LENGTH_SHORT);
        }
    }

    /**
     * Helper method to parse and validate integer input
     */
    private int parseAndValidateInt(TextInputEditText editText,
                                    String fieldName,
                                    int min, int max)
            throws NumberFormatException, IllegalArgumentException {

        String text = editText.getText().toString().trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }

        int value = Integer.parseInt(text);
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    String.format("%s must be between %d and %d", fieldName, min, max));
        }

        return value;
    }

    /**
     * Verify that the message bytes match the input values
     */
    private boolean verifyMessageStructure(GenericSceneSet message,
                                           int sceneId, int type, int press,
                                           int mode, int device, int state, int tid) {
        byte[] params = message.getParameters();
        if (params == null || params.length != 4) {
            Log.e("SCENE_CMD", "Invalid message parameters");
            return false;
        }

        // Verify byte 0: scene_id
        if ((params[0] & 0xFF) != sceneId) {
            Log.e("SCENE_CMD", String.format(
                    "Scene ID mismatch: expected %d, got %d", sceneId, params[0] & 0xFF));
            return false;
        }

        // Verify byte 1: type + press
        int extractedType = (params[1] >> 2) & 0x3F;
        int extractedPress = params[1] & 0x03;
        if (extractedType != type || extractedPress != press) {
            Log.e("SCENE_CMD", String.format(
                    "Type/Press mismatch: expected type=%d press=%d, got type=%d press=%d",
                    type, press, extractedType, extractedPress));
            return false;
        }

        // Verify byte 2: mode + device + state
        int extractedMode = (params[2] >> 5) & 0x07;
        int extractedDevice = (params[2] >> 2) & 0x07;
        int extractedState = params[2] & 0x03;
        if (extractedMode != mode || extractedDevice != device || extractedState != state) {
            Log.e("SCENE_CMD", String.format(
                    "Mode/Device/State mismatch: expected mode=%d dev=%d state=%d, got mode=%d dev=%d state=%d",
                    mode, device, state, extractedMode, extractedDevice, extractedState));
            return false;
        }

        // Verify byte 3: tid
        if ((params[3] & 0xFF) != tid) {
            Log.e("SCENE_CMD", String.format(
                    "TID mismatch: expected %d, got %d", tid, params[3] & 0xFF));
            return false;
        }

        Log.d("SCENE_CMD", "Message verification PASSED");
        return true;
    }

    /**
     * Enhanced logging with hex representation
     */
    private void logSceneCommand(GenericSceneSet message, int elementAddress, MeshNetwork network) {
        byte[] params = message.getParameters();

        Log.d("SCENE_CMD", "========== GenericSceneSet ==========");
        Log.d("SCENE_CMD", String.format("Source Addr      : 0x%04X", network.getProvisionerAddress()));
        Log.d("SCENE_CMD", String.format("Dest Addr        : 0x%04X (Element)", elementAddress));
        Log.d("SCENE_CMD", "------------------------------------");
        Log.d("SCENE_CMD", "Field     : Dec    : Hex    : Binary");
        Log.d("SCENE_CMD", "------------------------------------");
        Log.d("SCENE_CMD", String.format("Scene ID  : %3d    : 0x%02X   : %8s",
                params[0] & 0xFF, params[0] & 0xFF,
                String.format("%8s", Integer.toBinaryString(params[0] & 0xFF)).replace(' ', '0')));

        Log.d("SCENE_CMD", String.format("Byte 1    : %3d    : 0x%02X   : %8s",
                params[1] & 0xFF, params[1] & 0xFF,
                String.format("%8s", Integer.toBinaryString(params[1] & 0xFF)).replace(' ', '0')));
        Log.d("SCENE_CMD", String.format("  - Type  : %3d    : 0x%02X   :    (bits 7-2)",
                (params[1] >> 2) & 0x3F, (params[1] >> 2) & 0x3F));
        Log.d("SCENE_CMD", String.format("  - Press : %3d    : 0x%02X   :    (bits 1-0)",
                params[1] & 0x03, params[1] & 0x03));

        Log.d("SCENE_CMD", String.format("Byte 2    : %3d    : 0x%02X   : %8s",
                params[2] & 0xFF, params[2] & 0xFF,
                String.format("%8s", Integer.toBinaryString(params[2] & 0xFF)).replace(' ', '0')));
        Log.d("SCENE_CMD", String.format("  - Mode  : %3d    : 0x%02X   :   (bits 7-5)",
                (params[2] >> 5) & 0x07, (params[2] >> 5) & 0x07));
        Log.d("SCENE_CMD", String.format("  - Device: %3d    : 0x%02X   :   (bits 4-2)",
                (params[2] >> 2) & 0x07, (params[2] >> 2) & 0x07));
        Log.d("SCENE_CMD", String.format("  - State : %3d    : 0x%02X   :   (bits 1-0)",
                params[2] & 0x03, params[2] & 0x03));

        Log.d("SCENE_CMD", String.format("TID       : %3d    : 0x%02X   : %8s",
                params[3] & 0xFF, params[3] & 0xFF,
                String.format("%8s", Integer.toBinaryString(params[3] & 0xFF)).replace(' ', '0')));
        Log.d("SCENE_CMD", "------------------------------------");
        Log.d("SCENE_CMD", String.format("Full Message     : %02X %02X %02X %02X",
                params[0] & 0xFF, params[1] & 0xFF, params[2] & 0xFF, params[3] & 0xFF));
        Log.d("SCENE_CMD", "====================================");
    }
}