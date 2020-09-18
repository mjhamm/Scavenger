package com.app.scavenger;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class RecipeItemScreen extends AppCompatActivity {

    private TextView recipeName, recipeSource, recipeServings, recipeCalories, recipeCarbs, recipeFat, recipeProtein, recipeAttributes, recipeIngredients, viewComments, mCommentsMainTitle;
    private ImageView recipeImage;
    private ImageButton recipeLike, recipeMore;
    private CardView recipeHolder, mNutritionCard;
    private RatingBar ratingBar;
    private MaterialButton viewRecipeButton;
    private RecyclerView mInstructionsRecyclerView, mCommentsRecyclerView;
    private ConstraintLayout mMainConstraintLayout;

    private String userId, internalUrl, internalUrlFormatted, name, source, itemId, image, url, reportReason, servingsText, caloriesText, carbsText, fatText, proteinText;
    private ArrayList<String> ingredients, attributes, instructions;
    private boolean isLiked, logged;
    private int rating, servingsInt, caloriesInt, carbsInt, fatInt, proteinInt;
    private int commentCount = 0;

    private ConnectionDetector con;
    private FirebaseFirestore db;
    private DatabaseHelper myDb;
    private SharedPreferences sharedPreferences;
    private long mLastClickTime = 0;

    interface GetRecipeInfoAPI {
        @GET("/search?")
        Call<String> getRecipeData(@Query("r") String internalUrl, @Query("app_id") String appId, @Query("app_key") String appKey);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update to the status bar on lower SDK's
        // Makes bar on lower SDK's black with white icons

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.getWindow().getDecorView().setSystemUiVisibility( View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        setContentView(R.layout.activity_recipe_item_screen);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());

        ActivityCompat.postponeEnterTransition(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        db = FirebaseFirestore.getInstance();
        myDb = DatabaseHelper.getInstance(this);
        con = new ConnectionDetector(this);

        instructions = new ArrayList<>();
        //instructions.add("1");
        ingredients = new ArrayList<>();
        attributes = new ArrayList<>();

        getInfoFromSharedPrefs();

        recipeName = findViewById(R.id.recipe_name_detail);
        recipeSource = findViewById(R.id.recipe_source_detail);
        recipeImage = findViewById(R.id.recipe_image_detail);
        recipeLike = findViewById(R.id.recipe_like_detail);
        recipeMore = findViewById(R.id.recipe_more_detail);
        recipeHolder = findViewById(R.id.recipe_image_holder_detail);
        ratingBar = findViewById(R.id.ratingBar_detail);
        recipeCalories = findViewById(R.id.calories_amount_detail);
        recipeServings = findViewById(R.id.servings_detail);
        recipeIngredients = findViewById(R.id.ingredients_detail);
        recipeAttributes = findViewById(R.id.recipe_attributes_detail);
        recipeCarbs = findViewById(R.id.carbs_amount_detail);
        recipeFat = findViewById(R.id.fat_amount_detail);
        recipeProtein = findViewById(R.id.protein_amount_detail);
        viewRecipeButton = findViewById(R.id.view_recipe_button);
        mNutritionCard = findViewById(R.id.nutritionCard);
        mCommentsRecyclerView = findViewById(R.id.comments_recyclerView);
        mInstructionsRecyclerView = findViewById(R.id.instructions_recyclerView);
        mMainConstraintLayout = findViewById(R.id.constraint_layout);
        mCommentsMainTitle = findViewById(R.id.comments_main_title);

        ImageButton mBackButton = findViewById(R.id.item_screen_back);
        mBackButton.setOnClickListener(v -> supportFinishAfterTransition());

        ImageButton commentButton = findViewById(R.id.comment_button);
        viewComments = findViewById(R.id.view_comments);

        if (logged) {
            commentButton.setVisibility(View.VISIBLE);
        } else {
            commentButton.setVisibility(View.GONE);
        }

        commentButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommentsActivity.class);
            intent.putExtra("focus", true);
            intent.putExtra("recipe_name", name);
            intent.putExtra("recipe_source", source);
            startActivity(intent);
        });

        viewComments.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommentsActivity.class);
            intent.putExtra("focus", false);
            intent.putExtra("recipe_name", name);
            intent.putExtra("recipe_source", source);
            startActivity(intent);
        });

        viewRecipeButton.setOnClickListener(v -> {
            boolean inAppBrowsingOn = sharedPreferences.getBoolean("inAppBrowser", true);
            if (inAppBrowsingOn) {
                openURLInChromeCustomTab(this, url);
            } else {
                openInDefaultBrowser(this, url);
            }
        });

        // Nutrition Card Click Listener
        // shows information about how we get our data
        mNutritionCard.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(Constants.nutritionInformationTitle)
                .setMessage(Constants.nutritionInformation)
                .setPositiveButton("Got It!", (dialog, which) -> dialog.dismiss()).create()
                .show());

        if (getIntent() != null) {

            String activityId = getIntent().getExtras().getString("activity_id");

            name = getIntent().getExtras().getString("recipe_name");
            source = getIntent().getExtras().getString("recipe_source");
            isLiked = getIntent().getExtras().getBoolean("recipe_liked");
            itemId = getIntent().getExtras().getString("recipe_id");
            image = getIntent().getExtras().getString("recipe_image");
            rating = getIntent().getExtras().getInt("recipe_rating");
            url = getIntent().getExtras().getString("recipe_url");

            recipeName.setText(name);
            recipeSource.setText(source);

            if (isLiked) {
                // set the image to filled
                recipeLike.setImageResource(R.drawable.like_filled);
                // if item isn't liked
            } else {
                // set the image to outline
                recipeLike.setImageResource(R.drawable.like_outline);
            }

            ratingBar.setNumStars(rating);

            if (image != null) {
                Picasso.get()
                        .load(image)
                        .fit()
                        .config(Bitmap.Config.RGB_565)
                        .into(recipeImage, new Callback() {
                            @Override
                            public void onSuccess() {
                                supportStartPostponedEnterTransition();
                            }

                            @Override
                            public void onError(Exception e) {
                                supportStartPostponedEnterTransition();
                            }
                        });
            } else {
                // if the image url is not found, set the drawable to null
                recipeImage.setImageDrawable(null);
            }

            if (activityId != null) {
                internalUrl = getIntent().getExtras().getString("recipe_uri");
                if (activityId.equals("search")) {
                    callToApi();
                } else {
                    getRecipeInfoFB();
                }
            } else {
                callToApi();
            }

            // After check for comments
            if (commentCount == 0) {
                viewComments.setText("No Comments");
            } else if (commentCount == 1) {
                viewComments.setText("View " + commentCount + " comment");
            } else {
                viewComments.setText("View all " + commentCount + " comments");
            }

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(mMainConstraintLayout);

            // After check for instructions
            if (instructions.size() == 0) {
                constraintSet.setVisibility(viewRecipeButton.getId(), ConstraintSet.VISIBLE);
                viewRecipeButton.setEnabled(true);
            } else {
                constraintSet.setVisibility(viewRecipeButton.getId(), ConstraintSet.GONE);
                viewRecipeButton.setEnabled(false);
                constraintSet.connect(mCommentsMainTitle.getId(), ConstraintSet.TOP, mInstructionsRecyclerView.getId(), ConstraintSet.BOTTOM);
            }

            constraintSet.applyTo(mMainConstraintLayout);
        }

        recipeMore.setOnClickListener(v -> {

            PopupMenu popupMenu = new PopupMenu(this, recipeMore);
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

        // Creates animation for Like button - Animation to grow and shrink heart when clicked
        Animation scaleAnimation_Like = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        // sets the amount of time that the animation will play for
        scaleAnimation_Like.setDuration(500);
        // create an overshoot interpolator to give like animation growing look
        OvershootInterpolator overshootInterpolator_Like = new OvershootInterpolator(4);
        scaleAnimation_Like.setInterpolator(overshootInterpolator_Like);

        // like button on click listener
        recipeLike.setOnClickListener(v -> {
            // checks whether or not the device is connected to the internet
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(Constants.noInternetTitle)
                        .setMessage(Constants.noInternetMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1500) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                if (logged) {
                    v.startAnimation(scaleAnimation_Like);
                    if (isLiked) {
                        recipeLike.setImageResource(R.drawable.like_outline);
                        isLiked = false;
                        /*try {
                            removeDataFromFirebase(itemId);
                            myDb.removeDataFromView(itemId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }*/
                    } else {
                        recipeLike.setImageResource(R.drawable.like_filled);
                        isLiked = true;
//                        try {
//                            saveDataToFirebase(item.getItemId(), item.getmRecipeName(), item.getmSourceName(), item.getmImageUrl(), item.getmRecipeURL(), item.getmServings(),
//                                    item.getmCalories(), item.getmCarbs(), item.getmFat(), item.getmProtein(), item.getmRecipeAttributes(), item.getmIngredients());
//                            myDb.addDataToView(itemId);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
                    }
                } else {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("You need to be Signed In")
                            .setMessage("You must Sign Up or Sign In, in order to Like recipes.")
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                }
            }
        });
    }

    private void getRecipeInfoFB() {
        retrieveLikesFromFirebase(itemId);
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

    // Retrieves the user's likes from Firebase using their userId
    private void retrieveLikesFromFirebase(String itemId) {

        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        // reference to the users likes
        DocumentReference likeRef = db.collection(Constants.firebaseUser).document(userId).collection(Constants.firebaseLikes).document(itemId);
        // orders those likes by timestamp in descending order to show the most recent like on top
        likeRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.getLong(Constants.ITEM_YIELD) != null) {
                        servingsInt = queryDocumentSnapshots.getLong(Constants.ITEM_YIELD).intValue();
                    }

                    if (queryDocumentSnapshots.getLong(Constants.ITEM_CAL) != null) {
                        caloriesInt = queryDocumentSnapshots.getLong(Constants.ITEM_CAL).intValue();
                    }

                    if (queryDocumentSnapshots.getLong(Constants.ITEM_CARB) != null) {
                        carbsInt = queryDocumentSnapshots.getLong(Constants.ITEM_CARB).intValue();
                    }
                    if (queryDocumentSnapshots.getLong(Constants.ITEM_FAT) != null) {
                        fatInt = queryDocumentSnapshots.getLong(Constants.ITEM_FAT).intValue();
                    }
                    if (queryDocumentSnapshots.getLong(Constants.ITEM_PROTEIN) != null) {
                        proteinInt = queryDocumentSnapshots.getLong(Constants.ITEM_PROTEIN).intValue();
                    }
                    if (queryDocumentSnapshots.exists()) {
                        attributes = (ArrayList<String>) queryDocumentSnapshots.get(Constants.ITEM_ATT);
                        ingredients = (ArrayList<String>) queryDocumentSnapshots.get(Constants.ITEM_INGR);
                    }

                    recipeServings.setText(servingsInt + " Servings");

                    caloriesText = String.valueOf(caloriesInt);
                    recipeCalories.setText(caloriesText);
                    recipeCarbs.setText(carbsInt + "g");
                    recipeFat.setText(fatInt + "g");
                    recipeProtein.setText(proteinInt + "g");
                    recipeAttributes.setText(TextUtils.join("", attributes));
                    recipeIngredients.setText(TextUtils.join("", ingredients));

                });
    }

    private void callToApi() {

        Retrofit retrofit = NetworkClient.getRetrofitClient();
        GetRecipeInfoAPI apiService = retrofit.create(GetRecipeInfoAPI.class);

        Call<String> call = apiService.getRecipeData(internalUrl, Constants.appId, Constants.appKey);

        call.enqueue(new retrofit2.Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {

                        Log.d("RECIPEITEMSCREEN", response.body());

                        String result = response.body();
                        getRecipeData(result);

                        recipeServings.setText(servingsText + " Servings");
                        recipeCalories.setText(caloriesText);
                        recipeCarbs.setText(carbsText);
                        recipeFat.setText(fatText);
                        recipeProtein.setText(proteinText);
                        recipeAttributes.setText(TextUtils.join("", attributes));
                        recipeIngredients.setText(TextUtils.join("", ingredients));

                    } else {
                        Log.i("onEmptyResponse", "Returned Empty Response");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {}
        });
    }

    private void getRecipeData(String response) {
        try {
            JSONObject ing, totalNutrients, carbs, fat, protein;
            JSONArray dietLabelsArray, healthLabelsArray, ingredientsArray;
            ArrayList<String> list_healthLabels, list_ingredients;
            String labels, total_ing;

            JSONArray hit = new JSONArray(response);

            //Log.d("RECIPEITEMSCREEN", "response: " + response);

            for (int i = 0; i < hit.length(); i++) {
                JSONObject obj = hit.getJSONObject(i);

                servingsInt = obj.getInt("yield");

                Log.d("RECIPEITEMSCREEN", "Servings: " + servingsInt);

                servingsText = String.valueOf(servingsInt);

                //Ingredients
                ingredientsArray = obj.getJSONArray("ingredients");
                list_ingredients = new ArrayList<>();

                for (int m = 0; m < ingredientsArray.length(); m++) {
                    ing = ingredientsArray.getJSONObject(m);
                    total_ing = ing.getString("text");

                    // UPDATE - 1.0.1
                    // Replaces huge spaces in between ingredients
                    total_ing = total_ing.replace("\n", "");

                    // Gets rid of duplicate ingredients in recipe
                    if (!list_ingredients.contains("\n\u2022 " + total_ing + "\n")) {
                        list_ingredients.add("\n\u2022 " + total_ing + "\n");
                    }
                    ingredients = list_ingredients;

                }

                caloriesInt = obj.getInt("calories");

                dietLabelsArray = obj.getJSONArray("dietLabels");
                list_healthLabels = new ArrayList<>();
                for (int j = 0; j < dietLabelsArray.length(); j++) {
                    String diets = dietLabelsArray.getString(j);
                    list_healthLabels.add("\n\u2022 " + diets + "\n");
                }

                healthLabelsArray = obj.getJSONArray("healthLabels");
                for (int k = 0; k < healthLabelsArray.length(); k++) {
                    if (healthLabelsArray.length() <= 3) {
                        labels = healthLabelsArray.getString(k);
                        list_healthLabels.add("\n\u2022 " + labels + "\n");
                    }
                }

                attributes = list_healthLabels;

                totalNutrients = obj.getJSONObject("totalNutrients");
                //Carbs
                carbs = totalNutrients.getJSONObject("CHOCDF");
                if (carbs.getInt("quantity") >= 0 && carbs.getInt("quantity") < 1) {
                    carbsInt = 1;
                } else {
                    carbsInt = carbs.getInt("quantity");
                }

                //Fat
                fat = totalNutrients.getJSONObject("FAT");
                if (fat.getInt("quantity") >= 0 && fat.getInt("quantity") < 1) {
                    fatInt = 1;
                } else {
                    fatInt = fat.getInt("quantity");
                }

                //Protein
                protein = totalNutrients.getJSONObject("PROCNT");
                if (protein.getInt("quantity") >= 0 && protein.getInt("quantity") < 1) {
                    proteinInt = 1;
                } else {
                    proteinInt = protein.getInt("quantity");
                }

                caloriesText = String.valueOf(caloriesInt);
                carbsText = carbsInt + "g";
                fatText = fatInt + "g";
                proteinText = proteinInt + "g";

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void copyRecipe() {
        ClipboardManager clipboardManager = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("Copy URL", url);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(clipData);
        }
        //toastMessage("Recipe URL copied");
    }

    private void shareRecipe() {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String shareSub = "Check out this awesome recipe from Scavenger!";
        String shareBody = recipeName.getText() + "\n" + "Made By: " + recipeSource.getText() + "\n\n" + url;
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT,shareSub);
        sharingIntent.putExtra(Intent.EXTRA_TEXT,shareBody);
        this.startActivity(Intent.createChooser(sharingIntent, "Share Using:"));
    }

    private void reportRecipe() {
        if (!con.connectedToInternet()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(Constants.noInternetTitle)
                    .setMessage(Constants.noInternetMessage)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
        } else {
            final CharSequence[] listItems = {"Inappropriate Image","Inappropriate Website","Profanity"};
            new MaterialAlertDialogBuilder(this)
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
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(Constants.noInternetTitle)
                                    .setMessage(Constants.noInternetMessage)
                                    .setPositiveButton("OK", (dialog1, which1) -> dialog1.dismiss())
                                    .create()
                                    .show();
                        } else {
                            Log.d("Recipe Item Screen", reportReason);
                            //sendReportToDb(reportReason, item);
                        }
                    })
                    .setNegativeButton("Cancel", ((dialog, which) -> dialog.cancel()))
                    .create()
                    .show();
        }
    }

    @Override
    protected void onStop() {
        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && !isFinishing()){
            new Instrumentation().callActivityOnSaveInstanceState(this, new Bundle());
        }
        super.onStop();
    }

    // Gets the Recipe Item's ID
    // Removes the document in the users Likes with the Item ID
    private void removeDataFromFirebase(String itemId) {
        CollectionReference likesRef = db.collection(Constants.firebaseUser).document(userId).collection(Constants.firebaseLikes);

        likesRef.document(itemId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    //Log.d(TAG, "Successfully removed like");
                })
                .addOnFailureListener(e -> {
                    //Log.d(TAG, "Failed to remove like" + e.toString());
                });
    }

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", null);
    }
}