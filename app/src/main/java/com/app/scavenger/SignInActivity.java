package com.app.scavenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.os.SystemClock;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
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

public class SignInActivity extends AppCompatActivity {

    private final int RC_SIGN_IN = 102;
    private static final String TAG = "SIGN_IN_ACTIVITY: ";

    private MaterialButton signInButton;
    private Context mContext;
    private EditText emailEdit, passEdit;
    private FrameLayout progressHolder;
    private TextView signInTerms, mResendVerificationButton;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private String name = null, email = null, pass = null;
    private DatabaseHelper myDb;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager callbackManager;
    private ConnectionDetector con;
    private long mLastClickTime = 0;

    // Required empty public constructor
    public SignInActivity() {}

    @Override
    protected void onResume() {
        super.onResume();

        if (sharedPreferences.getBoolean("logged", false)) {
            finish();
        }

        if (sharedPreferences.getBoolean("verify", false)) {
            Toast.makeText(mContext, "A verification has been sent to your email. Please verify your email in order to Sign In", Toast.LENGTH_LONG).show();

            if (mResendVerificationButton != null) {
                mResendVerificationButton.setVisibility(View.VISIBLE);
            }
        } else {
            if (mResendVerificationButton != null) {
                mResendVerificationButton.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        String termsText = "By Signing In, you agree to Scavenger's Terms & Conditions and Privacy Policy.";
        SpannableString termsSS = new SpannableString(termsText);
        termsSS.setSpan(new URLSpan(Constants.scavengerTermsURL), 40,58, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        termsSS.setSpan(new URLSpan(Constants.scavengerPrivacyURL), 63,77, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        signInTerms.setText(termsSS);
        signInTerms.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Google Info
        MaterialButton mGoogleSignIn = findViewById(R.id.google_signIn);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()
                .requestEmail()
                .requestIdToken(getString(R.string.clientId_web_googleSignIn))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Facebook Info
        MaterialButton mFacebookSignIn = findViewById(R.id.facebook_signIn);
        callbackManager = CallbackManager.Factory.create();

        mContext = getApplicationContext();

        MaterialButton mAppleSignIn = findViewById(R.id.apple_signIn);

        TopToolbar topToolbar = findViewById(R.id.signIn_toolbar);
        topToolbar.setTitle("Sign In");

        con = new ConnectionDetector(this);

        mAuth = FirebaseAuth.getInstance();
        myDb = DatabaseHelper.getInstance(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mResendVerificationButton = findViewById(R.id.verify_text);
        signInButton = findViewById(R.id.signIn_Button);
        // Views from Sign In activity
        TextView signUpText = findViewById(R.id.signUp_text);
        TextView forgotPass = findViewById(R.id.forgot_signIn);
        emailEdit = findViewById(R.id.email_editText);
        passEdit = findViewById(R.id.password_editText);
        signInTerms = findViewById(R.id.accept_terms_signin);
        progressHolder = findViewById(R.id.signIn_progressHolder);

        mResendVerificationButton.setOnClickListener(v -> {
            hideKeyboard(this);
            if (SystemClock.elapsedRealtime() - mLastClickTime < 5000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            if (mAuth.getCurrentUser() != null) {
                Log.d(TAG, "current user: " + mAuth.getCurrentUser());
                mAuth.getCurrentUser().sendEmailVerification();
                Toast.makeText(this, "A verification has been sent to your email. Please verify your email in order to Sign In", Toast.LENGTH_LONG).show();
            }
        });

        // Sign in with email button ---------------------------------------------------------------
        signInButton.setOnClickListener(v -> {
            hideKeyboard(this);
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(Constants.noInternetTitle)
                        .setMessage(Constants.noInternetMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                progressHolder.setVisibility(View.VISIBLE);
                email = emailEdit.getText().toString();
                pass = passEdit.getText().toString();
                mAuth.signInWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    if (user.isEmailVerified()) {
                                        retrieveLikesFromFirebase(user);
                                        updatePrefInfo(user.getUid());
                                        //sendDataToFirebase(user);
                                    } else {
                                        new MaterialAlertDialogBuilder(SignInActivity.this)
                                                .setTitle("Email Address not Verified")
                                                .setMessage("The Email Address that you have entered has not been verified. Please check your email and verify in order to Sign In. If you continue to have issues, please reach out to Scavenger Support at support@thescavengerapp.com.")
                                                .setPositiveButton("OK", (dialog, which) -> {
                                                    dialog.dismiss();
                                                    progressHolder.setVisibility(View.GONE);
                                                    emailEdit.requestFocus();
                                                })
                                                .create()
                                                .show();
                                    }
                                }
                            } else {
                                Log.w(TAG, "SignInWithEmail:failure", task.getException());
                                new MaterialAlertDialogBuilder(SignInActivity.this)
                                        .setTitle("Invalid Email or Password")
                                        .setMessage("The Email Address or Password that you have used is invalid. Please check typing and try again. If you continue to have issues, please reach out to Scavenger Support at support@thescavengerapp.com.")
                                        .setPositiveButton("OK", (dialog, which) -> {
                                            dialog.dismiss();
                                            progressHolder.setVisibility(View.GONE);
                                            emailEdit.requestFocus();
                                        })
                                        .create()
                                        .show();
                            }
                        });
            }
        });

        // -----------------------------------------------------------------------------------------
        // Email Edit Text Watcher
        emailEdit.addTextChangedListener(signInTextWatcher);
        // Password Edit Text Watcher
        passEdit.addTextChangedListener(signInTextWatcher);

        // Forgot password activity launch textview
        forgotPass.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPassword.class);
            startActivity(intent);
        });

