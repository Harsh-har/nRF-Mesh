package no.nordicsemi.android.swaromesh.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import dagger.hilt.android.AndroidEntryPoint;
import no.nordicsemi.android.swaromesh.ProvisioningActivity;
import no.nordicsemi.android.swaromesh.R;
import no.nordicsemi.android.swaromesh.adapter.ExtendedBluetoothDevice;
import no.nordicsemi.android.swaromesh.ble.adapter.DevicesAdapter;
import no.nordicsemi.android.swaromesh.databinding.ActivityScannerBinding;
import no.nordicsemi.android.swaromesh.utils.Utils;
import no.nordicsemi.android.swaromesh.viewmodels.ScannerLiveData;
import no.nordicsemi.android.swaromesh.viewmodels.ScannerStateLiveData;
import no.nordicsemi.android.swaromesh.viewmodels.ScannerViewModel;
import no.nordicsemi.android.swaromesh.viewmodels.SharedViewModel;

import java.util.UUID;

@AndroidEntryPoint
public class ScannerActivity extends AppCompatActivity implements DevicesAdapter.OnItemClickListener {

    private static final int REQUEST_ACCESS_FINE_LOCATION = 1022;
    private static final int REQUEST_ACCESS_BLUETOOTH_PERMISSION = 1023;
    private static final long AUTO_CONNECT_RETRY_DELAY_MS = 1000; // Retry every 1 second
    private static final long TARGET_CONNECT_TIMEOUT_MS = 30000; // 30 seconds timeout
    private static final long AUTO_CONNECT_AFTER_PROVISIONING_DELAY = 2000; // 2 seconds delay

    private ActivityScannerBinding binding;
    private ScannerViewModel mViewModel;

    private boolean mScanWithProxyService = true;
    private boolean mSilentConnect = false;

    private boolean mAutoConnectStarted = false;
    private boolean mIsNewlyProvisioned = false;
    private String targetProxyMac;

    // Auto-connect after provisioning
    private boolean mShouldAutoConnectAfterProvisioning = false;
    private String mProvisionedDeviceMac = null;

    private Handler mAutoConnectHandler;
    private Runnable mAutoConnectRunnable;
    private long mScanStartTime;

    private final ActivityResultLauncher<Intent> provisioner =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    mIsNewlyProvisioned = true;

                    ExtendedBluetoothDevice provisionedDevice = result.getData().getParcelableExtra(Utils.EXTRA_DEVICE);
                    boolean autoConnectAfterProvisioning = result.getData().getBooleanExtra(Utils.EXTRA_AUTO_CONNECT_AFTER_PROVISIONING, false);

