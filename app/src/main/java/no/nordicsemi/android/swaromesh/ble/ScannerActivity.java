package no.nordicsemi.android.swaromesh.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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

import java.util.UUID;

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

@AndroidEntryPoint
public class ScannerActivity extends AppCompatActivity implements DevicesAdapter.OnItemClickListener {

    private static final int REQUEST_ACCESS_FINE_LOCATION = 1022;
    private static final int REQUEST_ACCESS_BLUETOOTH_PERMISSION = 1023;

    private static final long AUTO_CONNECT_DELAY_MS = 100;

    private ActivityScannerBinding binding;
    private ScannerViewModel mViewModel;
    private boolean mScanWithProxyService;
    private boolean mAutoConnectStarted = false;
    private boolean mIsNewlyProvisioned = false; // Flag to indicate new node provisioning

    // Provisioning launcher (FAB -> Add New Node)
    private final ActivityResultLauncher<Intent> provisioner =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    mIsNewlyProvisioned = true; // Mark that we just provisioned a new node
                    setResultIntent(result.getData());
                }
            });

    private final ActivityResultLauncher<Intent> enableBluetooth =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    startScan(mViewModel.getScannerRepository().getScannerState());
                }
            });

    private final ActivityResultLauncher<Intent> reconnect =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    final Intent data = result.getData();
                    if (data == null) {
                        setResult(Activity.RESULT_OK);
                    } else {
                        // Pass the "newly provisioned node" flag to network fragment
                        data.putExtra(Utils.EXTRA_NEWLY_PROVISIONED_NODE, mIsNewlyProvisioned);
                        setResult(Activity.RESULT_OK, data);
                    }
                    finish();
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
        }

        if (!mScanWithProxyService && mViewModel.getBleMeshManager().isConnected()) {
            setResult(Activity.RESULT_OK);
            finish();
            return;
        }

        final RecyclerView recyclerViewDevices = binding.recyclerViewBleDevices;
        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDevices.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
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
    }

    @Override
    protected void onStart() {
        super.onStart();
        mViewModel.getScannerRepository().getScannerState().startScanning();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopScan();
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
            reconnect.launch(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACCESS_FINE_LOCATION || requestCode == REQUEST_ACCESS_BLUETOOTH_PERMISSION) {
            mViewModel.getScannerRepository().getScannerState().startScanning();
        }
    }

    private void onEnableLocationClicked() {
        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    private void onEnableBluetoothClicked() {
        enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
    }

    private void onGrantLocationPermissionClicked() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_ACCESS_FINE_LOCATION);
    }

    private void onGrantBluetoothPermissionClicked() {
        if (Utils.isSorAbove()) {
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
        if (Utils.isBluetoothScanAndConnectPermissionsGranted(this) && Utils.isLocationPermissionsGranted(this)) {

            binding.noBluetoothPermissions.getRoot().setVisibility(View.GONE);
            binding.noLocationPermission.getRoot().setVisibility(View.GONE);

            if (state.isBluetoothEnabled()) {
                binding.bluetoothOff.getRoot().setVisibility(View.GONE);

                if (!state.isScanning()) {
                    final UUID scanUuid = mScanWithProxyService ?
                            BleMeshManager.MESH_PROVISIONING_UUID :
                            BleMeshManager.MESH_PROXY_UUID;
                    mViewModel.getScannerRepository().startScan(scanUuid);
                    binding.stateScanning.setVisibility(View.VISIBLE);
                }

                // ------------------- AUTO CONNECT LOGIC (Proxy Mode) -------------------
                if (!mScanWithProxyService && !mAutoConnectStarted) {
                    mAutoConnectStarted = true;

                    binding.stateScanning.setVisibility(View.INVISIBLE);
                    binding.noDevices.getRoot().setVisibility(View.GONE);
                    binding.bluetoothOff.getRoot().setVisibility(View.GONE);
                    binding.noLocationPermission.getRoot().setVisibility(View.GONE);
                    binding.noBluetoothPermissions.getRoot().setVisibility(View.GONE);

                    binding.getRoot().postDelayed(() -> {
                        if (mViewModel.getBleMeshManager().isConnected()) {
                            setResult(Activity.RESULT_OK);
                            finish();
                            return;
                        }

                        final ScannerLiveData resultsLiveData = mViewModel.getScannerRepository().getScannerResults();

                        if (resultsLiveData != null && resultsLiveData.getDevices() != null && !resultsLiveData.getDevices().isEmpty()) {
                            final ExtendedBluetoothDevice device = resultsLiveData.getDevices().get(0);
                            stopScan();
                            final Intent intent = new Intent(this, ReconnectActivity.class);
                            intent.putExtra(Utils.EXTRA_DEVICE, device);
                            reconnect.launch(intent);
                        } else {
                            mAutoConnectStarted = false;
                            binding.getRoot().postDelayed(this::startScanRetry, 100);
                        }

                    }, AUTO_CONNECT_DELAY_MS);
                }

            } else {
                binding.bluetoothOff.getRoot().setVisibility(View.VISIBLE);
                binding.stateScanning.setVisibility(View.INVISIBLE);
            }

            if (state.isEmpty()) {
                binding.noDevices.getRoot().setVisibility(View.VISIBLE);
            } else {
                binding.noDevices.getRoot().setVisibility(View.GONE);
            }
        } else {
            binding.noBluetoothPermissions.getRoot().setVisibility(View.VISIBLE);
        }
    }

    private void startScanRetry() {
        if (!mScanWithProxyService) {
            final ScannerStateLiveData state = mViewModel.getScannerRepository().getScannerState();
            if (state.isBluetoothEnabled() && !state.isScanning()) {
                mViewModel.getScannerRepository().startScan(BleMeshManager.MESH_PROXY_UUID);
            }
        }
    }

    private void stopScan() {
        mViewModel.getScannerRepository().stopScan();
    }

    private void setResultIntent(final Intent data) {
        data.putExtra(Utils.EXTRA_NEWLY_PROVISIONED_NODE, mIsNewlyProvisioned);
        setResult(Activity.RESULT_OK, data);
        finish();
    }
}
