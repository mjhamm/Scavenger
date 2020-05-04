package com.app.scavenger;

import android.animation.LayoutTransition;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import animations.Animations;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    public static final String TAG = "LOG: ";

    // Firestore Labels ----------------------------------------------------------
    private static final String ITEM_ID = "itemId";
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
    private String userId = null;
    private boolean logged = false;
    private String name = null;
    private String email = null;
    private boolean expanded = false;
    private SharedPreferences sharedPreferences;
    /*private CollectionReference favoritesRef = db.collection("Users").document(mUserId).collection("Favorites");*/
    private ArrayList<RecipeItem> mRecipeItems;
    private Context mContext;
    private LayoutInflater mInflater;

    SearchAdapter(Context context, ArrayList<RecipeItem> recipeItems, String userId, boolean logged) {
        this.userId = userId;
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mRecipeItems = recipeItems;
        this.logged = logged;
    }

    @NonNull
    @Override
    public SearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.card_item, parent, false);
        getInfoFromSharedPrefs();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
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
        ArrayList<String> ingredients_list = item.getmIngredients();
        ArrayList<String> attributes_list = item.getmRecipeAttributes();

        if (item.isClicked()) {
            holder.mRelativeLayout.setVisibility(View.VISIBLE);
        } else {
            holder.mRelativeLayout.setVisibility(View.GONE);
        }

        if (item.isFavorited()) {
            holder.favorite_button.setTag(position);
            holder.favorite_button.setImageResource(R.mipmap.heart_icon_filled);
        } else {
            holder.favorite_button.setTag(position);
            holder.favorite_button.setImageResource(R.mipmap.heart_icon_outline_white);
        }

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
        ingredients.setText(TextUtils.join("", ingredients_list));
        attributes.setText(TextUtils.join("", attributes_list));
    }

    private void saveDataToFirebase(String itemId, String name, String source, String image, String url, int servings, int calories, int carbs, int fat, int protein, ArrayList<String> attributes, ArrayList<String> ingredients) {
        Map<String, Object> item = new HashMap<>();
        CollectionReference favoritesRef = db.collection("Users").document(userId).collection("Favorites");

        item.put(ITEM_ID, itemId);
        item.put(ITEM_NAME, name);
        item.put(ITEM_SOURCE, source);
        item.put(ITEM_IMAGE, image);
        item.put(ITEM_URL, url);
        item.put(ITEM_YIELD, servings);
        item.put(ITEM_CAL, calories);
        item.put(ITEM_CARB, carbs);
        item.put(ITEM_FAT, fat);
        item.put(ITEM_PROTEIN, protein);
        item.put(ITEM_ATT, attributes);
        item.put(ITEM_INGR, ingredients);

        favoritesRef.document(itemId).set(item)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("LOG: ", "Item saved to Firestore");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(mContext, "Error", Toast.LENGTH_SHORT).show();
                        Log.d("LOG: ", e.toString());
                    }
                });
    }

    private void removeDataFromFirebase(RecipeItem recipeItem, String itemId) {
        CollectionReference favoritesRef = db.collection("Users").document(userId).collection("Favorites");

        favoritesRef.document(recipeItem.getItemId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Successfully removed favorite");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Failed to remove favorite" + e.toString());
                    }
                });
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView recipeName, recipeSource, recipeServings, recipeCalories, recipeIngredients, recipeCarbs, recipeFat, recipeProtein, recipeAttributes;
        private ImageView recipeImage;
        private MaterialCardView mNutritionCard, mViewRecipe;
        private RelativeLayout mRelativeLayout;
        private ConstraintLayout mConstraintLayout;
        private RecipeItem item;
        private ImageButton more_button, favorite_button;
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
            mConstraintLayout = itemView.findViewById(R.id.card_constraintLayout);
            more_button = itemView.findViewById(R.id.more_button);
            mViewRecipe = itemView.findViewById(R.id.viewRecipe_button);
            itemView.setOnClickListener(this);

            // Recipe Image Click Listener
            recipeImage.setOnClickListener(v -> {
                int position = getAdapterPosition();

                final Animation animationUp;
                final Animation animationDown;

                animationUp = AnimationUtils.loadAnimation(mContext, R.anim.slide_up);
                animationDown = AnimationUtils.loadAnimation(mContext, R.anim.slide_down);


                // Adapter Position
                // Gets the item at the position
                item = mRecipeItems.get(position);
                // Checks if the item is clicked
                // Sets the layout visible/gone
                if (item.isClicked()) {
                    //mRelativeLayout.startAnimation(animationUp);
                    mRelativeLayout.setVisibility(View.GONE);
                    mRelativeLayout.startAnimation(animationUp);
                    item.setClicked(false);
                    //notifyItemChanged(position);
                } else {
                    //mRelativeLayout.startAnimation(animationDown);
                    mRelativeLayout.setVisibility(View.VISIBLE);
                    mRelativeLayout.startAnimation(animationDown);
                    item.setClicked(true);
                    //notifyItemChanged(position);
                }
            });

            //Creates animation for Love button - Animation to grow and shrink heart when clicked - light
            Animation scaleAnimation_Favorite = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnimation_Favorite.setDuration(500);
            OvershootInterpolator overshootInterpolator_Favorite = new OvershootInterpolator(4);
            scaleAnimation_Favorite.setInterpolator(overshootInterpolator_Favorite);
            favorite_button.setOnClickListener(v -> {
                getInfoFromSharedPrefs();
                item = mRecipeItems.get(getAdapterPosition());
                v.getTag();
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1500) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                if (logged) {
                    v.startAnimation(scaleAnimation_Favorite);
                    if (item.isFavorited()) {
                        favorite_button.setImageResource(R.mipmap.heart_icon_outline_white);
                        item.setFavorited(false);
                        try {
                            removeDataFromFirebase(item, item.getItemId());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        favorite_button.setImageResource(R.mipmap.heart_icon_filled);
                        item.setFavorited(true);
                        try {
                            saveDataToFirebase(item.getItemId(), item.getmRecipeName(), item.getmSourceName(), item.getmImageUrl(), item.getmRecipeURL(), item.getmServings(),
                                    item.getmCalories(), item.getmCarbs(), item.getmFat(), item.getmProtein(), item.getmRecipeAttributes(), item.getmIngredients());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle("You need to be Signed In.")
                            .setMessage("You must sign up or sign in, in order to Favorite recipes.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                }
                            })
                            .create()
                            .show();
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

    @Override
    public long getItemId(int position) {
        //RecipeItem item = mRecipeItems.get(position);
        return position;
    }

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", null);
        email = sharedPreferences.getString("email", null);
        name = sharedPreferences.getString("name", null);
    }
}

