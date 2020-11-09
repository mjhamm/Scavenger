package com.app.scavenger;

import android.app.Instrumentation;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class RecipeItemScreen extends AppCompatActivity {

    public static final int NEW_COMMENT = 201;

    private TextView instructionsMainText, ingredientsMainText, nutritionMainTitle, calMainText, carbMainText, fatMainText, proteinMainText, recipeName, recipeSource, recipeServings, recipeCalories, recipeCarbs, recipeFat, recipeProtein, recipeAttributes, recipeIngredients, viewComments, mCommentsMainTitle, notConnectedText;
    private ImageView recipeImage;
    private ImageButton recipeLike, recipeMore, commentButton;
    private CardView mNutritionCard;
    private RatingBar ratingBar;
    private MaterialButton viewRecipeButton, mRetryConnection;
    private RecyclerView mInstructionsRecyclerView, mCommentsRecyclerView;
    private ConstraintLayout mMainConstraintLayout;
    private LinearLayoutManager mLayoutManager;
    private CommentsAdapter commentsAdapter;
    private View nutritionLine, ingredientsLine, instructionsLine, commentsLine;
    private ProgressBar mDetailLoading;

    private String userId, internalUrl, name, source, /*itemId,*/ image, url, reportReason, servingsText, caloriesText, carbsText, fatText, proteinText, activityId;
    private ArrayList<String> ingredients, attributes, instructions;
    private ArrayList<CommentItem> commentItems;
    private boolean isLiked, logged;
    private int servingsInt, caloriesInt, carbsInt, fatInt, proteinInt, position, itemId;
    private int commentCount = 0;
    private float rating;
    private InstructionsAdapter instructionsAdapter;

    private ConnectionDetector con;
    private FirebaseFirestore db;
    private DatabaseHelper myDb;
    private SharedPreferences sharedPreferences;
    private long mLastClickTime = 0;

    interface GetRecipeInfoAPI {
        @GET("{itemId}/information?")
        Call<String> getRecipeData(@Path("itemId") int itemId, @Query("apiKey") String apiKey, @Query("includeNutrition") boolean includeNutr);
        /*@GET("/search?")
        Call<String> getRecipeData(@Query("r") String internalUrl, @Query("app_id") String appId, @Query("app_key") String appKey);*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDetailLoading.setVisibility(View.GONE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update to the status bar on lower SDK's
        // Makes bar on lower SDK's black with white icons

        ActivityCompat.postponeEnterTransition(this);

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }*/

        setContentView(R.layout.activity_recipe_item_top);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        db = FirebaseFirestore.getInstance();
        myDb = DatabaseHelper.getInstance(this);
        con = new ConnectionDetector(this);

        instructions = new ArrayList<>();
        ingredients = new ArrayList<>();
        attributes = new ArrayList<>();

        commentItems = new ArrayList<>();

        getInfoFromSharedPrefs();

        recipeName = findViewById(R.id.recipe_name_detail);
        recipeSource = findViewById(R.id.recipe_source_detail);
        recipeImage = findViewById(R.id.recipe_image_detail);
        recipeLike = findViewById(R.id.recipe_like_detail);
        recipeMore = findViewById(R.id.recipe_more_detail);
        ratingBar = findViewById(R.id.ratingBar_detail);
        recipeCalories = findViewById(R.id.calories_amount_detail);
        recipeServings = findViewById(R.id.servings_detail);
        recipeIngredients = findViewById(R.id.ingredients_detail);
        ingredientsMainText = findViewById(R.id.ingredients_main_title);
        instructionsMainText = findViewById(R.id.instructions_main_title);
        recipeAttributes = findViewById(R.id.recipe_attributes_detail);
        recipeCarbs = findViewById(R.id.carbs_amount_detail);
        recipeFat = findViewById(R.id.fat_amount_detail);
        recipeProtein = findViewById(R.id.protein_amount_detail);
        calMainText = findViewById(R.id.calories_text);
        carbMainText = findViewById(R.id.carbs_text);
        fatMainText = findViewById(R.id.fat_text);
        proteinMainText = findViewById(R.id.protein_text);
        viewRecipeButton = findViewById(R.id.view_recipe_button);
        nutritionMainTitle = findViewById(R.id.nutritionFacts_main_title);
        mNutritionCard = findViewById(R.id.nutritionCard);
        mCommentsRecyclerView = findViewById(R.id.comments_recyclerView);
        mInstructionsRecyclerView = findViewById(R.id.instructions_recyclerView);
        mMainConstraintLayout = findViewById(R.id.constraint_layout);
        mCommentsMainTitle = findViewById(R.id.comments_main_title);
        mDetailLoading = findViewById(R.id.loading_detail);
        notConnectedText = findViewById(R.id.not_connected_text_recipeItem);
        commentButton = findViewById(R.id.comment_button);
        viewComments = findViewById(R.id.view_comments);
        nutritionLine = findViewById(R.id.nutrition_underline);
        ingredientsLine = findViewById(R.id.ingredients_underline);
        instructionsLine = findViewById(R.id.instructions_underline);
        commentsLine = findViewById(R.id.comments_underline);
        mRetryConnection = findViewById(R.id.recipe_retry_con_button);

        notConnectedText.setVisibility(View.GONE);
        mRetryConnection.setVisibility(View.GONE);

        if (getIntent() != null) {

            name = getIntent().getExtras().getString("recipe_name");
            source = getIntent().getExtras().getString("recipe_source");
            isLiked = getIntent().getExtras().getBoolean("recipe_liked");
            itemId = getIntent().getExtras().getInt("recipe_id");
//            itemId = getIntent().getExtras().getString("recipe_id");
            image = getIntent().getExtras().getString("recipe_image");

            //Log.d("RecipeItemScreen: ","image: " + image);
            //rating = getIntent().getExtras().getInt("recipe_rating");

            url = getIntent().getExtras().getString("recipe_url");
            position = getIntent().getExtras().getInt("position");

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

            if (con.connectedToInternet()) {

                activityId = getIntent().getExtras().getString("activity_id");
                if (activityId != null) {
                    //internalUrl = getIntent().getExtras().getString("recipe_uri");
                    if (activityId.equals("search")) {
                        callToApi();
                    } else {
                        getRecipeInfoFB();
                    }
                } else {
                    callToApi();
                }

                mLayoutManager = new LinearLayoutManager(this);
                DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, mLayoutManager.getOrientation());
                mCommentsRecyclerView.addItemDecoration(dividerItemDecoration);

                // retrieve comments from FB for the item
                retrieveCommentsFromFB(itemId, name, source);

                /*for (int i = 0; i < 4; i++) {
                    instructions.add("This is step number " + (i + 1) + ". The following instructions want you to preheat your oven." +
                            " Once you have preheated your oven, continue to the next step which will walk you through the next thing to do.");
                }

                instructionsAdapter = new InstructionsAdapter(this, instructions);

                mInstructionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                mInstructionsRecyclerView.setAdapter(instructionsAdapter);*/

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

                if (logged) {
                    commentButton.setVisibility(View.VISIBLE);
                } else {
                    commentButton.setVisibility(View.GONE);
                }
            } else {
                notConnectedText.setVisibility(View.VISIBLE);
                mRetryConnection.setVisibility(View.VISIBLE);
                hideAllLayouts();
            }

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
        } else {
            Toast.makeText(this, "Something went wrong while retrieving data. Please try again", Toast.LENGTH_SHORT).show();
        }
        
        ImageButton mBackButton = findViewById(R.id.item_screen_back);
        mBackButton.setOnClickListener(v -> {
            ratingBar.setVisibility(View.GONE);
            supportFinishAfterTransition();
        });

        commentButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommentsActivity.class);
            intent.putExtra("focus", true);
            intent.putExtra("recipe_name", name);
            intent.putExtra("recipe_source", source);
            intent.putExtra("recipe_id", itemId);
            startActivityForResult(intent, NEW_COMMENT);
        });

        viewComments.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommentsActivity.class);
            intent.putExtra("focus", false);
            intent.putExtra("recipe_name", name);
            intent.putExtra("recipe_source", source);
            intent.putExtra("recipe_id", itemId);
            startActivityForResult(intent, NEW_COMMENT);
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

        recipeMore.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, recipeMore);
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_copy) {
                    copyRecipe();
                    return true;
                } else if (item.getItemId() == R.id.menu_view) {
                    viewRecipe();
                    return true;
                } else if (item.getItemId() == R.id.menu_share) {
                    shareRecipe();
                    return true;
                } else {
                    reportRecipe();
                    return true;
                }
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
                    // update recyclerview item on other screen

                    if (isLiked) {
                        v.startAnimation(scaleAnimation_Like);
                        recipeLike.setImageResource(R.drawable.like_outline);
                        isLiked = false;
                        try {
                            removeDataFromFirebase(itemId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        myDb.removeDataFromView(itemId);
                    } else {
                        v.startAnimation(scaleAnimation_Like);
                        recipeLike.setImageResource(R.drawable.like_filled);
                        isLiked = true;
                        try {
                            saveDataToFirebase(itemId, name, source, image, url);
                            myDb.addDataToView(itemId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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

        mRetryConnection.setOnClickListener(v -> {
            finish();
            startActivity(getIntent());
        });

    }

    private float itemRating(double rating) {
        float newRating = (float) rating / 20;
        if (newRating <= 0) {
            return (float) 0.1;
        } else if (newRating > 5) {
            return 5;
        } else {
            return newRating;
        }
    }

    private void viewRecipe() {
        boolean inAppBrowsingOn = sharedPreferences.getBoolean("inAppBrowser", true);
        if (inAppBrowsingOn) {
            openURLInChromeCustomTab(this, url);
        } else {
            openInDefaultBrowser(this, url);
        }
    }

    private void hideAllLayouts() {
        nutritionMainTitle.setVisibility(View.GONE);
        nutritionLine.setVisibility(View.GONE);
        recipeServings.setVisibility(View.GONE);
        mNutritionCard.setVisibility(View.GONE);
        calMainText.setVisibility(View.GONE);
        recipeCalories.setVisibility(View.GONE);
        carbMainText.setVisibility(View.GONE);
        recipeCarbs.setVisibility(View.GONE);
        fatMainText.setVisibility(View.GONE);
        recipeFat.setVisibility(View.GONE);
        proteinMainText.setVisibility(View.GONE);
        recipeProtein.setVisibility(View.GONE);
        recipeAttributes.setVisibility(View.GONE);
        ingredientsMainText.setVisibility(View.GONE);
        ingredientsLine.setVisibility(View.GONE);
        recipeIngredients.setVisibility(View.GONE);
        instructionsMainText.setVisibility(View.GONE);
        instructionsLine.setVisibility(View.GONE);
        mInstructionsRecyclerView.setVisibility(View.GONE);
        viewRecipeButton.setVisibility(View.GONE);
        mCommentsMainTitle.setVisibility(View.GONE);
        commentsLine.setVisibility(View.GONE);
        commentButton.setVisibility(View.GONE);
        mCommentsRecyclerView.setVisibility(View.GONE);
        viewComments.setVisibility(View.GONE);
    }

    // Method that gets run when a new comment is created in the comments activity
    private void updateCommentsRecyclerView() {
        retrieveCommentsFromFB(itemId, name, source);
    }

    private void retrieveCommentsFromFB(int recipeId, String recipeName, String recipeSource) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        commentItems.clear();

        String itemIdString = String.format("%s", itemId);

        // reference to the users likes
        CollectionReference commentsRef = db.collection(Constants.firebaseComments).document(itemIdString).collection("comments");
        // orders those likes by timestamp in descending order to show the most recent like on top
        commentsRef.orderBy(Constants.firebaseTime, com.google.firebase.firestore.Query.Direction.DESCENDING).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    String name, detail;

                    // if the number of likes the user has is 0
                    // set adapter for recycler to null
                    // this is so no possible overlap of another user can come through
                    if (queryDocumentSnapshots.isEmpty()) {
                        mCommentsRecyclerView.setAdapter(null);
                        // if the number of likes the user has is not 0
                        // clear the list and adapter
                    } else {
                        // go through each item in the snapshot from Firebase and set a new comment item with the information
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {

                            CommentItem commentItem = new CommentItem();
                            name = documentSnapshot.getString(Constants.COMMENT_ITEM);
                            detail = documentSnapshot.getString(Constants.COMMENT_DETAIL);

                            commentItem.setName(name);
                            commentItem.setDetail(detail);

                            if (commentItems.size() < 2) {
                                commentItems.add(commentItem);
                            }
                        }
                        // create the adapter with the new list
                        commentsAdapter = new CommentsAdapter(this, commentItems, recipeId, recipeName, recipeSource);
                        // set adapter
                        mCommentsRecyclerView.setLayoutManager(mLayoutManager);
                        mCommentsRecyclerView.setAdapter(commentsAdapter);

                        commentCount = queryDocumentSnapshots.size();
                    }
                    // After check for comments
                    if (commentCount == 0) {
                        viewComments.setText("No comments");
                    } else if (commentCount == 1) {
                        viewComments.setText("View " + commentCount + " comment");
                    } else {
                        viewComments.setText("View all " + commentCount + " comments");
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
    private void retrieveLikesFromFirebase(int itemId) {

        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        // reference to the users likes
        DocumentReference likeRef = db.collection(Constants.firebaseUser).document(userId).collection(Constants.firebaseLikes).document(String.valueOf(itemId));
        // orders those likes by timestamp in descending order to show the most recent like on top
        likeRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    internalUrl = queryDocumentSnapshots.getString(Constants.ITEM_INTERNAL_URL);
                    callToApi();
                });
    }

    // saves an item to Firebase
    private void saveDataToFirebase(int itemId, String recipeName, String recipeSource, String recipeImage, String recipeUrl) {
        // create new hashmap that holds the item information that will be saved to Firebase
        HashMap<String, Object> itemMap = new HashMap<>();
        // reference to the likes on Firebase
        CollectionReference likesRef = db.collection(Constants.firebaseUser).document(userId).collection(Constants.firebaseLikes);

        // creates a new date and timestamp to be used to order the likes
        Date now = new Date();
        Timestamp timestamp = new Timestamp(now);

        // each part of the recipe item to be put into the hashmap for Firebase
        itemMap.put(Constants.ITEM_ID, itemId);
        itemMap.put(Constants.ITEM_NAME, recipeName);
        itemMap.put(Constants.ITEM_SOURCE, recipeSource);
        itemMap.put(Constants.ITEM_IMAGE, recipeImage);
        itemMap.put(Constants.ITEM_URL, recipeUrl);
        itemMap.put("Timestamp", timestamp);

        // sets the data in Firebase
        likesRef.document(String.valueOf(itemId)).set(itemMap)
                .addOnSuccessListener(aVoid -> {
                    // clears the query inside of the likes fragment and clears focus
                    // this avoids problems with potential filtering of the likes fragment when adding a new item to likes
                    //updateQuery();
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
                    Toast.makeText(this, "Error saving Like. Please try again", Toast.LENGTH_SHORT).show();
                    Log.d("LOG: ", e.toString());
                });
    }

    private void callToApi() {

        Log.d("RecipeItemScreen", "internalUrl: " + internalUrl);

        mDetailLoading.setVisibility(View.VISIBLE);

        Retrofit retrofit = NetworkClient.getRetrofitClient();
        GetRecipeInfoAPI apiService = retrofit.create(GetRecipeInfoAPI.class);

        Call<String> call = apiService.getRecipeData(itemId, Constants.apiKey, true);
        //Call<String> call = apiService.getRecipeData(internalUrl, Constants.appId, Constants.appKey);

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
                        ratingBar.setRating(rating);
                        recipeAttributes.setText(TextUtils.join("", attributes));
                        recipeIngredients.setText(TextUtils.join("", ingredients));

                    } else {
                        Log.i("onEmptyResponse", "Returned Empty Response");
                    }
                } else {
                    Log.d("RECIPEITEMSCREEN", "not successful");
                }
                mDetailLoading.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                mDetailLoading.setVisibility(View.GONE);
            }
        });
    }

    private void getRecipeData(String response) {
        try {
            JSONObject nutr, ing;
            String total_ing;
            ArrayList<String> list_ingredients = new ArrayList<>();
//            JSONObject totalNutrients, carbs, fat, protein;
//            JSONArray healthLabelsArray;
            ArrayList<String> list_healthLabels = new ArrayList<>();
            String labels;

            JSONObject hit = new JSONObject(response);

            // servings integer
            servingsInt = hit.getInt("servings");
            // servings string
            servingsText = String.valueOf(servingsInt);

            // rating
            rating = itemRating(hit.getDouble("spoonacularScore"));

            // nutrition info
            JSONObject nutrition = hit.getJSONObject("nutrition");
            // nutrients
            JSONArray nutrients = nutrition.getJSONArray("nutrients");
            for (int i = 0; i < nutrients.length(); i++) {
                nutr = nutrients.getJSONObject(i);
                // calories
                if (nutr.getString("title").equalsIgnoreCase("calories")) {
                    if (nutr.getInt("amount") >= 0 && nutr.getInt("amount") < 1) {
                        caloriesInt = 1;
                    } else {
                        caloriesInt = nutr.getInt("amount");
                        caloriesInt = caloriesInt * servingsInt;
                    }
                    // carbs
                } else if (nutr.getString("title").equalsIgnoreCase("carbohydrates")) {
                    if (nutr.getInt("amount") >= 0 && nutr.getInt("amount") < 1) {
                        carbsInt = 1;
                    } else {
                        carbsInt = nutr.getInt("amount");
                        carbsInt = carbsInt * servingsInt;
                    }
                    // fat
                } else if (nutr.getString("title").equalsIgnoreCase("fat")) {
                    if (nutr.getInt("amount") >= 0 && nutr.getInt("amount") < 1) {
                        fatInt = 1;
                    } else {
                        fatInt = nutr.getInt("amount");
                        fatInt = fatInt * servingsInt;
                    }
                    // protein
                } else if (nutr.getString("title").equalsIgnoreCase("protein")) {
                    if (nutr.getInt("amount") >= 0 && nutr.getInt("amount") < 1) {
                        proteinInt = 1;
                    } else {
                        proteinInt = nutr.getInt("amount");
                        proteinInt = proteinInt * servingsInt;
                    }
                }
            }
            // multiply by number of servings to get total nutrients
            // calories
            caloriesText = String.valueOf(caloriesInt);
            // carbs
            carbsText = carbsInt + "g";
            // fat
            fatText = fatInt + "g";
            // protein
            proteinText = proteinInt + "g";

            // diets
            JSONArray dietLabelsArray = hit.getJSONArray("diets");
            for (int j = 0; j < dietLabelsArray.length(); j++) {
                labels = dietLabelsArray.getString(j);
                if (!list_healthLabels.contains("\n\u2022 " + labels + "\n")) {
                    list_healthLabels.add("\n\u2022 " + labels + "\n");
                }
                attributes = list_healthLabels;
            }

            // ingredients
            JSONArray ingredientsArray = hit.getJSONArray("extendedIngredients");
            for (int k = 0; k < ingredientsArray.length(); k++) {
                ing = ingredientsArray.getJSONObject(k);
                total_ing = ing.getString("original");

                // UPDATE - 1.0.1
                // Replaces huge spaces in between ingredients
                total_ing = total_ing.replace("\n", "");

                // Gets rid of duplicate ingredients in recipe
                if (!list_ingredients.contains("\n\u2022 " + total_ing + "\n")) {
                    list_ingredients.add("\n\u2022 " + total_ing + "\n");
                }
                ingredients = list_ingredients;
            }

            // instructions
            /*JSONArray instructionsArray = hit.getJSONArray("analyzedInstructions");
            for (int m = 0; m < instructionsArray.length(); m++) {

            }*/

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*private void getRecipeData(String response) {
        try {
            JSONObject ing, totalNutrients, carbs, fat, protein;
            JSONArray dietLabelsArray, healthLabelsArray, ingredientsArray;
            ArrayList<String> list_healthLabels, list_ingredients;
            String labels, total_ing;

            JSONArray hit = new JSONArray(response);

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
    }*/

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
    private void removeDataFromFirebase(int itemId) {
        CollectionReference likesRef = db.collection(Constants.firebaseUser).document(userId).collection(Constants.firebaseLikes);

        likesRef.document(String.valueOf(itemId))
                .delete()
                .addOnSuccessListener(aVoid -> {

                    int likes = sharedPreferences.getInt("numLikes", 0);
                    likes -= 1;
                    // add it to numLikes
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("numLikes", likes);
                    editor.apply();
                    Log.d("RecipeItemScreen: ", "Successfully removed like");
                })
                .addOnFailureListener(e -> Log.d("RecipeItemScreen: ", "Failed to remove like" + e.toString()));
    }

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NEW_COMMENT && resultCode == RESULT_OK) {
            Log.d("RECIPEITEMSCREEN", "RESULT");
            updateCommentsRecyclerView();
        }
    }

    @Override
    public void onBackPressed() {
        ratingBar.setVisibility(View.GONE);
        super.onBackPressed();
    }

    @Override
    public void finish() {

        // Send a result back to the recipeitemscreen to let it know to recheck the comments
        Intent returnIntent = new Intent();
        returnIntent.putExtra("position", position);
        returnIntent.putExtra("liked", isLiked);
        returnIntent.putExtra("itemId", itemId);

        //By not passing the intent in the result, the calling activity will get null data.
        setResult(RESULT_OK, returnIntent);

        super.finish();
    }
}