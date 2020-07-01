package com.app.scavenger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context mContext;
    private Preference signIn, signOut, help, about;
    private SharedPreferences sharedPreferences;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private DatabaseHelper myDb;
    private AccessToken accessToken;
    private ConnectionDetector con;

    private RefreshFragments mCallback;

    // Shared Preferences Data
    //-----------------------------------------
    private String userId = null;
    private boolean logged = false;
    private String name = null;
    private String email = null;
    //------------------------------------------

    interface RefreshFragments {
        void refreshSearch();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);
        mContext = getContext();
        signIn = findPreference("signIn");
        signOut = findPreference("signOut");
        help = findPreference("help");
        about = findPreference("about");
        //SwitchPreference matchIngr = findPreference("match");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        //getInfoFromSharedPrefs();

        if (sharedPreferences.getBoolean("logged", false)) {
            signOut.setVisible(true);
            signIn.setVisible(false);
        } else {
            signOut.setVisible(false);
            signIn.setVisible(true);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        myDb = DatabaseHelper.getInstance(mContext);
        con = new ConnectionDetector(mContext);
        //GOOGLE INFORMATION -------------------------------------------------------
        // Requests the information from Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.clientId_web_googleSignIn))
                .requestProfile()
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso);

        //--------------------------------------------------------------------------

        // Facebook Information ----------------------------------------------------

        accessToken = AccessToken.getCurrentAccessToken();

        // -------------------------------------------------------------------------
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
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle("No Internet connection found")
                        .setMessage("You don't have an Internet connection. Please reconnect and try again.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                logoutDialog();
            }
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
            //refresh();
            mAuth.signOut();

            if (GoogleSignIn.getLastSignedInAccount(mContext) != null) {
                mGoogleSignInClient.signOut();
            }

            if (accessToken != null && !accessToken.isExpired()) {
                LoginManager.getInstance().logOut();
            }

            myDb.clearData();

            // CHECK: Let search fragment know to reload on sign out
            refresh();

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("logged", false);
            editor.putString("name", null);
            editor.putString("email", null);
            editor.putString("userId", null);
            editor.putBoolean("refresh", false);
            editor.apply();
            logged = false;
            name = null;
            email = null;
            userId = null;
            toastMessage();
            signOut.setVisible(false);
            signIn.setVisible(true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals("match")) {
            if (sharedPreferences.getBoolean("match", false)) {
                new MaterialAlertDialogBuilder(mContext, R.style.ReminderAlertTheme)
                        .setTitle("Just a quick thing")
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
        //getInfoFromSharedPrefs();

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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            mCallback = (RefreshFragments) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString());
        }
    }

    @Override
    public void onDetach() {
        mCallback = null;
        super.onDetach();
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

    //method for creating a Toast
    private void toastMessage() {
        Toast.makeText(mContext, "Successfully signed out", Toast.LENGTH_SHORT).show();
    }

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", null);
        email = sharedPreferences.getString("email", null);
        name = sharedPreferences.getString("name", null);
    }

    public void refresh() {
        mCallback.refreshSearch();
    }
}