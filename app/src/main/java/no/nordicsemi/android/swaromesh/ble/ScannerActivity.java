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

    private boolean mSilentConnect = false; // determines automatic proxy connection
    private boolean mProxyEnabled = false; // will be passed from NetworkFragment

    private final ActivityResultLauncher<Intent> provisioner =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
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

        Toolbar toolbar = binding.toolbar;
        toolbar.setTitle(R.string.title_scanner);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent() != null) {
            mScanWithProxyService = getIntent().getBooleanExtra(Utils.EXTRA_DATA_PROVISIONING_SERVICE, true);
            mSilentConnect = getIntent().getBooleanExtra(Utils.EXTRA_SILENT_CONNECT, false);
            mProxyEnabled = !mScanWithProxyService && mSilentConnect; // Only enable auto-connect in proxy mode

            if (mScanWithProxyService) {
                getSupportActionBar().setSubtitle(R.string.sub_title_scanning_nodes);
            } else {
                getSupportActionBar().setSubtitle(R.string.sub_title_scanning_proxy_node);
            }
        }

        setupRecyclerView();

        binding.noDevices.actionEnableLocation.setOnClickListener(v -> onEnableLocationClicked());
        binding.bluetoothOff.actionEnableBluetooth.setOnClickListener(v -> onEnableBluetoothClicked());
        binding.noLocationPermission.actionGrantLocationPermission.setOnClickListener(v -> onGrantLocationPermissionClicked());
        binding.noLocationPermission.actionPermissionSettings.setOnClickListener(v -> onPermissionSettingsClicked());
        binding.noBluetoothPermissions.actionGrantBluetoothPermission.setOnClickListener(v -> onGrantBluetoothPermissionClicked());

        mViewModel.getScannerRepository().getScannerState().observe(this, this::startScan);
    }

    private void setupRecyclerView() {
        RecyclerView recyclerViewDevices = binding.recyclerViewBleDevices;
        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(this));

        SimpleItemAnimator itemAnimator = (SimpleItemAnimator) recyclerViewDevices.getItemAnimator();
        if (itemAnimator != null) itemAnimator.setSupportsChangeAnimations(false);

        DevicesAdapter adapter = new DevicesAdapter(this, mViewModel.getScannerRepository().getScannerResults());
        adapter.setOnItemClickListener(this);
        recyclerViewDevices.setAdapter(adapter);
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(ExtendedBluetoothDevice device) {
        if (mViewModel.getBleMeshManager().isConnected()) mViewModel.disconnect();

        Intent intent;
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACCESS_FINE_LOCATION || requestCode == REQUEST_ACCESS_BLUETOOTH_PERMISSION) {
            mViewModel.getScannerRepository().getScannerState().startScanning();
        }
    }

    private void startScan(ScannerStateLiveData state) {
        if (!Utils.isBluetoothScanAndConnectPermissionsGranted(this) || !Utils.isLocationPermissionsGranted(this)) return;

        if (!state.isBluetoothEnabled()) return;

        UUID scanUuid = mScanWithProxyService ? BleMeshManager.MESH_PROVISIONING_UUID : BleMeshManager.MESH_PROXY_UUID;
        if (!state.isScanning()) mViewModel.getScannerRepository().startScan(scanUuid);

        // ------------------- AUTO CONNECT (Proxy Mode) -------------------
        if (!mScanWithProxyService && mProxyEnabled && !mAutoConnectStarted) {
            mAutoConnectStarted = true;

            if (mSilentConnect) binding.getRoot().setVisibility(View.INVISIBLE);

            binding.getRoot().postDelayed(() -> {
                if (mViewModel.getBleMeshManager().isConnected()) {
                    setResult(Activity.RESULT_OK);
                    return;
                }

                if (mViewModel.getScannerRepository().getScannerResults().getDevices().isEmpty()) {
                    mAutoConnectStarted = false;
                    binding.getRoot().postDelayed(() -> startScan(state), AUTO_CONNECT_DELAY_MS);
                    return;
                }

                ExtendedBluetoothDevice device = mViewModel.getScannerRepository().getScannerResults().getDevices().get(0);
                stopScan();
                Intent intent = new Intent(this, ReconnectActivity.class);
                intent.putExtra(Utils.EXTRA_DEVICE, device);
                reconnect.launch(intent);
            }, AUTO_CONNECT_DELAY_MS);
        }
    }

    private void stopScan() {
        mViewModel.getScannerRepository().stopScan();
    }

    private void setResultIntent(Intent data) {
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    private void onEnableLocationClicked() {
        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    private void onEnableBluetoothClicked() {
        enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
    }

    private void onGrantLocationPermissionClicked() {
        Utils.markLocationPermissionRequested(this);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
    }

    private void onGrantBluetoothPermissionClicked() {
        if (Utils.isSorAbove()) {
            Utils.markBluetoothPermissionsRequested(this);
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, REQUEST_ACCESS_BLUETOOTH_PERMISSION);
        }
    }

    private void onPermissionSettingsClicked() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null)));
    }
}
