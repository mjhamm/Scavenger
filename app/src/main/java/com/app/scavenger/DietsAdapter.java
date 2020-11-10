package com.app.scavenger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DietsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int DIET_ITEM = 0;
    public static final int DIET_HEADER = 1;

    private final List<Object> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    DietsAdapter(Context context, List<Object> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.setHasStableIds(true);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == DIET_ITEM) {
            view = mInflater.inflate(R.layout.row_diet_item, parent, false);
            return new ItemViewHolder(view);
        } else {
            view = mInflater.inflate(R.layout.row_diet_header, parent, false);
            return new HeaderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        switch (holder.getItemViewType()) {
            case 0:
                DietItem item = (DietItem) mData.get(position);
                ItemViewHolder viewHolder = (ItemViewHolder) holder;
                viewHolder.itemView.setTag(position);

                if (item.isChecked()) {
                    viewHolder.dietText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_check_24, 0, 0,0);
                } else {
                    viewHolder.dietText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0,0);
                }

                viewHolder.dietText.setText(item.getItemName());

                break;

            case 1:
                DietHeader header = (DietHeader) mData.get(position);
                HeaderViewHolder viewHolder1 = (HeaderViewHolder) holder;

                viewHolder1.mHeaderText.setText(header.getName());

                break;
        }
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 || position == 11) {
            return DIET_HEADER;
        } else {
            return DIET_ITEM;
        }
    }

    @Override
    public long getItemId(int position) {
        if (mData.get(position) != null) {
            return mData.get(position).hashCode();
        } else {
            return 0;
        }
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(int position);
    }

    // stores and recycles views as they are scrolled off screen
    public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView dietText;

        ItemViewHolder(View itemView) {
            super(itemView);
            dietText = itemView.findViewById(R.id.dietText);
            itemView.setOnClickListener(this);

            dietText.setOnClickListener(v -> {
                DietItem item = (DietItem) mData.get(getAdapterPosition());

                if (item.isChecked()) {
                    item.setChecked(false);
                    dietText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0,0);
                } else {
                    item.setChecked(true);
                    dietText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_check_24, 0, 0,0);
                }
            });
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(getAdapterPosition());
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final TextView mHeaderText;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            mHeaderText = itemView.findViewById(R.id.diets_header);
        }
    }
}
