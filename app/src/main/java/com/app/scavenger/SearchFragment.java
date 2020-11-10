package com.app.scavenger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Random;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

import static android.app.Activity.RESULT_OK;
import static com.app.scavenger.MainActivity.RECIPEITEMSCREENCALL;

public class SearchFragment extends Fragment {

    private static final String TAG = "SEARCH_FRAGMENT: ";
    // --Commented out by Inspection (11/10/2020 10:33 AM):public static final int SEARCH_UPDATED = 104;

    private RecyclerView mSearchRecyclerView;
    // testing
    //private ArrayList<String> itemIds;
    private ArrayList<Integer> itemIds;
    private SearchAdapter adapter;
    private Context mContext;
    private SearchView mSearchView;
    private ImageView mSearch_mainBG;
    private TextView startup_message, matchMessage;
    private ShimmerFrameLayout shimmer;
    private int fromIngr = 0;
    private int toIngr = 10;
    private LinearLayoutManager mLayoutManager;
    private ArrayList<RecipeItem> recipeItemArrayList;
    private String queryString = null;
    private ConnectionDetector con;
    private EndlessRecyclerViewScrollListener scrollListener;
    private boolean logged = false;
    private DatabaseHelper myDb;

    interface ApiService {
        @GET("complexSearch?")
        Call<String> getRecipeData(@Query("apiKey") String apiKey, @Query("query") String ingredients, @Query("addRecipeInformation") boolean addInfo, @Query("instructionsRequired") boolean instrRequired, @Query("offset") int fromIngr, @Query("number") int toIngr);
        // testing
        /*@GET("/search?")
        Call<String> getRecipeData(@Query("q") String ingredients, @Query("app_id") String appId, @Query("app_key") String appKey, @Query("ingr") int numIngredients, @Query("from") int fromIngr, @Query("to") int toIngr);*/
    }

    // Required empty public constructor
    public SearchFragment() {}

    // Create a new instance of Search Fragment
    static SearchFragment newInstance() {
        return new SearchFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        myDb = DatabaseHelper.getInstance(mContext);
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        logged = currentUser != null;

        recipeItemArrayList = new ArrayList<>();
        itemIds = new ArrayList<>();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;
        try {
            view = inflater.inflate(R.layout.fragment_search, container, false);
        } catch (Exception e) {
            Log.d(TAG, "onCreateView", e);
            throw  e;
        }

        startup_message = view.findViewById(R.id.startup_message);
        mSearchView = view.findViewById(R.id.search_searchView);
        shimmer = view.findViewById(R.id.search_shimmerLayout);
        matchMessage = view.findViewById(R.id.match_message);
        ProgressBar mProgressBar = view.findViewById(R.id.main_progressBar);
        mSearch_mainBG = view.findViewById(R.id.search_mainBG);
        mSearchRecyclerView = view.findViewById(R.id.search_recyclerView);

        mLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);
        mSearchRecyclerView.setLayoutManager(mLayoutManager);

        con = new ConnectionDetector(mContext);

        if (savedInstanceState != null) {
            queryString = savedInstanceState.getString("query");
            mSearchView.setQuery(queryString, false);
        }

