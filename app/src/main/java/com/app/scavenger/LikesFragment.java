package com.app.scavenger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class LikesFragment extends Fragment {

    private static final String TAG = "Favorites Fragment: ";
    private LikesAdapter adapter;
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
    private TextView likes_message;
    private MaterialButton retryConButton;
    private ShimmerFrameLayout shimmer;
    //--------------------------------------------

    //private FirebaseAuth mAuth;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private SharedPreferences sharedPreferences;
    private ConnectionDetector con;
    //private String queryString = null;
    private LinearLayoutManager mLayoutManager;
    private String itemId, name, source, image, url;
    private int serves = 0;
    private int cals = 0;
    private int carb = 1;
    private int fat = 1;
    private int protein = 1;
    private ArrayList<String> att = new ArrayList<>();
    private ArrayList<String> ingr = new ArrayList<>();

    // Liked Items
    private final ArrayList<RecipeItem> recipeItemList = new ArrayList<>();

    public LikesFragment() {
        // Required empty public constructor
    }

    // Create a new instance of Favorites Fragment
    static LikesFragment newInstance() {
        //LikesFragment likesFragment = new LikesFragment();
        //Bundle args = new Bundle();
        //likesFragment.setArguments(args);
        return new LikesFragment();
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

        if (con.connectedToInternet() && logged) {
            retrieveLikesFromFirebase();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // TODO: Check to see if this can be done once and only changed if info from sharedPrefs changes
        if (!hidden) {
            getInfoFromSharedPrefs();
            if (!con.connectedToInternet()) {
                if (!logged) {
                    recipeItemList.clear();
                    if (adapter != null) {
                        adapter.clearList();
                    }
                    mFavoriteRecyclerView.setAdapter(null);
                    likes_message.setVisibility(View.VISIBLE);
                    likes_message.setText(R.string.not_signed_in);
                } else {
                    if (recipeItemList.isEmpty()) {
                        likes_message.setText(R.string.favorites_not_connected);
                        likes_message.setVisibility(View.VISIBLE);
                        retryConButton.setVisibility(View.VISIBLE);
                    } else {
                        likes_message.setVisibility(View.GONE);
                        retryConButton.setVisibility(View.GONE);
                    }
                }
            } else {
                retryConButton.setVisibility(View.GONE);
                likes_message.setText("");
                if (!logged) {
                    recipeItemList.clear();
                    if (adapter != null) {
                        adapter.clearList();
                    }
                    mFavoriteRecyclerView.setAdapter(null);
                    likes_message.setVisibility(View.VISIBLE);
                    likes_message.setText(R.string.not_signed_in);
                } else {
                    if (recipeItemList.isEmpty() || numLikes != actualNumLikes) {
                        shimmer.setVisibility(View.VISIBLE);
                        shimmer.startShimmer();
                        retrieveLikesFromFirebase();
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_likes, container, false);

        likes_message = view.findViewById(R.id.favorite_message);
        mFavoriteSearch = view.findViewById(R.id.favorites_searchView);
        retryConButton = view.findViewById(R.id.fav_retry_con_button);
        shimmer = view.findViewById(R.id.likes_shimmerLayout);
        mFavoriteRecyclerView = view.findViewById(R.id.favorites_recyclerView);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        con = new ConnectionDetector(mContext);
        adapter = new LikesAdapter(mContext, recipeItemList, userId);
        mFavoriteSearch.setMaxWidth(Integer.MAX_VALUE);
        mLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);

        mFavoriteRecyclerView.setHasFixedSize(true);
        mFavoriteRecyclerView.setItemViewCacheSize(10);
        RecyclerView.ItemAnimator animator = mFavoriteRecyclerView.getItemAnimator();

        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        retryConButton.setOnClickListener(v -> retryConnection());

        mFavoriteSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (logged) {
                    adapter.getFilter().filter(newText);
                }
                return false;
            }
        });

        mFavoriteRecyclerView.setOnTouchListener((v, event) -> {
            mFavoriteSearch.clearFocus();
            return false;
        });

        return view;
    }

    public void hasZeroLikes() {
        if (logged) {
            if (adapter != null) {
                adapter = null;
            }
            likes_message.setVisibility(View.VISIBLE);
            likes_message.setText(R.string.no_favorites);
        }
    }

    public void clearFilter() {
        mFavoriteSearch.setQuery("", false);
        mFavoriteSearch.clearFocus();
    }

    private void retryConnection() {

        if (!con.connectedToInternet()) {
            if (!logged) {
                recipeItemList.clear();
                if (adapter != null) {
                    adapter.clearList();
                }
                mFavoriteRecyclerView.setAdapter(null);
                likes_message.setVisibility(View.VISIBLE);
                likes_message.setText(R.string.not_signed_in);
            } else {
                if (recipeItemList.isEmpty()) {
                    likes_message.setText(R.string.favorites_not_connected);
                    likes_message.setVisibility(View.VISIBLE);
                    retryConButton.setVisibility(View.VISIBLE);
                } else {
                    likes_message.setVisibility(View.GONE);
                    retryConButton.setVisibility(View.GONE);
                }
            }
        } else {
            retryConButton.setVisibility(View.GONE);
            likes_message.setText("");
            if (!logged) {
                recipeItemList.clear();
                if (adapter != null) {
                    adapter.clearList();
                }
                mFavoriteRecyclerView.setAdapter(null);
                likes_message.setVisibility(View.VISIBLE);
                likes_message.setText(R.string.not_signed_in);
            } else {
                if (recipeItemList.isEmpty() || numLikes != actualNumLikes) {
                    shimmer.setVisibility(View.VISIBLE);
                    shimmer.startShimmer();
                    retrieveLikesFromFirebase();
                }
            }
        }
    }

    private void retrieveLikesFromFirebase() {
        mFavoriteRecyclerView.setVisibility(View.GONE);
        likes_message.setVisibility(View.GONE);

        CollectionReference favoritesRef = db.collection(USER_COLLECTION).document(userId).collection(USER_FAVORITES);
        favoritesRef.orderBy("Timestamp", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        likes_message.setVisibility(View.VISIBLE);
                        likes_message.setText(R.string.no_favorites);
                        recipeItemList.clear();
                        if (adapter != null) {
                            adapter.clearList();
                        }
                        mFavoriteRecyclerView.setAdapter(null);
                    } else {
                        recipeItemList.clear();
                        if (adapter != null) {
                            adapter.clearList();
                        }
                        likes_message.setVisibility(View.GONE);
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
                        adapter = new LikesAdapter(mContext, recipeItemList, userId);
                        mFavoriteRecyclerView.setVisibility(View.VISIBLE);
                        mFavoriteRecyclerView.setAdapter(adapter);
                        mFavoriteRecyclerView.setLayoutManager(mLayoutManager);
                    }
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("actualNumLikes", queryDocumentSnapshots.size());
                    editor.putInt("numLikes", queryDocumentSnapshots.size());
                    editor.apply(); // apply
                    Log.d(TAG, "Recipe List Size: " + recipeItemList.size());
                });
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
        numLikes = actualNumLikes;
    }

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", "");
        numLikes = sharedPreferences.getInt("numLikes", 0);
        actualNumLikes = sharedPreferences.getInt("actualNumLikes", 0);
    }
}
