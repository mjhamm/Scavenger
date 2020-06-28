package com.app.scavenger;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Movie;
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
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = "SEARCH_ADAPTER";

    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_AD = 1;
    private boolean isLoadingAdded = false;

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
    private String userId;
    private boolean logged;
    private UpdateQuery mUpdateQuery;
    private ConnectionDetector con;
    private SharedPreferences sharedPreferences;
    //private ArrayList<RecipeItem> mRecipeItems;
    private ArrayList<Object> mRecipeItems;
    private Context mContext;
    private LayoutInflater mInflater;
    private FirebaseAuth mAuth;
    private DatabaseHelper myDb;

    interface UpdateQuery {
        void updateQuery();
    }

    SearchAdapter(Context context/*, ArrayList<RecipeItem> recipeItems*/, ArrayList<Object> recipeItems , String userId, boolean logged) {
        this.userId = userId;
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        //this.mRecipeItems = recipeItems;
        this.mRecipeItems = recipeItems;
        this.logged = logged;
        this.setHasStableIds(true);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        getInfoFromSharedPrefs();
        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        myDb = DatabaseHelper.getInstance(mContext);
        con = new ConnectionDetector(mContext);
        mUpdateQuery = (UpdateQuery) mContext;

        switch (viewType) {
            case VIEW_TYPE_AD:
                View unifiedNativeLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.ad_unified, parent, false);
                return new UnifiedNativeAdHolder(unifiedNativeLayoutView);
            case VIEW_TYPE_ITEM:
                // Fall through
            default:
                View cardItemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_card_item, parent, false);
                return new ItemViewHolder(cardItemLayoutView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        switch (viewType) {
            case VIEW_TYPE_AD:
                UnifiedNativeAd nativeAd = (UnifiedNativeAd) mRecipeItems.get(position);
                populateNativeAdView(nativeAd, ((UnifiedNativeAdHolder) holder).getAdView());
                break;
            case VIEW_TYPE_ITEM:
                // Fall through
            default:
                populateItemData((ItemViewHolder) holder, position);
        }

    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mRecipeItems == null ? 0 : mRecipeItems.size();
    }

    @Override
    public int getItemViewType(int position) {

        Object recyclerViewItem = mRecipeItems.get(position);
        if (mRecipeItems.size() > 3) {
            if (recyclerViewItem instanceof UnifiedNativeAd) {
                return VIEW_TYPE_AD;
            }
        }
        return VIEW_TYPE_ITEM;
    }

    @Override
    public long getItemId(int position) {
        if (mRecipeItems.get(position) != null) {
            return mRecipeItems.get(position).hashCode();// .getItemId().hashCode();
        } else {
            return 0;
        }
    }

    /*
    HELPERS
    ________________________________________________________________________________________________________________________________________
     */

    private void populateItemData(ItemViewHolder holder, int position) {
        RecipeItem item = (RecipeItem) mRecipeItems.get(position);

        boolean isExpanded = ((RecipeItem) mRecipeItems.get(position)).isClicked();
        holder.expandableLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        if (item.isFavorited()) {
            holder.favorite_button.setTag(position);
            holder.favorite_button.setImageResource(R.mipmap.heart_icon_filled);
        } else {
            holder.favorite_button.setTag(position);
            holder.favorite_button.setImageResource(R.mipmap.heart_icon_outline_white);
        }

        if (item.getmImageUrl() != null) {
            Picasso.get()
                    .load(item.getmImageUrl())
                    .fit()
                    .into(holder.recipeImage);
        } else {
            holder.recipeImage.setImageDrawable(null);
        }

        holder.recipeName.setText(item.getmRecipeName());
        holder.recipeSource.setText(item.getmSourceName());
        holder.recipeServings.setText(String.format(mContext.getString(R.string.servings_text),item.getmServings()));

        holder.recipeCalories.setText(String.valueOf(item.getmCalories()));
        holder.recipeCarbs.setText(String.valueOf(item.getmCarbs()));
        holder.recipeFat.setText(String.valueOf(item.getmFat()));
        holder.recipeProtein.setText(String.valueOf(item.getmProtein()));
        holder.recipeIngredients.setText(TextUtils.join("", item.getmIngredients()));
        holder.recipeAttributes.setText(TextUtils.join("", item.getmRecipeAttributes()));
    }

    private void populateNativeAdView(UnifiedNativeAd nativeAd,
                                      UnifiedNativeAdView adView) {
        // Some assets are guaranteed to be in every UnifiedNativeAd.
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        NativeAd.Image icon = nativeAd.getIcon();

        if (icon == null) {
            adView.getIconView().setVisibility(View.INVISIBLE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(icon.getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) adView.getStarRatingView())
                    .setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        // Assign native ad object to the native view.
        adView.setNativeAd(nativeAd);
    }

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", null);
    }

    private void saveDataToFirebase(String itemId, String name, String source, String image, String url, int servings, int calories, int carbs, int fat, int protein, ArrayList<String> attributes, ArrayList<String> ingredients) {
        Map<String, Object> item = new HashMap<>();
        CollectionReference favoritesRef = db.collection("Users").document(userId).collection("Favorites");

        Date now = new Date();
        Timestamp timestamp = new Timestamp(now);

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
        item.put("Timestamp", timestamp);

        favoritesRef.document(itemId).set(item)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        updateQuery();
                        Log.d("LOG: ", "Item saved to Firebase");
                        int likes = sharedPreferences.getInt("numLikes", 0);
                        likes += 1;
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("numLikes", likes);
                        editor.apply();
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

    private void removeDataFromFirebase(RecipeItem recipeItem) {
        CollectionReference favoritesRef = db.collection("Users").document(userId).collection("Favorites");

        favoritesRef.document(recipeItem.getItemId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        updateQuery();
                        Log.d(TAG, "Successfully removed Like");
                        int likes = sharedPreferences.getInt("numLikes", 0);
                        likes -= 1;
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("numLikes", likes);
                        editor.apply();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Failed to remove Like" + e.toString());
                    }
                });
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

    public void updateQuery() {
        mUpdateQuery.updateQuery();
    }

    /*
   VIEW HOLDERS
   _________________________________________________________________________________________________
    */

    // ITEM VIEW HOLDER
    public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView recipeName, recipeSource, recipeServings, recipeCalories, recipeIngredients, recipeCarbs, recipeFat, recipeProtein, recipeAttributes;
        private ImageView recipeImage;
        private CardView mNutritionCard, mViewRecipe;
        private RecipeItem item;
        private ImageButton more_button, favorite_button;
        private String reportReason = null;
        private ConstraintLayout expandableLayout;
        private boolean rotated;
        private long mLastClickTime = 0;

        ItemViewHolder( @NonNull View itemView) {
            super(itemView);
            recipeName = itemView.findViewById(R.id.recipe_name);
            recipeSource = itemView.findViewById(R.id.recipe_source);
            recipeImage = itemView.findViewById(R.id.recipe_image);
            favorite_button = itemView.findViewById(R.id.recipe_favorite);
            more_button = itemView.findViewById(R.id.more_button);
            mViewRecipe = itemView.findViewById(R.id.viewRecipe_button);
            expandableLayout = itemView.findViewById(R.id.expandableLayout);
            recipeServings = itemView.findViewById(R.id.servings_total);
            recipeCalories = itemView.findViewById(R.id.calories_amount);
            recipeIngredients = itemView.findViewById(R.id.list_of_ingredients);
            recipeCarbs = itemView.findViewById(R.id.carbs_amount);
            recipeFat = itemView.findViewById(R.id.fat_amount);
            recipeProtein = itemView.findViewById(R.id.protein_amount);
            mNutritionCard = itemView.findViewById(R.id.facts_cardView);
            recipeAttributes = itemView.findViewById(R.id.recipe_attributes);

            itemView.setOnClickListener(this);

            // Recipe Image Click Listener
            recipeImage.setOnClickListener(v -> {
                int position = getAdapterPosition();
                // Adapter Position
                // Gets the item at the position
                item = (RecipeItem) mRecipeItems.get(position);
                // Checks if the item is clicked
                // Sets the layout visible/gone
                item.setClicked(!item.isClicked());
                notifyItemChanged(position);
            });

            //Creates animation for Love button - Animation to grow and shrink heart when clicked - light
            Animation scaleAnimation_Favorite = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnimation_Favorite.setDuration(500);
            OvershootInterpolator overshootInterpolator_Favorite = new OvershootInterpolator(4);
            scaleAnimation_Favorite.setInterpolator(overshootInterpolator_Favorite);
            favorite_button.setOnClickListener(v -> {
                if (!con.connectedToInternet()) {
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle("No Internet connection found")
                            .setMessage("You don't have an Internet connection. Please reconnect and try again.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create()
                            .show();
                } else {
                    getInfoFromSharedPrefs();
                    item = (RecipeItem) mRecipeItems.get(getAdapterPosition());
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
                                removeDataFromFirebase(item);
                                myDb.removeDataFromView(item.getItemId());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            favorite_button.setImageResource(R.mipmap.heart_icon_filled);
                            item.setFavorited(true);
                            try {
                                saveDataToFirebase(item.getItemId(), item.getmRecipeName(), item.getmSourceName(), item.getmImageUrl(), item.getmRecipeURL(), item.getmServings(),
                                        item.getmCalories(), item.getmCarbs(), item.getmFat(), item.getmProtein(), item.getmRecipeAttributes(), item.getmIngredients());
                                myDb.addDataToView(item.getItemId());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        new MaterialAlertDialogBuilder(mContext)
                                .setTitle("You need to be Signed In")
                                .setMessage("You must Sign Up or Sign In, in order to Like recipes.")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create()
                                .show();
                    }
                }
            });

            more_button.setOnClickListener(v -> {
                Animation cw = AnimationUtils.loadAnimation(mContext, R.anim.menu_clockwise);
                Animation acw = AnimationUtils.loadAnimation(mContext, R.anim.menu_anti_clockwise);

                item = (RecipeItem) mRecipeItems.get(getAdapterPosition());

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

                /*if (!rotated) {
                    more_button.startAnimation(cw);
                    rotated = true;
                    cw.setFillAfter(true);
                }

                popupMenu.setOnDismissListener(dismiss -> {
                    more_button.startAnimation(acw);
                    rotated = false;
                    acw.setFillAfter(true);
                });*/
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
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle("No Internet connection found")
                        .setMessage("You don't have an Internet connection. Please reconnect and try again.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
            } else {
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
                        .setPositiveButton("Report",(dialog, which) -> {
                            if (!con.connectedToInternet()) {
                                new MaterialAlertDialogBuilder(mContext)
                                        .setTitle("No Internet connection found")
                                        .setMessage("You don't have an Internet connection. Please reconnect and try again.")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .create()
                                        .show();
                            } else {
                                sendReportToDb(reportReason, item);
                            }
                        })
                        .setNegativeButton("Cancel", ((dialog, which) -> dialog.cancel()))
                        .create()
                        .show();
            }
        }

        private void sendReportToDb(String reason, RecipeItem item) {
            // Send report to Server under reports with Phone information

            Calendar calendar = Calendar.getInstance();

            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int year = calendar.get(Calendar.YEAR);

            calendar.clear();
            calendar.set(year, month, day);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());
            String strDate = simpleDateFormat.format(calendar.getTime());

            Date now = new Date();
            Timestamp timestamp = new Timestamp(now);

            Map<String, Object> reportInfo = new HashMap<>();

            CollectionReference reportingReference = db.collection("RecipeReports").document(strDate).collection("reports");

            reportInfo.put("Recipe Report Reason", reason);
            reportInfo.put("Timestamp", timestamp);
            reportInfo.put("Recipe Image", item.getmImageUrl());
            reportInfo.put("Recipe Name", item.getmRecipeName());
            reportInfo.put("Recipe Source", item.getmSourceName());
            reportInfo.put("Recipe Ingredients", item.getmIngredients());
            reportInfo.put("Recipe URL", item.getmRecipeURL());

            reportingReference.document().set(reportInfo)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG,"Report saved to Firebase");
                            Toast.makeText(mContext, "Reported for " + reason + ". Thank you.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(mContext, "Error sending report", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, e.toString());
                        }
                    });
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
            item = (RecipeItem) mRecipeItems.get(getAdapterPosition());
            return item.getmRecipeURL();
        }

        private String retrieveRecipeSource() {
            item = (RecipeItem) mRecipeItems.get(getAdapterPosition());
            return item.getmSourceName();
        }

        private String retrieveRecipeName() {
            item = (RecipeItem) mRecipeItems.get(getAdapterPosition());
            return item.getmRecipeName();
        }
    }

    // LOADING VIEW HOLDER
    private static class UnifiedNativeAdHolder extends RecyclerView.ViewHolder {

        private UnifiedNativeAdView adView;

        public UnifiedNativeAdView getAdView() {
            return adView;
        }

        public UnifiedNativeAdHolder(@NonNull View view) {
            super(view);
            adView = (UnifiedNativeAdView) view.findViewById(R.id.ad_view);

            // The MediaView will display a video asset if one is present in the ad, and the
            // first image asset otherwise.
            adView.setMediaView((MediaView) adView.findViewById(R.id.ad_media));

            // Register the view used for each individual asset.
            adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
            adView.setBodyView(adView.findViewById(R.id.ad_body));
            adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
            adView.setIconView(adView.findViewById(R.id.ad_icon));
            adView.setPriceView(adView.findViewById(R.id.ad_price));
            adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
            adView.setStoreView(adView.findViewById(R.id.ad_store));
            adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
        }
    }
}

