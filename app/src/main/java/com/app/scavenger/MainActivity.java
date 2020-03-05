package com.app.scavenger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {


    private static final String TAG = "LOG: ";
    private final Fragment fragment1 = new SearchFragment();
    private final Fragment fragment2 = new FavoritesFragment();
    private Fragment fragment3 = null;
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment active = fragment1;
    private FavoriteAdapter favoriteAdapter;
    private SearchAdapter searchAdapter;
    private ArrayList<RecipeItem> recipeItems;
    private GoogleSignInClient googleSignInClient;
    private GoogleApiClient googleApiClient;
    private String name = null;
    private String email = null;
    private boolean logged = false;
    private GoogleSignInAccount account;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        account = GoogleSignIn.getLastSignedInAccount(this);

        if (account != null) {
            logged = true;
            name = account.getDisplayName();
            email = account.getEmail();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this,this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        recipeItems = new ArrayList<>();

        fragment3 = Account.newInstance(name, email, logged);
        Log.e(TAG, "Logged: " + logged + " NAME: " + name);

        fm.beginTransaction().add(R.id.fragment_container, fragment3, "3").hide(fragment3).commit();
        fm.beginTransaction().add(R.id.fragment_container, fragment2, "2").hide(fragment2).commit();
        fm.beginTransaction().add(R.id.fragment_container, fragment1, "1").commit();

        BottomNavigationView mNavView = findViewById(R.id.bottom_nav_view);

        mNavView.setOnNavigationItemSelectedListener(item -> {
            switch(item.getItemId()) {
                case R.id.action_search:
                    fm.beginTransaction().hide(active).show(fragment1).commit();
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


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }
}