        // No account signup activity launch textview
        signUpText.setTextColor(Color.BLUE);
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
        });

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

        // -----------------------------------------------------------------------------------------

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

        // -----------------------------------------------------------------------------------------

        // Sign in with apple button ---------------------------------------------------------------

        mAppleSignIn.setOnClickListener(v -> {
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(Constants.noInternetTitle)
                        .setMessage(Constants.noInternetMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                appleSignIn();
            }
        });

        // -----------------------------------------------------------------------------------------
    }

    // Sends user information to Firebase
    private void sendDataToFirebase(FirebaseUser user) {
        if (user != null) {
            HashMap<String, Object> data = new HashMap<>();
            data.put("name", name);
            //data.put("email", email);
            db.collection(Constants.firebaseUser).document(user.getUid()).set(data);
        }
    }

    // Facebook Sign In information and Methods -----------------------------------------------------------------------------------------------------------------

    private void facebookSignIn() {
        callbackManager = CallbackManager.Factory.create();

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
                        //toastMessage("Signed in successfully");
                        if (user != null) {
                            name = user.getDisplayName();
                            email = user.getEmail();
                            retrieveLikesFromFirebase(user);
                            sendDataToFirebase(user);
                            updatePrefInfo(user.getUid());
                        }
                        //finish();
                        //progressHolder.setVisibility(View.GONE);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        toastMessage("Authentication Failed. Please try again or reach out to support@theScavengerApp.com for assistance");
                    }
                    /*finish();
                    progressHolder.setVisibility(View.GONE);*/
                });
    }


    // ----------------------------------------------------------------------------------------------------------------------------------------------------------

    // Google Sign In information and Methods -----------------------------------------------------------------------------------------------------------------

    private void googleSignIn() {
        Intent intent = mGoogleSignInClient.getSignInIntent();
        // DEPRECATED
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
                        //toastMessage("Signed in successfully");
                        if (user != null) {
                            name = user.getDisplayName();
                            email = user.getEmail();
                            retrieveLikesFromFirebase(user);
                            sendDataToFirebase(user);
                            updatePrefInfo(user.getUid());
                        }
                        //finish();
                        //progressHolder.setVisibility(View.GONE);
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

    // Apple Sign In information and Methods ------------------------------------------------------------------------------------------------------------------

    private void appleSignIn() {
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
                Log.d(TAG, "appleSignIn");
                FirebaseUser user = authResult.getUser();
                //toastMessage("Signed in successfully");
                if (user != null) {
                    if (user.getDisplayName() == null) {
                        name = "Anonymous";
                    } else {
                        name = user.getDisplayName();
                    }
                    email = user.getEmail();
                    retrieveLikesFromFirebase(user);
                    sendDataToFirebase(user);
                    updatePrefInfo(user.getUid());
                }
                //finish();
                //progressHolder.setVisibility(View.GONE);
            }).addOnFailureListener(e -> {
                toastMessage("Issue Signing in. Please try again");
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
                    //toastMessage("Signed in successfully");
                    if (user != null) {
                        if (user.getDisplayName() == null) {
                            name = "Anonymous";
                        } else {
                            name = user.getDisplayName();
                        }
                        email = user.getEmail();
                        retrieveLikesFromFirebase(user);
                        sendDataToFirebase(user);
                        updatePrefInfo(user.getUid());
                    }
                    //finish();
                    //progressHolder.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    toastMessage("Issue Signing up. Please try again");
                    Log.w(TAG, "activitySignIn:onFailure", e);
                    progressHolder.setVisibility(View.GONE);
                });
    }

    // --------------------------------------------------------------------------------------------------------------------------------------------------------

    // On Activity Result
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode,data);
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            progressHolder.setVisibility(View.VISIBLE);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle: " + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.d(TAG, "Google sign in failed", e);
                progressHolder.setVisibility(View.GONE);
            }
        }
    }

    private void updatePrefInfo(String userId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("logged", true);
        editor.putString("userId", userId);
        editor.putBoolean("refresh", true);
        editor.putBoolean("verify", false);
        editor.apply();

        progressHolder.setVisibility(View.GONE);
        finish();
    }

    private void retrieveLikesFromFirebase(FirebaseUser user) {
        CollectionReference likesRef = db.collection(Constants.firebaseUser).document(user.getUid()).collection(Constants.firebaseLikes);
        likesRef.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    int itemId;
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            itemId = documentSnapshot.getLong("itemId").intValue();
                            myDb.addDataToView(itemId);
                        }
                    }
                });
    }

    //method for creating a Toast
    private void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void checkFieldsForValid() {
        if (emailEdit.getText().toString().trim().isEmpty()) {
            signInButton.setEnabled(false);
        } else {
            if (emailEdit.getText().toString().trim().contains("@")) {
                signInButton.setEnabled(!passEdit.getText().toString().trim().isEmpty());
            } else {
                signInButton.setEnabled(false);
            }
        }
    }

    private final TextWatcher signInTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkFieldsForValid();
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

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

    @Override
    protected void onDestroy() {
        signInTerms.setText("");
        signInTerms.setMovementMethod(null);

        if (callbackManager != null) {
            callbackManager = null;
        }

        super.onDestroy();
    }
}
