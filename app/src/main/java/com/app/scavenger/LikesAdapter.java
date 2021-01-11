package com.app.scavenger;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import static com.app.scavenger.MainActivity.RECIPEITEMSCREENCALL;

@SuppressWarnings("unchecked")
public class LikesAdapter extends RecyclerView.Adapter<LikesAdapter.ViewHolder> implements Filterable {

// --Commented out by Inspection START (11/10/2020 10:21 AM):
//    //private static final String TAG = "LOG: ";
//    public static final int LIKE_UPDATED = 104;
// --Commented out by Inspection STOP (11/10/2020 10:21 AM)

    // Database variables
    private FirebaseFirestore db;
    private DatabaseHelper myDb;

    // Interface variables
    private UpdateSearch mCallback;
    private CheckZeroLikes mZeroLikes;
    private final Fragment likesFragment;

    private final String userId;
    private final ArrayList<RecipeItem> mRecipeItemsFull;
    private ArrayList<RecipeItem> mRecipeItems;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final SharedPreferences sharedPreferences;
    private ConnectionDetector con;

    // Interface to update the search fragment if the information in the removed items is not 0
    interface UpdateSearch {
        void updateSearch();
    }

    // Interface to be used to check if the amount of Likes a user has is actually zero
    interface CheckZeroLikes {
        void checkZeroLikes();
    }

    LikesAdapter(Context context, Fragment likesFragment, ArrayList<RecipeItem> recipeItems, String userId) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mRecipeItems = recipeItems;
        this.userId = userId;
        this.mRecipeItemsFull = recipeItems;
        this.likesFragment = likesFragment;
        mRecipeItemsFull.size();
        this.setHasStableIds(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @NonNull
    @Override
    public LikesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.row_card_item_likes, parent, false);
        con = new ConnectionDetector(mContext);
        myDb = DatabaseHelper.getInstance(mContext);

