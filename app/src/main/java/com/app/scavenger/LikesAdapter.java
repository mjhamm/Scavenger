package com.app.scavenger;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unchecked")
public class LikesAdapter extends RecyclerView.Adapter<LikesAdapter.ViewHolder> implements Filterable {

    private static final String TAG = "LOG: ";

    // Database variables
    private FirebaseFirestore db;
    private DatabaseHelper myDb;

    // Interface variables
    private UpdateSearch mCallback;
    private CheckZeroLikes mZeroLikes;

    private final String userId;
    private final ArrayList<RecipeItem> mRecipeItemsFull;
    private ArrayList<RecipeItem> mRecipeItems;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final SharedPreferences sharedPreferences;
    private ConnectionDetector con;
    private int filterCount;

    // Interface to update the search fragment if the information in the removed items is not 0
    interface UpdateSearch {
        void updateSearch();
    }

    // Interface to be used to check if the amount of Likes a user has is actually zero
    interface CheckZeroLikes {
        void checkZeroLikes();
    }

    LikesAdapter(Context context, ArrayList<RecipeItem> recipeItems, String userId) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mRecipeItems = recipeItems;
        this.userId = userId;
        this.mRecipeItemsFull = recipeItems;
        filterCount = mRecipeItemsFull.size();
        this.setHasStableIds(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @NonNull
    @Override
    public LikesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.row_card_item, parent, false);
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
            filterCount = results.count;
            mRecipeItems = (ArrayList<RecipeItem>) results.values;
            notifyDataSetChanged();
        }
    };

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mRecipeItems.size();
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

        // variable for checking whether the item is expanded or not
        boolean isExpanded = mRecipeItems.get(position).isClicked();
        // if the variable for isExpanded is true - mBottomCard is Visible
        // else - mBottomCard is Gone
        holder.mBottomCard.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

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
        // # of Servings
        holder.recipeServings.setText(String.format(mContext.getString(R.string.servings_text),item.getmServings()));
        // # of Calories
        holder.recipeCalories.setText(String.valueOf(item.getmCalories()));
        // # of Carbs
        holder.recipeCarbs.setText(String.valueOf(item.getmCarbs()));
        // # of Fat
        holder.recipeFat.setText(String.valueOf(item.getmFat()));
        // # of Protein
        holder.recipeProtein.setText(String.valueOf(item.getmProtein()));
        // Recipe Ingredients
        holder.recipeIngredients.setText(TextUtils.join("", item.getmIngredients()));
        // Recipe Attributes
        holder.recipeAttributes.setText(TextUtils.join("", item.getmRecipeAttributes()));
    }

    // Gets the Recipe Item's ID
    // Removes the document in the users Likes with the Item ID
    private void removeDataFromFirebase(RecipeItem recipeItem) {
        CollectionReference likesRef = db.collection(Constants.firebaseUser).document(userId).collection(Constants.firebaseLikes);

        likesRef.document(recipeItem.getItemId())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully removed like"))
                .addOnFailureListener(e -> Log.d(TAG, "Failed to remove like" + e.toString()));
    }

    // Opens the Recipe in the default web browser
    private static void openInDefaultBrowser(Context context, String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(browserIntent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e("DefaultBrowserError: ", "Activity Error");
        }
    }

    // Opens the Recipe in Google Chrome
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
        private final TextView recipeName, recipeSource, recipeServings, recipeCalories, recipeIngredients, recipeCarbs, recipeFat, recipeProtein, recipeAttributes;
        private final ImageView recipeImage;
        private final ImageButton more_button, like_button;
        private final CardView mBottomCard;

        // View Holder variables
        private RecipeItem recipeItem;
        private String reportReason = null;

        ViewHolder( @NonNull View itemView) {
            super(itemView);
            recipeName = itemView.findViewById(R.id.recipe_name);
            recipeSource = itemView.findViewById(R.id.recipe_source);
            recipeImage = itemView.findViewById(R.id.recipe_image);
            like_button = itemView.findViewById(R.id.recipe_like);
            more_button = itemView.findViewById(R.id.more_button);
            recipeServings = itemView.findViewById(R.id.servings_total);
            recipeCalories = itemView.findViewById(R.id.calories_amount);
            ImageView edamamBranding = itemView.findViewById(R.id.edamam_branding);
            recipeIngredients = itemView.findViewById(R.id.list_of_ingredients);
            recipeCarbs = itemView.findViewById(R.id.carbs_amount);
            recipeFat = itemView.findViewById(R.id.fat_amount);
            mBottomCard = itemView.findViewById(R.id.bottomCardView);
            recipeProtein = itemView.findViewById(R.id.protein_amount);
            CardView mNutritionCard = itemView.findViewById(R.id.facts_cardView);
            recipeAttributes = itemView.findViewById(R.id.recipe_attributes);
            CardView mViewRecipe = itemView.findViewById(R.id.viewRecipe_button);

            itemView.setOnClickListener(this);

            // Recipe Image Click Listener
            recipeImage.setOnClickListener(v -> {
                int position = getAdapterPosition();
                // Adapter Position
                // Gets the item at the position
                recipeItem = mRecipeItems.get(position);
                // Checks if the item is clicked
                // Sets the layout visible/gone
                recipeItem.setClicked(!recipeItem.isClicked());
                notifyItemChanged(position);
            });

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
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle("Remove this recipe from your Likes?")
                            .setMessage("This removes this recipe from your Likes. You will need to go and locate it again.")
                            .setCancelable(false)
                            // Positive button - Remove the item from Firebase
                            .setPositiveButton("Remove", (dialog, which) -> {
                                try {
                                    removeDataFromFirebase(recipeItem);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                // remove the item from the like item list in Database
                                myDb.removeDataFromView(recipeItem.getItemId());
                                // add the item to the removed table in Database
                                myDb.addRemovedItem(recipeItem.getItemId());

                                // Let search adapter know that something has changed
                                update();

                                // iterates through the items in filtered items to remove the item with the item's ID
                                Iterator<RecipeItem> i = mRecipeItems.iterator();
                                while (i.hasNext()) {
                                    RecipeItem item = i.next();
                                    if (item.getItemId().equals(recipeItem.getItemId())) {
                                        i.remove();
                                    }
                                }

                                // // iterates through the items in the full list to remove the item with the item's ID
                                Iterator<RecipeItem> fullIterator = mRecipeItemsFull.iterator();
                                while (fullIterator.hasNext()) {
                                    RecipeItem item = fullIterator.next();
                                    if (item.getItemId().equals(recipeItem.getItemId())) {
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

                            })
                            // dismiss the alert if cancel button is clicked
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
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
                    switch (item.getItemId()) {
                        // copy the recipe url
                        case R.id.fav_menu_copy:
                            copyRecipe();
                            return true;
                            // share the recipe through text, email, facebook
                        case R.id.fav_menu_share:
                            shareRecipe();
                            return true;
                            // report the recipe for profanity, nudity, or website
                        case R.id.fav_menu_report:
                            reportRecipe();
                            return true;
                    }
                    return false;
                });
                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.like_menu, popupMenu.getMenu());
                popupMenu.show();
            });

            // Branding for Edamam Click Listener
            // shows information about how we get our data
            edamamBranding.setOnClickListener(v -> new MaterialAlertDialogBuilder(mContext)
                    .setTitle(Constants.nutritionInformationTitle)
                    .setMessage(Constants.nutritionInformation)
                    .setPositiveButton("Got It!", (dialog, which) -> dialog.dismiss()).create()
                    .show());

            // Nutrition Card Click Listener
            // shows information about how we get our data
            mNutritionCard.setOnClickListener(v -> new MaterialAlertDialogBuilder(mContext)
                    .setTitle(Constants.nutritionInformationTitle)
                    .setMessage(Constants.nutritionInformation)
                    .setPositiveButton("Got It!", (dialog, which) -> dialog.dismiss()).create()
                    .show());

            // Servings Click Listener
            // shows information about how we get our data
            recipeServings.setOnClickListener(v -> new MaterialAlertDialogBuilder(mContext)
                    .setTitle(Constants.nutritionInformationTitle)
                    .setMessage(Constants.nutritionInformation)
                    .setPositiveButton("Got It!", (dialog, which) -> dialog.dismiss()).create()
                    .show());

            // View Recipe Click Listener
            mViewRecipe.setOnClickListener(v -> {
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
                        .setSingleChoiceItems(listItems, -1, (dialog, which) -> {
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
            Map<String, Object> reportInfo = new HashMap<>();

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
                        Log.d(TAG,"Report saved to Firebase");
                        toastMessage("Reported for " + reason + ". Thank you");
                    })
                    .addOnFailureListener(e -> {
                        toastMessage("Error sending report");
                        Log.d(TAG, e.toString());
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
    }
}
