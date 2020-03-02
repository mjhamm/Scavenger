package com.app.scavenger;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context mContext;
    private Preference feedback, help, about, legal;
    private SwitchPreference matchIngr;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);
        mContext = getContext();
        feedback = findPreference("feedback");
        help = findPreference("help");
        about = findPreference("about");
        legal = findPreference("legal");
        matchIngr = findPreference("match");
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("match")) {
            if (sharedPreferences.getBoolean("match", false)) {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle("Just a Quick Thing")
                        .setMessage("In order to get the best results with Match Ingredients, separate your ingredients with a comma (',')")
                        .setCancelable(false)
                        .setPositiveButton("Got It!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create()
                        .show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}