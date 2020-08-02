package com.app.scavenger;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
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

    // variables for constructor
    private final ArrayList<Object> mRecipeItems;
    private final Context mContext;
    private String userId;
    private boolean logged;

    // Database
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DatabaseHelper myDb;

    private ConnectionDetector con;
    private SharedPreferences sharedPreferences;

    // Callbacks
    private UpdateQuery mUpdateQuery;

    interface UpdateQuery {
        void updateQuery();
    }

    // Constructor
    SearchAdapter(Context context, ArrayList<Object> recipeItems, boolean logged) {
        //this.userId = userId;
        this.mContext = context;
        this.mRecipeItems = recipeItems;
        this.logged = logged;
        // sets stableIds to true for better scrolling
        this.setHasStableIds(true);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        // get information from shared preferences
        getInfoFromSharedPrefs();
        // get instance of Database
        myDb = DatabaseHelper.getInstance(mContext);
        con = new ConnectionDetector(mContext);
        // set up callback
        mUpdateQuery = (UpdateQuery) mContext;

        View cardItemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_card_item, parent, false);
        return new ItemViewHolder(cardItemLayoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // populate information for the Recipe Item
        populateItemData((ItemViewHolder) holder, position);
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mRecipeItems == null ? 0 : mRecipeItems.size();
    }

    // get the itemview type
    // has the chance to return multiple view types (ads)
    @Override
    public int getItemViewType(int position) {
        // Recipe Item View Type
        return 0;
    }

    // returns the hashcode of the item
    @Override
    public long getItemId(int position) {
        if (mRecipeItems.get(position) != null) {
            return mRecipeItems.get(position).hashCode();
        } else {
            return 0;
        }
    }

    /*
    HELPERS
    ________________________________________________________________________________________________________________________________________
     */

    // Code for adding Recipe Items inside of the Search Recyclerview
    private void populateItemData(ItemViewHolder holder, int position) {
        // gets the item at the list's position
        RecipeItem item = (RecipeItem) mRecipeItems.get(position);

        // boolean for whether or not the item at the list position is expanded or not
        boolean isExpanded = ((RecipeItem) mRecipeItems.get(position)).isClicked();
        // if the isExpanded boolean is true -
        // Show the bottom card
        // else
        // hide the bottom card
        holder.mBottomCard.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // checks if the item at the list position is liked or not
        // if it is liked
        if (item.isLiked()) {
            // set the image to filled
            holder.like_button.setTag(position);
            holder.like_button.setImageResource(R.drawable.like_filled);
            // if item isn't liked
        } else {
            // set the image to outline
            holder.like_button.setTag(position);
            holder.like_button.setImageResource(R.drawable.like_outline);
        }

        // if the item's image url isn't null
        // use picasso to load the image into the imageview
        if (item.getmImageUrl() != null) {
            Picasso.get()
                    .load(item.getmImageUrl())
                    .fit()
                    .config(Bitmap.Config.RGB_565)
                    .into(holder.recipeImage);
            // show nothing if the url is null
            // prevents crashes if the url is null
        } else {
            holder.recipeImage.setImageDrawable(null);
        }

        // sets the info for each item
        // name
        holder.recipeName.setText(item.getmRecipeName());
        // source
        holder.recipeSource.setText(item.getmSourceName());
        // servings
        holder.recipeServings.setText(String.format(mContext.getString(R.string.servings_text),item.getmServings()));
        // calories
        holder.recipeCalories.setText(String.valueOf(item.getmCalories()));
        // carbs
        holder.recipeCarbs.setText(String.valueOf(item.getmCarbs()));
        // fat
        holder.recipeFat.setText(String.valueOf(item.getmFat()));
        // protein
        holder.recipeProtein.setText(String.valueOf(item.getmProtein()));
        // ingredients
        holder.recipeIngredients.setText(TextUtils.join("", item.getmIngredients()));
        // attributes
        holder.recipeAttributes.setText(TextUtils.join("", item.getmRecipeAttributes()));
    }

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", null);
    }

    // saves an item to Firebase
    private void saveDataToFirebase(String itemId, String name, String source, String image, String url, int servings, int calories, int carbs, int fat, int protein, ArrayList<String> attributes, ArrayList<String> ingredients) {
        // create new hashmap that holds the item information that will be saved to Firebase
        HashMap<String, Object> itemMap = new HashMap<>();
        // reference to the likes on Firebase
        CollectionReference likesRef = db.collection(Constants.firebaseUser).document(userId).collection(Constants.firebaseLikes);

        // creates a new date and timestamp to be used to order the likes
        Date now = new Date();
        Timestamp timestamp = new Timestamp(now);

        // each part of the recipe item to be put into the hashmap for Firebase
        itemMap.put(Constants.ITEM_ID, itemId);
        itemMap.put(Constants.ITEM_NAME, name);
        itemMap.put(Constants.ITEM_SOURCE, source);
        itemMap.put(Constants.ITEM_IMAGE, image);
        itemMap.put(Constants.ITEM_URL, url);
        itemMap.put(Constants.ITEM_YIELD, servings);
        itemMap.put(Constants.ITEM_CAL, calories);
        itemMap.put(Constants.ITEM_CARB, carbs);
        itemMap.put(Constants.ITEM_FAT, fat);
        itemMap.put(Constants.ITEM_PROTEIN, protein);
        itemMap.put(Constants.ITEM_ATT, attributes);
        itemMap.put(Constants.ITEM_INGR, ingredients);
        itemMap.put("Timestamp", timestamp);

        // sets the data in Firebase
        likesRef.document(itemId).set(itemMap)
                .addOnSuccessListener(aVoid -> {
                    // clears the query inside of the likes fragment and clears focus
                    // this avoids problems with potential filtering of the likes fragment when adding a new item to likes
                    updateQuery();
                    Log.d("LOG: ", "Item saved to Firebase");
                    // add 1 to numLikes
                    int likes = sharedPreferences.getInt("numLikes", 0);
                    likes += 1;
                    // add it to numLikes
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("numLikes", likes);
                    editor.apply();
                })
                .addOnFailureListener(e -> {
                    toastMessage("Error saving Like. Please try again");
                    Log.d("LOG: ", e.toString());
                });
    }

    // removes an item from Firebase
    private void removeDataFromFirebase(RecipeItem recipeItem) {
        // reference for likes in firebase
        CollectionReference likesRef = db.collection(Constants.firebaseUser).document(userId).collection(Constants.firebaseLikes);

        // deletes the document in firebase with the matching item id
        likesRef.document(recipeItem.getItemId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // updates the query inside of likes fragment
                    // this avoids problems with potential filtering of the likes fragment when removing an item from likes
                    updateQuery();
                    Log.d(TAG, "Successfully removed Like");
                    // remove 1 from numLikes
                    int likes = sharedPreferences.getInt("numLikes", 0);
                    likes -= 1;
                    // add it to numLikes
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("numLikes", likes);
                    editor.apply();
                })
                .addOnFailureListener(e -> Log.d(TAG, "Failed to remove Like" + e.toString()));
    }

    // open the recipe in the App Browser
    private void openInDefaultBrowser(Context context, String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(browserIntent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e("DefaultBrowserError: ", "Activity Error");
        }
    }

    // opens the recipe in the users default browser
    private void openURLInChromeCustomTab(Context context, String url) {
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

    //method for creating a Toast
    private void toastMessage(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    // callback for clear the query in the likes fragment
    public void updateQuery() {
        mUpdateQuery.updateQuery();
    }

    /*
    VIEW HOLDERS
    ________________________________________________________________________________________________
    */

    // ITEM VIEW HOLDER
    public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // views inside recipeCardItem
        private final TextView recipeName, recipeSource, recipeServings, recipeCalories, recipeIngredients, recipeCarbs, recipeFat, recipeProtein, recipeAttributes;
        private final ImageView recipeImage;
        private final ImageButton more_button, like_button;
        private final CardView mBottomCard;

        // recipe item
        private RecipeItem item;

        // reason for report from user
        private String reportReason = null;
        // variable that checks whether or not the like button has been clicked in the last 1 second
        private long mLastClickTime = 0;

        ItemViewHolder( @NonNull View itemView) {
            super(itemView);
            recipeName = itemView.findViewById(R.id.recipe_name);
            recipeSource = itemView.findViewById(R.id.recipe_source);
            recipeImage = itemView.findViewById(R.id.recipe_image);
            like_button = itemView.findViewById(R.id.recipe_like);
            more_button = itemView.findViewById(R.id.more_button);
            CardView mViewRecipe = itemView.findViewById(R.id.viewRecipe_button);
            //CardView mAddToList = itemView.findViewById(R.id.addToList_button);
            ImageView edamamBranding = itemView.findViewById(R.id.edamam_branding);
            mBottomCard = itemView.findViewById(R.id.bottomCardView);
            recipeServings = itemView.findViewById(R.id.servings_total);
            recipeCalories = itemView.findViewById(R.id.calories_amount);
            recipeIngredients = itemView.findViewById(R.id.list_of_ingredients);
            recipeCarbs = itemView.findViewById(R.id.carbs_amount);
            recipeFat = itemView.findViewById(R.id.fat_amount);
            recipeProtein = itemView.findViewById(R.id.protein_amount);
            CardView mNutritionCard = itemView.findViewById(R.id.nutritionCard);
            recipeAttributes = itemView.findViewById(R.id.recipe_attributes);

            // sets the click listener for the recipe item
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

            // Creates animation for Like button - Animation to grow and shrink heart when clicked
            Animation scaleAnimation_Like = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            // sets the amount of time that the animation will play for
            scaleAnimation_Like.setDuration(500);
            // create an overshoot interpolator to give like animation growing look
            OvershootInterpolator overshootInterpolator_Like = new OvershootInterpolator(4);
            scaleAnimation_Like.setInterpolator(overshootInterpolator_Like);

            // like button on click listener
            like_button.setOnClickListener(v -> {
                // checks whether or not the device is connected to the internet
                if (!con.connectedToInternet()) {
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(Constants.noInternetTitle)
                            .setMessage(Constants.noInternetMessage)
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
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
                        v.startAnimation(scaleAnimation_Like);
                        if (item.isLiked()) {
                            like_button.setImageResource(R.drawable.like_outline);
                            item.setLiked(false);
                            try {
                                removeDataFromFirebase(item);
                                myDb.removeDataFromView(item.getItemId());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            like_button.setImageResource(R.drawable.like_filled);
                            item.setLiked(true);
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
                                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                .create()
                                .show();
                    }
                }
            });

            more_button.setOnClickListener(v -> {

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
            });

            mNutritionCard.setOnClickListener(v -> new MaterialAlertDialogBuilder(mContext)
                    .setTitle(Constants.nutritionInformationTitle)
                    .setMessage(Constants.nutritionInformation)
                    .setPositiveButton("Got It!", (dialog, which) -> dialog.dismiss()).create()
                    .show());

            mViewRecipe.setOnClickListener(v -> {
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
                        .setTitle(Constants.noInternetTitle)
                        .setMessage(Constants.noInternetMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                final CharSequence[] listItems = {"Inappropriate Image","Inappropriate Website","Profanity"};
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle("Why are you reporting this?")
                        .setSingleChoiceItems(listItems, 0, (dialog, which) -> {
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
                        })
                        .setPositiveButton("Report",(dialog, which) -> {
                            if (!con.connectedToInternet()) {
                                new MaterialAlertDialogBuilder(mContext)
                                        .setTitle(Constants.noInternetTitle)
                                        .setMessage(Constants.noInternetMessage)
                                        .setPositiveButton("OK", (dialog1, which1) -> dialog1.dismiss())
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

            HashMap<String, Object> reportInfo = new HashMap<>();

            CollectionReference reportingReference = db.collection("RecipeReports").document(strDate).collection("reports");

            reportInfo.put("Recipe Report Reason", reason);
            reportInfo.put("Timestamp", timestamp);
            reportInfo.put("Recipe Image", item.getmImageUrl());
            reportInfo.put("Recipe Name", item.getmRecipeName());
            reportInfo.put("Recipe Source", item.getmSourceName());
            reportInfo.put("Recipe Ingredients", item.getmIngredients());
            reportInfo.put("Recipe URL", item.getmRecipeURL());

            reportingReference.document().set(reportInfo)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG,"Report saved to Firebase");
                        toastMessage("Reported for " + reason + ". Thank you");
                    })
                    .addOnFailureListener(e -> {
                        toastMessage("Error sending report");
                        Log.d(TAG, e.toString());
                    });
        }

        private void copyRecipe() {
            ClipboardManager clipboardManager = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("Copy URL", retrieveRecipeUrl());
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(clipData);
            }
            toastMessage("Recipe URL copied");
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
}

