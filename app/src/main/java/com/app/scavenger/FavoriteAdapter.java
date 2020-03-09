package com.app.scavenger;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Locale;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {

    // Firestore Labels ----------------------------------------------------------
    private static final String ITEM_NAME = "name";
    private static final String ITEM_SOURCE = "source";
    private static final String ITEM_IMAGE = "image";
    private static final String ITEM_URL = "url";
    private static final String ITEM_YIELD = "servings";
    private static final String ITEM_CAL = "calories";
    private static final String ITEM_CARB = "carbs";
    private static final String ITEM_FAT = "fat";
    private static final String ITEM_PROTEIN = "protein";
    private static final String ITEM_ATT = "attributes";
    private static final String ITEM_INGR = "ingredients";
    //-----------------------------------------------------------------------------

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ArrayList<RecipeItem> mRecipeItems;
    private Context mContext;
    private LayoutInflater mInflater;

    FavoriteAdapter(Context context, ArrayList<RecipeItem> recipeItems) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mRecipeItems = recipeItems;
    }

    @NonNull
    @Override
    public FavoriteAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.fav_card_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteAdapter.ViewHolder holder, int position) {
        RecipeItem item = mRecipeItems.get(position);

        holder.mRelativeLayout.setVisibility(item.isClicked() ? View.VISIBLE : View.GONE);

        // Data from the recipe api
        String imageURL = item.getmImageUrl();
        int servings_int = item.getmServings();
        int calories_int = item.getmCalories();
        int carbs_int = item.getmCarbs();
        int fat_int = item.getmFat();
        int protein_int  = item.getmProtein();
        ArrayList<String> ingredients_list = item.getmIngredients();
        ArrayList<String> attributes_list = item.getmRecipeAttributes();

        TextView name = holder.recipeName;
        TextView source = holder.recipeSource;
        ImageView image = holder.recipeImage;
        TextView servings = holder.recipeServings;
        TextView calories = holder.recipeCalories;
        TextView carbs = holder.recipeCarbs;
        TextView fat = holder.recipeFat;
        TextView protein = holder.recipeProtein;
        TextView ingredients = holder.recipeIngredients;
        TextView attributes = holder.recipeAttributes;

        String servings_string = String.format(mContext.getString(R.string.servings_text),servings_int);
        String calories_string = String.format(Locale.getDefault(), "%d", calories_int);
        String carbs_string = String.format(Locale.getDefault(),"%d", carbs_int);
        String fat_string = String.format(Locale.getDefault(),"%d", fat_int);
        String protein_string = String.format(Locale.getDefault(),"%d", protein_int);

        Glide.with(mContext)
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
        ingredients.setText(TextUtils.join("", ingredients_list));
        attributes.setText(TextUtils.join("", attributes_list));

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
        private MaterialCardView mNutritionCard;
        private RelativeLayout mRelativeLayout;
        private RecipeItem item;
        private ImageButton more_button, favorite_button;
        private MaterialCardView mViewRecipe;
        private String reportReason = null;
        private boolean rotated;
        private long mLastClickTime = 0;

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
            mNutritionCard = itemView.findViewById(R.id.facts_cardView);
            recipeAttributes = itemView.findViewById(R.id.recipe_attributes);
            favorite_button = itemView.findViewById(R.id.recipe_favorite);
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

            more_button.setOnClickListener(v -> {
                Animation cw = AnimationUtils.loadAnimation(mContext, R.anim.menu_clockwise);
                Animation acw = AnimationUtils.loadAnimation(mContext, R.anim.menu_anti_clockwise);

                PopupMenu popupMenu = new PopupMenu(mContext, more_button);
                popupMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.fav_menu_copy:
                            copyRecipe();
                            return true;
                        case R.id.fav_menu_share:
                            shareRecipe();
                            return true;
                        case R.id.fav_menu_report:
                            reportRecipe();
                            return true;
                        case R.id.fav_menu_remove:
                            Toast.makeText(mContext, "Remove", Toast.LENGTH_SHORT).show();
                    }
                    return false;
                });
                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.favorite_menu, popupMenu.getMenu());
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

            mNutritionCard.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle("Some Information about Our Data")
                        .setMessage("Scavenger uses Edamam Search and your search criteria to look throughout the Internet in order to bring you " +
                                "the best information we can find. However, sometimes this information may not be 100% accurate. Using " +
                                "the View Recipe button to see the recipe on the actual website will give you the most accurate data. This includes Nutrition Information " +
                                "as well as the number of servings the amount of ingredients can make.")
                        .setPositiveButton("Got It!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create()
                        .show();
            });

            recipeServings.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle("Some Information about Our Data")
                        .setMessage("Scavenger uses Edamam Search and your search criteria to look throughout the Internet in order to bring you " +
                                "the best information we can find. However, sometimes this information may not be 100% accurate. Using " +
                                "the View Recipe button to see the recipe on the actual website will give you the most accurate data. This includes Nutrition Information " +
                                "as well as the number of servings the amount of ingredients can make.")
                        .setPositiveButton("Got It!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create()
                        .show();
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
            new MaterialAlertDialogBuilder(mContext)
                    .setTitle("Why are you reporting this?")
                    .setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
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
                        }
                    })
                    .setPositiveButton("Report",(dialog, which) -> Toast.makeText(mContext, "Reported for " + reportReason + ". Thank you.", Toast.LENGTH_SHORT).show())
                    .setNegativeButton("Cancel", ((dialog, which) -> dialog.cancel()))
                    .create()
                    .show();
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

        private String retrieveRecipeUrl() {
            return item.getmRecipeURL();
        }

        private String retrieveRecipeSource() {
            return item.getmSourceName();
        }

        private String retrieveRecipeName() {
            return item.getmRecipeName();
        }

        @Override
        public void onClick(View v) {

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

    //boolean that returns true if you are connected to internet and false if not
    private boolean checkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mRecipeItems.size();
    }
}
