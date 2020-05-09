package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity { // Account.SendDataToMain {

    //private static final String TAG = "LOG: ";

    private Fragment fragment1 = null;
    private Fragment fragment2 = null;
    private Fragment fragment3 = null;
    private final FragmentManager fm = getSupportFragmentManager();//
    private Fragment active = null;
    private boolean matchOn = false;
    private FirebaseAuth mAuth;

    // Shared Preferences Data
    //-----------------------------------------
    private String userId = null;
    private boolean logged = false;
    //------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            logged = true;
            userId = mAuth.getUid();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        BottomNavigationView mNavView = findViewById(R.id.bottom_nav_view);
        //GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        //mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        getInfoFromSharedPrefs();

        if (matchOn) {
            Toast.makeText(this, "Match ingredients is On", Toast.LENGTH_SHORT).show();
        }

//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestProfile()
//                .requestEmail()
//                .requestIdToken(getString(R.string.clientId_web_googleSignIn))
//                .build();

        fragment1 = SearchFragment.newInstance(userId, logged);
        fragment2 = FavoritesFragment.newInstance(userId, logged);
        fragment3 = new SettingsFragment();
        active = fragment1;

        fm.beginTransaction().add(R.id.fragment_container, fragment3, "3").hide(fragment3).commit();
        fm.beginTransaction().add(R.id.fragment_container, fragment2, "2").hide(fragment2).commit();
        fm.beginTransaction().add(R.id.fragment_container, fragment1, "1").commit();

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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        matchOn = sharedPreferences.getBoolean("match", false);
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", null);
    }
}