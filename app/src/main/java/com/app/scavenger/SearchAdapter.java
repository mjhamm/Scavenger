package com.app.scavenger;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.Locale;

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

        // Data from the recipe api
        String imageURL = item.getmImageUrl();
        int servings_int = item.getmServings();
        int calories_int = item.getmCalories();
        int carbs_int = item.getmCarbs();
        int fat_int = item.getmFat();
        int protein_int  = item.getmProtein();

        holder.mRelativeLayout.setVisibility(item.isClicked() ? View.VISIBLE : View.GONE);

        holder.mFavoriteButton.setChecked(item.isFavorited());

        TextView name = holder.recipeName;
        TextView source = holder.recipeSource;
        ImageView image = holder.recipeImage;
        TextView servings = holder.recipeServings;
        TextView calories = holder.recipeCalories;
        TextView carbs = holder.recipeCarbs;
        TextView fat = holder.recipeFat;
        TextView protein = holder.recipeProtein;

        String servings_string = String.format(mContext.getString(R.string.servings_text),servings_int);
        String calories_string = String.format(Locale.getDefault(), "%d", calories_int);
        String carbs_string = String.format(Locale.getDefault(),"%d", carbs_int);
        String fat_string = String.format(Locale.getDefault(),"%d", fat_int);
        String protein_string = String.format(Locale.getDefault(),"%d", protein_int);

        //Setting all items in each recipe item ------------
        GlideApp.with(mContext)
                .load(imageURL)
                .skipMemoryCache(true)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(image);

        name.setText(item.getmRecipeName());
        source.setText(item.getmSourceName());
        servings.setText(servings_string);
        calories.setText(calories_string);
        carbs.setText(carbs_string);
        fat.setText(fat_string);
        protein.setText(protein_string);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public static final String TAG = "LOG: ";

        private TextView recipeName;
        private TextView recipeSource;
        private TextView recipeServings;
        private TextView recipeCalories;
        private TextView recipeIngredients;
        private TextView recipeCarbs;
        private TextView recipeFat;
        private TextView recipeProtein;
        private TextView recipeAttributes;
        private ImageView recipeImage;
        private RelativeLayout mRelativeLayout;
        private MaterialCheckBox mFavoriteButton;
        private RecipeItem item;
        private ImageButton more_button;
        private MaterialCardView mViewRecipe;
        private String reportReason = null;
        private boolean rotated;

        ViewHolder( @NonNull View itemView) {
            super(itemView);
            recipeName = itemView.findViewById(R.id.recipe_name);
            recipeSource = itemView.findViewById(R.id.recipe_source);
            recipeImage = itemView.findViewById(R.id.recipe_image);
            recipeServings = itemView.findViewById(R.id.servings_total);
            recipeCalories = itemView.findViewById(R.id.calories_amount);
            recipeIngredients = itemView.findViewById(R.id.list_of_ingredients);
            recipeCarbs = itemView.findViewById(R.id.carbs_amount);
            recipeFat = itemView.findViewById(R.id.fat_amount);
            recipeProtein = itemView.findViewById(R.id.protein_amount);
            recipeAttributes = itemView.findViewById(R.id.recipe_attributes);
            mFavoriteButton = itemView.findViewById(R.id.recipe_favorite);
            mRelativeLayout = itemView.findViewById(R.id.ingredients_relativeLayout);
            more_button = itemView.findViewById(R.id.more_button);
            mViewRecipe = itemView.findViewById(R.id.viewRecipe_button);
            itemView.setOnClickListener(this);

            // Recipe Image Click Listener
            recipeImage.setOnClickListener(v -> {
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
            });

            mFavoriteButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item = mRecipeItems.get(getAdapterPosition());

                if (item.isFavorited()) {
                    item.setFavorited(false);
                } else {
                    item.setFavorited(true);
                }
            });

            more_button.setOnClickListener(v -> {
                Animation cw = AnimationUtils.loadAnimation(mContext, R.anim.menu_clockwise);
                Animation acw = AnimationUtils.loadAnimation(mContext, R.anim.menu_anti_clockwise);

                PopupMenu popupMenu = new PopupMenu(mContext, more_button);
                popupMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.menu_copy:
                            copyRecipe();
                            return true;
                        case R.id.menu_share:
                            shareRecipe();
                            return true;
                        case R.id.menu_report:
                            reportRecipe();
                            return true;
                    }
                    return false;
                });
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
            });

            mViewRecipe.setOnClickListener(v -> {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                boolean inAppBrowsingOn = sharedPreferences.getBoolean("inAppBrowser", true);
                if (inAppBrowsingOn) {
                    openURLInChromeCustomTab(mContext, retrieveRecipeUrl());
                } else {
                    openInDefaultBrowser(mContext, retrieveRecipeUrl());
                }
            });

        }

        private void reportRecipe() {
            final CharSequence[] listItems = {"Inappropriate Image","Inappropriate Website","Profanity"};
            AlertDialog.Builder reportDialog = new AlertDialog.Builder(mContext);
            reportDialog.setTitle("Why are you reporting this?");
            reportDialog.setSingleChoiceItems(listItems, -1, (dialog, item) -> {
                switch (item) {
                    case 0:
                        reportReason = "Inappropriate Image";
                        break;
                    case 1:
                        reportReason = "Inappropriate Website";
                        break;
                    case 2:
                        reportReason = "Profanity";
                        break;
                }
            });
            reportDialog.setPositiveButton("Report", (dialog, which) -> Toast.makeText(mContext, "Reported for " + reportReason + ".", Toast.LENGTH_SHORT).show());
            reportDialog.setNegativeButton("Cancel", (dialog, which) -> {/*Cancelled*/});

            AlertDialog alertDialog = reportDialog.create();
            reportReason = null;
            alertDialog.show();
            Button buttonPositive = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button buttonNegative = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        }

        private void copyRecipe() {
            ClipboardManager clipboardManager = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("Copy URL", retrieveRecipeUrl());
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(clipData);
            }
            Toast.makeText(mContext, "URL Copied.", Toast.LENGTH_SHORT).show();
        }

        private void shareRecipe() {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareSub = "Check out this awesome recipe from Scavenger!";
            String shareBody = retrieveRecipeName() + "\n" + "Made By: " + retrieveRecipeSource() + "\n\n" + retrieveRecipeUrl();
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT,shareSub);
            sharingIntent.putExtra(Intent.EXTRA_TEXT,shareBody);
            mContext.startActivity(Intent.createChooser(sharingIntent, "Share Using:"));
        }

        @Override
        public void onClick(View v) {

        }

        private String retrieveRecipeUrl() {
            return item.getmRecipeURL();
        }

        private String retrieveRecipeSource() {
            return item.getmSourceName();
        }

        private String retrieveRecipeName() {
            return item.getmRecipeName();
        }
    }

    private static void openInDefaultBrowser(Context context, String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(browserIntent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e("DefaultBrowserError: ", "Activity Error");
        }
    }

    private static void openURLInChromeCustomTab(Context context, String url) {
        try {
            CustomTabsIntent.Builder builder1 = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder1.build();
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            builder1.setInstantAppsEnabled(true);
            customTabsIntent.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.getPackageName()));
            builder1.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
            customTabsIntent.launchUrl(context, Uri.parse(url));
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e("ChromeCustomTabError: ", "Activity Error");
        }
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mRecipeItems.size();
    }
}

