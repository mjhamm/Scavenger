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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
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
    private DatabaseHelper myDb;

    private String userId;
    private UpdateSearch mCallback;
    private CheckZeroLikes mZeroLikes;

    private ArrayList<RecipeItem> mRecipeItemsFull;
    private ArrayList<RecipeItem> mRecipeItems;
    private Context mContext;
    private LayoutInflater mInflater;
    private SharedPreferences sharedPreferences;
    private ConnectionDetector con;
    private int filterCount;

    private boolean refresh = false;

    interface UpdateSearch {
        void updateSearch();
    }

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
        mCallback = (UpdateSearch) mContext;
        mZeroLikes = (CheckZeroLikes) mContext;
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LikesAdapter.ViewHolder holder, int position) {
        RecipeItem item = mRecipeItems.get(position);

        boolean isExpanded = mRecipeItems.get(position).isClicked();
        holder.expandableLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        item.setFavorited(true);
        holder.favorite_button.setTag(position);
        holder.favorite_button.setImageResource(R.mipmap.heart_icon_filled);

        String servings_string = String.format(mContext.getString(R.string.servings_text),item.getmServings());

        if (item.getmImageUrl() != null) {
            Picasso.get()
                    .load(item.getmImageUrl())
                    .fit()
                    .memoryPolicy(MemoryPolicy.NO_CACHE)
                    .networkPolicy(NetworkPolicy.NO_CACHE)
                    .config(Bitmap.Config.RGB_565)
                    .into(holder.recipeImage);
        } else {
            holder.recipeImage.setImageDrawable(null);
        }


        holder.recipeName.setText(item.getmRecipeName());
        holder.recipeSource.setText(item.getmSourceName());
        holder.recipeServings.setText(servings_string);
        holder.recipeCalories.setText(String.valueOf(item.getmCalories()));
        holder.recipeCarbs.setText(String.valueOf(item.getmCarbs()));
        holder.recipeFat.setText(String.valueOf(item.getmFat()));
        holder.recipeProtein.setText(String.valueOf(item.getmProtein()));
        holder.recipeIngredients.setText(TextUtils.join("", item.getmIngredients()));
        holder.recipeAttributes.setText(TextUtils.join("", item.getmRecipeAttributes()));
    }

    @Override
    public Filter getFilter() {
        return likeFilter;
    }

    private Filter likeFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            List<RecipeItem> filteredList = new ArrayList<>();
            if (charSequence.toString().isEmpty()) {
                filteredList.addAll(mRecipeItemsFull);
            } else {
                for (RecipeItem item : mRecipeItemsFull) {
                    if (item.getmRecipeName().toLowerCase().contains(charSequence.toString().toLowerCase()) || item.getmSourceName().toLowerCase().contains(charSequence.toString().toLowerCase()) || item.getmIngredients().toString().toLowerCase().contains(charSequence.toString().toLowerCase())) {
                        filteredList.add(item);
                    }
                }
            }
            Log.d(TAG, "1. filterCount: " + filterCount);
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            Log.d(TAG, "2. filterCount: " + filterCount);
            filterCount = results.count;
            mRecipeItems = (ArrayList<RecipeItem>) results.values;
            notifyDataSetChanged();
        }
    };

    public int getFilterItemCount() {
        return filterCount;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

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
        private RecipeItem recipeItem;
        private ImageButton more_button, favorite_button;
        private ConstraintLayout expandableLayout;
        private String reportReason = null;

        ViewHolder( @NonNull View itemView) {
            super(itemView);
            recipeName = itemView.findViewById(R.id.recipe_name);
            recipeSource = itemView.findViewById(R.id.recipe_source);
            recipeImage = itemView.findViewById(R.id.recipe_image);
            favorite_button = itemView.findViewById(R.id.recipe_favorite);
            more_button = itemView.findViewById(R.id.more_button);
            recipeServings = itemView.findViewById(R.id.servings_total);
            recipeCalories = itemView.findViewById(R.id.calories_amount);
            CardView bottomCard = itemView.findViewById(R.id.bottomCardView);
            recipeIngredients = itemView.findViewById(R.id.list_of_ingredients);
            recipeCarbs = itemView.findViewById(R.id.carbs_amount);
            recipeFat = itemView.findViewById(R.id.fat_amount);
            expandableLayout = itemView.findViewById(R.id.expandableLayout);
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

            favorite_button.setOnClickListener(v -> {
                // Gets the item at the position
                if (!con.connectedToInternet()) {
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle("No Internet connection found")
                            .setMessage("You don't have an Internet connection. Please reconnect and try again.")
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                } else {
                    recipeItem = mRecipeItems.get(getAdapterPosition());
                    String recipeItemId = recipeItem.getItemId();
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle("Remove this recipe from your Likes?")
                            .setMessage("This removes this recipe from your Likes. You will need to go and locate it again.")
                            .setCancelable(false)
                            .setPositiveButton("Remove", (dialog, which) -> {
                                try {
                                    removeDataFromFirebase(recipeItem);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                myDb.removeDataFromView(recipeItem.getItemId());
                                myDb.addRemovedItem(recipeItem.getItemId());
                                update();
                                // CHECK - Let fragment know to reload
                                for (Iterator<RecipeItem> iterator = mRecipeItemsFull.iterator(); iterator.hasNext();) {
                                    if (iterator.next().getItemId().equals(recipeItemId)) {
                                        iterator.remove();
                                    }
                                }
                                if (!mRecipeItems.isEmpty()) {
                                    mRecipeItems.remove(getAdapterPosition());
                                }
                                if (mRecipeItemsFull.isEmpty()) {
                                    checkZeroLikes();
                                }
                                int actualNumLikes = sharedPreferences.getInt("actualNumLikes", 0);
                                actualNumLikes -= 1;
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putInt("actualNumLikes", actualNumLikes);
                                editor.apply();
                                notifyDataSetChanged();
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                }
            });

            more_button.setOnClickListener(v -> {
                int position = getAdapterPosition();
                recipeItem = mRecipeItems.get(position);

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
                    }
                    return false;
                });
                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.like_menu, popupMenu.getMenu());
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

            mNutritionCard.setOnClickListener(v -> new MaterialAlertDialogBuilder(mContext)
                    .setTitle("Some Information about Our Data")
                    .setMessage("Scavenger uses Edamam Search and your search criteria to look throughout the Internet in order to bring you " +
                            "the best information we can find. However, sometimes this information may not be 100% accurate. Using " +
                            "the View Recipe button to see the recipe on the actual website will give you the most accurate data. This includes Nutrition Information " +
                            "as well as the number of servings the amount of ingredients can make.")
                    .setPositiveButton("Got It!", (dialog, which) -> dialog.dismiss()).create()
                    .show());

            recipeServings.setOnClickListener(v -> new MaterialAlertDialogBuilder(mContext)
                    .setTitle("Some Information about Our Data")
                    .setMessage("Scavenger uses Edamam Search and your search criteria to look throughout the Internet in order to bring you " +
                            "the best information we can find. However, sometimes this information may not be 100% accurate. Using " +
                            "the View Recipe button to see the recipe on the actual website will give you the most accurate data. This includes Nutrition Information " +
                            "as well as the number of servings the amount of ingredients can make.")
                    .setPositiveButton("Got It!", (dialog, which) -> dialog.dismiss()).create()
                    .show());

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
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
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
                                        .setTitle("No Internet connection found")
                                        .setMessage("You don't have an Internet connection. Please reconnect and try again.")
                                        .setPositiveButton("OK", (dialog1, which1) -> dialog1.dismiss())
                                        .create()
                                        .show();
                            } else {
                                sendReportToDb(reportReason, recipeItem);
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

        private String retrieveRecipeUrl() {
            recipeItem = mRecipeItems.get(getAdapterPosition());
            return recipeItem.getmRecipeURL();
        }

        private String retrieveRecipeSource() {
            recipeItem = mRecipeItems.get(getAdapterPosition());
            return recipeItem.getmSourceName();
        }

        private String retrieveRecipeName() {
            recipeItem = mRecipeItems.get(getAdapterPosition());
            return recipeItem.getmRecipeName();
        }

        @Override
        public void onClick(View v) {

        }
    }

    private void removeDataFromFirebase(RecipeItem recipeItem) {
        CollectionReference favoritesRef = db.collection("Users").document(userId).collection("Favorites");

        favoritesRef.document(recipeItem.getItemId())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully removed favorite"))
                .addOnFailureListener(e -> Log.d(TAG, "Failed to remove favorite" + e.toString()));
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

    public void clearList() {
        //int size = mRecipeItems.size();
        mRecipeItemsFull.clear();
        notifyDataSetChanged();
    }

    //method for creating a Toast
    private void toastMessage(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mRecipeItems.size();
    }

    public void update() {
        mCallback.updateSearch();
    }

    public void checkZeroLikes() {
        mZeroLikes.checkZeroLikes();
    }

    @Override
    public long getItemId(int position) {
        RecipeItem item = mRecipeItems.get(position);
        return item.hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}