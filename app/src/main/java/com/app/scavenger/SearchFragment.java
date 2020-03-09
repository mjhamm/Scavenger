package com.app.scavenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.shimmer.ShimmerFrameLayout;
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

    private RecyclerView mSearchRecyclerView;
    private ArrayList<RecipeItem> recipeItems;
    private SearchAdapter adapter;
    private Context mContext;
    private SearchView mSearchView;
    private TextView startup_message;
    private TextView match_textView;
    private SharedPreferences sharedPreferences;
    private ShimmerFrameLayout shimmer;
    private String userId = null;
    private boolean logged = false;
    private int fromIngr = 0;
    private int toIngr = 10;

    public SearchFragment() {
        // Required empty public constructor
    }

    public static SearchFragment newInstance(String userId, boolean logged) {
        SearchFragment searchFragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putBoolean("logged", logged);
        searchFragment.setArguments(args);
        return searchFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
            logged = getArguments().getBoolean("logged");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    interface ApiService {
        @GET("/search?")
        Call<String> getRecipeData(@Query("q") String ingredients, @Query("app_id") String appId, @Query("app_key") String appKey, @Query("ingr") int numIngredients, @Query("from") int fromIngr, @Query("to") int toIngr);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        startup_message = view.findViewById(R.id.startup_message);
        mSearchView = view.findViewById(R.id.search_searchView);
        match_textView = view.findViewById(R.id.matchIngr_textView);
        mSearchView.setMaxWidth(Integer.MAX_VALUE);
        shimmer = view.findViewById(R.id.search_shimmerLayout);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        if (sharedPreferences.getBoolean("match",false)) {
            match_textView.setVisibility(View.GONE);
        } else {
            match_textView.setVisibility(View.VISIBLE);
        }

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

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (checkConnection()) {
                    mSearchView.clearFocus();
                    mSearchView.setImeOptions(6);
                    getIngredients();
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

    @Override
    public void onResume() {
        super.onResume();
        if (sharedPreferences.getBoolean("match",true)) {
            match_textView.setVisibility(View.VISIBLE);
        } else {
            match_textView.setVisibility(View.GONE);
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
        startup_message.setVisibility(View.GONE);
        shimmer.setVisibility(View.VISIBLE);
        shimmer.startShimmer();
        Retrofit retrofit = NetworkClient.getRetrofitClient();

        ApiService apiService = retrofit.create(ApiService.class);


        Call<String> call = apiService.getRecipeData(getIngredientsSearch(), "bd790cc2", "56fdf6a5593ad5199e8040a29b9fbfd6", checkNumIngredients(), fromIngr, toIngr);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
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
            public void onFailure(Call call, Throwable t) {

            }
        });
    }

    private void writeRecycler(String response) {
        try {
            JSONObject obj = new JSONObject(response);
            ArrayList<RecipeItem> recipeItemArrayList = new ArrayList<>();
            JSONArray dataArray = obj.getJSONArray("hits");

            for (int i = 0; i < dataArray.length(); i++) {

                RecipeItem item = new RecipeItem();
                JSONObject hits = dataArray.getJSONObject(i);
                JSONObject recipes = hits.getJSONObject("recipe");
                item.setmImageUrl(recipes.getString("image"));
                item.setmRecipeName(recipes.getString("label"));
                item.setmSourceName(recipes.getString("source"));
                item.setmRecipeURL(recipes.getString("url"));
                item.setmServings(recipes.getInt("yield"));
                item.setmCalories(recipes.getInt("calories"));
                item.setmUniqueURI(recipes.getString("uri"));

                ArrayList<String> list_healthLabels = new ArrayList<>();

                JSONArray dietLabelsArray = recipes.getJSONArray("dietLabels");
                for (int j = 0; j < dietLabelsArray.length(); j++) {
                    String diets = dietLabelsArray.getString(j);
                    list_healthLabels.add("\n\u2022 " + diets + "\n");
                }

                JSONArray healthLabelsArray = recipes.getJSONArray("healthLabels");
                String labels;
                for (int k = 0; k < healthLabelsArray.length(); k++) {
                    if (healthLabelsArray.length() <= 3) {
                        labels = healthLabelsArray.getString(k);
                        list_healthLabels.add("\n\u2022 " + labels + "\n");
                    }
                }
                item.setmRecipeAttributes(list_healthLabels);

                //Ingredients
                JSONArray ingredientsArray = recipes.getJSONArray("ingredients");
                ArrayList<String> list_ingredients = new ArrayList<>();
                for (int m = 0; m < ingredientsArray.length(); m++) {
                    JSONObject ing = ingredientsArray.getJSONObject(m);
                    String total_ing = ing.getString("text");
                    list_ingredients.add("\n\u2022 " + total_ing + "\n");
                    item.setmIngredients(list_ingredients);
                }

                JSONObject totalNutrients = recipes.getJSONObject("totalNutrients");
                //Carbs
                JSONObject carbs = totalNutrients.getJSONObject("CHOCDF");
                item.setmCarbs(carbs.getInt("quantity"));
                //Fat
                JSONObject fat = totalNutrients.getJSONObject("FAT");
                item.setmFat(fat.getInt("quantity"));
                //Protein
                JSONObject protein = totalNutrients.getJSONObject("PROCNT");
                item.setmProtein(protein.getInt("quantity"));

                recipeItemArrayList.add(item);
            }
            adapter = new SearchAdapter(mContext, recipeItemArrayList, userId, logged);
            mSearchRecyclerView.setAdapter(adapter);
            mSearchRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));

            if (recipeItemArrayList.isEmpty()) {
                startup_message.setVisibility(View.VISIBLE);
                startup_message.setText("We Couldn't Find Any Recipes :(\n" +
                        "Sorry About That!");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    //Gets the input from Searchview and returns it as string
    private String getIngredientsSearch() {
        return mSearchView.getQuery().toString();
    }

    //boolean that returns true if you are connected to internet and false if not
    private boolean checkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    //method for creating a Toast
    private void toastMessage(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }
}
