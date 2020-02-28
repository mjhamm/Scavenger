package com.app.scavenger;

import android.content.Context;
import android.media.Image;
import android.text.Layout;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private ArrayList<RecipeItem> mRecipeItems;
    private Context mContext;
    private LayoutInflater mInflater;

    SearchAdapter(Context context, ArrayList<RecipeItem> recipeItems) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mRecipeItems = recipeItems;
    }

    @NonNull
    @Override
    public SearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.card_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchAdapter.ViewHolder holder, int position) {
        RecipeItem item = mRecipeItems.get(position);

        holder.mRelativeLayout.setVisibility(item.isClicked() ? View.VISIBLE : View.GONE);

        holder.mFavoriteButton.setChecked(item.isFavorited() ? true : false);

        String imageURL = item.getmImageUrl();
        TextView name = holder.recipeName;
        TextView source = holder.recipeSource;
        ImageView image = holder.recipeImage;
        name.setText(item.getmRecipeName());
        source.setText(item.getmSourceName());

        /*Glide.with(mContext)
                .load(imageURL)
                .skipMemoryCache(true)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(image);*/

    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public static final String TAG = "LOG: ";

        private TextView recipeName;
        private TextView recipeSource;
        private ImageView recipeImage;
        private RelativeLayout mRelativeLayout;
        private MaterialCheckBox mFavoriteButton;
        private RecipeItem item;
        private ImageButton more_button;
        private boolean rotated;

        ViewHolder( @NonNull View itemView) {
            super(itemView);
            recipeName = itemView.findViewById(R.id.recipe_name);
            recipeSource = itemView.findViewById(R.id.recipe_source);
            recipeImage = itemView.findViewById(R.id.recipe_image);
            mFavoriteButton = itemView.findViewById(R.id.recipe_favorite);
            mRelativeLayout = itemView.findViewById(R.id.ingredients_relativeLayout);
            more_button = itemView.findViewById(R.id.more_button);
            itemView.setOnClickListener(this);

            // Recipe Image Click Listener
            recipeImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    // Adapter Position
                    // Gets the item at the position
                    item = mRecipeItems.get(position);
                    // Checks if the item is clicked
                    // Sets the layout visible/gone
                    if (item.isClicked()) {
                        mRelativeLayout.setVisibility(View.GONE);
                        item.setClicked(false);
                    } else {
                        mRelativeLayout.setVisibility(View.VISIBLE);
                        item.setClicked(true);
                    }
                }
            });

            mFavoriteButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    item = mRecipeItems.get(getAdapterPosition());

                    if (item.isFavorited()) {
                        item.setFavorited(false);
                    } else {
                        item.setFavorited(true);
                    }
                }
            });

            more_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Animation cw = AnimationUtils.loadAnimation(mContext, R.anim.menu_clockwise);
                    Animation acw = AnimationUtils.loadAnimation(mContext, R.anim.menu_anti_clockwise);

                    PopupMenu popupMenu = new PopupMenu(mContext, more_button);
                    popupMenu.setOnMenuItemClickListener(item -> false);
                    MenuInflater inflater = popupMenu.getMenuInflater();
                    inflater.inflate(R.menu.more_menu, popupMenu.getMenu());
                    popupMenu.show();

                    if (!rotated) {
                        more_button.startAnimation(cw);
                        rotated = true;
                        cw.setFillAfter(true);
                    }

                    popupMenu.setOnDismissListener(dismiss -> {
                        more_button.startAnimation(acw);
                        rotated = false;
                        acw.setFillAfter(true);
                    });
                }
            });
        }

        @Override
        public void onClick(View v) {

        }
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mRecipeItems.size();
    }
}
