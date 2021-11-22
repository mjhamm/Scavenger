 package com.app.scavenger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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
import com.app.scavenger.interfaces.ApiService;
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
import static android.app.Activity.RESULT_OK;
import static com.app.scavenger.MainActivity.RECIPEITEMSCREENCALL;

public class SearchFragment extends Fragment {

    private static final String TAG = "SEARCH_FRAGMENT: ";

    private RecyclerView mSearchRecyclerView;
    private ArrayList<Integer> itemIds;
    private SearchAdapter adapter;
    private Context mContext;
    private SearchView mSearchView;
    private ImageView mSearch_mainBG;
    private TextView startup_message;
    private ShimmerFrameLayout shimmer;
    private int fromIngr = 0;
    private int toIngr = 10;
    private LinearLayoutManager mLayoutManager;
    private ArrayList<RecipeItem> recipeItemArrayList;
    private String queryString = null;
    private ConnectionDetector con;
    private EndlessRecyclerViewScrollListener scrollListener;
    private boolean logged = false;
    private boolean loadingRandoms = false;
    private boolean searchingRecipes = false;
    private boolean searchingIngredients = false;
    private DatabaseHelper myDb;

    // Required empty public constructor
    public SearchFragment() {}

    // Create a new instance of Search Fragment
    static SearchFragment newInstance() {
        return new SearchFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        mContext = getContext();
        myDb = DatabaseHelper.getInstance(mContext);
        recipeItemArrayList = new ArrayList<>();
        itemIds = new ArrayList<>();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        logged = currentUser != null;
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");
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

        mSearchView.setMaxWidth(Integer.MAX_VALUE);

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
                    if (recipeItemArrayList.size() >= 9 && searchingRecipes && !searchingIngredients && !loadingRandoms) {
                        getMoreAsync();
                    } else if (recipeItemArrayList.size() >= 9 && !searchingRecipes && searchingIngredients && !loadingRandoms) {
                        getMoreIngrAsync();
                    } else if (recipeItemArrayList.size() >= 9 && !searchingRecipes && !searchingIngredients && loadingRandoms) {
                        getMoreRandomAsync();
                    } else {
                        searchingRecipes = false;
                        searchingIngredients = false;
                        loadingRandoms = false;
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
                    callToApiRecipes();
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
                //matchMessage.setVisibility(View.GONE);
                startup_message.setVisibility(View.VISIBLE);
                setMessageToRandom();
                mSearch_mainBG.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.default_bg_screen));
                break;
                // no recipes
            case 1:
                //matchMessage.setVisibility(View.VISIBLE);
                startup_message.setVisibility(View.VISIBLE);
                startup_message.setText(R.string.no_recipes_found);
                mSearch_mainBG.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.no_recipes_bg_screen));
                break;
                // no internet
            case 2:
                //matchMessage.setVisibility(View.GONE);
                startup_message.setVisibility(View.VISIBLE);
                startup_message.setText(R.string.no_internet_connection);
                mSearch_mainBG.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.no_internet_bg_screen));
                break;
        }
    }

    public void updateSearchFrag() {
        Log.d(TAG, "updateSearchFrag");
        Log.d(TAG, "recipeItemSize: " + recipeItemArrayList.size());
        if (recipeItemArrayList != null && !recipeItemArrayList.isEmpty()) {
            Log.d(TAG, "recipeItemArrayList not empty");
            Cursor removedItems = myDb.getRemovedItems();
            Log.d(TAG, "num removed items" + removedItems.getCount());
            while (removedItems.moveToNext()) {
                for (RecipeItem item : recipeItemArrayList) {
                    if (item != null) {
                        // testing
                        if (item.getItemId() == removedItems.getInt(1)) {
                            Log.d(TAG, "Item ID: " + item.getItemId());
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
            mSearch_mainBG.setVisibility(View.GONE);
            shimmer.setVisibility(View.VISIBLE);
            shimmer.startShimmer();
        }
    }

    // Async tasks for writing the recyclerview ---------------------------------------------------------------------------------------------------------------------

    // async task for writing recycler using recipe query
    private void writeRecyclerRecipeAsync(String response) {
        new Thread(() -> {
            writeRecycler(response);
            try {
                if (isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (adapter == null) {
                            adapter = new SearchAdapter(mContext, this, recipeItemArrayList, logged);
                        }

                        if (recipeItemArrayList.size() <= 5) {
                            callToApiIngr();
                        } else {
                            mSearchRecyclerView.setAdapter(adapter);
                            shimmer.stopShimmer();
                            shimmer.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (final Exception e) {
                Log.i(TAG, "Exception in Thread");
            }
        }).start();
    }

    // async task for writing recycler using ingredients query
    private void writeRecyclerIngrAsync(String response) {
        new Thread(() -> {
            writeRecycler(response);
            try {
                if (isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (adapter == null) {
                            adapter = new SearchAdapter(mContext, this, recipeItemArrayList, logged);
                        }

                        int randomRecipesInt = 9 - recipeItemArrayList.size();
                        if (recipeItemArrayList.size() <= 5) {
                            getRandomRecipes(randomRecipesInt);
                        } else {
                            mSearchRecyclerView.setAdapter(adapter);
                            shimmer.stopShimmer();
                            shimmer.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (final Exception e) {
                Log.i(TAG, "Exception in Thread");
            }
        }).start();
    }

    // async task for writing recycler using random recipes
    private void writeRecyclerRandomAsync(String response) {
        new Thread(() -> {
            writeRecyclerRandom(response);
            Log.d(TAG, "1");
            try {
                if (isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        Log.d(TAG, "2");
                        if (adapter == null) {
                            Log.d(TAG, "3");
                            adapter = new SearchAdapter(mContext, this, recipeItemArrayList, logged);
                        }

                        mSearchRecyclerView.setAdapter(adapter);
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);
                    });
                }
            } catch (final Exception e) {
                Log.i(TAG, "Exception in Thread");
            }
        }).start();
    }

    // Writing items inside the recyclerview - initial api calls --------------------------------------------------------------------------------

    private void writeRecycler(String response) {
        try {
            JSONObject hits;

            JSONObject obj = new JSONObject(response);
            JSONArray dataArray = obj.getJSONArray("results");

            for (int i = 0; i < dataArray.length(); i++) {

                RecipeItem item = new RecipeItem(RecipeItem.TYPE_ITEM);
                hits = dataArray.getJSONObject(i);

                // Image
                item.setmImageUrl(hits.getString("image"));
                // Name
                item.setmRecipeName(hits.getString("title"));
                // Source
                item.setmSourceName(hits.getString("sourceName"));
                // URL
                if (hits.getString("sourceUrl").equals("null") || hits.getString("sourceUrl").isEmpty()) {
                    item.setmRecipeURL("https://www.thescavengerapp.com/recipe-not-found");
                } else {
                    item.setmRecipeURL(hits.getString("sourceUrl"));
                }
                // Unique ID
                item.setItemId(hits.getInt("id"));

                // checks if item in contained in db liked items to set as liked
                if (itemIds.contains(item.getItemId())) {
                    item.setLiked(true);
                }

                // remove items with null source
                if (!item.getmSourceName().equalsIgnoreCase("null")) {
                    recipeItemArrayList.add(item);
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Writing items inside the recyclerview - random api calls --------------------------------------------------------------------------------

    private void writeRecyclerRandom(String response) {
        try {
            JSONObject hits;

            JSONObject obj = new JSONObject(response);
            JSONArray dataArray = obj.getJSONArray("recipes");

            for (int i = 0; i < dataArray.length(); i++) {

                RecipeItem item = new RecipeItem(RecipeItem.TYPE_ITEM);
                hits = dataArray.getJSONObject(i);

                // Image
                item.setmImageUrl(hits.getString("image"));
                //Log.d(TAG, "image: " + hits.get("image"));
                // Name
                item.setmRecipeName(hits.getString("title"));
                // Source
                item.setmSourceName(hits.getString("sourceName"));
                // URL
                if (hits.getString("sourceUrl").equals("null") || hits.getString("sourceUrl").isEmpty()) {
                    item.setmRecipeURL("https://www.thescavengerapp.com/recipe-not-found");
                } else {
                    item.setmRecipeURL(hits.getString("sourceUrl"));
                }
                // Unique ID
                item.setItemId(hits.getInt("id"));

                // checks if item in contained in db liked items to set as liked
                if (itemIds.contains(item.getItemId())) {
                    item.setLiked(true);
                }

                // remove items with null source
                if (!item.getmSourceName().equalsIgnoreCase("null")) {
                    recipeItemArrayList.add(item);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // initial api call to spoonacular checking for a recipe query ------------------------------------------------------------------------------------------------
    private void callToApiRecipes() {

        searchingRecipes = true;
        searchingIngredients = false;
        // not loading randoms with initial search
        loadingRandoms = false;

        Retrofit retrofit = NetworkClient.getRetrofitClient();
        ApiService apiService = retrofit.create(ApiService.class);

        fromIngr = 0;
        toIngr = 10;

        Call<String> call = apiService.getRecipeData(Constants.apiKey, getIngredientsSearch(),true, false, fromIngr ,toIngr, true);

        queryString = mSearchView.getQuery().toString();
        if (call != null) {
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    Log.d(TAG, "response: " + response.code());
                    if (response.isSuccessful()) {
                        Log.d(TAG, "success: ");
                        if (response.body() != null) {
                            String result = response.body();
                            writeRecyclerRecipeAsync(result);
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
    }

    // initial api call to spoonacular checking for an ingredients query ------------------------------------------------------------------------------------------------
    private void callToApiIngr() {

        searchingRecipes = false;
        searchingIngredients = true;
        // not loading randoms with initial search
        loadingRandoms = false;

        Retrofit retrofit = NetworkClient.getRetrofitClient();
        ApiService apiService = retrofit.create(ApiService.class);

        fromIngr = 0;
        toIngr = 10;

        Call<String> call = apiService.getRecipeDataIngr(Constants.apiKey, getIngredientsSearch(), ingredientsComma(), true, false, fromIngr ,toIngr, true);

        if (call != null) {
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    Log.d(TAG, "response: " + response.code());
                    if (response.isSuccessful()) {
                        Log.d(TAG, "success: ");
                        if (response.body() != null) {
                            String result = response.body();

                            writeRecyclerIngrAsync(result);
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
    }

    private void getMoreIngrAsync() {
        new Thread(this::getMoreRecipesIngr).start();
    }

    private void getMoreRecipesIngr() {

        searchingRecipes = true;
        searchingIngredients = false;
        // not loading randoms when getting more recipes
        loadingRandoms = false;

        fromIngr = toIngr + 1;
        toIngr = fromIngr + 10;
        Retrofit retrofit = NetworkClient.getRetrofitClient();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<String> call = apiService.getRecipeDataIngr(Constants.apiKey, getIngredientsSearch(), ingredientsComma(), true, false, fromIngr, toIngr, true);
        if (call != null) {
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
    }

    // Load more Recipes - Recipe -----------------------------------------------------------------------------------------------------------------------
    private void getMoreAsync() {
        new Thread(this::getMoreRecipes).start();
    }

    private void getMoreRecipes() {

        searchingRecipes = true;
        searchingIngredients = false;
        // not loading randoms when getting more recipes
        loadingRandoms = false;

        fromIngr = toIngr + 1;
        toIngr = fromIngr + 10;
        Retrofit retrofit = NetworkClient.getRetrofitClient();

        ApiService apiService = retrofit.create(ApiService.class);

        // 7 / 1.1.1
        // updated 2/15/2021 w/ 'diet' and 'intolerances' for diets and intolerances
        Call<String> call = apiService.getRecipeData(Constants.apiKey, getIngredientsSearch(), true, false, fromIngr, toIngr, true);
        if (call != null) {
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
    }

    // Get random recipes -----------------------------------------------------------------------------------------------
    private void getRandomRecipes(int randomNumberInt) {

        Log.d(TAG, "GET RANDOM RECIPES");

        searchingRecipes = false;
        searchingIngredients = false;
        loadingRandoms = true;

        // add new item to arraylist to let recyclerview adapter know to create text header in recycler
        RecipeItem headerItem = new RecipeItem(RecipeItem.TYPE_HEADER);
        recipeItemArrayList.add(headerItem);

        Retrofit retrofit = NetworkClient.getRetrofitClient();
        ApiService apiService = retrofit.create(ApiService.class);

        Call<String> call = apiService.getRandomRecipeData(Constants.apiKey, randomNumberInt);
        if (call != null) {
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    //Log.d(TAG, "random recipes response: " + response.code());
                    if (response.isSuccessful()) {
                        Log.d(TAG, "random recipes success: ");
                        if (response.body() != null) {
                            String result = response.body();

                            writeRecyclerRandomAsync(result);

                        } else {
                            recipeItemArrayList.clear();
                            adapter.notifyDataSetChanged();
                            Log.i("onEmptyResponse", "Returned Empty Response");
                            Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                            shimmer.stopShimmer();
                            shimmer.setVisibility(View.GONE);
                            changeBGImage(0);
                            toastMessage("Something went wrong. Please try again");
                        }
                    } else {
                        recipeItemArrayList.clear();
                        adapter.notifyDataSetChanged();
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
    }

    private void getMoreRandomAsync() {
        new Thread(this::loadMoreRandomRecipes).start();
    }

    // Get more random recipes
    // reach out to Spoonacular API to get 10 more random recipes to add into recipeItemArrayList
    private void loadMoreRandomRecipes() {

        searchingRecipes = false;
        searchingIngredients = false;
        loadingRandoms = true;

        Retrofit retrofit = NetworkClient.getRetrofitClient();
        ApiService apiService = retrofit.create(ApiService.class);

        Call<String> call = apiService.getRandomRecipeData(Constants.apiKey, 10);

        if (call != null) {
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "random recipes success: ");
                        if (response.body() != null) {
                            String result = response.body();

                            writeRecyclerRandom(result);

                            adapter.notifyDataSetChanged();
                            shimmer.stopShimmer();
                            shimmer.setVisibility(View.GONE);
                        } else {
                            recipeItemArrayList.clear();
                            adapter.notifyDataSetChanged();
                            Log.i("onEmptyResponse", "Returned Empty Response");
                            Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                            shimmer.stopShimmer();
                            shimmer.setVisibility(View.GONE);
                            changeBGImage(0);
                            toastMessage("Something went wrong. Please try again");
                        }
                    } else {
                        recipeItemArrayList.clear();
                        adapter.notifyDataSetChanged();
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
        return mSearchView.getQuery().toString().replace("\u00F1", "n").replace("\u00E3", "a").replace("\u1EBD", "e");
    }

    private String ingredientsComma() {
        return mSearchView.getQuery().toString().replace(" ", ",").replace("\t", ",");
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