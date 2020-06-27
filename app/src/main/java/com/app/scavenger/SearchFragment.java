package com.app.scavenger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class SearchFragment extends Fragment /*implements SignInActivity.RefreshSearchFrag*/ {

    private static final String TAG = "SEARCH_FRAGMENT: ";
    public static final String ARG_ITEM_IDS = "itemIds";
    public static final int NUMBER_OF_ADS = 2;

    private RecyclerView mSearchRecyclerView;
    private ArrayList<String> itemIds;
    private SearchAdapter adapter;
    private Context mContext;
    private SearchView mSearchView;
    private ProgressBar mProgressBar;
    private TextView startup_message, matchMessage;
    private ShimmerFrameLayout shimmer;
    private int fromIngr = 0;
    private int toIngr = 10;
    private LinearLayoutManager mLayoutManager;
    private ArrayList<Object> recipeItemArrayList;
    private String queryString = null;
    private FirebaseAuth mAuth;
    private ConnectionDetector con;
    private int numItemsBefore = 0;
    private int numItemsAfter = 0;
    private boolean numItemsChanged = true;
    private int adIndex = 3;
    private EndlessRecyclerViewScrollListener scrollListener;

    private DatabaseHelper myDb;
    private Cursor likesData;

    private AdLoader adLoader;
    private List<UnifiedNativeAd> mNativeAds = new ArrayList<>();

    // Shared Preferences Data
    //-----------------------------------------
    private String userId = null;
    private boolean logged = false;
    //------------------------------------------

    interface ApiService {
        @GET("/search?")
        Call<String> getRecipeData(@Query("q") String ingredients, @Query("app_id") String appId, @Query("app_key") String appKey, @Query("ingr") int numIngredients, @Query("from") int fromIngr, @Query("to") int toIngr);
    }

    // Required empty public constructor
    public SearchFragment() {}

    // Create a new instance of Search Fragment
    static SearchFragment newInstance() {
        SearchFragment searchFragment = new SearchFragment();
        Bundle args = new Bundle();
        //args.putStringArrayList(ARG_ITEM_IDS, itemIds);
        searchFragment.setArguments(args);
        return searchFragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        logged = currentUser != null;
        myDb.removeAllItemsFromRemoveTable();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        mAuth = FirebaseAuth.getInstance();
        myDb = DatabaseHelper.getInstance(mContext);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;
        try {
            view = inflater.inflate(R.layout.fragment_search, container, false);
        } catch (Exception e) {
            Log.d(TAG, "onCreateView", e);
            throw  e;
        }

        startup_message = view.findViewById(R.id.startup_message);
        mSearchView = view.findViewById(R.id.search_searchView);
        mSearchView.setMaxWidth(Integer.MAX_VALUE);
        shimmer = view.findViewById(R.id.search_shimmerLayout);
        matchMessage = view.findViewById(R.id.match_message);
        mProgressBar = view.findViewById(R.id.main_progressBar);

        con = new ConnectionDetector(mContext);

        mProgressBar.setVisibility(View.GONE);

        recipeItemArrayList = new ArrayList<>();
        itemIds = new ArrayList<>();

        mLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);

        Random random_start_number = new Random();

        //creates a random number and sets the welcome text to a specific text based on number
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

        mSearchRecyclerView = view.findViewById(R.id.search_recyclerView);
        mSearchRecyclerView.setHasFixedSize(true);

        RecyclerView.ItemAnimator animator = mSearchRecyclerView.getItemAnimator();

        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        mSearchRecyclerView.setItemViewCacheSize(10);

        mSearchRecyclerView.setOnTouchListener((v, event) -> {
            mSearchView.clearFocus();
            return false;
        });

        scrollListener = new EndlessRecyclerViewScrollListener(mLayoutManager, mProgressBar, mContext, toIngr) {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (isLastVisible() && !con.connectedToInternet()) {
                    Toast.makeText(mContext, "Failed to load more recipes. Please check your Internet connection.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (con.connectedToInternet() && numItemsChanged) {
                    getMoreRecipes();
                }
            }
        };

        mSearchRecyclerView.addOnScrollListener(scrollListener);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchView.clearFocus();
                mSearchView.setImeOptions(6);
                getIngredients();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                return false;
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            queryString = savedInstanceState.getString("query");
            mSearchView.setQuery(queryString, false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("query", queryString);
    }

    private void insertAdsInRecipeItems() {
        if (mNativeAds.size() <= 0) {
            return;
        }

        Log.d(TAG, "mNativeAds.size(): " + mNativeAds.size());

        int offset = (recipeItemArrayList.size() / mNativeAds.size());
        Log.d(TAG, "RecipeArrayListSize(): " + recipeItemArrayList.size());
        Log.d(TAG, "OFFSET: " + offset);
        //int adIndex = 3;
        for (UnifiedNativeAd ad : mNativeAds) {
            recipeItemArrayList.add(adIndex, ad);
            adIndex = adIndex + offset;
        }
    }

    private void loadNativeAds() {

        AdLoader.Builder builder = new AdLoader.Builder(mContext, getString(R.string.ad_unit_id));
        adLoader = builder.forUnifiedNativeAd(
                unifiedNativeAd -> {
                    // A native ad loaded successfully, check if the ad loader has finished loading
                    // and if so, insert the ads into the list.
                    mNativeAds.add(unifiedNativeAd);
                    if (!adLoader.isLoading()) {
                        insertAdsInRecipeItems();
                    }
                }).withAdListener(
                new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        // A native ad failed to load, check if the ad loader has finished loading
                        // and if so, insert the ads into the list.
                        Log.e("MainActivity", "The previous native ad failed to load. Attempting to"
                                + " load another.");
                        if (!adLoader.isLoading()) {
                            insertAdsInRecipeItems();
                        }
                    }
                }).build();

        // Load the Native Express ad.
        adLoader.loadAds(new AdRequest.Builder().build(), NUMBER_OF_ADS);
    }

    public void updateSearchFrag() {
        if (!recipeItemArrayList.isEmpty()) {
            Cursor removedItems = myDb.getRemovedItems();
            while (removedItems.moveToNext()) {
                for (Object item : recipeItemArrayList) {
                    if (item instanceof RecipeItem) {
                        if (((RecipeItem) item).getItemId().equals(removedItems.getString(1))) {
                            ((RecipeItem)item).setFavorited(false);
                            myDb.removeRemovedItem( ((RecipeItem) item).getItemId());
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

    public void refreshFrag() {
        try {
            // Reload the fragment
            mSearchView.setQuery("", false);
            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            adapter = null;
            mSearchRecyclerView = null;
            ft.detach(this).attach(this).commit();
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.d(TAG, e.toString());
        }
    }

    private int checkNumIngredients() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
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
    }

    private void getIngredients() {
        if (!con.connectedToInternet()) {
            new MaterialAlertDialogBuilder(mContext)
                    .setTitle("No Internet connection found")
                    .setMessage("You don't have an Internet connection. Please reconnect and try again.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
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
            shimmer.setVisibility(View.VISIBLE);
            shimmer.startShimmer();
            Retrofit retrofit = NetworkClient.getRetrofitClient();

            ApiService apiService = retrofit.create(ApiService.class);

            fromIngr = 0;
            toIngr = 10;

            Call<String> call = apiService.getRecipeData(getIngredientsSearch(), "bd790cc2", "56fdf6a5593ad5199e8040a29b9fbfd6", checkNumIngredients(), fromIngr, toIngr);
            queryString = getIngredientsSearch();
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                    if (response.isSuccessful()) {
                        if (response.body() != null) {
                            String result = response.body();
                            recipeItemArrayList.clear();
                            writeRecycler(result);
                            Log.d(TAG, "1. recipeItemListSize: " + recipeItemArrayList.size());
                            adIndex = 3;
                            loadNativeAds();
                            adapter = new SearchAdapter(mContext, recipeItemArrayList, userId, logged);
                            mSearchRecyclerView.setAdapter(adapter);
                            mSearchRecyclerView.setLayoutManager(mLayoutManager);

                            if (recipeItemArrayList.isEmpty()) {
                                startup_message.setVisibility(View.VISIBLE);
                                startup_message.setText("We Couldn't Find Any Recipes :(\n" + "Sorry About That!");
                                matchMessage.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Log.i("onEmptyResponse", "Returned Empty Response");
                        }
                    }
                }

                @Override
                public void onFailure(@NotNull Call call, @NotNull Throwable t) {}
            });
        }
    }

    private String randomItemId(RecipeItem item) {
        return item.getmRecipeURL().replace("/", "");
    }

    private void writeRecycler(String response) {
        try {
            JSONObject obj = new JSONObject(response);
            JSONArray dataArray = obj.getJSONArray("hits");

            JSONObject hits;
            JSONObject recipes;
            JSONObject ing;
            JSONObject totalNutrients;
            JSONObject carbs;
            JSONObject fat;
            JSONObject protein;

            JSONArray dietLabelsArray;
            JSONArray healthLabelsArray;
            JSONArray ingredientsArray;

            ArrayList<String> list_healthLabels;
            ArrayList<String> list_ingredients;

            String labels;
            String total_ing;

            for (int i = 0; i < dataArray.length(); i++) {
                RecipeItem item = new RecipeItem();
                hits = dataArray.getJSONObject(i);
                recipes = hits.getJSONObject("recipe");
                item.setmImageUrl(recipes.getString("image"));
                item.setmRecipeName(recipes.getString("label"));
                item.setmSourceName(recipes.getString("source"));
                item.setmRecipeURL(recipes.getString("url"));
                item.setmServings(recipes.getInt("yield"));
                item.setmCalories(recipes.getInt("calories"));

                dietLabelsArray = recipes.getJSONArray("dietLabels");
                list_healthLabels = new ArrayList<>();
                for (int j = 0; j < dietLabelsArray.length(); j++) {
                    String diets = dietLabelsArray.getString(j);
                    list_healthLabels.add("\n\u2022 " + diets + "\n");
                }

                healthLabelsArray = recipes.getJSONArray("healthLabels");
                for (int k = 0; k < healthLabelsArray.length(); k++) {
                    if (healthLabelsArray.length() <= 3) {
                        labels = healthLabelsArray.getString(k);
                        list_healthLabels.add("\n\u2022 " + labels + "\n");
                    }
                }
                item.setmRecipeAttributes(list_healthLabels);

                //Ingredients
                ingredientsArray = recipes.getJSONArray("ingredients");
                list_ingredients = new ArrayList<>();
                for (int m = 0; m < ingredientsArray.length(); m++) {
                    ing = ingredientsArray.getJSONObject(m);
                    total_ing = ing.getString("text");
                    list_ingredients.add("\n\u2022 " + total_ing + "\n");
                    item.setmIngredients(list_ingredients);
                }

                totalNutrients = recipes.getJSONObject("totalNutrients");
                //Carbs
                carbs = totalNutrients.getJSONObject("CHOCDF");
                if (carbs.getInt("quantity") > 0 && carbs.getInt("quantity") < 1) {
                    item.setmCarbs(1);
                } else {
                    item.setmCarbs(carbs.getInt("quantity"));
                }
                //Fat
                fat = totalNutrients.getJSONObject("FAT");
                if (fat.getInt("quantity") > 0 && fat.getInt("quantity") < 1) {
                    item.setmFat(1);
                } else {
                    item.setmFat(fat.getInt("quantity"));
                }
                //Protein
                protein = totalNutrients.getJSONObject("PROCNT");
                if (protein.getInt("quantity") > 0 && protein.getInt("quantity") < 1) {
                    item.setmProtein(1);
                } else {
                    item.setmProtein(protein.getInt("quantity"));
                }

                item.setItemId(randomItemId(item));

                if (itemIds.contains(item.getItemId())) {
                    item.setFavorited(true);
                }

                recipeItemArrayList.add(item);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
    }

    boolean isLastVisible() {
        int pos = mLayoutManager.findLastCompletelyVisibleItemPosition();
        int numItems =  adapter.getItemCount();
        return (pos >= numItems - 1);
    }

    private void itemsFromDB() {
        likesData = myDb.getListContents();
        itemIds.clear();
        likesData.moveToPosition(-1);
        while (likesData.moveToNext()) {
            itemIds.add(likesData.getString(1));
        }
        likesData.close();
    }

    //Gets the input from Searchview and returns it as string
    private String getIngredientsSearch() {
        return mSearchView.getQuery().toString();
    }

    private void getMoreRecipes() {
        if (toIngr >= 50 || (toIngr + 10) > 50) {
            return;
        }
        numItemsBefore = recipeItemArrayList.size();
        fromIngr = toIngr;
        toIngr = fromIngr + 10;
        Log.d(TAG, "from: " + fromIngr + " to: " + toIngr);
        Retrofit retrofit = NetworkClient.getRetrofitClient();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<String> call = apiService.getRecipeData(getIngredientsSearch(), "bd790cc2", "56fdf6a5593ad5199e8040a29b9fbfd6", checkNumIngredients(), fromIngr, toIngr);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        String result = response.body();
                        writeRecycler(result);

                        numItemsAfter = recipeItemArrayList.size();
                        if (numItemsAfter == numItemsBefore) {
                            numItemsChanged = false;
                        }

                        Log.d(TAG, "2. recipeItemListSize: " + recipeItemArrayList.size());

                        loadNativeAds();

                        //adapter.notifyDataSetChanged();
                    } else {
                        Log.i("onEmptyResponse", "Returned Empty Response");
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull Throwable t) {}
        });
    }

    //method for creating a Toast
//    private void toastMessage(String message) {
//        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
//    }

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", null);
    }
}
