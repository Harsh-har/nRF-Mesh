
package no.nordicsemi.android.nrfmesh.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import no.nordicsemi.android.nrfmesh.databinding.ProgressItemBinding;
import no.nordicsemi.android.nrfmesh.viewmodels.ProvisionerProgress;
import no.nordicsemi.android.nrfmesh.viewmodels.ProvisioningStatusLiveData;

public class ProvisioningProgressAdapter extends RecyclerView.Adapter<ProvisioningProgressAdapter.ViewHolder> {
    private final List<ProvisionerProgress> mProgress = new ArrayList<>();

    public ProvisioningProgressAdapter(@NonNull final ProvisioningStatusLiveData provisioningProgress) {
        this.mProgress.addAll(provisioningProgress.getStateList());
    }

    @NonNull
    @Override
    public ProvisioningProgressAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        return new ViewHolder(ProgressItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final ProvisionerProgress provisioningProgress = mProgress.get(position);
        if (provisioningProgress != null) {
            holder.image.setImageDrawable(ContextCompat.getDrawable(holder.itemView.getContext(), provisioningProgress.getResId()));
            holder.progress.setText(provisioningProgress.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return mProgress.size();
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public void refresh(final ArrayList<ProvisionerProgress> stateList) {
        mProgress.clear();
        mProgress.addAll(stateList);
        notifyDataSetChanged();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView progress;



        private ViewHolder(@NonNull final ProgressItemBinding binding) {
            super(binding.getRoot());
            image = binding.image;
            progress = binding.text;
        }
    }
}
