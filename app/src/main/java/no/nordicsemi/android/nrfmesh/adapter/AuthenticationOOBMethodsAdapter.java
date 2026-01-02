
package no.nordicsemi.android.nrfmesh.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import no.nordicsemi.android.mesh.utils.AuthenticationOOBMethods;
import no.nordicsemi.android.nrfmesh.databinding.OobTypeItemBinding;

public class AuthenticationOOBMethodsAdapter extends BaseAdapter {

    private final Context mContext;
    private final ArrayList<AuthenticationOOBMethods> mOOBTypes = new ArrayList<>();

    /**
     * Constructs AuthenticationOOBMethodsAdapter
     *
     * @param context  Context
     * @param oobTypes List of oob types
     */
    public AuthenticationOOBMethodsAdapter(@NonNull final Context context, @NonNull final List<AuthenticationOOBMethods> oobTypes) {
        this.mContext = context;
        mOOBTypes.clear();
        mOOBTypes.addAll(oobTypes);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (mOOBTypes.isEmpty())
            return 1;
        return mOOBTypes.size();
    }

    @Override
    public AuthenticationOOBMethods getItem(final int position) {
        return mOOBTypes.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View view = convertView;
        ViewHolder viewHolder;
        if (view == null) {
            final OobTypeItemBinding binding = OobTypeItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            view = binding.getRoot();
            viewHolder = new ViewHolder(binding);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        final AuthenticationOOBMethods oobType = mOOBTypes.get(position);
        viewHolder.oobTypeName.setText(AuthenticationOOBMethods.getAuthenticationMethodName(oobType));
        return view;
    }

    public boolean isEmpty() {
        return mOOBTypes.isEmpty();
    }

    public static final class ViewHolder {
        TextView oobTypeName;

        private ViewHolder(final OobTypeItemBinding binding) {
            oobTypeName = binding.oobTypeName;
        }
    }
}