        // sets BG Image to default
        if (!con.connectedToInternet()) {
            changeBGImage(2);
        } else {
            changeBGImage(0);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSearchView.setMaxWidth(Integer.MAX_VALUE);

        // check if the user has the match ingredients option on or not
        // if they do -
        // alert them and let them know
        if (sharedPreferences.getBoolean("match", false)) {
            toastMessage("Match ingredients is On");
        }

        scrollListener = new EndlessRecyclerViewScrollListener(mLayoutManager, mProgressBar, con) {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recipeItemArrayList.isEmpty() && isLastVisible() && !con.connectedToInternet()) {
                    toastMessage("Failed to load more recipes. Please check your Internet connection.");
                }
            }

            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (con.connectedToInternet()) {
                    if (recipeItemArrayList.size() >= 9) {
                        getMoreAsync();
                    }
                }
            }
        };

        mSearchRecyclerView.addOnScrollListener(scrollListener);

        mProgressBar.setVisibility(View.GONE);

        mSearchRecyclerView.setHasFixedSize(true);
        mSearchRecyclerView.setItemViewCacheSize(10);

        RecyclerView.ItemAnimator animator = mSearchRecyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        mSearchRecyclerView.setOnTouchListener((v, event) -> {
            if (mSearchView != null) {
                mSearchView.clearFocus();
            }
            return false;
        });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchView.clearFocus();
                mSearchView.setImeOptions(6);

                if (adapter != null) {
                    adapter = null;
                }

                if (!con.connectedToInternet()) {
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(Constants.noInternetTitle)
                            .setMessage(Constants.noInternetMessage)
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                } else {
                    getIngredients();
                    callToApi();
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return view;
    }

    private void changeBGImage(int image) {

        mSearch_mainBG.setVisibility(View.VISIBLE);
        switch (image) {
            // default
            case 0:
                matchMessage.setVisibility(View.GONE);
                startup_message.setVisibility(View.VISIBLE);
                setMessageToRandom();
                mSearch_mainBG.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.default_bg_screen));
                break;
                // no recipes
            case 1:
                matchMessage.setVisibility(View.VISIBLE);
                startup_message.setVisibility(View.VISIBLE);
                startup_message.setText(R.string.no_recipes_found);
                mSearch_mainBG.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.no_recipes_bg_screen));
                break;
                // no internet
            case 2:
                matchMessage.setVisibility(View.GONE);
                startup_message.setVisibility(View.VISIBLE);
                startup_message.setText(R.string.no_internet_connection);
                mSearch_mainBG.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.no_internet_bg_screen));
                break;
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden) {
            if (!con.connectedToInternet() && recipeItemArrayList.isEmpty()) {
                changeBGImage(2);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("query", queryString);
    }

    public void updateSearchFrag() {
        if (!recipeItemArrayList.isEmpty()) {
            Cursor removedItems = myDb.getRemovedItems();
            while (removedItems.moveToNext()) {
                for (RecipeItem item : recipeItemArrayList) {
                    if (item != null) {
                        // testing
                        if (item.getItemId() == removedItems.getInt(1)) {
                            item.setLiked(false);
                            myDb.removeRemovedItem(item.getItemId());
                        }
                    }
                }
            }
            removedItems.close();

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    public void checkSearchForLikeChange(int itemId, boolean liked) {
        Log.d(TAG, "checkSearchForLikeChange: " + recipeItemArrayList.size());
        if (adapter != null) {
            adapter.updateItemByItemId(itemId, liked);
            //adapter.updateItem(position, liked);
        }
    }

    private void setMessageToRandom() {
        // Random number
        Random random_start_number = new Random();

        //creates a random number 0-4 and sets the welcome text to a specific text based on number
        int start_message_int = random_start_number.nextInt(5);

        switch (start_message_int) {
            case 0:
                startup_message.setText(R.string.startup_message_1);
                break;
            case 1:
                startup_message.setText(R.string.startup_message_2);
                break;
            case 2:
                startup_message.setText(R.string.startup_message_3);
                break;
            case 3:
                startup_message.setText(R.string.startup_message_4);
                break;
            case 4:
                startup_message.setText(R.string.startup_message_5);
                break;
        }
    }

    // Refreshes the Search Fragment when a user signs in or signs up
    public void refreshFrag() {
            // sets query to empty
            mSearchView.setQuery("", false);
            // clears recipe list and adapter
            recipeItemArrayList.clear();
            if (adapter != null) {
                adapter = null;
            }
            // sets recyclerview adapter to null
            mSearchRecyclerView.setAdapter(null);
            // set BG Image to default
            changeBGImage(0);
            // sets startup message to random message
            setMessageToRandom();
    }

    /*private int checkNumIngredients() {

        int numIngr = 100;
        String[] ingredientsArray;
        boolean matchIngr = sharedPreferences.getBoolean("match", false);

        if (matchIngr) {
            if (mSearchView.getQuery().toString().contains(",")) {
                ingredientsArray = mSearchView.getQuery().toString().split(",");
            } else {
                ingredientsArray = mSearchView.getQuery().toString().split(" ");
            }
            numIngr = ingredientsArray.length;
        }
        return numIngr;
    }*/

    private void callToApi() {

        Retrofit retrofit = NetworkClient.getRetrofitClient();
        ApiService apiService = retrofit.create(ApiService.class);

        fromIngr = 0;
        toIngr = 10;

        // testing
        //Call<String> call = apiService.getRecipeData(getIngredientsSearch(), Constants.appId, Constants.appKey, checkNumIngredients(), fromIngr, toIngr);
        Call<String> call = apiService.getRecipeData(Constants.apiKey, getIngredientsSearch(), true, true, fromIngr ,toIngr);

        queryString = getIngredientsSearch();
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                Log.d(TAG, "response: " + response.code());
                if (response.isSuccessful()) {
                    Log.d(TAG, "success: ");
                    if (response.body() != null) {
                        String result = response.body();

                        //recipeItemArrayList.clear();

                        writeRecyclerAsync(result);
                    } else {
                        Log.i("onEmptyResponse", "Returned Empty Response");
                        Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);
                        changeBGImage(0);
                        toastMessage("Something went wrong. Please try again");
                    }
                } else {
                    Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                    changeBGImage(0);
                    toastMessage("Something went wrong. Please try again");
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                Log.d(TAG, "failure: " + "throwable: " + t.toString() + " call: " + call.toString());
            }
        });
    }

    private void getIngredients() {
        if (!con.connectedToInternet()) {
            changeBGImage(2);
        } else {
            if (logged) {
                itemsFromDB();
            }
            recipeItemArrayList.clear();
            mSearchRecyclerView.removeAllViews();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            // Updates Endless Scroll Listener
            scrollListener.resetState();
            startup_message.setVisibility(View.GONE);
            matchMessage.setVisibility(View.GONE);
            mSearch_mainBG.setVisibility(View.GONE);
            shimmer.setVisibility(View.VISIBLE);
            shimmer.startShimmer();

        }
    }

