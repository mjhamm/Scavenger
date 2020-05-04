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
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context mContext;
    private Preference signIn, signOut, help, about;
    private SwitchPreference matchIngr;
    private SharedPreferences sharedPreferences;
    private GoogleSignInClient mGoogleSignInClient;

    // Shared Preferences Data
    //-----------------------------------------
    private String userId = null;
    private boolean logged = false;
    private String name = null;
    private String email = null;
    //------------------------------------------

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);
        mContext = getContext();
        signIn = findPreference("signIn");
        signOut = findPreference("signOut");
        help = findPreference("help");
        about = findPreference("about");
        matchIngr = findPreference("match");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        getInfoFromSharedPrefs();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //GOOGLE INFORMATION -------------------------------------------------------
        // Requests the information from Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.clientId_web_googleSignIn))
                .requestProfile()
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso);

        //--------------------------------------------------------------------------

        if (sharedPreferences.getBoolean("logged", false)) {
            signOut.setVisible(true);
            signIn.setVisible(false);
        } else {
            signOut.setVisible(false);
            signIn.setVisible(true);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        signIn.setOnPreferenceClickListener(v -> {
            openSignIn();
            return false;
        });

        signOut.setOnPreferenceClickListener(v -> {
            logoutDialog();
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
    }

    private void openHelp() {
        startActivity(new Intent(mContext, Help.class));
    }

    private void openAbout() {
        startActivity(new Intent(mContext, About.class));
    }

    private void openSignIn() {
        startActivity(new Intent(mContext, SignInActivity.class));
    }

    private void logoutDialog() {
        new MaterialAlertDialogBuilder(mContext)
                .setTitle("Are you sure you want to Sign Out?")
                .setCancelable(false)
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    //LOG OUT
                    signOut();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .create()
                .show();
    }

    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(getActivity(), task -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("logged", false);
                    editor.putString("name", null);
                    editor.putString("email", null);
                    editor.putString("userId", null);
                    editor.apply();
                    logged = false;
                    name = null;
                    email = null;
                    userId = null;

                    signOut.setVisible(false);
                    signIn.setVisible(true);
                });
    }

//    private void deleteAccountFromFirebase() {
//        db.collection("Users").document(userId)
//                .delete()
//                .addOnSuccessListener(aVoid -> {
//                    Log.d(TAG, "Successfully removed account from Firebase");
//                })
//                .addOnFailureListener(e -> Log.d(TAG, "Failure to delete account from Firebase " + e.toString()));
//    }
//
//    private void deleteAccountFirst() {
//        new MaterialAlertDialogBuilder(mContext)
//                .setTitle("Are you sure you want to delete your account?")
//                .setMessage("If you delete your account, you will lose all of your favorites that you have saved.")
//                .setCancelable(false)
//                .setPositiveButton("I'm Sure!", (dialog, which) -> {
//                    deleteAccountSecond();
//                })
//                .setNegativeButton("Cancel", (dialog, which) -> {
//                    dialog.cancel();
//                })
//                .create()
//                .show();
//    }
//
//    private void deleteAccountSecond() {
//        new MaterialAlertDialogBuilder(mContext)
//                .setTitle("Just making sure. Do you really want to delete your account?")
//                .setMessage("Just double checking that you want to delete your account and lose all of your favorites?")
//                .setCancelable(false)
//                .setPositiveButton("Yep! Delete it!", (dialog, which) -> {
//                    //DELETE ACCOUNT
//                    //deleteAccountFromFirebase();
//                })
//                .setNegativeButton("Cancel", (dialog, which) -> {
//                    dialog.cancel();
//                })
//                .create()
//                .show();
//    }



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
        super.onResume();
        getInfoFromSharedPrefs();

        if (sharedPreferences.getBoolean("logged", false)) {
            signOut.setVisible(true);
            signIn.setVisible(false);
        } else {
            signOut.setVisible(false);
            signIn.setVisible(true);
        }

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

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", null);
        email = sharedPreferences.getString("email", null);
        name = sharedPreferences.getString("name", null);
    }
}