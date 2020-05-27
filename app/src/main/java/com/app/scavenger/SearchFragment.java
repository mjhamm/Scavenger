package com.app.scavenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.facebook.shimmer.ShimmerFrameLayout;
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

public class SearchFragment extends Fragment {

    private static final String TAG = "SEARCH_FRAGMENT: ";
    public static final String ARG_ITEM_IDS = "itemIds";

    private RecyclerView mSearchRecyclerView;
    private ArrayList<String> itemIds;
    private SearchAdapter adapter;
    private Context mContext;
    private SearchView mSearchView;
    private TextView startup_message, matchMessage;
    private ShimmerFrameLayout shimmer;
    private int fromIngr = 0;
    private int toIngr = 10;
    private LinearLayoutManager mLayoutManager;
    private ArrayList<RecipeItem> recipeItemArrayList;
    private String queryString = null;
    private FirebaseAuth mAuth;
    private ConnectionDetector con;

    private DatabaseHelper myDb;
    private Cursor likesData;

    // Shared Preferences Data
    //-----------------------------------------
    private String userId = null;
    private boolean logged = false;
    //------------------------------------------

    public SearchFragment() {
        // Required empty public constructor
    }

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
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();

        mAuth = FirebaseAuth.getInstance();
        myDb = DatabaseHelper.getInstance(mContext);
    }

    interface ApiService {
        @GET("/search?")
        Call<String> getRecipeData(@Query("q") String ingredients, @Query("app_id") String appId, @Query("app_key") String appKey, @Query("ingr") int numIngredients, @Query("from") int fromIngr, @Query("to") int toIngr);
    }

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

        con = new ConnectionDetector(mContext);

        recipeItemArrayList = new ArrayList<>();
        itemIds = new ArrayList<>();

        mLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);

        Random random_start_number = new Random();

        //creates a random number and sets the welcome text to a specific text based on number
        int start_message_int = random_start_number.nextInt(5);

        switch (start_message_int) {
            case 0:
                startup_message.setText("Scavenge For Recipes!");
                break;
            case 1:
                startup_message.setText("Find Recipes You'll Love!");
                break;
            case 2:
                startup_message.setText("Add Up All The\nThyme You Can Save!");
                break;
            case 3:
                startup_message.setText("Break An Egg, Make An Omelet!");
                break;
            case 4:
                startup_message.setText("Check The Way Back Of The Cabinet!");
                break;
        }

        mSearchRecyclerView = view.findViewById(R.id.search_recyclerView);
        mSearchRecyclerView.setHasFixedSize(true);
        mSearchRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mSearchRecyclerView.setItemViewCacheSize(10);

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
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        getInfoFromSharedPrefs();
        if (adapter != null) {
            mSearchRecyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("query", queryString);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            queryString = savedInstanceState.getString("query");
            mSearchView.setQuery(queryString, false);
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
                numIngr = ingredientsArray.length;
            } else {
                ingredientsArray = mSearchView.getQuery().toString().split(" ");
                numIngr = ingredientsArray.length;
            }
        }
        return numIngr;
    }

    private void getIngredients() {
        if (!con.isConnectingToInternet()) {
            new MaterialAlertDialogBuilder(mContext)
                    .setTitle("No Internet connection found.")
                    .setMessage("You don't have an Internet connection. Please reconnect and try again.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
        } else {
            if (logged) {
                itemsFromDB();
            }
            startup_message.setVisibility(View.GONE);
            matchMessage.setVisibility(View.GONE);
            shimmer.setVisibility(View.VISIBLE);
            shimmer.startShimmer();
            Retrofit retrofit = NetworkClient.getRetrofitClient();

            ApiService apiService = retrofit.create(ApiService.class);


            Call<String> call = apiService.getRecipeData(getIngredientsSearch(), "bd790cc2", "56fdf6a5593ad5199e8040a29b9fbfd6", checkNumIngredients(), fromIngr, toIngr);
            queryString = getIngredientsSearch();
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
                    if (response.isSuccessful()) {
                        if (response.body() != null) {
                            String result = response.body();
                            writeRecycler(result);
                            //Log.i("onSuccess", result);
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
        String updateRecipeName = item.getmRecipeName();
        String updateRecipeSource = item.getmSourceName();
        if (item.getmRecipeName().contains("/")) {
            updateRecipeName = item.getmRecipeName().replace("/", "");
        }
        if (item.getmSourceName().contains("/")) {
            updateRecipeSource = item.getmSourceName().replace("/", "");
        }
        return updateRecipeName + updateRecipeSource + item.getmCalories();
    }



    private void writeRecycler(String response) {
        recipeItemArrayList.clear();
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

            adapter = new SearchAdapter(mContext, recipeItemArrayList, userId, logged);
            adapter.setHasStableIds(true);
            mSearchRecyclerView.setItemAnimator(new DefaultItemAnimator());
            mSearchRecyclerView.setAdapter(adapter);
            mSearchRecyclerView.setLayoutManager(mLayoutManager);

            if (recipeItemArrayList.isEmpty()) {
                startup_message.setVisibility(View.VISIBLE);
                startup_message.setText("We Couldn't Find Any Recipes :(\n" + "Sorry About That!");
                matchMessage.setVisibility(View.VISIBLE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
    }

    private void itemsFromDB() {
        likesData = myDb.getListContents();
        itemIds.clear();
        likesData.moveToPosition(-1);
        while (likesData.moveToNext()) {
            itemIds.add(likesData.getString(1));
        }
        likesData.close();
        Log.d(TAG, "Number of Likes: " + itemIds.size() + " Item Ids: " + itemIds.toString());
    }

    //Gets the input from Searchview and returns it as string
    private String getIngredientsSearch() {
        return mSearchView.getQuery().toString();
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
