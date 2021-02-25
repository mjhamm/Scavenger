 package com.app.scavenger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

import static android.app.Activity.RESULT_OK;
import static com.app.scavenger.MainActivity.RECIPEITEMSCREENCALL;

public class SearchFragment extends Fragment {

    private static final String TAG = "SEARCH_FRAGMENT: ";
//    public static final int SEARCH_UPDATED = 104;

    private RecyclerView mSearchRecyclerView;
    private ArrayList<Integer> itemIds;
    private SearchAdapter adapter;
    private Context mContext;
    private SearchView mSearchView;
    private ImageView mSearch_mainBG;
    private TextView startup_message;
    private ShimmerFrameLayout shimmer;
    private int fromIngr = 0;
    private int toIngr = 10;
    private LinearLayoutManager mLayoutManager;
    private ArrayList<RecipeItem> recipeItemArrayList;
    private String queryString = null;
    private ConnectionDetector con;
    private EndlessRecyclerViewScrollListener scrollListener;
    private boolean logged = false;
    private boolean loadingRandoms = false;
    private boolean searchingRecipes = false;
    private boolean searchingIngredients = false;
    private DatabaseHelper myDb;

    // Diets and Intolerances
    // Added 2/15/2021
    // Version - 7 / 1.1.1
    private Chip mRatingChip, mDietsChip, mIntolerancesChip;
    private ArrayList<String> mDietsList;
    private ArrayList<String> mIntolsList;
    private int mRatingInt = -1;
    private int previousRatingInt = -1;
    private boolean rating = false;


