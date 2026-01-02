

package no.nordicsemi.android.nrfmesh;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import dagger.hilt.android.AndroidEntryPoint;
import no.nordicsemi.android.nrfmesh.databinding.ActivityMainBinding;
import no.nordicsemi.android.nrfmesh.viewmodels.SharedViewModel;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements
        NavigationBarView.OnItemSelectedListener,
        NavigationBarView.OnItemReselectedListener {

    private static final String CURRENT_FRAGMENT = "CURRENT_FRAGMENT";

    private SharedViewModel mViewModel;

    private NetworkFragment mNetworkFragment;
    private GroupsFragment mGroupsFragment;
    private ProxyFilterFragment mProxyFilterFragment;
    private Fragment mSettingsFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        mViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.app_name);

        mNetworkFragment =
                (NetworkFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_network);
        mGroupsFragment =
                (GroupsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_groups);
        mProxyFilterFragment =
                (ProxyFilterFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_proxy);
        mSettingsFragment =
                getSupportFragmentManager().findFragmentById(R.id.fragment_settings);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        bottomNavigationView.setOnItemSelectedListener(this);
        bottomNavigationView.setOnItemReselectedListener(this);

        if (savedInstanceState == null) {
            onNavigationItemSelected(bottomNavigationView.getMenu()
                    .findItem(R.id.action_network));
        } else {
            bottomNavigationView.setSelectedItemId(
                    savedInstanceState.getInt(CURRENT_FRAGMENT));
        }
    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        Boolean isConnected = mViewModel.isConnectedToProxy().getValue();
//        getMenuInflater().inflate(
//                isConnected != null && isConnected
//                        ? R.menu.disconnect
//                        : R.menu.connect,
//                menu
//        );
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.action_connect) {
//            mViewModel.navigateToScannerActivity(this, false);
//            return true;
//        } else if (item.getItemId() == R.id.action_disconnect) {
//            mViewModel.disconnect();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (item.getItemId() == R.id.action_network) {
            ft.show(mNetworkFragment).hide(mGroupsFragment)
                    .hide(mProxyFilterFragment).hide(mSettingsFragment);
        }
        else if (item.getItemId() == R.id.action_groups) {
            ft.hide(mNetworkFragment).show(mGroupsFragment)
                    .hide(mProxyFilterFragment).hide(mSettingsFragment);
        }
        else if (item.getItemId() == R.id.action_proxy) {
            ft.hide(mNetworkFragment).hide(mGroupsFragment)
                    .show(mProxyFilterFragment).hide(mSettingsFragment);
        }
        else if (item.getItemId() == R.id.action_settings) {
            ft.hide(mNetworkFragment).hide(mGroupsFragment)
                    .hide(mProxyFilterFragment).show(mSettingsFragment);
        }

        ft.commit();
        invalidateOptionsMenu();
        return true;
    }

    @Override
    public void onNavigationItemReselected(@NonNull MenuItem item) {
    }
}
