package no.nordicsemi.android.nrfmesh.node.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.mesh.models.VendorModel;
import no.nordicsemi.android.mesh.transport.Element;
import no.nordicsemi.android.mesh.transport.MeshModel;
import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode;
import no.nordicsemi.android.mesh.utils.CompositionDataParser;
import no.nordicsemi.android.nrfmesh.R;
import no.nordicsemi.android.nrfmesh.databinding.ElementItemBinding;

public class ElementAdapter extends RecyclerView.Adapter<ElementAdapter.ViewHolder> {

    private final AsyncListDiffer<Element> differ =
            new AsyncListDiffer<>(this, new ElementDiffCallback());

    private OnItemClickListener mOnItemClickListener;
    private ProvisionedMeshNode meshNode;

    // ✅ Generic OnOff Server SIG Model ID
    private static final int GENERIC_ON_OFF_SERVER_MODEL_ID = 0x1000;

    public void update(final ProvisionedMeshNode meshNode) {
        this.meshNode = meshNode;
        differ.submitList(populateList(meshNode));
    }

    private List<Element> populateList(@NonNull final ProvisionedMeshNode meshNode) {
        final List<Element> elements = new ArrayList<>();
        for (Element element : meshNode.getElements().values()) {
            try {
                elements.add(element.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        return elements;
    }

    public void setOnItemClickListener(@NonNull final OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        return new ViewHolder(
                ElementItemBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final Element element = differ.getCurrentList().get(position);

        holder.mElementTitle.setText(element.getName());

        // ✅ COUNT ONLY GENERIC ONOFF SERVER
        final int modelCount = getGenericOnOffServerCount(element);

        holder.mElementSubtitle.setText(
                holder.mElementSubtitle.getContext()
                        .getString(R.string.model_count, modelCount)
        );

        inflateModelViews(holder, new ArrayList<>(element.getMeshModels().values()));
    }

    /**
     * ✅ Count only Generic OnOff Server models
     */
    private int getGenericOnOffServerCount(@NonNull Element element) {
        int count = 0;
        for (MeshModel model : element.getMeshModels().values()) {
            if (model.getModelId() == GENERIC_ON_OFF_SERVER_MODEL_ID) {
                count++;
            }
        }
        return count;
    }

    /**
     * ✅ Inflate ONLY Generic OnOff Server
     * ❌ All other models hidden from user
     */
    private void inflateModelViews(final ViewHolder holder,
                                   final List<MeshModel> models) {

        holder.mModelContainer.removeAllViews();
        final Context context = holder.mModelContainer.getContext();

        for (MeshModel model : models) {

            // ❌ Skip all except Generic OnOff Server
            if (model.getModelId() != GENERIC_ON_OFF_SERVER_MODEL_ID) {
                continue;
            }

            final View modelView = LayoutInflater.from(context)
                    .inflate(R.layout.model_item, holder.mElementContainer, false);

            modelView.setTag(model.getModelId());

            final TextView modelNameView = modelView.findViewById(R.id.title);
            final TextView modelIdView = modelView.findViewById(R.id.subtitle);

            modelNameView.setText(model.getModelName());

            if (model instanceof VendorModel) {
                modelIdView.setText(
                        context.getString(
                                R.string.format_vendor_model_id,
                                CompositionDataParser.formatModelIdentifier(
                                        model.getModelId(), true
                                )
                        )
                );
            } else {
                modelIdView.setText(
                        context.getString(
                                R.string.format_sig_model_id,
                                CompositionDataParser.formatModelIdentifier(
                                        model.getModelId(), true
                                )
                        )
                );
            }

            modelView.setOnClickListener(v -> {
                final int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    final Element element = differ.getCurrentList().get(pos);
                    mOnItemClickListener.onModelClicked(meshNode, element, model);
                }
            });

            holder.mModelContainer.addView(modelView);
        }

        // ❗ Hide container if no Generic OnOff Server
        holder.mModelContainer.setVisibility(
                holder.mModelContainer.getChildCount() == 0
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    @Override
    public long getItemId(final int position) {
        return differ.getCurrentList().get(position).getElementAddress();
    }

    public interface OnItemClickListener {
        void onElementClicked(@NonNull final Element element);

        void onModelClicked(@NonNull final ProvisionedMeshNode meshNode,
                            @NonNull final Element element,
                            @NonNull final MeshModel model);
    }

    final class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        ConstraintLayout mElementContainer;
        ImageView mIcon;
        TextView mElementTitle;
        TextView mElementSubtitle;
        ImageButton mElementExpand;
        ImageButton mEdit;
        LinearLayout mModelContainer;

        private ViewHolder(@NonNull final ElementItemBinding binding) {
            super(binding.getRoot());
            mElementContainer = binding.elementItemContainer;
            mIcon = binding.icon;
            mElementTitle = binding.elementTitle;
            mElementSubtitle = binding.elementSubtitle;
            mElementExpand = binding.elementExpand;
            mEdit = binding.edit;
            mModelContainer = binding.modelContainer;

            mElementExpand.setOnClickListener(this);
            mEdit.setOnClickListener(this);
        }

        @Override
        public void onClick(final View v) {
            if (v.getId() == R.id.element_expand) {
                if (mModelContainer.getVisibility() == View.VISIBLE) {
                    mElementExpand.setImageResource(R.drawable.ic_round_expand_more);
                    mModelContainer.setVisibility(View.GONE);
                } else {
                    mElementExpand.setImageResource(R.drawable.ic_round_expand_less);
                    mModelContainer.setVisibility(View.VISIBLE);
                }
            } else if (v.getId() == R.id.edit) {
                mOnItemClickListener.onElementClicked(
                        differ.getCurrentList().get(getAbsoluteAdapterPosition())
                );
            }
        }
    }
}