        db = FirebaseFirestore.getInstance();
        mCallback = (UpdateSearch) mContext;
        mZeroLikes = (CheckZeroLikes) mContext;
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LikesAdapter.ViewHolder holder, int position) {
        populateItemData(holder, position);
    }

    // Filter information for using the SearchView on the Likes fragment to filter through Recipes
    @Override
    public Filter getFilter() {
        return likeFilter;
    }

    private final Filter likeFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            List<RecipeItem> filteredList = new ArrayList<>();
            // if the SearchView on the Likes fragment is empty
            // add the entire list of liked recipes to the arraylist
            if (charSequence.toString().isEmpty()) {
                filteredList.addAll(mRecipeItemsFull);
            } else {
                // For each item in the list
                // if the recipe name, source or ingredients contains the filter text
                // return the results in new arraylist
                for (RecipeItem item : mRecipeItemsFull) {
                    if (item.getmRecipeName().toLowerCase().contains(charSequence.toString().toLowerCase()) || item.getmSourceName().toLowerCase().contains(charSequence.toString().toLowerCase()) || item.getmIngredients().toString().toLowerCase().contains(charSequence.toString().toLowerCase())) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mRecipeItems = (ArrayList<RecipeItem>) results.values;
            notifyDataSetChanged();
        }
    };

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mRecipeItems == null ? 0 : mRecipeItems.size();
    }

    // Returns the hashcode of the item at the adapters position
    @Override
    public long getItemId(int position) {
        RecipeItem item = mRecipeItems.get(position);
        return item.hashCode();
    }

    // Returns the item view type at the position
    @Override
    public int getItemViewType(int position) {
        return position;
    }

    /*
    HELPERS
    ________________________________________________________________________________________________________________________________________
     */

    // Code for adding Recipe Items inside of the Search Recyclerview
    private void populateItemData(ViewHolder holder, int position) {
        // Gets the item at the position
        RecipeItem item = mRecipeItems.get(position);

        // For the Likes fragment, all items are in the state of being liked
        // Set each item to liked
        item.setLiked(true);
        // set their tag to the position of the item in the list
        holder.like_button.setTag(position);
        // sets the image of the button to the filled heart icon
        holder.like_button.setImageResource(R.drawable.like_filled);

        // checks if the image url at that position is not null
        // if not null - load the image from the url into the recipeImageView
        if (item.getmImageUrl() != null) {
            Picasso.get()
                    .load(item.getmImageUrl())
                    .fit()
                    .config(Bitmap.Config.RGB_565)
                    .into(holder.recipeImage);
        } else {
            // if the image url is not found, set the drawable to null
            holder.recipeImage.setImageDrawable(null);
        }

        // Recipe Name
        holder.recipeName.setText(item.getmRecipeName());
        // Recipe Source Name
        holder.recipeSource.setText(item.getmSourceName());
        // Recipe Rating
        //holder.mRatingBar.setNumStars(item.getItemRating());
    }

    // Gets the Recipe Item's ID
    // Removes the document in the users Likes with the Item ID
    private void removeDataFromFirebase(RecipeItem recipeItem) {
        CollectionReference likesRef = db.collection(Constants.firebaseUser).document(userId).collection(Constants.firebaseLikes);

        // testing
        likesRef.document(String.valueOf(recipeItem.getItemId()))
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Likes Adapter", "Successfully removed like"))
                .addOnFailureListener(e -> Log.d("Likes Adapter", "Failed to remove like" + e.toString()));
    }

    // Opens the Recipe in the default web browser
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
            CustomTabColorSchemeParams params = new CustomTabColorSchemeParams.Builder()
                    .setNavigationBarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
                    .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
                    .build();
            builder1.setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, params);
            builder1.setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left);
            builder1.setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right);
            CustomTabsIntent customTabsIntent = builder1.build();
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            builder1.setInstantAppsEnabled(true);
            customTabsIntent.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.getPackageName()));
            //builder1.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
            customTabsIntent.launchUrl(context, Uri.parse(url));
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e("ChromeCustomTabError: ", "Activity Error");
        }
    }

    // Clears the items in the Arraylist and notifies the adapter
    public void clearList() {
        mRecipeItemsFull.clear();
        notifyDataSetChanged();
    }

    // Method for creating a Toast
    private void toastMessage(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    // The callback for the Update the Search fragment after an item has been removed from Likes
    public void update() {
        mCallback.updateSearch();
    }

    // checks to see if the user doesn't have any likes
    public void checkZeroLikes() {
        mZeroLikes.checkZeroLikes();
    }

    /*
    VIEW HOLDERS
    ________________________________________________________________________________________________
    */

    // ITEM VIEW HOLDER
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // View Holder Recipe Card
        private final TextView recipeName, recipeSource;
        private final ImageView recipeImage;
        private final ImageButton more_button, like_button;
        //private final RatingBar mRatingBar;

        // View Holder variables
        private RecipeItem recipeItem;
        private String reportReason = null;

        ViewHolder( @NonNull View itemView) {
            super(itemView);
            recipeName = itemView.findViewById(R.id.recipe_name);
            recipeSource = itemView.findViewById(R.id.recipe_source);
            recipeImage = itemView.findViewById(R.id.recipe_image);
            //CardView recipeHolder = itemView.findViewById(R.id.image_holder);
            like_button = itemView.findViewById(R.id.recipe_like);
            more_button = itemView.findViewById(R.id.more_button);
            //mRatingBar = itemView.findViewById(R.id.ratingBar);

            itemView.setOnClickListener(this);

            // Recipe Image Click Listener
            recipeImage.setOnClickListener(this::openRecipeDetail);

            // Like Button Click Listener
            like_button.setOnClickListener(v -> {
                // Gets the item at the position
                int position = getAdapterPosition();

                // Checks if the user is connected to the internet
                // if false - alert user not connecting
                if (!con.connectedToInternet()) {
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(Constants.noInternetTitle)
                            .setMessage(Constants.noInternetMessage)
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                    // else - ask user if they want to remove the recipe from their likes
                } else {
                    recipeItem = mRecipeItems.get(position);
                                try {
                                    removeDataFromFirebase(recipeItem);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                // remove the item from the like item list in Database
                    // testing
                     myDb.removeDataFromView(recipeItem.getItemId());
                                // add the item to the removed table in Database
                    // testing
                     myDb.addRemovedItem(recipeItem.getItemId());

                                // Let search adapter know that something has changed
                                update();

                                // iterates through the items in filtered items to remove the item with the item's ID
                                Iterator<RecipeItem> i = mRecipeItems.iterator();
                                while (i.hasNext()) {
                                    RecipeItem item = i.next();
                                    // testing
                                    if (item.getItemId() == recipeItem.getItemId()) {
                                        i.remove();
                                    }
                                }

                                // // iterates through the items in the full list to remove the item with the item's ID
                                Iterator<RecipeItem> fullIterator = mRecipeItemsFull.iterator();
                                while (fullIterator.hasNext()) {
                                    RecipeItem item = fullIterator.next();
                                    // testing
                                    if (item.getItemId() == recipeItem.getItemId()) {
                                        fullIterator.remove();
                                    }
                                }

                                // notifies the adapter that an item was removed
                                notifyItemRemoved(position);

                                // if the full item list is empty
                                // alert the Likes fragment to update the Likes Message to say you have no likes
                                if (mRecipeItemsFull.isEmpty()) {
                                    checkZeroLikes();
                                }

                                // get the shared preferences value for actual number of likes
                                int actualNumLikes = sharedPreferences.getInt("actualNumLikes", 0);
                                // subtract 1 from the value
                                actualNumLikes -= 1;
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                // store the new value into numLikes and actualNumLikes
                                // this is so the program knows that these items are the same and the fragment won't keep refreshing
                                editor.putInt("actualNumLikes", actualNumLikes);
                                editor.putInt("numLikes", actualNumLikes);
                                editor.apply();
                }
            });

            // More Button Click Listener
            more_button.setOnClickListener(v -> {
                // get adapter position of the item in the list
                int position = getAdapterPosition();
                // get the recipe item at the position
                recipeItem = mRecipeItems.get(position);

                // create a menu with options (copy, share, report)
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

        // Method for reporting a recipe
        private void reportRecipe() {
            // check if the device is connected to the internet
            // if not connected
            // display alert box alerting the user they are not connected
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(Constants.noInternetTitle)
                        .setMessage(Constants.noInternetMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
                // if connected
                // display an alert asking the user what they want to report the recipe for
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
                                // sends the report to firebase
                                sendReportToDb(reportReason, recipeItem);
                            }
                        })
                        .setNegativeButton("Cancel", ((dialog, which) -> dialog.cancel()))
                        .create()
                        .show();
            }
        }

        // View the recipe on the recipe's website
        private void viewRecipe() {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            boolean inAppBrowsingOn = sharedPreferences.getBoolean("inAppBrowser", true);
            // checks if the default browser is on or off
            // if in app browsing is on
            // open in custom chrome tabs
            if (inAppBrowsingOn) {
                openURLInChromeCustomTab(mContext, retrieveRecipeUrl());
                // else - open in the users default browser
            } else {
                openInDefaultBrowser(mContext, retrieveRecipeUrl());
            }
        }

        // Send report to Server under reports with Phone information
        private void sendReportToDb(String reason, RecipeItem item) {

            // Get the current date and time
            Calendar calendar = Calendar.getInstance();
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int year = calendar.get(Calendar.YEAR);
            calendar.clear();
            calendar.set(year, month, day);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());
            String strDate = simpleDateFormat.format(calendar.getTime());

            // Create a timestamp for Firebase object
            Date now = new Date();
            Timestamp timestamp = new Timestamp(now);

            // Create a Hashmap that holds the information for the report
            HashMap<String, Object> reportInfo = new HashMap<>();

            // reference to the path of the Reports document on Firebase
            CollectionReference reportingReference = db.collection(Constants.firebaseRecipeReports).document(strDate).collection(Constants.firebaseReports);

            // Reason
            reportInfo.put("Recipe Report Reason", reason);
            // Timestamp
            reportInfo.put("Timestamp", timestamp);
            // Recipe Image URL
            reportInfo.put("Recipe Image", item.getmImageUrl());
            // Recipe Name
            reportInfo.put("Recipe Name", item.getmRecipeName());
            // Recipe Source
            reportInfo.put("RecipeSource", item.getmSourceName());
            // Recipe Ingredients
            reportInfo.put("Recipe Ingredients", item.getmIngredients());
            // Recipe URL
            reportInfo.put("Recipe URL", item.getmRecipeURL());

            // Send the data to Firebase
            reportingReference.document().set(reportInfo)
                    .addOnSuccessListener(aVoid -> {
                        //Log.d(TAG,"Report saved to Firebase");
                        toastMessage("Reported for " + reason + ". Thank you");
                    })
                    .addOnFailureListener(e -> {
                        toastMessage("Error sending report");
                        //Log.d(TAG, e.toString());
                    });
        }

        // Copies the Recipe URL
        private void copyRecipe() {
            ClipboardManager clipboardManager = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("Copy URL", retrieveRecipeUrl());
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(clipData);
            }
            toastMessage("Recipe URL copied");
        }

        // share your recipe
        private void shareRecipe() {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            // Subject
            String shareSub = "Check out this awesome recipe from Scavenger!";
            // Body
            String shareBody = retrieveRecipeName() + "\n" + "Made By: " + retrieveRecipeSource() + "\n\n" + retrieveRecipeUrl();
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT,shareSub);
            sharingIntent.putExtra(Intent.EXTRA_TEXT,shareBody);
            mContext.startActivity(Intent.createChooser(sharingIntent, "Share Using:"));
        }


        // Gets the Recipe Item's URL
        private String retrieveRecipeUrl() {
            recipeItem = mRecipeItems.get(getAdapterPosition());
            return recipeItem.getmRecipeURL();
        }

        // Gets the Recipe Item's Source
        private String retrieveRecipeSource() {
            recipeItem = mRecipeItems.get(getAdapterPosition());
            return recipeItem.getmSourceName();
        }

        // Gets the Recipe Item's Name
        private String retrieveRecipeName() {
            recipeItem = mRecipeItems.get(getAdapterPosition());
            return recipeItem.getmRecipeName();
        }

        @Override
        public void onClick(View v) {}

        private void openRecipeDetail(View v) {
            Intent intent = new Intent(mContext, RecipeItemScreen.class);
            //intent.putExtra("activity_id", "like");
            intent.putExtra("recipe_name", mRecipeItems.get(getAdapterPosition()).getmRecipeName());
            intent.putExtra("recipe_source", mRecipeItems.get(getAdapterPosition()).getmSourceName());
            intent.putExtra("recipe_liked", mRecipeItems.get(getAdapterPosition()).isLiked());
            intent.putExtra("recipe_id", mRecipeItems.get(getAdapterPosition()).getItemId());
            intent.putExtra("recipe_image", mRecipeItems.get(getAdapterPosition()).getmImageUrl());
            //intent.putExtra("recipe_rating", mRecipeItems.get(getAdapterPosition()).getItemRating());
            intent.putExtra("recipe_url", mRecipeItems.get(getAdapterPosition()).getmRecipeURL());
            //intent.putExtra("recipe_uri", mRecipeItems.get(getAdapterPosition()).getItemUri());
            intent.putExtra("position", getAdapterPosition());
            // deprecated
            likesFragment.startActivityForResult(intent, RECIPEITEMSCREENCALL);
        }
    }
}