                    if (autoConnectAfterProvisioning && provisionedDevice != null) {
                        mProvisionedDeviceMac = provisionedDevice.getAddress();
                        mShouldAutoConnectAfterProvisioning = true;
                        mIsNewlyProvisioned = true;

                        Log.d("AUTO_CONNECT", "Provisioning completed for device: " + mProvisionedDeviceMac);

                        showConnectingUI();
                        binding.textConnectingProgress.setText(
                                String.format("Provisioning complete!\nConnecting to %s...",
                                        formatMacForDisplay(mProvisionedDeviceMac))
                        );

                        startAutoConnectAfterProvisioning();
                    } else {
                        setResultIntent(result.getData());
                    }
                }
            });

    private final ActivityResultLauncher<Intent> enableBluetooth =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    mViewModel.getScannerRepository().getScannerState().startScanning();
                }
            });

    private final ActivityResultLauncher<Intent> reconnect =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    final Intent data = result.getData();
                    if (data == null) {
                        setResult(Activity.RESULT_OK);
                    } else {
                        data.putExtra(Utils.EXTRA_NEWLY_PROVISIONED_NODE, mIsNewlyProvisioned);
                        setResult(Activity.RESULT_OK, data);
                    }
                    finish();
                    overridePendingTransition(0, 0);
                } else {
                    if (!mScanWithProxyService && mSilentConnect) {
                        showScannerUI();
                        mAutoConnectStarted = false;
                        if (targetProxyMac != null) {
                            startAutoConnectLoop();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mViewModel = new ViewModelProvider(this).get(ScannerViewModel.class);

        final Toolbar toolbar = binding.toolbar;
        toolbar.setTitle(R.string.title_scanner);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Handle incoming intent for auto-connect after provisioning (e.g., after activity recreation)
        if (getIntent() != null) {
            mScanWithProxyService = getIntent().getBooleanExtra(Utils.EXTRA_DATA_PROVISIONING_SERVICE, true);
            mSilentConnect = getIntent().getBooleanExtra(Utils.EXTRA_SILENT_CONNECT, false);

            boolean autoConnectAfterProvisioning = getIntent().getBooleanExtra(Utils.EXTRA_AUTO_CONNECT_AFTER_PROVISIONING, false);
            if (autoConnectAfterProvisioning) {
                String deviceMac = getIntent().getStringExtra(Utils.EXTRA_TARGET_PROXY_MAC);
                if (deviceMac != null) {
                    mProvisionedDeviceMac = deviceMac;
                    mShouldAutoConnectAfterProvisioning = true;
                    mSilentConnect = true;
                    mScanWithProxyService = false;

                    Log.d("AUTO_CONNECT", "Auto-connect requested from intent for MAC: " + deviceMac);

                    showConnectingUI();
                    binding.textConnectingProgress.setText(
                            String.format("Auto-connecting to provisioned device: %s...",
                                    formatMacForDisplay(deviceMac))
                    );

                    startAutoConnectAfterProvisioning();
                }
            }

            if (getSupportActionBar() != null) {
                if (mScanWithProxyService) {
                    getSupportActionBar().setSubtitle(R.string.sub_title_scanning_nodes);
                } else {
                    getSupportActionBar().setSubtitle(R.string.sub_title_scanning_proxy_node);
                }
            }
        }

        if (!mScanWithProxyService && mViewModel.getBleMeshManager().isConnected()) {
            setResult(Activity.RESULT_OK);
            finish();
            overridePendingTransition(0, 0);
            return;
        }

        final RecyclerView recyclerViewDevices = binding.recyclerViewBleDevices;
        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(this));

        final DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(recyclerViewDevices.getContext(), DividerItemDecoration.VERTICAL);
        recyclerViewDevices.addItemDecoration(dividerItemDecoration);

        final SimpleItemAnimator itemAnimator = (SimpleItemAnimator) recyclerViewDevices.getItemAnimator();
        if (itemAnimator != null) itemAnimator.setSupportsChangeAnimations(false);

        final DevicesAdapter adapter = new DevicesAdapter(this, mViewModel.getScannerRepository().getScannerResults());
        adapter.setOnItemClickListener(this);
        recyclerViewDevices.setAdapter(adapter);

        binding.noDevices.actionEnableLocation.setOnClickListener(v -> onEnableLocationClicked());
        binding.bluetoothOff.actionEnableBluetooth.setOnClickListener(v -> onEnableBluetoothClicked());
        binding.noLocationPermission.actionGrantLocationPermission.setOnClickListener(v -> onGrantLocationPermissionClicked());
        binding.noLocationPermission.actionPermissionSettings.setOnClickListener(v -> onPermissionSettingsClicked());
        binding.noBluetoothPermissions.actionGrantBluetoothPermission.setOnClickListener(v -> onGrantBluetoothPermissionClicked());

        mViewModel.getScannerRepository().getScannerState().observe(this, this::startScan);

        targetProxyMac = getIntent().getStringExtra(Utils.EXTRA_TARGET_PROXY_MAC);

        mAutoConnectHandler = new Handler();

        if (targetProxyMac != null && !mShouldAutoConnectAfterProvisioning) {
            binding.textConnectingProgress.setText(
                    String.format("Looking for device: %s...", formatMacForDisplay(targetProxyMac))
            );
        }

        SharedViewModel sharedViewModel =
                new ViewModelProvider(this).get(SharedViewModel.class);

        sharedViewModel.getDeviceNameFilter().observe(this, filterName -> {
            mViewModel.getScannerRepository().setDeviceNameFilter(filterName);
        });
    }



    @Override
    protected void onStart() {
        super.onStart();
        mScanStartTime = System.currentTimeMillis();
        mViewModel.getScannerRepository().getScannerState().startScanning();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopScan();
        stopAutoConnectLoop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoConnectLoop();
        if (mAutoConnectHandler != null) {
            mAutoConnectHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(final ExtendedBluetoothDevice device) {
        if (mViewModel.getBleMeshManager().isConnected())
            mViewModel.disconnect();

        final Intent intent;
        if (mScanWithProxyService) {
            intent = new Intent(this, ProvisioningActivity.class);
            intent.putExtra(Utils.EXTRA_DEVICE, device);
            provisioner.launch(intent);
        } else {
            intent = new Intent(this, ReconnectActivity.class);
            intent.putExtra(Utils.EXTRA_DEVICE, device);
            intent.putExtra(Utils.EXTRA_SILENT_CONNECT, false);
            reconnect.launch(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ACCESS_FINE_LOCATION) {
            mViewModel.getScannerRepository().getScannerState().startScanning();
        } else if (requestCode == REQUEST_ACCESS_BLUETOOTH_PERMISSION) {
            mViewModel.getScannerRepository().getScannerState().startScanning();
        }
    }

    private void onEnableLocationClicked() {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    private void onEnableBluetoothClicked() {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        enableBluetooth.launch(enableIntent);
    }

    private void onGrantLocationPermissionClicked() {
        Utils.markLocationPermissionRequested(this);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_ACCESS_FINE_LOCATION);
    }

    private void onGrantBluetoothPermissionClicked() {
        if (Utils.isSorAbove()) {
            Utils.markBluetoothPermissionsRequested(this);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_ACCESS_BLUETOOTH_PERMISSION);
        }
    }

    private void onPermissionSettingsClicked() {
        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    private void startScan(final ScannerStateLiveData state) {
        if (!mScanWithProxyService && (mSilentConnect || mShouldAutoConnectAfterProvisioning) && targetProxyMac != null) {
            showConnectingUI();
            updateProgressText();
        }

        // Permissions
        if (!Utils.isBluetoothScanAndConnectPermissionsGranted(this)) {
            if (!mSilentConnect && !mShouldAutoConnectAfterProvisioning) {
                binding.noBluetoothPermissions.getRoot().setVisibility(View.VISIBLE);
                binding.bluetoothOff.getRoot().setVisibility(View.GONE);
                binding.stateScanning.setVisibility(View.INVISIBLE);
                binding.noDevices.getRoot().setVisibility(View.GONE);
            }
            return;
        } else {
            binding.noBluetoothPermissions.getRoot().setVisibility(View.GONE);
        }

        if (!Utils.isLocationPermissionsGranted(this)) {
            if (!mSilentConnect && !mShouldAutoConnectAfterProvisioning) {
                binding.noLocationPermission.getRoot().setVisibility(View.VISIBLE);
                binding.bluetoothOff.getRoot().setVisibility(View.GONE);
                binding.stateScanning.setVisibility(View.INVISIBLE);
                binding.noDevices.getRoot().setVisibility(View.GONE);
            }
            return;
        } else {
            binding.noLocationPermission.getRoot().setVisibility(View.GONE);
        }

        if (!state.isBluetoothEnabled()) {
            if (!mSilentConnect && !mShouldAutoConnectAfterProvisioning) {
                binding.bluetoothOff.getRoot().setVisibility(View.VISIBLE);
                binding.stateScanning.setVisibility(View.INVISIBLE);
                binding.noDevices.getRoot().setVisibility(View.GONE);
            }
            return;
        } else {
            binding.bluetoothOff.getRoot().setVisibility(View.GONE);
        }

        final UUID scanUuid;
        if (mShouldAutoConnectAfterProvisioning) {
            scanUuid = BleMeshManager.MESH_PROXY_UUID;
            Log.d("AUTO_CONNECT", "Starting scan with PROXY UUID");
        } else {
            scanUuid = mScanWithProxyService ? BleMeshManager.MESH_PROVISIONING_UUID : BleMeshManager.MESH_PROXY_UUID;
        }

        if (!state.isScanning()) {
            mViewModel.getScannerRepository().startScan(scanUuid);
            if (!mSilentConnect && !mShouldAutoConnectAfterProvisioning) {
                binding.stateScanning.setVisibility(View.VISIBLE);
            }
        }

        if (!mSilentConnect && !mShouldAutoConnectAfterProvisioning) {
            if (state.isEmpty()) {
                binding.noDevices.getRoot().setVisibility(View.VISIBLE);
            } else {
                binding.noDevices.getRoot().setVisibility(View.GONE);
            }
        }

        if (!mScanWithProxyService && (mSilentConnect || mShouldAutoConnectAfterProvisioning) && targetProxyMac != null && !mAutoConnectStarted) {
            Log.d("AUTO_CONNECT", "Starting auto-connect loop for MAC: " + targetProxyMac);
            startAutoConnectLoop();
        }
    }

    private void startAutoConnectAfterProvisioning() {
        if (mProvisionedDeviceMac == null) {
            Log.e("AUTO_CONNECT", "startAutoConnectAfterProvisioning: mProvisionedDeviceMac is null");
            return;
        }

        targetProxyMac = mProvisionedDeviceMac;
        Log.d("AUTO_CONNECT", "startAutoConnectAfterProvisioning: target MAC set to " + targetProxyMac);

        // Stop any ongoing scan
        stopScan();

        new Handler().postDelayed(() -> {
            runOnUiThread(() -> {
                Log.d("AUTO_CONNECT", "Starting scan with PROXY UUID after delay");
                mViewModel.getScannerRepository().startScan(BleMeshManager.MESH_PROXY_UUID);

                if (!mAutoConnectStarted) {
                    startAutoConnectLoop();
                }
            });
        }, AUTO_CONNECT_AFTER_PROVISIONING_DELAY);
    }

    private void startAutoConnectLoop() {
        if (mAutoConnectStarted) return;

        mAutoConnectStarted = true;
        mScanStartTime = System.currentTimeMillis();
        Log.d("AUTO_CONNECT", "Auto-connect loop started at " + mScanStartTime);

        mAutoConnectRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - mScanStartTime;
                Log.d("AUTO_CONNECT", "Loop check: elapsed " + elapsedTime + "ms");

                if (elapsedTime > TARGET_CONNECT_TIMEOUT_MS) {
                    Log.e("AUTO_CONNECT", "Target device not found after timeout");
                    if (mShouldAutoConnectAfterProvisioning) {
                        runOnUiThread(() -> {
                            binding.textConnectingProgress.setText(
                                    "Failed to connect after provisioning.\nPlease try manual connection."
                            );
                            new Handler().postDelayed(() -> {
                                setResult(Activity.RESULT_CANCELED);
                                finish();
                            }, 3000);
                        });
                    } else {
                        showTimeoutMessage();
                    }
                    stopAutoConnectLoop();
                    return;
                }

                ExtendedBluetoothDevice targetDevice = findTargetDevice();

                if (targetDevice != null) {
                    Log.i("AUTO_CONNECT", "✅ Target device found: " + targetDevice.getAddress());
                    stopScan();
                    stopAutoConnectLoop();

                    final Intent intent = new Intent(ScannerActivity.this, ReconnectActivity.class);
                    intent.putExtra(Utils.EXTRA_DEVICE, targetDevice);
                    intent.putExtra(Utils.EXTRA_SILENT_CONNECT, true);
                    if (mIsNewlyProvisioned || mShouldAutoConnectAfterProvisioning) {
                        intent.putExtra(Utils.EXTRA_NEWLY_PROVISIONED_NODE, true);
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                    reconnect.launch(intent);
                } else {
                    Log.d("AUTO_CONNECT", "Target device not found yet, scheduling next check");
                    updateProgressText();
                    mAutoConnectHandler.postDelayed(this, AUTO_CONNECT_RETRY_DELAY_MS);
                }
            }
        };

        mAutoConnectHandler.post(mAutoConnectRunnable);
    }

    private void stopAutoConnectLoop() {
        mAutoConnectStarted = false;
        if (mAutoConnectRunnable != null) {
            mAutoConnectHandler.removeCallbacks(mAutoConnectRunnable);
            mAutoConnectRunnable = null;
        }
        Log.d("AUTO_CONNECT", "Auto-connect loop stopped");
    }

    private ExtendedBluetoothDevice findTargetDevice() {
        final ScannerLiveData resultsLiveData = mViewModel.getScannerRepository().getScannerResults();

        if (resultsLiveData == null || resultsLiveData.getDevices() == null) {
            Log.d("AUTO_CONNECT", "findTargetDevice: resultsLiveData or devices is null");
            return null;
        }

        Log.d("AUTO_CONNECT", "Scan results size: " + resultsLiveData.getDevices().size());
        for (ExtendedBluetoothDevice device : resultsLiveData.getDevices()) {
            Log.d("AUTO_CONNECT", "Found device: " + device.getName() + " [" + device.getAddress() + "]");
            if (device.getAddress() != null && device.getAddress().equalsIgnoreCase(targetProxyMac)) {
                return device;
            }
        }
        return null;
    }

    private void updateProgressText() {
        if (targetProxyMac != null && binding.textConnectingProgress != null) {
            long elapsedSeconds = (System.currentTimeMillis() - mScanStartTime) / 1000;
            String formattedMac = formatMacForDisplay(targetProxyMac);

            String progressText;
            if (mShouldAutoConnectAfterProvisioning) {
                progressText = String.format(
                        "Provisioned device: %s\nConnecting... (%d seconds)",
                        formattedMac, elapsedSeconds
                );
            } else {
                progressText = String.format(
                        "Looking for device: %s\nElapsed: %d seconds...",
                        formattedMac, elapsedSeconds
                );
            }
            binding.textConnectingProgress.setText(progressText);
        }
    }

    private void showTimeoutMessage() {
        runOnUiThread(() -> {
            binding.textConnectingProgress.setText(
                    String.format("Device %s not found after %d seconds.\nShowing available devices...",
                            formatMacForDisplay(targetProxyMac), TARGET_CONNECT_TIMEOUT_MS / 1000)
            );
            new Handler().postDelayed(() -> showScannerUI(), 2000);
        });
    }

    private String formatMacForDisplay(String mac) {
        if (mac == null) return "Unknown";
        return mac.toUpperCase().replaceAll("(.{2})", "$1:").substring(0, 17);
    }

    private void stopScan() {
        mViewModel.getScannerRepository().stopScan();
    }

    private void showConnectingUI() {
        binding.appbarLayout.setVisibility(View.VISIBLE);
        binding.recyclerViewBleDevices.setVisibility(View.GONE);
        binding.noDevices.getRoot().setVisibility(View.GONE);
        binding.bluetoothOff.getRoot().setVisibility(View.GONE);
        binding.noLocationPermission.getRoot().setVisibility(View.GONE);
        binding.noBluetoothPermissions.getRoot().setVisibility(View.GONE);
        binding.stateScanning.setVisibility(View.GONE);
        binding.connectivityProgressContainer.setVisibility(View.VISIBLE);
    }

    private void showScannerUI() {
        binding.appbarLayout.setVisibility(View.VISIBLE);
        binding.recyclerViewBleDevices.setVisibility(View.VISIBLE);
        binding.connectivityProgressContainer.setVisibility(View.GONE);
        binding.stateScanning.setVisibility(View.VISIBLE);

        targetProxyMac = null;
        mShouldAutoConnectAfterProvisioning = false;
        stopAutoConnectLoop();
    }

    private void setResultIntent(final Intent data) {
        data.putExtra(Utils.EXTRA_NEWLY_PROVISIONED_NODE, mIsNewlyProvisioned);
        setResult(Activity.RESULT_OK, data);
        finish();
    }
}