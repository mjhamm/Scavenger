package com.app.scavenger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class FavoritesFragment extends Fragment {

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

    private static final String USER_COLLECTION = "Users";
    private static final String USER_FAVORITES = "Favorites";
    //-----------------------------------------------------------------------------

    private RecyclerView mFavoriteRecyclerView;
    private ArrayList<RecipeItem> favoriteItems;
    private FavoriteAdapter adapter;
    private Context mContext;
    private SearchView mFavoriteSearch;
    private TextView favorite_message;
    private ArrayList<RecipeItem> recipeItemList = new ArrayList<>();
    private MaterialButton retryConButton;
    private String userId = null;
    private boolean logged = false;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public FavoritesFragment() {
        // Required empty public constructor
    }

    public static FavoritesFragment newInstance(String userId, boolean logged) {
        FavoritesFragment favoritesFragment = new FavoritesFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putBoolean("logged", logged);
        favoritesFragment.setArguments(args);
        return favoritesFragment;
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
    public void onResume() {
        super.onResume();
        if (!checkConnection()) {
            favorite_message.setVisibility(View.VISIBLE);
            favorite_message.setText("You Don't Have an Internet Connection. In order to see your favorites you must be connected to the internet");
        } else if(!logged) {
            favorite_message.setVisibility(View.VISIBLE);
            favorite_message.setText("You are not logged in. Sign In or Sign Up to save and view your favorites.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        favorite_message = view.findViewById(R.id.favorite_message);
        mFavoriteSearch = view.findViewById(R.id.favorites_searchView);
        retryConButton = view.findViewById(R.id.fav_retry_con_button);
        mFavoriteSearch.setMaxWidth(Integer.MAX_VALUE);

        mFavoriteRecyclerView = view.findViewById(R.id.favorites_recyclerView);
        Log.e("Log: ", "USERID: " + userId);

        if (!checkConnection()) {
            favorite_message.setVisibility(View.VISIBLE);
            favorite_message.setText("You Don't Have an Internet Connection. In order to see your favorites you must be connected to the internet");
            retryConButton.setVisibility(View.VISIBLE);

            retryConButton.setOnClickListener(v -> {
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.detach(this).attach(this).commit();
            });
        } else {
            if (logged) {
                getFavorites();
            } else {
                favorite_message.setText("You are not logged in. Sign In or Sign Up to save and view your favorites.");
            }

        /*itemRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            RecipeItem item = new RecipeItem();
                            String name = documentSnapshot.getString(ITEM_NAME);
                            String source = documentSnapshot.getString(ITEM_SOURCE);
                            String image = documentSnapshot.getString(ITEM_IMAGE);
                            String url = documentSnapshot.getString(ITEM_URL);
                            int serves = documentSnapshot.getLong(ITEM_YIELD).intValue();
                            int cals = documentSnapshot.getLong(ITEM_CAL).intValue();
                            int carb = documentSnapshot.getLong(ITEM_CARB).intValue();
                            int fat = documentSnapshot.getLong(ITEM_FAT).intValue();
                            int protein = documentSnapshot.getLong(ITEM_PROTEIN).intValue();
                            ArrayList<String> att = (ArrayList<String>) documentSnapshot.get(ITEM_ATT);
                            ArrayList<String> ingr = (ArrayList<String>) documentSnapshot.get(ITEM_INGR);

                            item.setmRecipeName(name);
                            item.setmSourceName(source);
                            item.setmImageUrl(image);
                            item.setmRecipeURL(url);
                            item.setmServings(serves);
                            item.setmCalories(cals);
                            item.setmCarbs(carb);
                            item.setmFat(fat);
                            item.setmProtein(protein);
                            item.setmRecipeAttributes(att);
                            item.setmIngredients(ingr);

                            recipeItemList.add(item);

                            adapter = new FavoriteAdapter(mContext, recipeItemList);
                            mFavoriteRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
                            mFavoriteRecyclerView.setAdapter(adapter);
                        } else {
                            Toast.makeText(mContext, "Document does not exist", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(mContext, "Error!", Toast.LENGTH_SHORT).show();
                        Log.e("LOG: ", e.toString());
                    }
                });*/
        }

        return view;
    }

    public void getFavorites() {
        CollectionReference favoritesRef = db.collection("Users").document("ngYS08UwsqU9zC8EirA8").collection("Favorites");
        favoritesRef.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            RecipeItem item = new RecipeItem();
                            String name = documentSnapshot.getString(ITEM_NAME);
                            String source = documentSnapshot.getString(ITEM_SOURCE);
                            String image = documentSnapshot.getString(ITEM_IMAGE);
                            String url = documentSnapshot.getString(ITEM_URL);
                            int serves = documentSnapshot.getLong(ITEM_YIELD).intValue();
                            int cals = documentSnapshot.getLong(ITEM_CAL).intValue();
                            int carb = documentSnapshot.getLong(ITEM_CARB).intValue();
                            int fat = documentSnapshot.getLong(ITEM_FAT).intValue();
                            int protein = documentSnapshot.getLong(ITEM_PROTEIN).intValue();
                            ArrayList<String> att = (ArrayList<String>) documentSnapshot.get(ITEM_ATT);
                            ArrayList<String> ingr = (ArrayList<String>) documentSnapshot.get(ITEM_INGR);

                            item.setmRecipeName(name);
                            item.setmSourceName(source);
                            item.setmImageUrl(image);
                            item.setmRecipeURL(url);
                            item.setmServings(serves);
                            item.setmCalories(cals);
                            item.setmCarbs(carb);
                            item.setmFat(fat);
                            item.setmProtein(protein);
                            item.setmRecipeAttributes(att);
                            item.setmIngredients(ingr);

                            Log.e("LOG: ", "ITEM NAME: " + item.getmRecipeName());

                            recipeItemList.add(item);
                        }

                        if (!recipeItemList.isEmpty()) {
                            favorite_message.setVisibility(View.GONE);
                        } else {
                            favorite_message.setText("You Don't Have Any Favorites :(");
                        }

                        adapter = new FavoriteAdapter(mContext, recipeItemList);
                        mFavoriteRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
                        mFavoriteRecyclerView.setAdapter(adapter);
                    }
                });
    }

    //boolean that returns true if you are connected to internet and false if not
    private boolean checkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}
