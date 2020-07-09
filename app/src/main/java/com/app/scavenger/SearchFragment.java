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
//import com.google.android.gms.ads.AdListener;
//import com.google.android.gms.ads.AdLoader;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.jetbrains.annotations.NotNull;
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

public class SearchFragment extends Fragment /*implements SignInActivity.RefreshSearchFrag*/ {

    private static final String TAG = "SEARCH_FRAGMENT: ";

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
    private SharedPreferences sharedPreferences;
    private EndlessRecyclerViewScrollListener scrollListener;
    private String noRecipesFound;

    private DatabaseHelper myDb;

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
        return new SearchFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        mAuth = FirebaseAuth.getInstance();
        myDb = DatabaseHelper.getInstance(mContext);
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        logged = currentUser != null;

        recipeItemArrayList = new ArrayList<>();
        itemIds = new ArrayList<>();
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
        shimmer = view.findViewById(R.id.search_shimmerLayout);
        matchMessage = view.findViewById(R.id.match_message);
        mProgressBar = view.findViewById(R.id.main_progressBar);
        mSearchRecyclerView = view.findViewById(R.id.search_recyclerView);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSearchView.setMaxWidth(Integer.MAX_VALUE);

        mLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);
        mSearchRecyclerView.setLayoutManager(mLayoutManager);

        con = new ConnectionDetector(mContext);

        scrollListener = new EndlessRecyclerViewScrollListener(mLayoutManager, mProgressBar, con) {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (isLastVisible() && !con.connectedToInternet()) {
                    toastMessage();
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

        noRecipesFound = "We Couldn't Find Any Recipes.\n" + "Sorry About That!";

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

                getIngredients();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        setMessageToRandom();

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

    public void updateSearchFrag() {
        if (!recipeItemArrayList.isEmpty()) {
            Cursor removedItems = myDb.getRemovedItems();
            while (removedItems.moveToNext()) {
                for (Object item : recipeItemArrayList) {
                    if (item instanceof RecipeItem) {
                        if (((RecipeItem) item).getItemId().equals(removedItems.getString(1))) {
                            ((RecipeItem)item).setLiked(false);
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
        // shows startup message
        startup_message.setVisibility(View.VISIBLE);
        // hides match message if being shown
        matchMessage.setVisibility(View.GONE);
        // sets startup message to random message
        setMessageToRandom();
    }

    private int checkNumIngredients() {

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
                    .setTitle(Constants.noInternetTitle)
                    .setMessage(Constants.noInternetMessage)
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

            Call<String> call = apiService.getRecipeData(getIngredientsSearch(), Constants.appId, Constants.appKey, checkNumIngredients(), fromIngr, toIngr);
            queryString = getIngredientsSearch();
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                    if (response.isSuccessful()) {
                        if (response.body() != null) {
                            String result = response.body();
                            recipeItemArrayList.clear();
                            numItemsChanged = true;
                            numItemsBefore = 0;
                            writeRecycler(result);
                            if (adapter == null) {
                                adapter = new SearchAdapter(mContext, recipeItemArrayList, userId, logged);
                            }
                            mSearchRecyclerView.setAdapter(adapter);
                            if (recipeItemArrayList.isEmpty()) {
                                startup_message.setVisibility(View.VISIBLE);
                                startup_message.setText(noRecipesFound);
                                matchMessage.setVisibility(View.VISIBLE);
                            }
                            numItemsBefore = recipeItemArrayList.size();
                            shimmer.stopShimmer();
                            shimmer.setVisibility(View.GONE);
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
            JSONObject hits, recipes, ing, totalNutrients, carbs, fat, protein;
            JSONArray dietLabelsArray, healthLabelsArray, ingredientsArray;
            ArrayList<String> list_healthLabels, list_ingredients;
            String labels, total_ing;

            JSONObject obj = new JSONObject(response);
            JSONArray dataArray = obj.getJSONArray("hits");

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
                    item.setLiked(true);
                }
                
                recipeItemArrayList.add(item);
            }

        } catch (JSONException e) {
            e.printStackTrace();
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
        fromIngr = toIngr;
        toIngr = fromIngr + 10;
        Log.d(TAG, "from: " + fromIngr + " to: " + toIngr);
        Retrofit retrofit = NetworkClient.getRetrofitClient();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<String> call = apiService.getRecipeData(getIngredientsSearch(), Constants.appId, Constants.appKey, checkNumIngredients(), fromIngr, toIngr);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        String result = response.body();
                        writeRecycler(result);

                        numItemsAfter = recipeItemArrayList.size();
                        numItemsChanged = numItemsAfter != numItemsBefore;
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
    private void toastMessage() {
        Toast.makeText(mContext, "Failed to load more recipes. Please check your Internet connection.", Toast.LENGTH_SHORT).show();
    }
}