    interface ApiService {
        // getting random recipes
        // 7 / 1.1.1
        // updated 2/15/2021 w/ 'tags' for diets and intolerances
        @GET("random?")
        Call<String> getRandomRecipeData(@Query("apiKey") String apiKey, @Query("number") int toIngr, @Query("tags") String dietIntolInfo);
        // getting recipes based on recipe search
        // 7 / 1.1.1
        // updated 2/15/2021 w/ 'diet' and 'intolerances' for diets and intolerances
        @GET("complexSearch?")
        Call<String> getRecipeData(@Query("apiKey") String apiKey, @Query("query") String ingredients, @Query("addRecipeInformation") boolean addInfo, @Query("instructionsRequired") boolean instrRequired, @Query("offset") int fromIngr, @Query("number") int toIngr, @Query("diets") String diet, @Query("intolerances") String intolerances);
        // getting recipes based on ingredients
        // 7 / 1.1.1
        // updated 2/15/2021 w/ 'diet' and 'intolerances' for diets and intolerances
        @GET("complexSearch?")
        Call<String> getRecipeDataIngr(@Query("apiKey") String apiKey, @Query("query") String ingredients, @Query("includeIngredients") String includeIngr, @Query("addRecipeInformation") boolean addInfo, @Query("instructionsRequired") boolean instrRequired, @Query("offset") int fromIngr, @Query("number") int toIngr, @Query("diets") String diet, @Query("intolerances") String intolerances);
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
        Log.d(TAG, "onCreate");
        mContext = getContext();
        myDb = DatabaseHelper.getInstance(mContext);
        recipeItemArrayList = new ArrayList<>();
        itemIds = new ArrayList<>();
        mDietsList = new ArrayList<>();
        mIntolsList = new ArrayList<>();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        logged = currentUser != null;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden) {
            if (!con.connectedToInternet() && recipeItemArrayList.isEmpty()) {
                changeBGImage(2);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("query", queryString);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");
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
        ProgressBar mProgressBar = view.findViewById(R.id.main_progressBar);
        mSearch_mainBG = view.findViewById(R.id.search_mainBG);
        mSearchRecyclerView = view.findViewById(R.id.search_recyclerView);
        mRatingChip = view.findViewById(R.id.chip_rating);
        mDietsChip = view.findViewById(R.id.chip_diets);
        mIntolerancesChip = view.findViewById(R.id.chip_intol);

        mLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);
        mSearchRecyclerView.setLayoutManager(mLayoutManager);

        con = new ConnectionDetector(mContext);

        if (savedInstanceState != null) {
            queryString = savedInstanceState.getString("query");
            mSearchView.setQuery(queryString, false);
        }

        // sets BG Image to default
        if (!con.connectedToInternet()) {
            changeBGImage(2);
        } else {
            changeBGImage(0);
        }

        mSearchView.setMaxWidth(Integer.MAX_VALUE);

        scrollListener = new EndlessRecyclerViewScrollListener(mLayoutManager, mProgressBar, con) {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recipeItemArrayList.isEmpty() && isLastVisible() && !con.connectedToInternet()) {
                    toastMessage("Failed to load more recipes. Please check your Internet connection.");
                }
            }

            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (con.connectedToInternet()) {
                    if (recipeItemArrayList.size() >= 9 && searchingRecipes && !searchingIngredients && !loadingRandoms) {
                        getMoreAsync();
                    } else if (recipeItemArrayList.size() >= 9 && !searchingRecipes && searchingIngredients && !loadingRandoms) {
                        getMoreIngrAsync();
                    } else if (recipeItemArrayList.size() >= 9 && !searchingRecipes && !searchingIngredients && loadingRandoms) {
                        getMoreRandomAsync();
                    } else {
                        searchingRecipes = false;
                        searchingIngredients = false;
                        loadingRandoms = false;
                    }
                }
            }
        };

        mSearchRecyclerView.addOnScrollListener(scrollListener);

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

                if (!con.connectedToInternet()) {
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(Constants.noInternetTitle)
                            .setMessage(Constants.noInternetMessage)
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                } else {
                    getIngredients();
                    callToApiRecipes();
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        // updated 2/15/2021 for diets and intolerances
        // 7 / 1.1.1

        // listener for rating chip
        mRatingChip.setOnClickListener(v -> {
            BottomSheetDialog mBottomDialogRating = new BottomSheetDialog(mContext);

            if (getActivity() != null) {
                View sheetView = getActivity().getLayoutInflater().inflate(R.layout.bottom_sheet_ratings_dialog, container, false);
                MaterialButton retrieveButton = sheetView.findViewById(R.id.retrieve_recipes_rating);
                Button resetButton = sheetView.findViewById(R.id.reset_button);
                Chip chipHL = sheetView.findViewById(R.id.chip_high_low);
                Chip chipLH = sheetView.findViewById(R.id.chip_low_high);
                ChipGroup ratings_chipGroup = sheetView.findViewById(R.id.rating_chip_group);

                for (int i = 0; i < ratings_chipGroup.getChildCount(); i++) {
                    Chip chip = (Chip) ratings_chipGroup.getChildAt(i);
                    chip.setId(i);

                    if (mRatingInt == chip.getId()) {
                        chip.setSelected(true);
                        chip.setChecked(true);
                        resetButton.setVisibility(View.VISIBLE);
                        resetButton.setEnabled(true);
                    }
                }

                chipHL.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        chipHL.setSelected(!chipHL.isSelected());
                        if (chipHL.isSelected()) {
                            mRatingInt = 0;
                        }
                        if (chipLH.isSelected()) {
                            chipLH.setSelected(false);
                        }
                        hideShowReset(chipHL, resetButton);
                    }
                });

                chipLH.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        chipLH.setSelected(!chipLH.isSelected());
                        if (chipLH.isSelected()) {
                            mRatingInt = 1;
                        }
                        if (chipHL.isSelected()) {
                            chipHL.setSelected(false);
                        }
                        hideShowReset(chipLH, resetButton);
                    }
                });

                resetButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ratings_chipGroup.clearCheck();
                        for (int k = 0; k < ratings_chipGroup.getChildCount(); k++) {
                            Chip chip = (Chip) ratings_chipGroup.getChildAt(k);
                            chip.setSelected(false);
                        }
                        mRatingInt = -1;
                        resetButton.setVisibility(View.INVISIBLE);
                        resetButton.setEnabled(false);
                    }
                });

                // retrieve button for rating
                retrieveButton.setOnClickListener(retrieveClick -> {
                    cycleThroughChipGroup(ratings_chipGroup);
                    mBottomDialogRating.dismiss();
                });

                // dismiss listener for rating
                mBottomDialogRating.setOnDismissListener(dialog -> {
                    cycleThroughChipGroup(ratings_chipGroup);
                });


                mBottomDialogRating.setContentView(sheetView);
                mBottomDialogRating.show();
            }
        });

        // listener for diets chip
        mDietsChip.setOnClickListener(v -> {
            BottomSheetDialog mBottomDialogDiets = new BottomSheetDialog(mContext);

            if (getActivity() != null) {
                View sheetView = getActivity().getLayoutInflater().inflate(R.layout.bottom_sheet_diets_dialog, container, false);
                TextView group_textView = sheetView.findViewById(R.id.bottom_sheet_label);
                MaterialButton retrieveButton = sheetView.findViewById(R.id.retrieve_recipes_diets);
                ChipGroup diets_chipGroup = sheetView.findViewById(R.id.chip_group_diets_search);

                group_textView.setText("Diets");

                for (String dietString : Constants.DIETS_LIST) {
                    Chip chip = (Chip) getLayoutInflater().inflate(R.layout.custom_filter_chip, diets_chipGroup, false);
                    chip.setText(dietString);
                    chip.setTextSize(14);
                    chip.setCheckable(true);
                    chip.setCheckedIcon(ContextCompat.getDrawable(mContext, R.drawable.ic_baseline_check_24));

                    if (mDietsList.contains(chip.getText().toString())) {
                        chip.setChecked(true);
                    }

                    chip.setOnClickListener(clickChip1 -> {
                        if (chip.isChecked()) {
                            mDietsList.add(chip.getText().toString());
                        } else {
                            mDietsList.remove(chip.getText().toString());
                        }
                    });

                    diets_chipGroup.addView(chip);
                }

                retrieveButton.setOnClickListener(vDiets -> {

                    if (mDietsList.isEmpty()) {
                        updateChip(mDietsChip,
                                android.R.color.white,
                                android.R.color.darker_gray,
                                ContextCompat.getColor(mContext, android.R.color.black),
                                "Diets");
                    } else {
                        updateChip(mDietsChip,
                                R.color.colorPrimaryDark,
                                R.color.colorPrimaryDark,
                                ContextCompat.getColor(mContext, android.R.color.white),
                                String.format(Locale.getDefault(), "%s Selected", mDietsList.size()));
                    }

                    mBottomDialogDiets.dismiss();
                });

                mBottomDialogDiets.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (mDietsList.isEmpty()) {
                            updateChip(mDietsChip,
                                    android.R.color.white,
                                    android.R.color.darker_gray,
                                    ContextCompat.getColor(mContext, android.R.color.black),
                                    "Diets");
                        } else {
                            updateChip(mDietsChip,
                                    R.color.colorPrimaryDark,
                                    R.color.colorPrimaryDark,
                                    ContextCompat.getColor(mContext, android.R.color.white),
                                    String.format(Locale.getDefault(), "%s Selected", mDietsList.size()));
                        }
                    }
                });

                mBottomDialogDiets.setContentView(sheetView);
                mBottomDialogDiets.show();
            }

        });

        // listener for intolerances chip
        mIntolerancesChip.setOnClickListener(v -> {
            BottomSheetDialog mBottomDialogIntols = new BottomSheetDialog(mContext);

            if (getActivity() != null) {
                View intolsSheetView = getActivity().getLayoutInflater().inflate(R.layout.bottom_sheet_diets_dialog, container, false);
                TextView intols_textView = intolsSheetView.findViewById(R.id.bottom_sheet_label);
                MaterialButton intolsRetrieve = intolsSheetView.findViewById(R.id.retrieve_recipes_diets);
                ChipGroup intols_chipGroup = intolsSheetView.findViewById(R.id.chip_group_diets_search);

                intols_textView.setText("Intolerances");

                for(String intolsString : Constants.INTOLS_LIST) {
                    Chip chip = (Chip) getLayoutInflater().inflate(R.layout.custom_filter_chip, intols_chipGroup, false);
                    chip.setText(intolsString);
                    chip.setTextSize(14);
                    chip.setCheckable(true);
                    chip.setCheckedIcon(ContextCompat.getDrawable(mContext, R.drawable.ic_baseline_check_24));

                    if (mIntolsList.contains(chip.getText().toString())) {
                        chip.setChecked(true);
                    }

                    chip.setOnClickListener(v1 -> {
                        if (chip.isChecked()) {
                            mIntolsList.add(chip.getText().toString());
                        } else {
                            mIntolsList.remove(chip.getText().toString());
                        }
                    });

                    intols_chipGroup.addView(chip);

                    intolsRetrieve.setOnClickListener(vIntols -> {

                        if (mIntolsList.isEmpty()) {
                            updateChip(mIntolerancesChip,
                                    android.R.color.white,
                                    android.R.color.darker_gray,
                                    ContextCompat.getColor(mContext, android.R.color.black),
                                    "Intolerances");
                        } else {
                            updateChip(mIntolerancesChip,
                                    R.color.colorPrimaryDark,
                                    R.color.colorPrimaryDark,
                                    ContextCompat.getColor(mContext, android.R.color.white),
                                    String.format(Locale.getDefault(), "%s Selected", mIntolsList.size()));
                        }

                        mBottomDialogIntols.dismiss();
                    });
                }

                mBottomDialogIntols.setOnDismissListener(dialog -> {
                    if (mIntolsList.isEmpty()) {
                        updateChip(mIntolerancesChip,
                                android.R.color.white,
                                android.R.color.darker_gray,
                                ContextCompat.getColor(mContext, android.R.color.black),
                                "Intolerances");
                    } else {
                        updateChip(mIntolerancesChip,
                                R.color.colorPrimaryDark,
                                R.color.colorPrimaryDark,
                                ContextCompat.getColor(mContext, android.R.color.white),
                                String.format(Locale.getDefault(), "%s Selected", mIntolsList.size()));
                    }
                });

                mBottomDialogIntols.setContentView(intolsSheetView);
                mBottomDialogIntols.show();
            }
        });

        return view;
    }

    private void hideShowReset(Chip chip, Button resetButton) {
        if (chip.isSelected()) {
            resetButton.setVisibility(View.VISIBLE);
            resetButton.setEnabled(true);
        } else {
            resetButton.setVisibility(View.INVISIBLE);
            resetButton.setEnabled(false);
        }
    }

    private void cycleThroughChipGroup(ChipGroup chipGroup) {

        boolean ratingBool = false;

        for (int j = 0; j < chipGroup.getChildCount(); j++) {
            Chip chip = (Chip) chipGroup.getChildAt(j);

            if (chip.isSelected()) {
                ratingBool = true;
            }
        }

        if (!ratingBool) {
            mRatingInt = -1;
            mRatingChip.setText("Rating");
            mRatingChip.setChipBackgroundColorResource(android.R.color.white);
            mRatingChip.setChipStrokeColorResource(android.R.color.darker_gray);
            mRatingChip.setTextColor(ContextCompat.getColor(mContext, android.R.color.black));
        }

        if (mRatingInt == 0) {
            mRatingChip.setText("Highest to Lowest");
            mRatingChip.setChipBackgroundColorResource(R.color.colorPrimaryDark);
            mRatingChip.setChipStrokeColorResource(R.color.colorPrimaryDark);
            mRatingChip.setTextColor(ContextCompat.getColor(mContext, android.R.color.white));
        } else if (mRatingInt == 1) {
            mRatingChip.setText("Lowest to Highest");
            mRatingChip.setChipBackgroundColorResource(R.color.colorPrimaryDark);
            mRatingChip.setChipStrokeColorResource(R.color.colorPrimaryDark);
            mRatingChip.setTextColor(ContextCompat.getColor(mContext, android.R.color.white));
        }

        if (previousRatingInt != mRatingInt) {
            // SEARCH
            Log.d(TAG, "Previous Int = " + previousRatingInt + " mRatingInt = " + mRatingInt + " : SEARCH");
            previousRatingInt = mRatingInt;
        }
    }

    private void updateChip(Chip chip, int backgroundColor, int strokeColor, int textColor, String text) {
        chip.setChipBackgroundColorResource(backgroundColor);
        chip.setChipStrokeColorResource(strokeColor);
        chip.setTextColor(textColor);
        chip.setText(text);
    }

    private String convertListToString(ArrayList<String> stringArrayList) {
        return TextUtils.join(",", stringArrayList);
    }

    private String combineLists(ArrayList<String> dietsList, ArrayList<String> intolsList) {
        return TextUtils.join(",", dietsList) + TextUtils.join(",", intolsList);
    }

    private void changeBGImage(int image) {

        mSearch_mainBG.setVisibility(View.VISIBLE);
        switch (image) {
            // default
            case 0:
                //matchMessage.setVisibility(View.GONE);
                startup_message.setVisibility(View.VISIBLE);
                setMessageToRandom();
                mSearch_mainBG.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.default_bg_screen));
                break;
                // no recipes
            case 1:
                //matchMessage.setVisibility(View.VISIBLE);
                startup_message.setVisibility(View.VISIBLE);
                startup_message.setText(R.string.no_recipes_found);
                mSearch_mainBG.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.no_recipes_bg_screen));
                break;
                // no internet
            case 2:
                //matchMessage.setVisibility(View.GONE);
                startup_message.setVisibility(View.VISIBLE);
                startup_message.setText(R.string.no_internet_connection);
                mSearch_mainBG.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.no_internet_bg_screen));
                break;
        }
    }

    public void updateSearchFrag() {
        Log.d(TAG, "updateSearchFrag");
        Log.d(TAG, "recipeItemSize: " + recipeItemArrayList.size());
        if (recipeItemArrayList != null && !recipeItemArrayList.isEmpty()) {
            Log.d(TAG, "recipeItemArrayList not empty");
            Cursor removedItems = myDb.getRemovedItems();
            Log.d(TAG, "num removed items" + removedItems.getCount());
            while (removedItems.moveToNext()) {
                for (RecipeItem item : recipeItemArrayList) {
                    if (item != null) {
                        // testing
                        if (item.getItemId() == removedItems.getInt(1)) {
                            Log.d(TAG, "Item ID: " + item.getItemId());
                            item.setLiked(false);
                            myDb.removeRemovedItem(item.getItemId());
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

    public void checkSearchForLikeChange(int itemId, boolean liked) {
        Log.d(TAG, "checkSearchForLikeChange: " + recipeItemArrayList.size());
        if (adapter != null) {
            adapter.updateItemByItemId(itemId, liked);
            //adapter.updateItem(position, liked);
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
            // set BG Image to default
            changeBGImage(0);
            // sets startup message to random message
            setMessageToRandom();
    }

    private void getIngredients() {
        if (!con.connectedToInternet()) {
            changeBGImage(2);
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
            mSearch_mainBG.setVisibility(View.GONE);
            shimmer.setVisibility(View.VISIBLE);
            shimmer.startShimmer();
        }
    }

    // Async tasks for writing the recyclerview ---------------------------------------------------------------------------------------------------------------------

    // async task for writing recycler using recipe query
    private void writeRecyclerRecipeAsync(String response) {
        new Thread(() -> {
            writeRecycler(response);
            try {
                if (isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (adapter == null) {
                            adapter = new SearchAdapter(mContext, this, recipeItemArrayList, logged);
                        }

                        if (recipeItemArrayList.size() <= 5) {
                            callToApiIngr();
                        } else {
                            mSearchRecyclerView.setAdapter(adapter);
                            shimmer.stopShimmer();
                            shimmer.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (final Exception e) {
                Log.i(TAG, "Exception in Thread");
            }
        }).start();
    }

    // async task for writing recycler using ingredients query
    private void writeRecyclerIngrAsync(String response) {
        new Thread(() -> {
            writeRecycler(response);
            try {
                if (isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (adapter == null) {
                            adapter = new SearchAdapter(mContext, this, recipeItemArrayList, logged);
                        }

                        int randomRecipesInt = 9 - recipeItemArrayList.size();
                        if (recipeItemArrayList.size() <= 5) {
                            getRandomRecipes(randomRecipesInt);
                        } else {
                            mSearchRecyclerView.setAdapter(adapter);
                            shimmer.stopShimmer();
                            shimmer.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (final Exception e) {
                Log.i(TAG, "Exception in Thread");
            }
        }).start();
    }

    // async task for writing recycler using random recipes
    private void writeRecyclerRandomAsync(String response) {
        new Thread(() -> {
            writeRecyclerRandom(response);
            try {
                if (isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (adapter == null) {
                            adapter = new SearchAdapter(mContext, this, recipeItemArrayList, logged);
                        }

                        mSearchRecyclerView.setAdapter(adapter);
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);
                    });
                }
            } catch (final Exception e) {
                Log.i(TAG, "Exception in Thread");
            }
        }).start();
    }

    // Writing items inside the recyclerview - initial api calls --------------------------------------------------------------------------------

    private void writeRecycler(String response) {
        try {
            JSONObject hits;

            JSONObject obj = new JSONObject(response);
            JSONArray dataArray = obj.getJSONArray("results");

            for (int i = 0; i < dataArray.length(); i++) {

                RecipeItem item = new RecipeItem(RecipeItem.TYPE_ITEM);
                hits = dataArray.getJSONObject(i);

                // Image
                item.setmImageUrl(hits.getString("image"));
                //Log.d(TAG, "image: " + hits.get("image"));
                // Name
                item.setmRecipeName(hits.getString("title"));
                // Source
                item.setmSourceName(hits.getString("sourceName"));
                // URL
                if (hits.getString("sourceUrl").equals("null") || hits.getString("sourceUrl").isEmpty()) {
                    item.setmRecipeURL("https://www.thescavengerapp.com/recipe-not-found");
                } else {
                    item.setmRecipeURL(hits.getString("sourceUrl"));
                    //Log.d(TAG, "sourceUrl: " + hits.get("sourceUrl"));
                }
                // Rating
                item.setItemRating(itemRating(hits.getDouble("spoonacularScore")));
                //Log.d(TAG, "rating: " + hits.getDouble("spoonacularScore"));
                //Log.d(TAG, "New Rating: " + itemRating(hits.getDouble("spoonacularScore")));
                // Unique ID
                item.setItemId(hits.getInt("id"));
                //Log.d(TAG, "id: " + hits.get("id"));

                // checks if item in contained in db liked items to set as liked
                if (itemIds.contains(item.getItemId())) {
                    item.setLiked(true);
                }

                //Log.d(TAG, "RecipeName: " + item.getmRecipeName());
                //Log.d(TAG, "sourceName: " + item.getmSourceName());

                // remove items with null source
                if (!item.getmSourceName().equalsIgnoreCase("null")) {
                    recipeItemArrayList.add(item);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Writing items inside the recyclerview - random api calls --------------------------------------------------------------------------------

    private void writeRecyclerRandom(String response) {
        try {
            JSONObject hits;

            JSONObject obj = new JSONObject(response);
            JSONArray dataArray = obj.getJSONArray("recipes");

            for (int i = 0; i < dataArray.length(); i++) {

                RecipeItem item = new RecipeItem(RecipeItem.TYPE_ITEM);
                hits = dataArray.getJSONObject(i);

                // Image
                item.setmImageUrl(hits.getString("image"));
                //Log.d(TAG, "image: " + hits.get("image"));
                // Name
                item.setmRecipeName(hits.getString("title"));
                // Source
                item.setmSourceName(hits.getString("sourceName"));
                // URL
                if (hits.getString("sourceUrl").equals("null") || hits.getString("sourceUrl").isEmpty()) {
                    item.setmRecipeURL("https://www.thescavengerapp.com/recipe-not-found");
                } else {
                    item.setmRecipeURL(hits.getString("sourceUrl"));
                    //Log.d(TAG, "sourceUrl: " + hits.get("sourceUrl"));
                }
                // Rating
                item.setItemRating(itemRating(hits.getDouble("spoonacularScore")));
                //Log.d(TAG, "rating: " + hits.getDouble("spoonacularScore"));
                //Log.d(TAG, "New Rating: " + itemRating(hits.getDouble("spoonacularScore")));
                // Unique ID
                item.setItemId(hits.getInt("id"));
                //Log.d(TAG, "id: " + hits.get("id"));

                // checks if item in contained in db liked items to set as liked
                if (itemIds.contains(item.getItemId())) {
                    item.setLiked(true);
                }

                //Log.d(TAG, "RecipeName: " + item.getmRecipeName());
                //Log.d(TAG, "sourceName: " + item.getmSourceName());

                // remove items with null source
                if (!item.getmSourceName().equalsIgnoreCase("null")) {
                    recipeItemArrayList.add(item);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // initial api call to spoonacular checking for a recipe query ------------------------------------------------------------------------------------------------
    private void callToApiRecipes() {

        searchingRecipes = true;
        searchingIngredients = false;
        // not loading randoms with initial search
        loadingRandoms = false;

        Retrofit retrofit = NetworkClient.getRetrofitClient();
        ApiService apiService = retrofit.create(ApiService.class);

        fromIngr = 0;
        toIngr = 10;

        // 7 / 1.1.1
        // updated 2/15/2021 w/ 'diet' and 'intolerances' for diets and intolerances
        Call<String> call = apiService.getRecipeData(Constants.apiKey, getIngredientsSearch(),true, false, fromIngr ,toIngr, convertListToString(mDietsList), convertListToString(mIntolsList));

        Log.d(TAG, getIngredientsSearch());
        queryString = mSearchView.getQuery().toString();
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                Log.d(TAG, "response: " + response.code());
                if (response.isSuccessful()) {
                    Log.d(TAG, "success: ");
                    if (response.body() != null) {
                        String result = response.body();

                        writeRecyclerRecipeAsync(result);
                    } else {
                        Log.i("onEmptyResponse", "Returned Empty Response");
                        Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);
                        changeBGImage(0);
                        toastMessage("Something went wrong. Please try again");
                    }
                } else {
                    Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                    changeBGImage(0);
                    toastMessage("Something went wrong. Please try again");
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                Log.d(TAG, "failure: " + "throwable: " + t.toString() + " call: " + call.toString());
            }
        });
    }

    // initial api call to spoonacular checking for an ingredients query ------------------------------------------------------------------------------------------------
    private void callToApiIngr() {

        searchingRecipes = false;
        searchingIngredients = true;
        // not loading randoms with initial search
        loadingRandoms = false;

        Retrofit retrofit = NetworkClient.getRetrofitClient();
        ApiService apiService = retrofit.create(ApiService.class);

        fromIngr = 0;
        toIngr = 10;

        // 7 / 1.1.1
        // updated 2/15/2021 w/ 'diet' and 'intolerances' for diets and intolerances
        Call<String> call = apiService.getRecipeDataIngr(Constants.apiKey, getIngredientsSearch(), ingredientsComma(), true, false, fromIngr ,toIngr, convertListToString(mDietsList), convertListToString(mIntolsList));

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                Log.d(TAG, "response: " + response.code());
                if (response.isSuccessful()) {
                    Log.d(TAG, "success: ");
                    if (response.body() != null) {
                        String result = response.body();

                        writeRecyclerIngrAsync(result);
                    } else {
                        Log.i("onEmptyResponse", "Returned Empty Response");
                        Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);
                        changeBGImage(0);
                        toastMessage("Something went wrong. Please try again");
                    }
                } else {
                    Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                    changeBGImage(0);
                    toastMessage("Something went wrong. Please try again");
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                Log.d(TAG, "failure: " + "throwable: " + t.toString() + " call: " + call.toString());
            }
        });
    }

    private void getMoreIngrAsync() {
        new Thread(this::getMoreRecipesIngr).start();
    }

    private void getMoreRecipesIngr() {

        searchingRecipes = true;
        searchingIngredients = false;
        // not loading randoms when getting more recipes
        loadingRandoms = false;

        fromIngr = toIngr + 1;
        toIngr = fromIngr + 10;
        Retrofit retrofit = NetworkClient.getRetrofitClient();

        ApiService apiService = retrofit.create(ApiService.class);

        // 7 / 1.1.1
        // updated 2/15/2021 w/ 'diet' and 'intolerances' for diets and intolerances
        Call<String> call = apiService.getRecipeDataIngr(Constants.apiKey, getIngredientsSearch(), ingredientsComma(), true, false, fromIngr, toIngr, convertListToString(mDietsList), convertListToString(mIntolsList));
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {

                    if (response.body() != null) {
                        String result = response.body();
                        writeRecycler(result);
                    } else {
                        Log.i("onEmptyResponse", "Returned Empty Response");
                        Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);
                        changeBGImage(0);
                        toastMessage("Something went wrong. Please try again");
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {}
        });
    }

    // Load more Recipes - Recipe -----------------------------------------------------------------------------------------------------------------------
    private void getMoreAsync() {
        new Thread(this::getMoreRecipes).start();
    }

    private void getMoreRecipes() {

        searchingRecipes = true;
        searchingIngredients = false;
        // not loading randoms when getting more recipes
        loadingRandoms = false;

        fromIngr = toIngr + 1;
        toIngr = fromIngr + 10;
        Retrofit retrofit = NetworkClient.getRetrofitClient();

        ApiService apiService = retrofit.create(ApiService.class);

        // 7 / 1.1.1
        // updated 2/15/2021 w/ 'diet' and 'intolerances' for diets and intolerances
        Call<String> call = apiService.getRecipeData(Constants.apiKey, getIngredientsSearch(), true, false, fromIngr, toIngr, convertListToString(mDietsList), convertListToString(mIntolsList));
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {

                    if (response.body() != null) {
                        String result = response.body();
                        writeRecycler(result);
                    } else {
                        Log.i("onEmptyResponse", "Returned Empty Response");
                        Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);
                        changeBGImage(0);
                        toastMessage("Something went wrong. Please try again");
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {}
        });
    }

    // Get random recipes -----------------------------------------------------------------------------------------------
    private void getRandomRecipes(int randomNumberInt) {

        searchingRecipes = false;
        searchingIngredients = false;
        loadingRandoms = true;

        // add new item to arraylist to let recyclerview adapter know to create text header in recycler
        RecipeItem headerItem = new RecipeItem(RecipeItem.TYPE_HEADER);
        recipeItemArrayList.add(headerItem);

        // reach out to Spoonacular API to get randomNumberInt random recipes to add into recipeItemArrayList

        Retrofit retrofit = NetworkClient.getRetrofitClient();
        ApiService apiService = retrofit.create(ApiService.class);

        // 7 / 1.1.1
        // updated 2/15/2021 w/ 'tags' for diets and intolerances
        Call<String> call = apiService.getRandomRecipeData(Constants.apiKey, randomNumberInt, combineLists(mDietsList, mIntolsList));

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                //Log.d(TAG, "random recipes response: " + response.code());
                if (response.isSuccessful()) {
                    Log.d(TAG, "random recipes success: ");
                    if (response.body() != null) {
                        String result = response.body();

                        writeRecyclerRandomAsync(result);

                    } else {
                        recipeItemArrayList.clear();
                        adapter.notifyDataSetChanged();
                        Log.i("onEmptyResponse", "Returned Empty Response");
                        Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);
                        changeBGImage(0);
                        toastMessage("Something went wrong. Please try again");
                    }
                } else {
                    recipeItemArrayList.clear();
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                    changeBGImage(0);
                    toastMessage("Something went wrong. Please try again");
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                Log.d(TAG, "failure: " + "throwable: " + t.toString() + " call: " + call.toString());
            }
        });
    }

    private void getMoreRandomAsync() {
        new Thread(this::loadMoreRandomRecipes).start();
    }

    // Get more random recipes
    // reach out to Spoonacular API to get 10 more random recipes to add into recipeItemArrayList
    private void loadMoreRandomRecipes() {

        searchingRecipes = false;
        searchingIngredients = false;
        loadingRandoms = true;

        Retrofit retrofit = NetworkClient.getRetrofitClient();
        ApiService apiService = retrofit.create(ApiService.class);

        // 7 / 1.1.1
        // updated 2/15/2021 w/ 'diet' and 'intolerances' for diets and intolerances
        Call<String> call = apiService.getRandomRecipeData(Constants.apiKey, 10, combineLists(mDietsList, mIntolsList));

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                //Log.d(TAG, "random recipes response: " + response.code());
                if (response.isSuccessful()) {
                    Log.d(TAG, "random recipes success: ");
                    if (response.body() != null) {
                        String result = response.body();

                        writeRecyclerRandom(result);

                        adapter.notifyDataSetChanged();
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);
                    } else {
                        recipeItemArrayList.clear();
                        adapter.notifyDataSetChanged();
                        Log.i("onEmptyResponse", "Returned Empty Response");
                        Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);
                        changeBGImage(0);
                        toastMessage("Something went wrong. Please try again");
                    }
                } else {
                    recipeItemArrayList.clear();
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "something went wrong. Call: " + response.errorBody());
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                    changeBGImage(0);
                    toastMessage("Something went wrong. Please try again");
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                Log.d(TAG, "failure: " + "throwable: " + t.toString() + " call: " + call.toString());
            }
        });
    }

    private float itemRating(double rating) {
        float newRating = (float) rating / 20;
        if (newRating <= 0) {
            return (float) 0.1;
        } else if (newRating > 5) {
            return 5;
        } else {
            return newRating;
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
            // testing
            itemIds.add(likesData.getInt(1));
        }
        likesData.close();
    }

    //Gets the input from Searchview and returns it as string
    private String getIngredientsSearch() {
        return mSearchView.getQuery().toString().replace("\u00F1", "n").replace("\u00E3", "a").replace("\u1EBD", "e");
    }

    private String ingredientsComma() {
        return mSearchView.getQuery().toString().replace(" ", ",").replace("\t", ",");
    }

    //method for creating a Toast
    private void toastMessage(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    private void updateAdapter(int position, boolean liked) {

        if (adapter != null) {
            adapter.updateItem(position, liked);
        }
    }

    // DEPRECATED
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == RECIPEITEMSCREENCALL && resultCode == RESULT_OK) {

            Log.d(TAG, "onActivityResult");
            int position = data.getIntExtra("position", 0);
            boolean liked = data.getBooleanExtra("liked", false);

            updateAdapter(position, liked);
        }
        // deprecated
        super.onActivityResult(requestCode, resultCode, data);
    }
}