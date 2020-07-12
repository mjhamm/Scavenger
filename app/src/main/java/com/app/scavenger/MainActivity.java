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

    // Fragment variables
    private Fragment fragment1 = null;
    private Fragment fragment2 = null;
    private Fragment fragment3 = null;
    private Fragment active = null;
    private final FragmentManager fm = getSupportFragmentManager();

    // boolean for exit application
    private boolean doubleBackToExitPressedOnce;

    // Handler and runnable to check if the user has double hit back to exit the application
    private final Handler mHandler = new Handler();
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            doubleBackToExitPressedOnce = false;
        }
    };

    // Shared Preferences Data
    //-----------------------------------------
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private boolean match = false;
    private boolean refresh = false;
    //------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();
        // Get information from shared preferences
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

        // create instance of Database
        DatabaseHelper myDb = DatabaseHelper.getInstance(this);
        // create instance of shared preferences and editor
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPreferences.edit();

        // remove all the items in the Database inside of the Removed Items table
        // this makes it so new recipes that are and aren't liked are not confused
        myDb.removeAllItemsFromRemoveTable();

        BottomNavigationView mNavView = findViewById(R.id.bottom_nav_view);

        // create new instances of each fragment
        fragment1 = SearchFragment.newInstance();
        fragment2 = LikesFragment.newInstance();
        fragment3 = new SettingsFragment();
        // setup the search fragment as the active fragment
        active = fragment1;

        // add each fragment to the activity
        // this will make showing and hiding inside of the fragment container easier when the user
        // clicks on the corresponding item in the bottom nav bar
        fm.beginTransaction().add(R.id.fragment_container, fragment3, "3").hide(fragment3).commit();
        fm.beginTransaction().add(R.id.fragment_container, fragment2, "2").hide(fragment2).commit();
        fm.beginTransaction().add(R.id.fragment_container, fragment1, "1").commit();

        // select listener for the bottom nav view
        mNavView.setOnNavigationItemSelectedListener(item -> {
            // checks each menu item's id name
            switch(item.getItemId()) {
                // search fragment
                case R.id.action_search:
                    // get the search preferences when the user selects the search fragment
                    getInfoFromSharedPrefs();
                    // if refresh is true -
                    // if the fragment isn't null -
                    // refresh the search fragment
                    // this will make it so when the user signs in and signs out, it doesn't have
                    // a problem telling whether or not the user has things liked or not
                    if (refresh) {
                        if (fragment1 != null) {
                            SearchFragment searchFragment = (SearchFragment) fragment1;
                            searchFragment.refreshFrag();
                        }
                        // put false in the refresh shared preference
                        editor.putBoolean("refresh", false);
                        editor.apply();
                    }
                    // hide whatever fragment is active and show the search fragment
                    fm.beginTransaction().hide(active).show(fragment1).commit();
                    // checks the match ingredients option
                    // if it is on -
                    // alert the user
                    if (match) {
                        toastMessage("Match Ingredients is On");
                    }
                    // make the active fragment Search Fragment
                    active = fragment1;
                    return true;
                    // Likes Fragment
                case R.id.action_likes:
                    // if the active fragment isn't the Likes Fragment
                    // hide the active fragment and show the Likes Fragment
                    if (active != fragment2) {
                        fm.beginTransaction().hide(active).show(fragment2).commit();
                    }
                    // Make the Likes fragment the active fragment
                    active = fragment2;
                    return true;
                    // Settings fragment
                case R.id.action_settings:
                    // hide the active fragment and show the Settings Fragment
                    fm.beginTransaction().hide(active).show(fragment3).commit();
                    // make the Settings fragment the active fragment
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

    // cleans up any open callbacks as well as updates likes information inside of shared preferences
    @Override
    protected void onDestroy() {
        // updates the shared preferences "numLikes" and "actualNumLikes" to 0
        // this is so on startup of the app again, this information is then found again to be up-to-date
        // and not out of sync with the info stored on the device
        editor.putInt("actualNumLikes", 0);
        editor.putInt("numLikes", 0);
        editor.apply();

        // checks to see if the handler is not null and removes the runnable callback
        if (mHandler != null) { mHandler.removeCallbacks(mRunnable); }

        super.onDestroy();
    }

    // On Back Pressed
    @Override
    public void onBackPressed() {
        // checks if boolean for doubleBack is true -
        // if true -
        // close the application like normal
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        // if it was false -
        // set it to true, alert the user and if the user presses back again within 2 seconds
        // close the app
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