package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.widget.Toast;

//import com.google.android.gms.ads.MobileAds;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SettingsFragment.RefreshFragments, LikesAdapter.UpdateSearch, LikesAdapter.CheckZeroLikes, SearchAdapter.UpdateQuery {

    private Fragment fragment1 = null;
    private Fragment fragment2 = null;
    private Fragment fragment3 = null;
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment active = null;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private boolean doubleBackToExitPressedOnce;
    private final Handler mHandler = new Handler();

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            doubleBackToExitPressedOnce = false;
        }
    };

    // Shared Preferences Data
    //-----------------------------------------
    private boolean match = false;
    private boolean refresh = false;
    //------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();
        getInfoFromSharedPrefs();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update to the status bar on lower SDK's
        // Makes bar on lower SDK's black with white icons
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            this.getWindow().setStatusBarColor(getResources().getColor(android.R.color.black));
        }

        setContentView(R.layout.activity_main);

        DatabaseHelper myDb = DatabaseHelper.getInstance(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPreferences.edit();

        myDb.removeAllItemsFromRemoveTable();

        BottomNavigationView mNavView = findViewById(R.id.bottom_nav_view);

        if (match) {
            toastMessage("Match ingredients is On");
        }

        fragment1 = SearchFragment.newInstance();
        fragment2 = LikesFragment.newInstance();
        fragment3 = new SettingsFragment();
        active = fragment1;

        fm.beginTransaction().add(R.id.fragment_container, fragment3, "3").hide(fragment3).commit();
        fm.beginTransaction().add(R.id.fragment_container, fragment2, "2").hide(fragment2).commit();
        fm.beginTransaction().add(R.id.fragment_container, fragment1, "1").commit();

        mNavView.setOnNavigationItemSelectedListener(item -> {
            switch(item.getItemId()) {
                case R.id.action_search:
                    getInfoFromSharedPrefs();
                    if (refresh) {
                        if (fragment1 != null) {
                            SearchFragment searchFragment = (SearchFragment) fragment1;
                            searchFragment.refreshFrag();
                        }
                        editor.putBoolean("refresh", false);
                        editor.apply();
                    }
                    fm.beginTransaction().hide(active).show(fragment1).commit();
                    if (match) {
                        toastMessage("Match Ingredients is On");
                    }
                    active = fragment1;
                    return true;
                case R.id.action_likes:
                    if (active != fragment2) {
                        fm.beginTransaction().hide(active).show(fragment2).commit();
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
        match = sharedPreferences.getBoolean("match", false);
        refresh = sharedPreferences.getBoolean("refresh", false);
    }

    @Override
    protected void onDestroy() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("actualNumLikes", 0);
        editor.putInt("numLikes", 0);
        editor.apply();

        if (mHandler != null) { mHandler.removeCallbacks(mRunnable); }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        toastMessage("Back one more time to exit");

        mHandler.postDelayed(mRunnable, 2000);
    }

    //method for creating a Toast
    private void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

    @Override
    public void checkZeroLikes() {
        //Update Likes Fragment Here
        if (fragment2 != null) {
            LikesFragment likeFrag = (LikesFragment) fragment2;
            likeFrag.hasZeroLikes();
        }
    }

    @Override
    public void updateQuery() {
        //Update Likes Fragment Here
        if (fragment2 != null) {
            LikesFragment likeFrag = (LikesFragment) fragment2;
            likeFrag.clearFilter();
        }
    }
}