// --Commented out by Inspection START (11/10/2020 10:33 AM):
//    private String randomItemId(RecipeItem item) {
//        return item.getmRecipeURL().replace("/", "");
//    }
// --Commented out by Inspection STOP (11/10/2020 10:33 AM)

    private void writeRecyclerAsync(String response) {
        new Thread(() -> {
            writeRecycler(response);
            try {
                if (isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (adapter == null) {
                            adapter = new SearchAdapter(mContext, this, recipeItemArrayList, logged);
                        }

                        mSearchRecyclerView.setAdapter(adapter);
                        if (recipeItemArrayList.isEmpty()) {
                            // sets BG Image to No Recipes Image
                            changeBGImage(1);
                        }
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);
                    });
                }
            } catch (final Exception e) {
                Log.i(TAG, "Exception in Thread");
            }
        }).start();
    }

    // testing
//    private void writeRecycler(String response) {
//        try {
//            JSONObject hits, recipes;
//
//            JSONObject obj = new JSONObject(response);
//            JSONArray dataArray = obj.getJSONArray("hits");
//
//            for (int i = 0; i < dataArray.length(); i++) {
//
//                RecipeItem item = new RecipeItem();
//                hits = dataArray.getJSONObject(i);
//                recipes = hits.getJSONObject("recipe");
//
//                // Image
//                item.setmImageUrl(recipes.getString("image"));
//                // Name
//                item.setmRecipeName(recipes.getString("label"));
//                // Source
//                item.setmSourceName(recipes.getString("source"));
//                // URL
//                item.setmRecipeURL(recipes.getString("url"));
//                // Rating
//                item.setItemRating(randomItemRating());
//                // Internal URL
//                item.setItemUri(recipes.getString("uri"));
//                // Unique ID
//                item.setItemId(randomItemId(item));
//
//                // checks if item in contained in db liked items to set as liked
//                if (itemIds.contains(item.getItemId())) {
//                    item.setLiked(true);
//                }
//
//                recipeItemArrayList.add(item);
//            }
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }

    private void writeRecycler(String response) {
        try {
            JSONObject hits;

            JSONObject obj = new JSONObject(response);
            JSONArray dataArray = obj.getJSONArray("results");

            for (int i = 0; i < dataArray.length(); i++) {

                RecipeItem item = new RecipeItem();
                hits = dataArray.getJSONObject(i);

                // Image
                item.setmImageUrl(hits.getString("image"));
                Log.d(TAG, "image: " + hits.get("image"));
                // Name
                //if (hits.getString("title").equals("null")) {
                  //  item.setmRecipeName("Recipe Name Not Found");
                //} else {
                    item.setmRecipeName(hits.getString("title"));
                  //  Log.d(TAG, "title: " + hits.get("title"));
                //}
                // Source
                //if (hits.getString("sourceName").equals("null")) {
                  //  item.setmSourceName("Source Name Not Found");
                //} else {
                    item.setmSourceName(hits.getString("sourceName"));
                  //  Log.d(TAG, "sourceName: " + hits.get("sourceName"));
                //}
                // URL
                if (hits.getString("sourceUrl").equals("null") || hits.getString("sourceUrl").isEmpty()) {
                    item.setmRecipeURL("https://www.thescavengerapp.com");
                } else {
                    item.setmRecipeURL(hits.getString("sourceUrl"));
                    Log.d(TAG, "sourceUrl: " + hits.get("sourceUrl"));
                }
                // Rating
                item.setItemRating(itemRating(hits.getDouble("spoonacularScore")));
                Log.d(TAG, "rating: " + hits.getDouble("spoonacularScore"));
                Log.d(TAG, "New Rating: " + itemRating(hits.getDouble("spoonacularScore")));
                // Internal URL
                //item.setItemUri(results.getString("uri"));
                // Unique ID
                item.setItemId(hits.getInt("id"));
                Log.d(TAG, "id: " + hits.get("id"));

                // checks if item in contained in db liked items to set as liked
                if (itemIds.contains(item.getItemId())) {
                    item.setLiked(true);
                }

                Log.d(TAG, "RecipeName: " + item.getmRecipeName());
                Log.d(TAG, "sourceName: " + item.getmSourceName());

                // remove items with null source/title
                recipeItemArrayList.add(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private float itemRating(double rating) {
        /*int min = 1;
        int max = 5;
        return new Random().nextInt((max - min) + 1) + min;*/
        float newRating = (float) rating / 20;
        if (newRating <= 0) {
            return (float) 0.1;
        } else if (newRating > 5) {
            return 5;
        } else {
            return newRating;
        }
    }

    boolean isLastVisible() {
        int numItems = 0;
        int pos = mLayoutManager.findLastCompletelyVisibleItemPosition();
        if (adapter != null) {
            numItems =  adapter.getItemCount();
        }

        return (pos >= numItems - 1);
    }

    private void itemsFromDB() {
        Cursor likesData = myDb.getListContents();
        itemIds.clear();
        likesData.moveToPosition(-1);
        while (likesData.moveToNext()) {
            // testing
            itemIds.add(likesData.getInt(1));
        }
        likesData.close();
    }

    //Gets the input from Searchview and returns it as string
    private String getIngredientsSearch() {
        return mSearchView.getQuery().toString();
    }

    private void getMoreAsync() {
        new Thread(this::getMoreRecipes).start();
    }

    private void getMoreRecipes() {

        fromIngr = toIngr + 1;
        toIngr = fromIngr + 10;
        Retrofit retrofit = NetworkClient.getRetrofitClient();

        ApiService apiService = retrofit.create(ApiService.class);

        // testing
//        Call<String> call = apiService.getRecipeData(getIngredientsSearch(), Constants.appId, Constants.appKey, checkNumIngredients(), fromIngr, toIngr);
        Call<String> call = apiService.getRecipeData(Constants.apiKey, getIngredientsSearch(), true,true, fromIngr, toIngr);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {

                    if (response.body() != null) {
                        String result = response.body();
                        writeRecycler(result);
                    } else {
                        Log.i("onEmptyResponse", "Returned Empty Response");
                        Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);
                        changeBGImage(0);
                        toastMessage("Something went wrong. Please try again");
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {}
        });
    }

    //method for creating a Toast
    private void toastMessage(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    private void updateAdapter(int position, boolean liked) {

        if (adapter != null) {
            adapter.updateItem(position, liked);
        }
    }

    // DEPRECATED
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == RECIPEITEMSCREENCALL && resultCode == RESULT_OK) {

            Log.d(TAG, "onActivityResult");
            int position = data.getIntExtra("position", 0);
            boolean liked = data.getBooleanExtra("liked", false);

            updateAdapter(position, liked);
        }
        // deprecated
        super.onActivityResult(requestCode, resultCode, data);
    }
}