package com.app.scavenger;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GroceryListAdapter extends RecyclerView.Adapter<GroceryListAdapter.ViewHolder> {

    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private List<GroceryListItem> mData;
    private Context mContext;
    private final String userId;
    private ArrayList<String> groceryItems;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    GroceryListAdapter(Context context, List<GroceryListItem> data, ArrayList<String> groceryItems, String userId) {
        this.mInflater = LayoutInflater.from(context);
        this.mContext = context;
        this.mData = data;
        this.userId = userId;
        this.groceryItems = groceryItems;
    }

    @NonNull
    @Override
    public GroceryListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.row_grocery_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroceryListAdapter.ViewHolder holder, int position) {
        GroceryListItem groceryItem = mData.get(position);

        boolean isTapped = groceryItem.getGroceryItemTapped();
        boolean isSelected = groceryItem.getmGroceryItemSelected();
        boolean showSelectItem = groceryItem.isShowSelectItems();

        holder.groceryRemoveButton.setVisibility(isTapped ? View.VISIBLE : View.GONE);

        holder.grocerySelectButton.setVisibility(showSelectItem ? View.VISIBLE : View.GONE);
        if (isSelected)
            holder.grocerySelectButton.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_baseline_check_circle_24));
        else holder.grocerySelectButton.setBackground(ContextCompat.getDrawable(mContext, R.drawable.circle));

        holder.groceryTextView.setText(groceryItem.getGroceryItemName());
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView groceryTextView;
        final ImageButton groceryRemoveButton;
        final MaterialCheckBox grocerySelectButton;
        private GroceryListItem item;

        ViewHolder(View itemView) {
            super(itemView);
            groceryTextView = itemView.findViewById(R.id.grocerylist_row_text);
            groceryRemoveButton = itemView.findViewById(R.id.grocery_removeButton);
            grocerySelectButton = itemView.findViewById(R.id.select_item);
            itemView.setOnClickListener(this);

            groceryTextView.setOnClickListener(v -> {
                int position = getAdapterPosition();

                item = mData.get(position);

                if (item.isShowSelectItems()) {
                    item.setmGroceryItemSelected(!item.getmGroceryItemSelected());
                } else {
                    item.setmGroceryItemTapped(!item.getGroceryItemTapped());
                }
                notifyItemChanged(position);
            });

            groceryRemoveButton.setOnClickListener(v -> {
                int position = getAdapterPosition();

                item = mData.get(position);

                mData.remove(item);

                notifyItemRemoved(position);
            });

            grocerySelectButton.setOnClickListener(v -> {
                int position = getAdapterPosition();

                item = mData.get(position);

                item.setmGroceryItemSelected(!item.getmGroceryItemSelected());
                notifyItemChanged(position);
            });
        }


        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }
}
