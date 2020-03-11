package com.app.scavenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
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

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, Account.SendDataToMain {


    private static final String TAG = "LOG: ";
    private Fragment fragment1 = null;
    private Fragment fragment2 = null;
    private Fragment fragment3 = null;
    private final FragmentManager fm = getSupportFragmentManager();
    private SharedPreferences mSharedPreferences;
    private Fragment active = null;
    private String userId = null;
    private String name = null;
    private String email = null;
    private boolean logged = false;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (account != null) {
            logged = true;
            userId = account.getId();
            name = account.getDisplayName();
            email = account.getEmail();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()
                .requestEmail()
                .requestIdToken("565312817175-cipp792csradj5qukdb836j8e9tuq7gr.apps.googleusercontent.com")
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);

        ArrayList<RecipeItem> recipeItems = new ArrayList<>();

        fragment1 = SearchFragment.newInstance(userId, logged);
        fragment2 = FavoritesFragment.newInstance(userId, logged);
        fragment3 = Account.newInstance(userId, name, email, logged);
        active = fragment1;

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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    @Override
    public void getLoginData(String userId, boolean logged) {
        SearchFragment searchFragment = (SearchFragment) fm.findFragmentByTag("1");
        FavoritesFragment favoritesFragment = (FavoritesFragment) fm.findFragmentByTag("2");
        try {
            if (searchFragment != null) {
                searchFragment.getData(userId, logged);
                fm.beginTransaction().detach(fragment1).attach(fragment1).commit();
            }
            if (favoritesFragment != null) {
                favoritesFragment.getData(userId, logged);
                fm.beginTransaction().detach(fragment2).attach(fragment2).commit();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.d(TAG, e.toString());
        }
    }
}