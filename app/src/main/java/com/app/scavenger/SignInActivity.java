package com.app.scavenger;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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
import com.google.android.gms.tasks.OnCompleteListener;
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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SignInActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 101;
    private static final String TAG = "SIGN_IN_ACTIVITY: ";
    private static final String EMAIL = "email";

    private MaterialButton signInButton;
    private EditText emailEdit, passEdit;
    private FrameLayout progressHolder;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private String email = null, pass = null;
    private DatabaseHelper myDb;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager callbackManager;
    private ConnectionDetector con;

    // Shared Preferences Data
    //-----------------------------------------
//    private String userId = null;
//    private boolean logged = false;
    //------------------------------------------

    public SignInActivity() {
        // Required empty public constructor
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

        con = new ConnectionDetector(this);
        //getInfoFromSharedPrefs();

        mAuth = FirebaseAuth.getInstance();
        myDb = DatabaseHelper.getInstance(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        signInButton = findViewById(R.id.signIn_Button);
        // Views from Sign In activity
        TextView signUpText = findViewById(R.id.signUp_text);
        TextView forgotPass = findViewById(R.id.forgot_signIn);
        emailEdit = findViewById(R.id.email_editText);
        passEdit = findViewById(R.id.password_editText);
        TextView signInTerms = findViewById(R.id.accept_terms_signin);
        progressHolder = findViewById(R.id.signIn_progressHolder);
        ImageButton backButton = findViewById(R.id.signIn_back);

        // CHECK: Let search fragment know to reload on sign in

        backButton.setOnClickListener(v -> {
            finish();
        });

        String termsText = "By Signing In, you agree to Scavenger's Terms & Conditions and Privacy Policy.";
        SpannableString termsSS = new SpannableString(termsText);

        ClickableSpan clickableSpanTerms = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Toast.makeText(SignInActivity.this, "Terms & Conditions", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.BLUE);
                ds.setUnderlineText(false);
            }
        };

        ClickableSpan clickableSpanPrivacy = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Toast.makeText(SignInActivity.this, "Privacy Policy", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.BLUE);
                ds.setUnderlineText(false);
            }
        };

        termsSS.setSpan(clickableSpanTerms, 39, 58, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        termsSS.setSpan(clickableSpanPrivacy, 62, 77, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        signInTerms.setText(termsSS);
        signInTerms.setMovementMethod(LinkMovementMethod.getInstance());

        // Sign in with email button ---------------------------------------------------------------
        signInButton.setOnClickListener(v -> {
            hideKeyboard(SignInActivity.this);
            if (!con.isConnectingToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("No Internet connection found.")
                        .setMessage("You don't have an Internet connection. Please reconnect and try to Sign In again.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
            } else {
                progressHolder.setVisibility(View.VISIBLE);
                email = emailEdit.getText().toString();
                pass = passEdit.getText().toString();
                mAuth.signInWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    Toast.makeText(SignInActivity.this, "Sign In Successful", Toast.LENGTH_SHORT).show();
                                    if (user != null) {
                                        retrieveLikesFromFirebase(user);
                                        updatePrefInfo(true, user.getUid());
                                        sendDataToFirebase(user);
                                    }
                                    finish();
                                    progressHolder.setVisibility(View.GONE);
                                } else {
                                    Log.w(TAG, "SignInWithEmail:failure", task.getException());
                                    new MaterialAlertDialogBuilder(SignInActivity.this)
                                            .setTitle("Invalid Email or Password.")
                                            .setMessage("The Email Address or Password that you have used is invalid. Please check typing and try again. If you continue to have issues, please reach out to Scavenger Support at support@thescavengerapp.com.")
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                    progressHolder.setVisibility(View.GONE);
                                                    emailEdit.requestFocus();
                                                }
                                            })
                                            .create()
                                            .show();
                                }
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
            finish();
        });

        // Sign in with Facebook Button ------------------------------------------------------------
        mFacebookSignIn.setOnClickListener(v ->{
            if (!con.isConnectingToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("No Internet connection found.")
                        .setMessage("You don't have an Internet connection. Please reconnect and try to Sign In again.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
            } else {
                facebookSignIn();
            }
        });

        // -----------------------------------------------------------------------------------------

        // Sign in with google button --------------------------------------------------------------
        mGoogleSignIn.setOnClickListener(v -> {
            if (!con.isConnectingToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("No Internet connection found.")
                        .setMessage("You don't have an Internet connection. Please reconnect and try to Sign In again.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
            } else {
                googleSignIn();
            }
        });

        // -----------------------------------------------------------------------------------------
    }

    // Sends user information to Firebase
    private void sendDataToFirebase(FirebaseUser user) {
        if (user != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", user.getDisplayName());
            data.put("email", user.getEmail());
            db.collection("Users").whereEqualTo("userId", user.getUid()).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().getDocuments().size() == 0) {
                                db.collection("Users").document(user.getUid()).set(data);
                            }
                        }
                    });
        }
    }

    // Facebook Sign In information and Methods -----------------------------------------------------------------------------------------------------------------

    private void facebookSignIn() {
        callbackManager = CallbackManager.Factory.create();

        // Set Permissions
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList(EMAIL, "public_profile"));

        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                progressHolder.setVisibility(View.VISIBLE);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(SignInActivity.this, "Signed In Successfully", Toast.LENGTH_SHORT).show();
                            if (user != null) {
                                retrieveLikesFromFirebase(user);
                                updatePrefInfo(true, user.getUid());
                                sendDataToFirebase(user);
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(SignInActivity.this, "Authentication Failed. Please try again or reach out to support@theScavengerApp.com for assistance.", Toast.LENGTH_SHORT).show();
                        }
                        finish();
                        progressHolder.setVisibility(View.GONE);
                    }
                });
    }


    // ----------------------------------------------------------------------------------------------------------------------------------------------------------

    // Google Sign In information and Methods -----------------------------------------------------------------------------------------------------------------

    private void googleSignIn() {
        Intent intent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    // Authenticate Google Sign In with Firebase to make sure user exists and then can sign in
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(SignInActivity.this, "Signed In Successfully", Toast.LENGTH_SHORT).show();
                            if (user != null) {
                                retrieveLikesFromFirebase(user);
                                updatePrefInfo(true, user.getUid());
                                sendDataToFirebase(user);
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(SignInActivity.this, "Authentication Failed. Please try again or reach out to support@theScavengerApp.com for assistance.", Toast.LENGTH_SHORT).show();
                        }
                        finish();
                        progressHolder.setVisibility(View.GONE);
                    }
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

    private void updatePrefInfo(boolean logged, String userId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("logged", logged);
        editor.putString("userId", userId);
        editor.apply();
    }

    private void retrieveLikesFromFirebase(FirebaseUser user) {
        CollectionReference favoritesRef = db.collection("Users").document(user.getUid()).collection("Favorites");
        favoritesRef.get()
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

    private void checkFieldsForValid() {
        if (emailEdit.getText().toString().trim().isEmpty()) {
            signInButton.setEnabled(false);
        } else {
            if (emailEdit.getText().toString().trim().contains("@")) {
                if (!passEdit.getText().toString().trim().isEmpty()) {
                    signInButton.setEnabled(true);
                } else {
                    signInButton.setEnabled(false);
                }
            } else {
                signInButton.setEnabled(false);
            }
        }
    }

    private TextWatcher signInTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkFieldsForValid();
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    public static void hideKeyboard(Activity activity) {
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
}
