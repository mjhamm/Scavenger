package com.app.scavenger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class LikesFragment extends Fragment {

    //private static final String TAG = "Likes Fragment: ";

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
    private ImageView mLikes_BG;
    //--------------------------------------------

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private LikesAdapter adapter;
    private Context mContext;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private ConnectionDetector con;
    private String itemId, name, source, image, url;
    private int serves = 0;
    private int cals = 0;
    private int carb = 1;
    private int fat = 1;
    private int protein = 1;
    private boolean firstLoad = false;
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
        mLikes_BG = view.findViewById(R.id.likes_mainBG);
        mLikesRecyclerView = view.findViewById(R.id.likes_recyclerView);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);


        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        con = new ConnectionDetector(mContext);
        adapter = new LikesAdapter(mContext, recipeItemList, userId);

        // sets the width of the SearchView to be the width of the screen
        mLikeSearch.setMaxWidth(Integer.MAX_VALUE);

        mLikesRecyclerView.setHasFixedSize(true);
        mLikesRecyclerView.setItemViewCacheSize(10);
        mLikesRecyclerView.setLayoutManager(mLayoutManager);

        RecyclerView.ItemAnimator animator = mLikesRecyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        // when there is no internet connection the button allows user to attempt to reconnect and see their likes
        retryConButton.setOnClickListener(v -> retryConnection());

        // SearchView that takes the input and filters the data inside of the recipe item list
        // returns items that have the input data in it
        mLikeSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // only runs when logged in and adapter isn't null
                if (logged) {
                    if (adapter != null) {
                        // filter the text through the adapter and return items
                        adapter.getFilter().filter(newText);
                    }

                }
                return false;
            }
        });

        // clears the focus of the SearchView when the recyclerview is touched
        mLikesRecyclerView.setOnTouchListener((v, event) -> {
            mLikeSearch.clearFocus();
            return false;
        });

        return view;
    }

    // checks if the recipe item list is empty
    // if empty - set the adapter to null
    // show the likes message and let the user know they have no likes
    public void hasZeroLikes() {
        if (logged) {
            if (adapter != null) {
                adapter = null;
            }
            changeBGImage(1);
        }
    }

    private void changeBGImage(int image) {

        mLikes_BG.setVisibility(View.VISIBLE);
        switch (image) {
            // default
            case 0:
                likes_message.setVisibility(View.VISIBLE);
                likes_message.setText(R.string.not_signed_in);
                retryConButton.setVisibility(View.GONE);
                mLikes_BG.setImageDrawable(getResources().getDrawable(R.drawable.not_signedin_bg_screen));
                break;
            // no likes
            case 1:
                likes_message.setVisibility(View.VISIBLE);
                likes_message.setText(R.string.no_likes);
                retryConButton.setVisibility(View.GONE);
                mLikes_BG.setImageDrawable(getResources().getDrawable(R.drawable.no_likes_bg_screen));
                break;
            // no internet
            case 2:
                likes_message.setVisibility(View.VISIBLE);
                likes_message.setText(R.string.likes_not_connected);
                retryConButton.setVisibility(View.VISIBLE);
                mLikes_BG.setImageDrawable(getResources().getDrawable(R.drawable.no_internet_bg_screen));
                break;
        }
    }

    // clears the text inside the SearchView and clears it's focus
    public void clearFilter() {
        if (mLikeSearch != null) {
            mLikeSearch.setQuery("", false);
            mLikeSearch.clearFocus();
        }

    }

    // method that refreshes the fragment when the user is reconnected to the internet
    private void retryConnection() {
        checkingStatus();
    }

    // checks if the user is connected to the internet
    private void checkingStatus() {
        // if not connected to the internet
        if (!con.connectedToInternet()) {
            // if user isn't logged in -
            // clear the list
            // clear the adapter
            // nullify the recyclerview adapter
            // let user know they aren't signed in
            if (!logged) {
                // stop shimmerview
                shimmer.stopShimmer();
                shimmer.setVisibility(View.GONE);
                recipeItemList.clear();
                if (adapter != null) {
                    adapter.clearList();
                }
                mLikesRecyclerView.setAdapter(null);
                changeBGImage(0);
                // if user is logged in -
                // check if the list is empty
                // if true -
                // let the user know they aren't connected to the internet and their likes will be loaded when they reconnect
            } else {
                if (recipeItemList.isEmpty()) {
                    changeBGImage(2);
                    // if false -
                    // show the list in the recyclerview
                    // they won't be able to remove likes when they aren't connected to the internet
                    // should be allowed to use offline if list isn't empty
                } else {
                    likes_message.setVisibility(View.GONE);
                    retryConButton.setVisibility(View.GONE);
                    // stop shimmerview
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                }
            }
            // if connected to the internet
        } else {
            // always hide retry connection button
            retryConButton.setVisibility(View.GONE);
            // clear text of likes message
            // CHECK THIS
            //likes_message.setText("");
            // if not logged in -
            // clear list
            // clear adapter
            // nullify recyclerview adapter
            // let user know they are not signed in
            if (!logged) {
                recipeItemList.clear();
                if (adapter != null) {
                    adapter.clearList();
                }
                mLikesRecyclerView.setAdapter(null);
                changeBGImage(0);
                // if logged in -
                // if the list is empty and the numLikes from search is != actualNumLikes the user has from Firebase
                // start shimmerview
                // hide likes message
                // retrieve users likes from Firebase
            } else {
                if (!firstLoad) {
                    shimmer.setVisibility(View.VISIBLE);
                    shimmer.startShimmer();
                }
                if (recipeItemList.isEmpty()) {
                    retrieveLikesFromFirebase();
                } else if (numLikes != actualNumLikes) {
                    shimmer.setVisibility(View.VISIBLE);
                    shimmer.startShimmer();

                    retrieveLikesFromFirebase();
                }
            }
        }
    }

    // Retrieves the user's likes from Firebase using their userId
    private void retrieveLikesFromFirebase() {

        firstLoad = true;

        // reference to the users likes
        CollectionReference likesRef = db.collection(Constants.firebaseUser).document(userId).collection(Constants.firebaseLikes);
        // orders those likes by timestamp in descending order to show the most recent like on top
        likesRef.orderBy(Constants.firebaseTime, Query.Direction.DESCENDING).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // if the number of likes the user has is 0
                    // display likes message and let user know they have 0 likes
                    // clear list and adapter
                    // this is so no possible overlap of another user can come through
                    if (queryDocumentSnapshots.isEmpty()) {
                        changeBGImage(1);
                        recipeItemList.clear();
                        if (adapter != null) {
                            adapter.clearList();
                        }
                        mLikesRecyclerView.setAdapter(null);
                        // if the number of likes the user has is not 0
                        // clear the list and adapter
                        // hide likes message
                    } else {
                        recipeItemList.clear();
                        if (adapter != null) {
                            adapter.clearList();
                        }
                        likes_message.setVisibility(View.GONE);
                        mLikes_BG.setVisibility(View.GONE);

                        // go through each item in the snapshot from Firebase and set a new recipe item with the information
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            // creates a new recipe item for each item in the snapshot
                            RecipeItem item = new RecipeItem();
                            itemId = documentSnapshot.getString(ITEM_ID);
                            name = documentSnapshot.getString(ITEM_NAME);
                            source = documentSnapshot.getString(ITEM_SOURCE);
                            image = documentSnapshot.getString(ITEM_IMAGE);
                            url = documentSnapshot.getString(ITEM_URL);
                            if (documentSnapshot.getLong(ITEM_YIELD) != null) {
                                //noinspection ConstantConditions
                                serves = documentSnapshot.getLong(ITEM_YIELD).intValue();
                            }
                            if (documentSnapshot.getLong(ITEM_CAL) != null) {
                                //noinspection ConstantConditions
                                cals = documentSnapshot.getLong(ITEM_CAL).intValue();
                            }
                            if (documentSnapshot.getLong(ITEM_CARB) != null) {
                                //noinspection ConstantConditions
                                carb = documentSnapshot.getLong(ITEM_CARB).intValue();
                            }
                            if (documentSnapshot.getLong(ITEM_FAT) != null) {
                                //noinspection ConstantConditions
                                fat = documentSnapshot.getLong(ITEM_FAT).intValue();
                            }
                            if (documentSnapshot.getLong(ITEM_PROTEIN) != null) {
                                //noinspection ConstantConditions
                                protein = documentSnapshot.getLong(ITEM_PROTEIN).intValue();
                            }
                            if (documentSnapshot.exists()) {
                                //noinspection unchecked
                                att = (ArrayList<String>) documentSnapshot.get(ITEM_ATT);
                                //noinspection unchecked
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

                            // in order to make sure there is no doubles of items in the user's list
                            // if the list already contains the exact item, it won't add it
                            if (!recipeItemList.contains(item)) {
                                recipeItemList.add(item);
                            }
                        }
                        // create the adapter with the new list
                        adapter = new LikesAdapter(mContext, recipeItemList, userId);
                        // set adapter
                        mLikesRecyclerView.setAdapter(adapter);
                    }

                    // edit sharedPreferences
                    // add actualNumLikes and numLikes as the number of items received from the snapshot
                    editor = sharedPreferences.edit();
                    editor.putInt("actualNumLikes", queryDocumentSnapshots.size());
                    editor.putInt("numLikes", queryDocumentSnapshots.size());
                    editor.apply(); // apply

                    // set numLikes = actualNumLikes
                    // this is so the code to run retrieve doesn't constantly run
                    numLikes = actualNumLikes;

                    // stop shimmerview
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
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
