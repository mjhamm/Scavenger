package com.app.scavenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;

public class FavoritesFragment extends Fragment {

    private static final String TAG = "Favorites Fragment: ";
    private FavoriteAdapter adapter;
    private Context mContext;

    // Firestore Labels ----------------------------------------------------------
    private static final String ITEM_ID = "itemId";
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

    // Shared Preferences Data
    //-----------------------------------------
    private String userId = null;
    private boolean logged = false;
    private int numLikes = 0;
    private int actualNumLikes = 0;
    //------------------------------------------

    // Items from Layout -------------------------
    private RecyclerView mFavoriteRecyclerView;
    private SearchView mFavoriteSearch;
    private TextView favorite_message;
    private MaterialButton retryConButton;
    private MaterialCardView progressHolder;
    //--------------------------------------------

    private FirebaseAuth mAuth;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private SharedPreferences sharedPreferences;

    // Liked Items
    private ArrayList<RecipeItem> recipeItemList = new ArrayList<>();
    private ArrayList<RecipeItem> likedItems = new ArrayList<>();

    public FavoritesFragment() {
        // Required empty public constructor
    }

    // Create a new instance of Favorites Fragment
    static FavoritesFragment newInstance() {
        FavoritesFragment favoritesFragment = new FavoritesFragment();
        Bundle args = new Bundle();
        favoritesFragment.setArguments(args);
        return favoritesFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
    }

    @Override
    public void onStart() {
        super.onStart();
        getInfoFromSharedPrefs();
        if (logged) {
            retrieveLikesFromFirebase();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            getInfoFromSharedPrefs();
            /* Added */
            if (!logged) {
                recipeItemList.clear();
                if (adapter != null) {
                    adapter.clearList();
                }
                mFavoriteRecyclerView.setAdapter(adapter);
                favorite_message.setVisibility(View.VISIBLE);
                favorite_message.setText(R.string.not_signed_in);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        favorite_message = view.findViewById(R.id.favorite_message);
        mFavoriteSearch = view.findViewById(R.id.favorites_searchView);
        retryConButton = view.findViewById(R.id.fav_retry_con_button);
        mFavoriteSearch.setMaxWidth(Integer.MAX_VALUE);
        mFavoriteRecyclerView = view.findViewById(R.id.favorites_recyclerView);
        progressHolder = view.findViewById(R.id.likes_progressHolder);
        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        adapter = new FavoriteAdapter(mContext, recipeItemList, userId);

        mFavoriteRecyclerView.setHasFixedSize(true);
        mFavoriteRecyclerView.setItemViewCacheSize(10);
        mFavoriteRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mFavoriteRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));

        return view;
    }

//    private void retryConnectionInfo() {
//        try {
//            // Reload the fragment
//            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
//            ft.detach(this).attach(this).commit();
//        } catch (NullPointerException e) {
//            e.printStackTrace();
//            Log.d(TAG, e.toString());
//        }
//    }

    private void retrieveLikesFromFirebase() {
        CollectionReference favoritesRef = db.collection(USER_COLLECTION).document(userId).collection(USER_FAVORITES);
        favoritesRef.orderBy("Timestamp", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    String itemId;
                    String name;
                    String source;
                    String image;
                    String url;
                    int serves = 0;
                    int cals = 0;
                    int carb = 1;
                    int fat = 1;
                    int protein = 1;
                    ArrayList<String> att = new ArrayList<>();
                    ArrayList<String> ingr = new ArrayList<>();
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.isEmpty()) {
                            favorite_message.setVisibility(View.VISIBLE);
                            favorite_message.setText(R.string.no_favorites);
                        } else {
                            recipeItemList.clear();
                            if (adapter != null) {
                                adapter.clearList();
                            }
                            favorite_message.setVisibility(View.GONE);
                            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                RecipeItem item = new RecipeItem();
                                itemId = documentSnapshot.getString(ITEM_ID);
                                name = documentSnapshot.getString(ITEM_NAME);
                                source = documentSnapshot.getString(ITEM_SOURCE);
                                image = documentSnapshot.getString(ITEM_IMAGE);
                                url = documentSnapshot.getString(ITEM_URL);
                                if (documentSnapshot.getLong(ITEM_YIELD) != null) {
                                    serves = documentSnapshot.getLong(ITEM_YIELD).intValue();
                                }
                                if (documentSnapshot.getLong(ITEM_CAL) != null) {
                                    cals = documentSnapshot.getLong(ITEM_CAL).intValue();
                                }
                                if (documentSnapshot.getLong(ITEM_CARB) != null) {
                                    carb = documentSnapshot.getLong(ITEM_CARB).intValue();
                                }
                                if (documentSnapshot.getLong(ITEM_FAT) != null) {
                                    fat = documentSnapshot.getLong(ITEM_FAT).intValue();
                                }
                                if (documentSnapshot.getLong(ITEM_PROTEIN) != null) {
                                    protein = documentSnapshot.getLong(ITEM_PROTEIN).intValue();
                                }
                                if (documentSnapshot.exists()) {
                                    att = (ArrayList<String>) documentSnapshot.get(ITEM_ATT);
                                    ingr = (ArrayList<String>) documentSnapshot.get(ITEM_INGR);
                                }
                                item.setItemId(itemId);
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

                                if (!recipeItemList.contains(item)) {
                                    recipeItemList.add(item);
                                }
                            }
                        }
                        SharedPreferences.Editor editor = sharedPreferences.edit(); // added
                        editor.putInt("actualNumLikes", queryDocumentSnapshots.size()); // added
                        editor.putInt("numLikes", queryDocumentSnapshots.size());
                        editor.apply(); // apply
                        Log.d(TAG, "Recipe List Size: " + recipeItemList.size());
                    }
                });
        numLikes = actualNumLikes;
        adapter = new FavoriteAdapter(mContext, recipeItemList, userId);
        mFavoriteRecyclerView.setAdapter(adapter);
    }

    //boolean that returns true if you are connected to internet and false if not
//    private boolean checkConnection() {
//        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
//        return activeNetwork != null && activeNetwork.isConnected();
//    }

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", "");
        numLikes = sharedPreferences.getInt("numLikes", 0);
        actualNumLikes = sharedPreferences.getInt("actualNumLikes", 0);
    }
}
