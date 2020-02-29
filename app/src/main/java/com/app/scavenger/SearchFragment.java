package com.app.scavenger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;
import io.reactivex.Single;
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

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
    }

    public interface ApiService {
        @GET("/search?")
        Call<String> getRecipeData(@Query("q") String ingredients, @Query("app_id") String appId, @Query("app_key") String appKey, @Query("ingr") int numIngredients);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        startup_message = view.findViewById(R.id.startup_message);
        mSearchView = view.findViewById(R.id.search_searchView);
        mSearchView.setMaxWidth(Integer.MAX_VALUE);

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

        //recipeItems = new ArrayList<>();
        //recipeItems = RecipeItem.createContactsList(20);

        mSearchRecyclerView = view.findViewById(R.id.search_recyclerView);
        //mSearchRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mSearchRecyclerView.setHasFixedSize(true);

        //adapter = new SearchAdapter(mContext,recipeItems);
        //mSearchRecyclerView.setAdapter(adapter);

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

    private void getIngredients() {
        Retrofit retrofit = NetworkClient.getRetrofitClient();

        ApiService apiService = retrofit.create(ApiService.class);


        Call<String> call = apiService.getRecipeData(getIngredientsSearch(), "bd790cc2", "56fdf6a5593ad5199e8040a29b9fbfd6", 100);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        String result = response.body().toString();
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

                recipeItemArrayList.add(item);
            }
            startup_message.setVisibility(View.GONE);
            adapter = new SearchAdapter(mContext, recipeItemArrayList);
            mSearchRecyclerView.setAdapter(adapter);
            mSearchRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
