package com.app.scavenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView mNavView = findViewById(R.id.bottom_nav_view);
        //FrameLayout mFragmentContainer = findViewById(R.id.fragment_container);

        loadFragment(new SearchFragment());

        mNavView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment;
                switch(item.getItemId()) {
                    case R.id.action_search:
                        fragment = new SearchFragment();
                        loadFragment(fragment);
                        return true;
                    case R.id.action_favorites:
                        fragment = new FavoritesFragment();
                        loadFragment(fragment);
                        return true;
                    case R.id.action_account:
                        //Check login token
                        //If logged in, show Account Logged
                        //If not logged in, show Sign In/ Sign Up
                        fragment = new AccountLogged();
                        loadFragment(fragment);
                        return true;
                }
                return false;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        //load fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}