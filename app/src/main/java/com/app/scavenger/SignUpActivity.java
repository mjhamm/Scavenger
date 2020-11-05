package com.app.scavenger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private final int RC_SIGN_UP = 102;
    public static final String TAG = "LOG: ";

    // Views from Sign Up activity
    private EditText fullName, emailEdit, passEdit, passConfirmEdit;
    private TextView passNoMatch, termsTextView;
    private MaterialCheckBox termsCheck;
    private FrameLayout progressHolder;
    private MaterialButton signUpButton;

    private String name = null, email = null, pass = null;

    private ConnectionDetector con;
    private SharedPreferences sharedPreferences;
    private GoogleSignInClient mGoogleSignUpClient;
    private DatabaseHelper myDb;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;
    private CallbackManager callbackManager;
    private boolean checkVerify = false;

    @Override
    protected void onStart() {
        super.onStart();
        // Gets the Firebase user
        /*FirebaseUser currentUser = mAuth.getCurrentUser();
        // checks if the user isn't null
        if (currentUser != null) {
            // update the shared preferences with the user's userId
            updatePrefInfo(currentUser.getUid());
        }*/
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // get the instance of Firebase
        mAuth = FirebaseAuth.getInstance();

        // get the instance of shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // instance of Database
        myDb = DatabaseHelper.getInstance(this);
        // connection detector for whether or not the device is connected to the internet
        con = new ConnectionDetector(this);

        // Google information
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.clientId_web_googleSignIn))
                .requestProfile()
                .requestEmail()
                .build();
        mGoogleSignUpClient = GoogleSignIn.getClient(this, gso);
        MaterialButton googleSignUpButton = findViewById(R.id.google_signUp);

        // Facebook Info
        MaterialButton facebookSignUpButton = findViewById(R.id.facebook_signUp);
        callbackManager = CallbackManager.Factory.create();

        MaterialButton appleSignUpButton = findViewById(R.id.apple_signUp);

        fullName = findViewById(R.id.fullName_editText);
        emailEdit = findViewById(R.id.email_editText);
        passEdit = findViewById(R.id.password_editText);
        passConfirmEdit = findViewById(R.id.passwordConfirm_editText);
        passNoMatch = findViewById(R.id.passNoMatch);
        termsTextView = findViewById(R.id.signUpTerms);
        termsCheck = findViewById(R.id.signUpCheckbox);
        signUpButton = findViewById(R.id.signUp_Button);

        progressHolder = findViewById(R.id.signUp_progressHolder);

        TopToolbar topToolbar = findViewById(R.id.signUp_toolbar);
        topToolbar.setTitle("Sign Up");

        // check box for terms
        termsCheck.setOnCheckedChangeListener((buttonView, isChecked) -> checkFieldsForEmpty());

        // set up for terms string
        // this will allow users to click the privacy policy and terms
        String termsText = "By Signing Up, you agree to Scavenger's Terms & Conditions and Privacy Policy.";
        SpannableString termsSS = new SpannableString(termsText);
        termsSS.setSpan(new URLSpan(Constants.scavengerTermsURL), 40,58, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        termsSS.setSpan(new URLSpan(Constants.scavengerPrivacyURL), 63,77, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        termsTextView.setText(termsSS);
        termsTextView.setMovementMethod(LinkMovementMethod.getInstance());

        // Edit Text fields for sign up
        // ------------------------------------------------------------------------------------------------------
        // Full Name
        fullName.addTextChangedListener(signUpTextWatcher);
        // Email
        emailEdit.addTextChangedListener(signUpTextWatcher);
        // Password
        passEdit.addTextChangedListener(signUpTextWatcher);
        // Password Confirm
        passConfirmEdit.addTextChangedListener(signUpTextWatcher);
        // ------------------------------------------------------------------------------------------------------

        googleSignUpButton.setOnClickListener(v -> {
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(Constants.noInternetTitle)
                        .setMessage(Constants.noInternetMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                if (termsCheck.isChecked()) {
                    googleSignUp();
                } else {
                    toastMessage("Please accept the Terms & Conditions");
                }
            }
        });

        // Sign in with Facebook Button ------------------------------------------------------------
        facebookSignUpButton.setOnClickListener(v ->{
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(Constants.noInternetTitle)
                        .setMessage(Constants.noInternetMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                if (termsCheck.isChecked()) {
                    facebookSignUp();
                } else {
                    toastMessage("Please accept the Terms & Conditions");
                }
            }
        });

        // -----------------------------------------------------------------------------------------
        
        signUpButton.setOnClickListener(v -> {
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(Constants.noInternetTitle)
                        .setMessage(Constants.noInternetMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                progressHolder.setVisibility(View.VISIBLE);
                name = fullName.getText().toString();
                Log.d(TAG, "name: " + name);
                email = emailEdit.getText().toString();
                pass = passEdit.getText().toString();
                mAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                //toastMessage("Signed up successfully.");
                                if (user != null) {
                                    user.sendEmailVerification();
                                    checkVerify = true;
                                    sendDataToFirebase(user);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putBoolean("verify", checkVerify);
                                    editor.apply();
                                    //myDb.clearData();
                                    //retrieveLikesFromFirebase(user);
                                    //updatePrefInfo(user.getUid());
                                }
                                progressHolder.setVisibility(View.GONE);
                                finish();
                                // Sign in success
                            } else {
                                // Sign in failed
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                hideKeyboard(SignUpActivity.this);
                                new MaterialAlertDialogBuilder(SignUpActivity.this)
                                        .setTitle("Email Address already in use.")
                                        .setMessage("This email address is already in use by another account. Please Sign In, or Sign Up with a different email address.")
                                        .setPositiveButton("OK", (dialog, which) -> {
                                            dialog.dismiss();
                                            progressHolder.setVisibility(View.GONE);
                                            fullName.setText("");
                                            passEdit.setText("");
                                            passConfirmEdit.setText("");
                                            emailEdit.setText("");
                                            fullName.requestFocus();
                                        })
                                        .create()
                                        .show();
                            }
                        });
            }
        });

        // -----------------------------------------------------------------------------------------

        appleSignUpButton.setOnClickListener(v -> {
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(Constants.noInternetTitle)
                        .setMessage(Constants.noInternetMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                if (termsCheck.isChecked()) {
                    appleSignUp();
                } else {
                    toastMessage("Please accept the Terms & Conditions");
                }
            }
        });
    }

    // Apple Sign In information and Methods ------------------------------------------------------------------------------------------------------------------

    private void appleSignUp() {
        progressHolder.setVisibility(View.VISIBLE);
        OAuthProvider.Builder provider = OAuthProvider.newBuilder("apple.com");
        List<String> scopes = new ArrayList<String>() {
            {
                add("email");
                add("name");
            }
        };
        provider.setScopes(scopes);

        mAuth = FirebaseAuth.getInstance();
        Task<AuthResult> pending = mAuth.getPendingAuthResult();
        if (pending != null) {
            pending.addOnSuccessListener(authResult -> {
                // Get the user profile with authResult.getUser() and
                // authResult.getAdditionalUserInfo(), and the ID
                // token from Apple with authResult.getCredential().
                FirebaseUser user = authResult.getUser();
                //toastMessage("Signed up successfully");
                if (user != null) {
                    if (user.getDisplayName() == null) {
                        name = "Anonymous";
                    } else {
                        name = user.getDisplayName();
                    }
                    email = user.getEmail();
                    retrieveLikesFromFirebase(user);
                    updatePrefInfo(user.getUid());
                    sendDataToFirebase(user);
                }
                progressHolder.setVisibility(View.GONE);
                finish();
            }).addOnFailureListener(e -> {
                toastMessage("Issue Signing up. Please try again");
                Log.w(TAG, "checkPending:onFailure", e);
                progressHolder.setVisibility(View.GONE);
            });
        } else {
            startSignInWithApple(provider);
            Log.d(TAG, "pending: null");
        }
        //Log.d(TAG, "Apple Sign In");
    }

    private void startSignInWithApple(OAuthProvider.Builder provider) {
        mAuth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "activitySignIn:onSuccess:" + authResult.getUser());
                    FirebaseUser user = authResult.getUser();
                    //toastMessage("Signed up successfully");
                    if (user != null) {
                        if (user.getDisplayName() == null) {
                            name = "Anonymous";
                        } else {
                            name = user.getDisplayName();
                        }
                        email = user.getEmail();
                        retrieveLikesFromFirebase(user);
                        updatePrefInfo(user.getUid());
                        sendDataToFirebase(user);
                    }
                    progressHolder.setVisibility(View.GONE);
                    finish();
                })
                .addOnFailureListener(e -> {
                    toastMessage("Issue Signing up. Please try again");
                    Log.w(TAG, "activitySignIn:onFailure", e);
                    progressHolder.setVisibility(View.GONE);
                });
    }

    // --------------------------------------------------------------------------------------------------------------------------------------------------------

    // Sends user information to Firebase
    private void sendDataToFirebase(FirebaseUser user) {
        if (user != null) {
            HashMap<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("email", email);
            db.collection(Constants.firebaseUser).document(user.getUid()).set(data);
        }
    }

    // Google Sign Up information and Methods -----------------------------------------------------------------------------------------------------------------

    private void googleSignUp() {
        Intent signInIntent = mGoogleSignUpClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_UP);
    }

    // Authenticate Google Sign In with Firebase to make sure user exists and then can sign in
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signUpWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        toastMessage("Signed up successfully");
                        if (user != null) {
                            myDb.clearData();
                            name = user.getDisplayName();
                            email = user.getEmail();
                            retrieveLikesFromFirebase(user);
                            updatePrefInfo(user.getUid());
                            sendDataToFirebase(user);
                        }
                        progressHolder.setVisibility(View.GONE);
                        finish();
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signUpWithCredential:failure", task.getException());
                        toastMessage("Authentication Failed. Please try again or reach out to support@theScavengerApp.com for assistance");
                    }
                });
    }

    // Facebook Sign In information and Methods -----------------------------------------------------------------------------------------------------------------

    private void facebookSignUp() {
        callbackManager = CallbackManager.Factory.create();

        // Set Permissions
        String EMAIL = "email";
        String PUBLIC_PROFILE = "public_profile";
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList(EMAIL, PUBLIC_PROFILE));

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
                        toastMessage("Signed up successfully");
                        if (user != null) {
                            myDb.clearData();
                            name = user.getDisplayName();
                            email = user.getEmail();
                            retrieveLikesFromFirebase(user);
                            updatePrefInfo(user.getUid());
                            sendDataToFirebase(user);
                        }
                        progressHolder.setVisibility(View.GONE);
                        finish();
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        toastMessage("Authentication Failed. Please try again or reach out to support@theScavengerApp.com for assistance");
                    }
                    /*finish();
                    progressHolder.setVisibility(View.GONE);*/
                });
    }


    // --------------------------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode,data);
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_UP) {
            progressHolder.setVisibility(View.VISIBLE);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                //noinspection ConstantConditions
                Log.d(TAG, "firebaseAuthWithGoogle: " + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.d(TAG, "Google sign up failed", e);
                progressHolder.setVisibility(View.GONE);
            }
        }
    }

    private final TextWatcher signUpTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkFieldsForEmpty();
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private void updatePrefInfo(String userId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("logged", true);
        editor.putString("userId", userId);
        editor.putBoolean("refresh", true);
        editor.putBoolean("verify", checkVerify);
        editor.apply();
    }

    private void checkFieldsForEmpty() {
        if (!passConfirmEdit.getText().toString().trim().equals(passEdit.getText().toString().trim())) {
            passNoMatch.setVisibility(View.VISIBLE);
            signUpButton.setEnabled(false);
        } else {
            passNoMatch.setVisibility(View.GONE);
        }
        if (!fullName.getText().toString().trim().isEmpty() && !emailEdit.getText().toString().trim().isEmpty() && !passEdit.getText().toString().trim().isEmpty() && !passConfirmEdit.getText().toString().trim().isEmpty()) {
            if (!emailEdit.getText().toString().trim().contains("@")) {
                signUpButton.setEnabled(false);
            } else {
                if (!termsCheck.isChecked()) {
                    signUpButton.setEnabled(false);
                } else {
                    if (!passConfirmEdit.getText().toString().trim().equals(passEdit.getText().toString().trim())) {
                        passNoMatch.setVisibility(View.VISIBLE);
                        signUpButton.setEnabled(false);
                    } else {
                        passNoMatch.setVisibility(View.GONE);
                        signUpButton.setEnabled(true);
                    }
                }
            }
        } else {
            signUpButton.setEnabled(false);
        }
    }

    private void retrieveLikesFromFirebase(FirebaseUser user) {
        CollectionReference likesRef = db.collection(Constants.firebaseUser).document(user.getUid()).collection(Constants.firebaseLikes);
        likesRef.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    String itemId;
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            itemId = documentSnapshot.getString("itemId");
                            myDb.addDataToView(itemId);
                        }
                    }
                });
    }

    public void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        view.clearFocus();
    }

    //method for creating a Toast
    private void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        termsTextView.setText("");
        termsTextView.setMovementMethod(null);

        if (callbackManager != null) {
            callbackManager = null;
        }
    }
}
