package com.app.scavenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import android.accounts.Account;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity { // Account.SendDataToMain {


    private static final String TAG = "LOG: ";
    private Fragment fragment1 = null;
    private Fragment fragment2 = null;
    private Fragment fragment3 = null;
    private final FragmentManager fm = getSupportFragmentManager();
    private SharedPreferences mSharedPreferences;
    private Fragment active = null;
    private boolean matchOn = false;

    // Shared Preferences Data
    //-----------------------------------------
    private String userId = null;
    private boolean logged = false;
    private String name = null;
    private String email = null;
    //------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        getInfoFromSharedPrefs();

        if (matchOn) {
            Toast.makeText(this, "Match Ingredients is On", Toast.LENGTH_SHORT).show();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()
                .requestEmail()
                .requestIdToken(getString(R.string.clientId_web_googleSignIn))
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);

        ArrayList<RecipeItem> recipeItems = new ArrayList<>();

        fragment1 = SearchFragment.newInstance(userId, logged);
        fragment2 = FavoritesFragment.newInstance(userId, logged);
        fragment3 = new SettingsFragment();
        active = fragment1;

        fm.beginTransaction().add(R.id.fragment_container, fragment3, "3").hide(fragment3).commit();
        fm.beginTransaction().add(R.id.fragment_container, fragment2, "2").hide(fragment2).commit();
        fm.beginTransaction().add(R.id.fragment_container, fragment1, "1").commit();

        BottomNavigationView mNavView = findViewById(R.id.bottom_nav_view);

        mNavView.setOnNavigationItemSelectedListener(item -> {
            getInfoFromSharedPrefs();
            switch(item.getItemId()) {
                case R.id.action_search:
                    fm.beginTransaction().hide(active).show(fragment1).commit();
                    if (matchOn) {
                        Toast.makeText(this, "Match Ingredients is On", Toast.LENGTH_SHORT).show();
                    }
                    active = fragment1;
                    return true;
                case R.id.action_favorites:
                    fm.beginTransaction().hide(active).show(fragment2).commit();
                    active = fragment2;
                    return true;
                case R.id.action_account:
                    fm.beginTransaction().hide(active).show(fragment3).commit();
                    active = fragment3;
                    return true;
            }
            return false;
        });
    }

//    @Override
//    public void getLoginData(String userId, boolean logged) {
//        SearchFragment searchFragment = (SearchFragment) fm.findFragmentByTag("1");
//        FavoritesFragment favoritesFragment = (FavoritesFragment) fm.findFragmentByTag("2");
//        try {
//            if (searchFragment != null) {
//                searchFragment.getData(userId, logged);
//                //fm.beginTransaction().detach(fragment1).attach(fragment1).commit();
//            }
//            if (favoritesFragment != null) {
//                favoritesFragment.getData(userId, logged);
//                fm.beginTransaction().detach(fragment2).attach(fragment2).commit();
//            }
//        } catch (NullPointerException e) {
//            e.printStackTrace();
//            Log.d(TAG, e.toString());
//        }
//    }

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        matchOn = sharedPreferences.getBoolean("match", false);
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", null);
        email = sharedPreferences.getString("email", null);
        name = sharedPreferences.getString("name", null);
    }
}