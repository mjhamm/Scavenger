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

import androidx.annotation.NonNull;
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

    private static final String TAG = "Likes Fragment: ";
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
    //-----------------------------------------------------------------------------

    // Shared Preferences Data
    //-----------------------------------------
    private String userId = null;
    private boolean logged = false;
    private int numLikes = 0;
    private int actualNumLikes = 0;
    //------------------------------------------

    // Items from Layout -------------------------
    private RecyclerView mLikesRecyclerView;
    private SearchView mLikeSearch;
    private TextView likes_message;
    private MaterialButton retryConButton;
    private ShimmerFrameLayout shimmer;
    private LinearLayoutManager mLayoutManager;
    //--------------------------------------------

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private ConnectionDetector con;
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

    // Required empty public constructor
    public LikesFragment() {}

    // Create a new instance of Likes Fragment
    // Add a bundle if you want to pass through variables on creation
    static LikesFragment newInstance() {
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

        // checks the Shared Preferences
        /*getInfoFromSharedPrefs();

        numLikes = 0;

        // if connected to the internet and logged in
        // retrieve the Likes of the User from Firebase
        if (con.connectedToInternet() && logged) {
            retrieveLikesFromFirebase();
        }*/
    }

    @Override
    public void onResume() {
        super.onResume();

        // check if the SearchView isn't empty
        // if not, empty the query so the filter doesn't get messed up
        if (!mLikeSearch.getQuery().toString().isEmpty()) {
            mLikeSearch.setQuery("", false);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // when the Like fragment becomes unhidden
        // check shared preferences and go through all other options to see if the fragment should retrieve the Likes of the user from Firebase
        if (!hidden) {
            getInfoFromSharedPrefs();
            checkingStatus();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_likes, container, false);

        likes_message = view.findViewById(R.id.like_message);
        mLikeSearch = view.findViewById(R.id.likes_searchView);
        retryConButton = view.findViewById(R.id.fav_retry_con_button);
        shimmer = view.findViewById(R.id.likes_shimmerLayout);
        mLikesRecyclerView = view.findViewById(R.id.likes_recyclerView);
        mLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);


        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        //editor = sharedPreferences.edit();
        con = new ConnectionDetector(mContext);
        adapter = new LikesAdapter(mContext, recipeItemList, userId);

        mLikeSearch.setMaxWidth(Integer.MAX_VALUE);

        mLikesRecyclerView.setHasFixedSize(true);
        mLikesRecyclerView.setItemViewCacheSize(10);
        mLikesRecyclerView.setLayoutManager(mLayoutManager);

        RecyclerView.ItemAnimator animator = mLikesRecyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        retryConButton.setOnClickListener(v -> retryConnection());

        mLikeSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (logged) {
                    if (adapter != null) {
                        adapter.getFilter().filter(newText);
                    }

                }
                return false;
            }
        });

        mLikesRecyclerView.setOnTouchListener((v, event) -> {
            mLikeSearch.clearFocus();
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
            likes_message.setText(R.string.no_likes);
        }
    }

    public void clearFilter() {
        if (mLikeSearch != null) {
            mLikeSearch.setQuery("", false);
            mLikeSearch.clearFocus();
        }

    }

    private void retryConnection() {
        checkingStatus();
    }

    private void checkingStatus() {
        if (!con.connectedToInternet()) {
            if (!logged) {
                recipeItemList.clear();
                if (adapter != null) {
                    adapter.clearList();
                }
                mLikesRecyclerView.setAdapter(null);
                likes_message.setVisibility(View.VISIBLE);
                likes_message.setText(R.string.not_signed_in);
            } else {
                if (recipeItemList.isEmpty()) {
                    likes_message.setText(R.string.likes_not_connected);
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
                mLikesRecyclerView.setAdapter(null);
                likes_message.setVisibility(View.VISIBLE);
                likes_message.setText(R.string.not_signed_in);
            } else {
                if (recipeItemList.isEmpty() || numLikes != actualNumLikes) {
                    retryConButton.setVisibility(View.GONE);
                    likes_message.setVisibility(View.GONE);

                    shimmer.setVisibility(View.VISIBLE);
                    shimmer.startShimmer();

                    retrieveLikesFromFirebase();

                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                }
            }
        }
    }

    private void retrieveLikesFromFirebase() {
        CollectionReference likesRef = db.collection(Constants.firebaseUser).document(userId).collection(Constants.firebaseLikes);
        likesRef.orderBy(Constants.firebaseTime, Query.Direction.DESCENDING).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        likes_message.setVisibility(View.VISIBLE);
                        likes_message.setText(R.string.no_likes);
                        recipeItemList.clear();
                        if (adapter != null) {
                            adapter.clearList();
                        }
                        mLikesRecyclerView.setAdapter(null);
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
                            mLikesRecyclerView.setVisibility(View.VISIBLE);
                            mLikesRecyclerView.setAdapter(adapter);
                    }
                    editor = sharedPreferences.edit();
                    editor.putInt("actualNumLikes", queryDocumentSnapshots.size());
                    editor.putInt("numLikes", queryDocumentSnapshots.size());
                    editor.apply(); // apply
                    Log.d(TAG, "Recipe List Size: " + recipeItemList.size());

                    numLikes = actualNumLikes;
                });
    }

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", "");
        numLikes = sharedPreferences.getInt("numLikes", 0);
        actualNumLikes = sharedPreferences.getInt("actualNumLikes", 0);
    }
}
