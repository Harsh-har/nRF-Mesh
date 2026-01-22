package no.nordicsemi.android.swaromesh;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dagger.hilt.android.AndroidEntryPoint;
import no.nordicsemi.android.swaromesh.transport.ProvisionedMeshNode;
import no.nordicsemi.android.swaromesh.ble.ScannerActivity;
import no.nordicsemi.android.swaromesh.databinding.FragmentNetworkBinding;
import no.nordicsemi.android.swaromesh.dialog.DialogFragmentDeleteNode;
import no.nordicsemi.android.swaromesh.dialog.DialogFragmentError;
import no.nordicsemi.android.swaromesh.node.NodeConfigurationActivity;
import no.nordicsemi.android.swaromesh.node.adapter.NodeAdapter;
import no.nordicsemi.android.swaromesh.utils.Utils;
import no.nordicsemi.android.swaromesh.viewmodels.SharedViewModel;
import no.nordicsemi.android.swaromesh.widgets.ItemTouchHelperAdapter;
import no.nordicsemi.android.swaromesh.widgets.RemovableItemTouchHelperCallback;
import no.nordicsemi.android.swaromesh.widgets.RemovableViewHolder;

import static android.app.Activity.RESULT_OK;

@AndroidEntryPoint
public class NetworkFragment extends Fragment implements
        NodeAdapter.OnItemClickListener,
        ItemTouchHelperAdapter,
        DialogFragmentDeleteNode.DialogFragmentDeleteNodeListener {

    private FragmentNetworkBinding binding;
    private SharedViewModel mViewModel;
    private NodeAdapter mNodeAdapter;

    private final ActivityResultLauncher<Intent> provisioner =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleActivityResult);

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup viewGroup, @Nullable final Bundle savedInstanceState) {
        binding = FragmentNetworkBinding.inflate(getLayoutInflater());
        mViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        final ExtendedFloatingActionButton fab = binding.fabAddNode;
        final RecyclerView mRecyclerViewNodes = binding.recyclerViewProvisionedNodes;
        final View noNetworksConfiguredView = binding.noNetworksConfigured.getRoot();

        // ------------------- RecyclerView Setup -------------------
        mNodeAdapter = new NodeAdapter(this, mViewModel.getNodes());
        mNodeAdapter.setOnItemClickListener(this);

        mRecyclerViewNodes.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerViewNodes.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        final ItemTouchHelper.Callback itemTouchHelperCallback = new RemovableItemTouchHelperCallback(this);
        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerViewNodes);

        mRecyclerViewNodes.setAdapter(mNodeAdapter);

        // ------------------- Observe Nodes -------------------
        mViewModel.getNodes().observe(getViewLifecycleOwner(), nodes -> {
            if (nodes != null && !nodes.isEmpty()) {
                noNetworksConfiguredView.setVisibility(View.GONE);
            } else {
                noNetworksConfiguredView.setVisibility(View.VISIBLE);
            }
            requireActivity().invalidateOptionsMenu();
        });

        mViewModel.isConnectedToProxy().observe(getViewLifecycleOwner(), isConnected -> {
            if (isConnected != null) {
                requireActivity().invalidateOptionsMenu();
            }
        });

        // ------------------- FAB Scroll Logic -------------------
        mRecyclerViewNodes.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull final RecyclerView recyclerView, final int dx, final int dy) {
                super.onScrolled(recyclerView, dx, dy);
                final LinearLayoutManager m = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (m != null) {
                    if (m.findFirstCompletelyVisibleItemPosition() == 0) {
                        fab.extend();
                    } else {
                        fab.shrink();
                    }
                }
            }
        });

        fab.setOnClickListener(v -> {
            final Intent intent = new Intent(requireContext(), ScannerActivity.class);
            intent.putExtra(Utils.EXTRA_DATA_PROVISIONING_SERVICE, true);
            provisioner.launch(intent);
        });

        // ------------------- SearchView Setup -------------------
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mNodeAdapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mNodeAdapter.filter(newText);
                return true;
            }
        });

        // ------------------- Show empty view on search results -------------------
        mNodeAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (mNodeAdapter.getItemCount() == 0) {
                    noNetworksConfiguredView.setVisibility(View.VISIBLE);
                } else {
                    noNetworksConfiguredView.setVisibility(View.GONE);
                }
            }
        });

        return binding.getRoot();
    }

    // ------------------- NodeAdapter Listener -------------------
    @Override
    public void onConfigureClicked(final ProvisionedMeshNode node) {
        mViewModel.setSelectedMeshNode(node);
        final Intent meshConfigurationIntent = new Intent(requireActivity(), NodeConfigurationActivity.class);
        requireActivity().startActivity(meshConfigurationIntent);
    }

    // ------------------- Swipe to delete -------------------
    @Override
    public void onItemDismiss(final RemovableViewHolder viewHolder) {
        final int position = viewHolder.getAdapterPosition();
        if (!mNodeAdapter.isEmpty()) {
            final DialogFragmentDeleteNode fragmentDeleteNode = DialogFragmentDeleteNode.newInstance(position);
            fragmentDeleteNode.show(getChildFragmentManager(), null);
        }
    }

    @Override
    public void onItemDismissFailed(final RemovableViewHolder viewHolder) {
        //Do nothing
    }

    @Override
    public void onNodeDeleteConfirmed(final int position) {
        final ProvisionedMeshNode node = mNodeAdapter.getItem(position);
        if (mViewModel.getNetworkLiveData().getMeshNetwork().deleteNode(node)) {
            mViewModel.displaySnackBar(requireActivity(), binding.container, getString(R.string.node_deleted), Snackbar.LENGTH_LONG);
        }
    }

    @Override
    public void onNodeDeleteCancelled(final int position) {
        mNodeAdapter.notifyItemChanged(position);
    }

    // ------------------- Activity Result Handler -------------------
    private void handleActivityResult(final ActivityResult result) {
        final Intent data = result.getData();
        if (result.getResultCode() == RESULT_OK && data != null) {
            final boolean provisioningSuccess = data.getBooleanExtra(Utils.PROVISIONING_COMPLETED, false);
            final DialogFragmentError fragmentConfigError;
            if (provisioningSuccess) {
                final boolean provisionerUnassigned = data.getBooleanExtra(Utils.PROVISIONER_UNASSIGNED, false);
                if (provisionerUnassigned) {
                    fragmentConfigError =
                            DialogFragmentError.newInstance(getString(R.string.title_init_config_error)
                                    , getString(R.string.provisioner_unassigned_msg));
                    fragmentConfigError.show(getChildFragmentManager(), null);
                } else {
                    final boolean compositionDataReceived = data.getBooleanExtra(Utils.COMPOSITION_DATA_COMPLETED, false);
                    final boolean defaultTtlGetCompleted = data.getBooleanExtra(Utils.DEFAULT_GET_COMPLETED, false);
                    final boolean appKeyAddCompleted = data.getBooleanExtra(Utils.APP_KEY_ADD_COMPLETED, false);
                    final String title = getString(R.string.title_init_config_error);
                    final String message;
                    if (compositionDataReceived) {
                        if (defaultTtlGetCompleted) {
                            if (!appKeyAddCompleted) {
                                showErrorDialog(title, getString(R.string.init_config_error_app_key_msg));
                            }
                        } else {
                            showErrorDialog(title, getString(R.string.init_config_error_default_ttl_get_msg));
                        }
                    }
                }
            }
            requireActivity().invalidateOptionsMenu();
        }
    }

    private void showErrorDialog(@NonNull final String title, @NonNull final String message) {
        final DialogFragmentError dialogFragmentError = DialogFragmentError.newInstance(title, message);
        dialogFragmentError.show(getChildFragmentManager(), null);
    }
}
