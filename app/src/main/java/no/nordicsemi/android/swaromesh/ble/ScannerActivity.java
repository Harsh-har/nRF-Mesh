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

import java.util.UUID;

@AndroidEntryPoint
public class ScannerActivity extends AppCompatActivity implements DevicesAdapter.OnItemClickListener {

    private static final int REQUEST_ACCESS_FINE_LOCATION = 1022;
    private static final int REQUEST_ACCESS_BLUETOOTH_PERMISSION = 1023;
    private static final long AUTO_CONNECT_RETRY_DELAY_MS = 1000; // Retry every 1 second
    private static final long TARGET_CONNECT_TIMEOUT_MS = 30000; // 30 seconds timeout

    private ActivityScannerBinding binding;
    private ScannerViewModel mViewModel;

    private boolean mScanWithProxyService = true;
    private boolean mSilentConnect = false;

    private boolean mAutoConnectStarted = false;
    private boolean mIsNewlyProvisioned = false;
    private String targetProxyMac;

    // New: Handler for continuous checking
    private Handler mAutoConnectHandler;
    private Runnable mAutoConnectRunnable;
    private long mScanStartTime;

    private final ActivityResultLauncher<Intent> provisioner =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    mIsNewlyProvisioned = true;
                    setResultIntent(result.getData());
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
                    // If auto connect cancelled/failed -> show manual list again
                    if (!mScanWithProxyService && mSilentConnect) {
                        showScannerUI();
                        mAutoConnectStarted = false;
                        // Restart auto-connect if target MAC is still set
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

        if (getIntent() != null) {
            mScanWithProxyService =
                    getIntent().getBooleanExtra(Utils.EXTRA_DATA_PROVISIONING_SERVICE, true);

            mSilentConnect =
                    getIntent().getBooleanExtra(Utils.EXTRA_SILENT_CONNECT, false);

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

        final DevicesAdapter adapter =
                new DevicesAdapter(this, mViewModel.getScannerRepository().getScannerResults());
        adapter.setOnItemClickListener(this);
        recyclerViewDevices.setAdapter(adapter);

        binding.noDevices.actionEnableLocation.setOnClickListener(v -> onEnableLocationClicked());
        binding.bluetoothOff.actionEnableBluetooth.setOnClickListener(v -> onEnableBluetoothClicked());
        binding.noLocationPermission.actionGrantLocationPermission.setOnClickListener(v -> onGrantLocationPermissionClicked());
        binding.noLocationPermission.actionPermissionSettings.setOnClickListener(v -> onPermissionSettingsClicked());
        binding.noBluetoothPermissions.actionGrantBluetoothPermission.setOnClickListener(v -> onGrantBluetoothPermissionClicked());

        mViewModel.getScannerRepository().getScannerState().observe(this, this::startScan);

        targetProxyMac = getIntent().getStringExtra(Utils.EXTRA_TARGET_PROXY_MAC);

        // Initialize handler for auto-connect loop
        mAutoConnectHandler = new Handler();

        // New: Setup progress UI text for targeted scanning
        if (targetProxyMac != null) {
            binding.textConnectingProgress.setText(
                    String.format("Looking for device: %s...", formatMacForDisplay(targetProxyMac))
            );
        }
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
        mAutoConnectHandler.removeCallbacksAndMessages(null);
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
        // If auto proxy connect enabled -> show progress UI (no black screen)
        if (!mScanWithProxyService && mSilentConnect && targetProxyMac != null) {
            showConnectingUI();
            updateProgressText();
        }

        // Permissions
        if (!Utils.isBluetoothScanAndConnectPermissionsGranted(this)) {
            if (!mSilentConnect) {
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
            if (!mSilentConnect) {
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
            if (!mSilentConnect) {
                binding.bluetoothOff.getRoot().setVisibility(View.VISIBLE);
                binding.stateScanning.setVisibility(View.INVISIBLE);
                binding.noDevices.getRoot().setVisibility(View.GONE);
            }
            return;
        } else {
            binding.bluetoothOff.getRoot().setVisibility(View.GONE);
        }

        // Start scan
        final UUID scanUuid = mScanWithProxyService
                ? BleMeshManager.MESH_PROVISIONING_UUID
                : BleMeshManager.MESH_PROXY_UUID;

        if (!state.isScanning()) {
            mViewModel.getScannerRepository().startScan(scanUuid);

            if (!mSilentConnect) {
                binding.stateScanning.setVisibility(View.VISIBLE);
            }
        }

        // Normal empty UI only for manual/provisioning
        if (!mSilentConnect) {
            if (state.isEmpty()) {
                binding.noDevices.getRoot().setVisibility(View.VISIBLE);
            } else {
                binding.noDevices.getRoot().setVisibility(View.GONE);
            }
        }

        // Auto connect in proxy mode (background) - Start the loop
        if (!mScanWithProxyService && mSilentConnect && targetProxyMac != null && !mAutoConnectStarted) {
            startAutoConnectLoop();
        }
    }

    /**
     * NEW: Start continuous auto-connect loop
     */
    private void startAutoConnectLoop() {
        if (mAutoConnectStarted) return;

        mAutoConnectStarted = true;
        mScanStartTime = System.currentTimeMillis();

        // Create runnable that checks for target device periodically
        mAutoConnectRunnable = new Runnable() {
            @Override
            public void run() {
                // Check timeout
                long elapsedTime = System.currentTimeMillis() - mScanStartTime;
                if (elapsedTime > TARGET_CONNECT_TIMEOUT_MS) {
                    Log.d("PROXY_SCAN", "Target device not found after timeout");
                    showTimeoutMessage();
                    stopAutoConnectLoop();
                    return;
                }

                // Check if target device is found
                ExtendedBluetoothDevice targetDevice = findTargetDevice();

                if (targetDevice != null) {
                    Log.d("PROXY_SCAN", "✅ TARGET MAC MATCH FOUND: " + targetDevice.getAddress());
                    stopScan();
                    stopAutoConnectLoop();

                    // Connect to target device
                    final Intent intent = new Intent(ScannerActivity.this, ReconnectActivity.class);
                    intent.putExtra(Utils.EXTRA_DEVICE, targetDevice);
                    intent.putExtra(Utils.EXTRA_SILENT_CONNECT, true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                    reconnect.launch(intent);
                } else {
                    // Update progress text
                    updateProgressText();

                    // Schedule next check
                    mAutoConnectHandler.postDelayed(this, AUTO_CONNECT_RETRY_DELAY_MS);
                }
            }
        };

        // Start the loop
        mAutoConnectHandler.post(mAutoConnectRunnable);
    }

    /**
     * NEW: Stop auto-connect loop
     */
    private void stopAutoConnectLoop() {
        mAutoConnectStarted = false;
        if (mAutoConnectRunnable != null) {
            mAutoConnectHandler.removeCallbacks(mAutoConnectRunnable);
            mAutoConnectRunnable = null;
        }
    }

    /**
     * NEW: Find device with target MAC address
     */
    private ExtendedBluetoothDevice findTargetDevice() {
        final ScannerLiveData resultsLiveData =
                mViewModel.getScannerRepository().getScannerResults();

        if (resultsLiveData == null ||
                resultsLiveData.getDevices() == null ||
                resultsLiveData.getDevices().isEmpty()) {
            return null;
        }

        for (ExtendedBluetoothDevice device : resultsLiveData.getDevices()) {
            Log.d("PROXY_SCAN",
                    "Checking device MAC = " + device.getAddress() +
                            " | Target MAC = " + targetProxyMac);

            if (device.getAddress() != null &&
                    device.getAddress().equalsIgnoreCase(targetProxyMac)) {
                return device;
            }
        }

        return null;
    }

    /**
     * NEW: Update progress text with elapsed time
     */
    private void updateProgressText() {
        if (targetProxyMac != null && binding.textConnectingProgress != null) {
            long elapsedSeconds = (System.currentTimeMillis() - mScanStartTime) / 1000;
            String formattedMac = formatMacForDisplay(targetProxyMac);

            String progressText = String.format(
                    "Looking for device: %s\nElapsed: %d seconds...",
                    formattedMac,
                    elapsedSeconds
            );

            binding.textConnectingProgress.setText(progressText);
        }
    }

    /**
     * NEW: Show timeout message and fallback to manual UI
     */
    private void showTimeoutMessage() {
        runOnUiThread(() -> {
            binding.textConnectingProgress.setText(
                    String.format("Device %s not found after %d seconds.\nShowing available devices...",
                            formatMacForDisplay(targetProxyMac),
                            TARGET_CONNECT_TIMEOUT_MS / 1000)
            );

            // Wait 2 seconds then show manual UI
            new Handler().postDelayed(() -> {
                showScannerUI();
                // Optionally show a snackbar

            }, 2000);
        });
    }

    /**
     * NEW: Format MAC address for display
     */
    private String formatMacForDisplay(String mac) {
        if (mac == null) return "Unknown";
        // Format as XX:XX:XX:XX:XX:XX
        return mac.toUpperCase().replaceAll("(.{2})", "$1:").substring(0, 17);
    }

    private void stopScan() {
        mViewModel.getScannerRepository().stopScan();
    }

    /**
     * Show progress UI for background proxy connect
     */
    private void showConnectingUI() {
        // Keep toolbar visible (looks clean)
        binding.appbarLayout.setVisibility(View.VISIBLE);

        // Hide list and empty states
        binding.recyclerViewBleDevices.setVisibility(View.GONE);
        binding.noDevices.getRoot().setVisibility(View.GONE);
        binding.bluetoothOff.getRoot().setVisibility(View.GONE);
        binding.noLocationPermission.getRoot().setVisibility(View.GONE);
        binding.noBluetoothPermissions.getRoot().setVisibility(View.GONE);
        binding.stateScanning.setVisibility(View.GONE);

        // Show progress container (loader)
        binding.connectivityProgressContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Restore manual scanner UI
     */
    private void showScannerUI() {
        binding.appbarLayout.setVisibility(View.VISIBLE);
        binding.recyclerViewBleDevices.setVisibility(View.VISIBLE);
        binding.connectivityProgressContainer.setVisibility(View.GONE);
        binding.stateScanning.setVisibility(View.VISIBLE);

        // Clear target MAC to prevent auto-connect attempts
        targetProxyMac = null;
        stopAutoConnectLoop();
    }

    private void setResultIntent(final Intent data) {
        data.putExtra(Utils.EXTRA_NEWLY_PROVISIONED_NODE, mIsNewlyProvisioned);
        setResult(Activity.RESULT_OK, data);
        finish();
    }
}