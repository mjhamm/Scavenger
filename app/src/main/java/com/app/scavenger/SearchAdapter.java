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
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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

import static com.app.scavenger.MainActivity.RECIPEITEMSCREENCALL;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = "SEARCH_ADAPTER";
    // --Commented out by Inspection (11/10/2020 10:34 AM):public static final int SEARCH_UPDATED = 104;

    // variables for constructor
    private final ArrayList<RecipeItem> mRecipeItems;
    private final Context mContext;
    private final Fragment searchFragment;
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
    SearchAdapter(Context context, Fragment searchFragment, ArrayList<RecipeItem> recipeItems, boolean logged) {
        this.mContext = context;
        this.mRecipeItems = recipeItems;
        this.logged = logged;
        this.searchFragment = searchFragment;
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
        RecipeItem item = mRecipeItems.get(position);

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
        // rating
        // testing
        holder.mRatingBar.setRating(item.getItemRating());
//        holder.mRatingBar.setNumStars(item.getItemRating());
    }

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", null);
    }

    // saves an item to Firebase
    private void saveDataToFirebase(RecipeItem item) {/*String itemId, String internalUrl, String name, String source, String image, String url*//*, int servings, int calories, int carbs, int fat, int protein, ArrayList<String> attributes, ArrayList<String> ingredients*//*) {*/
        // create new hashmap that holds the item information that will be saved to Firebase
        HashMap<String, Object> itemMap = new HashMap<>();
        // reference to the likes on Firebase
        CollectionReference likesRef = db.collection(Constants.firebaseUser).document(userId).collection(Constants.firebaseLikes);

        // creates a new date and timestamp to be used to order the likes
        Date now = new Date();
        Timestamp timestamp = new Timestamp(now);

        // each part of the recipe item to be put into the hashmap for Firebase
        itemMap.put(Constants.ITEM_ID, item.getItemId());
        //itemMap.put(Constants.ITEM_INTERNAL_URL, item.getItemUri());
        itemMap.put(Constants.ITEM_NAME, item.getmRecipeName());
        itemMap.put(Constants.ITEM_SOURCE, item.getmSourceName());
        itemMap.put(Constants.ITEM_IMAGE, item.getmImageUrl());
        itemMap.put(Constants.ITEM_URL, item.getmRecipeURL());
        //itemMap.put(Constants.ITEM_RATING, item.getItemRating());
        itemMap.put("Timestamp", timestamp);

        // sets the data in Firebase
        // testing
        likesRef.document(String.valueOf(item.getItemId())).set(itemMap)
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
        // testing
        likesRef.document(String.valueOf(recipeItem.getItemId()))
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

    public void updateItem(int position, boolean liked) {
        mRecipeItems.get(position).setLiked(liked);
        notifyItemChanged(position);
    }

    public void updateItemByItemId(int itemId, boolean liked) {
        for (RecipeItem item : mRecipeItems) {
            // testing
            if (item.getItemId() == itemId) {
                item.setLiked(liked);
            }
        }
        notifyDataSetChanged();
    }

    /*
    VIEW HOLDERS
    ________________________________________________________________________________________________
    */

    // ITEM VIEW HOLDER
    public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // views inside recipeCardItem
        private final TextView recipeName, recipeSource;
        private final ImageView recipeImage;
        private final ImageButton more_button, like_button;
        private final RatingBar mRatingBar;

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
            mRatingBar = itemView.findViewById(R.id.ratingBar);

            // sets the click listener for the recipe item
            itemView.setOnClickListener(this);

            // Recipe Image Click Listener
            recipeImage.setOnClickListener(v -> {
                if (!con.connectedToInternet()) {
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(Constants.noInternetTitle)
                            .setMessage("Please reconnect to the Internet in order to view recipe information.")
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                } else {
                    Intent intent = new Intent(mContext, RecipeItemScreen.class);
                    intent.putExtra("activity_id", "search");
                    intent.putExtra("recipe_name", mRecipeItems.get(getAdapterPosition()).getmRecipeName());
                    intent.putExtra("recipe_source", mRecipeItems.get(getAdapterPosition()).getmSourceName());
                    intent.putExtra("recipe_liked", mRecipeItems.get(getAdapterPosition()).isLiked());
                    intent.putExtra("recipe_id", mRecipeItems.get(getAdapterPosition()).getItemId());
                    intent.putExtra("recipe_image", mRecipeItems.get(getAdapterPosition()).getmImageUrl());
                    intent.putExtra("recipe_rating", mRecipeItems.get(getAdapterPosition()).getItemRating());
                    intent.putExtra("recipe_url", mRecipeItems.get(getAdapterPosition()).getmRecipeURL());
                    //intent.putExtra("recipe_uri", mRecipeItems.get(getAdapterPosition()).getItemUri());
                    intent.putExtra("position", getAdapterPosition());
                    // DEPRECATED
                    searchFragment.startActivityForResult(intent, RECIPEITEMSCREENCALL);
                }
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
                    item = mRecipeItems.get(getAdapterPosition());
                    v.getTag();
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1500) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    if (logged) {
                        if (item.isLiked()) {
                                        v.startAnimation(scaleAnimation_Like);
                                        like_button.setImageResource(R.drawable.like_outline);
                                        item.setLiked(false);
                                        try {
                                            removeDataFromFirebase(item);
                                            // testing
                                            myDb.removeDataFromView(item.getItemId());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                        } else {
                            v.startAnimation(scaleAnimation_Like);
                            like_button.setImageResource(R.drawable.like_filled);
                            item.setLiked(true);
                            try {
                                saveDataToFirebase(item);
                                // testing
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

                item = mRecipeItems.get(getAdapterPosition());

                PopupMenu popupMenu = new PopupMenu(mContext, more_button);
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_copy) {
                        copyRecipe();
                    } else if (item.getItemId() == R.id.menu_view) {
                        viewRecipe();
                    } else if (item.getItemId() == R.id.menu_share) {
                        shareRecipe();
                    } else {
                        reportRecipe();
                    }
                    return false;
                });
                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.more_menu, popupMenu.getMenu());
                popupMenu.show();
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

        private void viewRecipe() {
            boolean inAppBrowsingOn = sharedPreferences.getBoolean("inAppBrowser", true);
            if (inAppBrowsingOn) {
                openURLInChromeCustomTab(mContext, retrieveRecipeUrl());
            } else {
                openInDefaultBrowser(mContext, retrieveRecipeUrl());
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
            item = mRecipeItems.get(getAdapterPosition());
            return item.getmRecipeURL();
        }

        private String retrieveRecipeSource() {
            item = mRecipeItems.get(getAdapterPosition());
            return item.getmSourceName();
        }

        private String retrieveRecipeName() {
            item = mRecipeItems.get(getAdapterPosition());
            return item.getmRecipeName();
        }
    }
}

