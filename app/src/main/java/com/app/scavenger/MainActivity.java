package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SettingsFragment.RefreshFragments, FavoriteAdapter.UpdateSearch {

    //private static final String TAG = "LOG: ";

    private static final String ITEM_ID = "itemId";
    private static final String USER_COLLECTION = "Users";
    private static final String USER_FAVORITES = "Favorites";

    private Fragment fragment1 = null;
    private Fragment fragment2 = null;
    private Fragment fragment3 = null;
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment active = null;
    private FirebaseAuth mAuth;
    private DatabaseHelper myDB;
    private SharedPreferences sharedPreferences;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ArrayList<String> itemIds;

    private boolean doubleBackToExitPressedOnce;
    private Handler mHandler = new Handler();

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            doubleBackToExitPressedOnce = false;
        }
    };

    // Shared Preferences Data
    //-----------------------------------------
    private String userId = null;
    private boolean logged = false;
    private boolean match = false;
    private int numLikes = 0;
    private int actualNumLikes = 0;
    private boolean refresh = false;
    //------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();
        getInfoFromSharedPrefs();
        if (logged) {
            userId = mAuth.getUid();
            retrieveLikesFromFirebase();
            numLikes = actualNumLikes;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        BottomNavigationView mNavView = findViewById(R.id.bottom_nav_view);

        myDB = DatabaseHelper.getInstance(this);

        itemIds = new ArrayList<>();

        getInfoFromSharedPrefs();

        if (match) {
            Toast.makeText(this, "Match ingredients is On", Toast.LENGTH_SHORT).show();
        }

        fragment1 = SearchFragment.newInstance();
        fragment2 = FavoritesFragment.newInstance();
        fragment3 = new SettingsFragment();
        active = fragment1;

        fm.beginTransaction().add(R.id.fragment_container, fragment3, "3").hide(fragment3).commit();
        fm.beginTransaction().add(R.id.fragment_container, fragment2, "2").hide(fragment2).commit();
        fm.beginTransaction().add(R.id.fragment_container, fragment1, "1").commit();

        mNavView.setOnNavigationItemSelectedListener(item -> {
            getInfoFromSharedPrefs();
            switch(item.getItemId()) {
                case R.id.action_search:
                    if (refresh) {
                        if (fragment1 != null) {
                            SearchFragment searchFragment = (SearchFragment) fragment1;
                            searchFragment.refreshFrag();
                        }
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("refresh", false);
                        editor.apply();
                    }
                    fm.beginTransaction().hide(active).show(fragment1).commit();
                    if (match) {
                        Toast.makeText(this, "Match Ingredients is On", Toast.LENGTH_SHORT).show();
                    }
                    active = fragment1;
                    return true;
                case R.id.action_favorites:
                    if (active != fragment2) {
                        if (logged && (numLikes != actualNumLikes)) {
                            fm.beginTransaction().hide(active).detach(fragment2).attach(fragment2).show(fragment2).commit();
                        } else {
                            fm.beginTransaction().hide(active).show(fragment2).commit();
                        }
                    }
                    active = fragment2;
                    return true;
                case R.id.action_settings:
                    fm.beginTransaction().hide(active).show(fragment3).commit();
                    active = fragment3;
                    return true;
            }
            return false;
        });
    }

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        match = sharedPreferences.getBoolean("match", false);
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", null);
        numLikes = sharedPreferences.getInt("numLikes", 0);
        actualNumLikes = sharedPreferences.getInt("actualNumLikes", 0);
        refresh = sharedPreferences.getBoolean("refresh", false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("actualNumLikes", 0);
        editor.apply();

        if (mHandler != null) { mHandler.removeCallbacks(mRunnable); }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Back one more time to exit.", Toast.LENGTH_SHORT).show();

        mHandler.postDelayed(mRunnable, 2000);
    }

    private void retrieveLikesFromFirebase() {
        CollectionReference favoritesRef = db.collection(USER_COLLECTION).document(userId).collection(USER_FAVORITES);
        favoritesRef.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        actualNumLikes = queryDocumentSnapshots.size();
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("actualNumLikes", queryDocumentSnapshots.size());
                        editor.apply();
                        Log.d("Retrieve from Firebase", "actual Number of Likes: " + queryDocumentSnapshots.size());
                    }
                });
        numLikes = actualNumLikes;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void refreshSearch() {
        //Refresh Search Fragment Here
        if (fragment1 != null) {
            SearchFragment searchFragment = (SearchFragment) fragment1;
            searchFragment.refreshFrag();
        }
    }

    @Override
    public void updateSearch() {
        //Update Search Fragment Here
        if (fragment1 != null) {
            SearchFragment searchFragment = (SearchFragment) fragment1;
            searchFragment.updateSearchFrag();
        }
    }
}