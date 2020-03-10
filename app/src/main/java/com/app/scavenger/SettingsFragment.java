package com.app.scavenger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context mContext;
    private Preference account, feedback, help, about, legal;
    private SwitchPreference matchIngr;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);
        mContext = getContext();
        account = findPreference("account");
        feedback = findPreference("feedback");
        help = findPreference("help");
        about = findPreference("about");
        legal = findPreference("legal");
        matchIngr = findPreference("match");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        account.setOnPreferenceClickListener(v -> {
            openAccountInfo();
            return false;
        });

        help.setOnPreferenceClickListener(v -> {
            openHelp();
            return false;
        });

        about.setOnPreferenceClickListener(v -> {
            openAbout();
            return false;
        });

        legal.setOnPreferenceClickListener(v -> {
            openLegal();
            return false;
        });
    }

    private void openHelp() {
        startActivity(new Intent(mContext, Help.class));
    }

    private void openAbout() {
        startActivity(new Intent(mContext, About.class));
    }

    private void openLegal() {
        startActivity(new Intent(mContext, Legal.class));
    }

    private void openAccountInfo() {
        startActivity(new Intent(mContext, AccountInfo.class));
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("match")) {
            if (sharedPreferences.getBoolean("match", false)) {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle("Just a Quick Thing")
                        .setMessage("In order to get the best results with Match Ingredients, separate your ingredients with a comma (',')")
                        .setCancelable(false)
                        .setPositiveButton("Got It!", (dialog, which) -> dialog.dismiss()).create()
                        .show();
            }
        }
    }

    @Override
    public void onResume() {
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}