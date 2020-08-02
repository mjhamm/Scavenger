package com.app.scavenger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.HashMap;

public class GroceryListActivity extends AppCompatActivity {

    private final int RC_SIGN_IN = 101;
    public static final String TAG = "GroceryListActivity";

    private ConstraintLayout mSignInLayout;
    private MaterialButton mEmailSignIn, mGoogleSignIn, mFacebookSignIn;
    private TextView mSignUpText;
    private FrameLayout progressHolder;
    private RecyclerView mGroceryRecyclerView;
    private SharedPreferences sharedPreferences;
    private TextView terms;
    private FirebaseAuth mAuth;
    private ConnectionDetector con;
    private CallbackManager callbackManager;
    private GoogleSignInClient mGoogleSignInClient;
    private ImageButton mMoreButton, mAddItemButton;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onStart() {
        super.onStart();

        String termsText = "By Signing In, you agree to Scavenger's Terms & Conditions and Privacy Policy.";
        SpannableString termsSS = new SpannableString(termsText);
        termsSS.setSpan(new URLSpan("https://www.thescavengerapp.com/terms-and-conditions"), 40,58, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        termsSS.setSpan(new URLSpan("https://www.thescavengerapp.com/privacy-policy"), 63,77, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        terms.setText(termsSS);
        terms.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update to the status bar on lower SDK's
        // Makes bar on lower SDK's black with white icons
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            this.getWindow().setStatusBarColor(getResources().getColor(android.R.color.black));
        }

        setContentView(R.layout.activity_grocery_list);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        con = new ConnectionDetector(this);
        mAuth = FirebaseAuth.getInstance();

        mMoreButton = findViewById(R.id.list_clear);
        mAddItemButton = findViewById(R.id.add_custom_item_button);
        terms = findViewById(R.id.accept_terms_signin);
        mSignUpText = findViewById(R.id.signUp_text);
        mGroceryRecyclerView = findViewById(R.id.grocery_recyclerView);
        mEmailSignIn = findViewById(R.id.signIn_Button);
        mSignInLayout = findViewById(R.id.signIn_layout);
        progressHolder = findViewById(R.id.signIn_progressHolder);

        // Google Info -----------------------------------------------------------------------------------
        mGoogleSignIn = findViewById(R.id.google_signIn);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()
                .requestEmail()
                .requestIdToken(getString(R.string.clientId_web_googleSignIn))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        // -----------------------------------------------------------------------------------------------

        // Facebook Info ---------------------------------------------------------------------------------
        mFacebookSignIn = findViewById(R.id.facebook_signIn);
        callbackManager = CallbackManager.Factory.create();
        // -----------------------------------------------------------------------------------------------

        // close the activity
        ImageButton closeButton = findViewById(R.id.list_close);
        closeButton.setOnClickListener(v -> finish());

        // Open up sign up activity
        mSignUpText.setTextColor(Color.BLUE);
        mSignUpText.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));

        hideLayouts();

        mAddItemButton.setOnClickListener(v -> {
            addCustomItem();
        });

        mMoreButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, mMoreButton);
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_clearlist) {
                    // clear list
                    clearList();
                }
                return false;
            });
            MenuInflater inflater = popupMenu.getMenuInflater();
            inflater.inflate(R.menu.grocery_list_menu, popupMenu.getMenu());
            popupMenu.show();
        });

        // SIGN IN WITH EMAIL ----------------------------------------------------------------------------------------------

        // Sign in with Email
        mEmailSignIn.setOnClickListener(v -> {
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(Constants.noInternetTitle)
                        .setMessage(Constants.noInternetMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                // Sign In to Firebase with Email
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
                builder.setTitle("Sign In with Email");
                final View customView = getLayoutInflater().inflate(R.layout.dialog_sign_in_grocery, null);
                builder.setView(customView);

                builder.setPositiveButton("Sign In", (dialog, which) -> {
                    // make sure the user is still connected to the internet when they hit sign in
                    if (!con.connectedToInternet()) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(Constants.noInternetTitle)
                                .setMessage(Constants.noInternetMessage)
                                .setPositiveButton("OK", (dialog1, which1) -> dialog.dismiss())
                                .create()
                                .show();
                    } else {
                        EditText emailEditText = customView.findViewById(R.id.email_editText);
                        String email = emailEditText.getText().toString();
                        EditText passEditText = customView.findViewById(R.id.password_editText);
                        String pass = passEditText.getText().toString();
                        if (!email.isEmpty() && !pass.isEmpty()) {
                            progressHolder.setVisibility(View.VISIBLE);
                            mAuth.signInWithEmailAndPassword(email, pass)
                                    .addOnCompleteListener(this, task -> {
                                        if (task.isSuccessful()) {
                                            FirebaseUser user = mAuth.getCurrentUser();
                                            Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show();
                                            if (user != null) {
                                                Log.d("GroceryListActivity", "Signed In Successfully");

                                                // retrieve users grocery list
                                                retrieveListFromFirebase(user);

                                                // update shared preference data for the user
                                                updatePrefInfo(user.getUid());

                                                sendDataToFirebase(user);

                                                // hide layouts
                                                hideLayouts();
                                            }
                                            progressHolder.setVisibility(View.GONE);
                                            dialog.dismiss();
                                        } else {
                                            Log.w("GroceryListActivity", "SignInWithEmail:failure", task.getException());
                                            new MaterialAlertDialogBuilder(this)
                                                    .setTitle("Invalid Email or Password.")
                                                    .setMessage("The Email Address or Password that you have used is invalid. Please check typing and try again. If you continue to have issues, please reach out to Scavenger Support at support@thescavengerapp.com.")
                                                    .setPositiveButton("OK", (dialog1, which1) -> {
                                                        dialog.dismiss();
                                                        emailEditText.requestFocus();
                                                    })
                                                    .create()
                                                    .show();
                                        }
                                    });
                        } else {
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle("Problem Signing In")
                                    .setMessage("There was an issue Signing In. This could be because the email or password was blank. Please check and try again. If you continue to have issues, please reach out to Scavenger Support at support@thescavengerapp.com")
                                    .setPositiveButton("OK", (dialog1, which1) -> dialog.dismiss())
                                    .create()
                                    .show();
                        }
                    }
                    progressHolder.setVisibility(View.GONE);
                });

                builder.setNegativeButton("Cancel", ((dialog, which) -> dialog.cancel()));
                builder.create();
                builder.show();
            }
        });

        //  --------------------------------------------------------------------------------------------------------------------------------

        // SIGN IN WITH GOOGLE

        // Sign in with google button --------------------------------------------------------------
        mGoogleSignIn.setOnClickListener(v -> {
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(Constants.noInternetTitle)
                        .setMessage(Constants.noInternetMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                googleSignIn();
            }
        });

        //  --------------------------------------------------------------------------------------------------------------------------------

        // SIGN IN WITH FACEBOOK

        // Sign in with Facebook Button ------------------------------------------------------------
        mFacebookSignIn.setOnClickListener(v ->{
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(Constants.noInternetTitle)
                        .setMessage(Constants.noInternetMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                facebookSignIn();
            }
        });

        //  --------------------------------------------------------------------------------------------------------------------------------

    }

    // Google Sign In information and Methods -----------------------------------------------------------------------------------------------------------------

    private void googleSignIn() {
        Intent intent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    // Authenticate Google Sign In with Firebase to make sure user exists and then can sign in
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show();
                        if (user != null) {
                            retrieveListFromFirebase(user);

                            updatePrefInfo(user.getUid());

                            sendDataToFirebase(user);

                            hideLayouts();
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(this, "Authentication Failed. Please try again or reach out to support@theScavengerApp.com for assistance", Toast.LENGTH_SHORT).show();
                    }
                    progressHolder.setVisibility(View.GONE);
                });
    }

    // --------------------------------------------------------------------------------------------------------------------------------------------------------

    // Facebook Sign In information and Methods -----------------------------------------------------------------------------------------------------------------

    private void facebookSignIn() {

        // Set Permissions
        String EMAIL = "email";
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList(EMAIL, "public_profile"));

        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                progressHolder.setVisibility(View.VISIBLE);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {}

            @Override
            public void onError(FacebookException error) {}

        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show();
                        if (user != null) {
                            retrieveListFromFirebase(user);

                            updatePrefInfo(user.getUid());

                            sendDataToFirebase(user);

                            hideLayouts();
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(this, "Authentication Failed. Please try again or reach out to support@theScavengerApp.com for assistance", Toast.LENGTH_SHORT).show();
                    }
                    progressHolder.setVisibility(View.GONE);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode,data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            progressHolder.setVisibility(View.VISIBLE);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                //noinspection ConstantConditions
                Log.d(TAG, "firebaseAuthWithGoogle: " + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.d(TAG, "Google sign in failed", e);
                progressHolder.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        hideLayouts();
    }

    // hide or show layouts based on whether the user is logged in or not
    private void hideLayouts() {
        // check shared preferences to see if the user is logged in or not and set variable "logged"
        boolean logged = sharedPreferences.getBoolean("logged", false);

        // Set the visibility of layouts based on whether or not the user is logged in or not
        if (logged) {
            mGroceryRecyclerView.setVisibility(View.VISIBLE);
            mSignInLayout.setVisibility(View.GONE);
            mMoreButton.setVisibility(View.VISIBLE);
            mAddItemButton.setVisibility(View.VISIBLE);

        } else {
            mGroceryRecyclerView.setVisibility(View.GONE);
            mSignInLayout.setVisibility(View.VISIBLE);
            mMoreButton.setVisibility(View.GONE);
            mAddItemButton.setVisibility(View.GONE);
        }
    }

    private void clearList() {
        Log.d(TAG, "Clear List");
    }

    private void addCustomItem() {
        Log.d(TAG, "Add Custom Item");
    }

    private void retrieveListFromFirebase(FirebaseUser user) {
        Log.d(TAG, "retrieveList");
        /*CollectionReference groceryItems = db.collection(Constants.firebaseUser).document(user.getUid()).collection(Constants.firebaseGrocery);
        groceryItems.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {

                        }
                    }
                });*/
    }

    private void updatePrefInfo(String userId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("logged", true);
        editor.putString("userId", userId);
        editor.putBoolean("refresh", true);
        editor.apply();
    }

    // Sends user information to Firebase
    private void sendDataToFirebase(FirebaseUser user) {
        if (user != null) {
            HashMap<String, Object> data = new HashMap<>();
            data.put("name", user.getDisplayName());
            data.put("email", user.getEmail());
            db.collection(Constants.firebaseUser).document(user.getUid()).set(data);
        }
    }

    // opens the recipe in the users default browser
    private void openURLInChromeCustomTab(Context context, String url) {
        try {
            CustomTabsIntent.Builder builder1 = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder1.build();
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            builder1.setInstantAppsEnabled(true);
            customTabsIntent.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.getPackageName()));
            builder1.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
            customTabsIntent.launchUrl(context, Uri.parse(url));
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e("ChromeCustomTabError: ", "Activity Error");
        }
    }

    // open the recipe in the App Browser
    private void openInDefaultBrowser(Context context, String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(browserIntent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e("DefaultBrowserError: ", "Activity Error");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        terms.setText("");
        terms.setMovementMethod(null);

        if (callbackManager != null) {
            callbackManager = null;
        }
    }
}