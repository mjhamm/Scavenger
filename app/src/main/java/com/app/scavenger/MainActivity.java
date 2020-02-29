package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private final Fragment fragment1 = new SearchFragment();
    private final Fragment fragment2 = new FavoritesFragment();
    private final Fragment fragment3 = new AccountNotLogged();
    private final Fragment fragment4 = new AccountLogged();
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment active = fragment1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fm.beginTransaction().add(R.id.fragment_container, fragment4, "4").hide(fragment4).commit();
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
                    //Check login token
                    //If logged in, show Account Logged
                    //If not logged in, show Sign In/ Sign Up
                    return true;
            }
            return false;
        });
    }
}