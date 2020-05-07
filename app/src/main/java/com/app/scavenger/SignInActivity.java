package com.app.scavenger;

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
import androidx.appcompat.widget.AppCompatButton;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignInActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 101;
    private static final String TAG = "LOG: ";

    private Context mContext;
    private GoogleSignInClient mGoogleSignInClient;
    private TextView signUpText, forgotPass, signInTerms;
    private MaterialButton signInButton;
    private EditText emailEdit, passEdit;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private boolean emailEmpty = true, passEmpty = true;

    // Shared Preferences Data
    //-----------------------------------------
    private String userId = null;
    private boolean logged = false;
    private String name = null;
    private String email = null;
    //------------------------------------------
    private String pass = null;

    public SignInActivity() {
        // Required empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();
        
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        mContext = getApplicationContext();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()
                .requestEmail()
                .requestIdToken(getString(R.string.clientId_web_googleSignIn))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso);

        mAuth = FirebaseAuth.getInstance();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        AppCompatButton mGoogleSignIn = findViewById(R.id.google_signIn);
        signInButton = findViewById(R.id.signIn_Button);
        signUpText = findViewById(R.id.signUp_text);
        forgotPass = findViewById(R.id.forgot_signIn);
        emailEdit = findViewById(R.id.email_editText);
        passEdit = findViewById(R.id.password_editText);
        signInTerms = findViewById(R.id.accept_terms_signin);

        String termsText = "By Signing In, you agree to Scavenger's Terms & Conditions and Privacy Policy.";
        SpannableString termsSS = new SpannableString(termsText);

        ClickableSpan clickableSpanTerms = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Toast.makeText(mContext, "Terms & Conditions", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(mContext, "Privacy Policy", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.BLUE);
                ds.setUnderlineText(false);
            }
        };

        termsSS.setSpan(clickableSpanTerms, 39, 58, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        termsSS.setSpan(clickableSpanPrivacy, 62, 76, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        signInTerms.setText(termsSS);
        signInTerms.setMovementMethod(LinkMovementMethod.getInstance());
        
        signInButton.setOnClickListener(v -> {
            email = emailEdit.getText().toString();
            pass = passEdit.getText().toString();
            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                Toast.makeText(mContext, "Sign In Successful", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.w(TAG, "SignInWithEmail:failure", task.getException());
                                Toast.makeText(mContext, "Sign in Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

        emailEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0) {
                    emailEmpty = true;
                    signInButton.setEnabled(false);
                } else {
                    if (!s.toString().trim().contains("@")) {
                        emailEmpty = true;
                        signInButton.setEnabled(false);
                    } else {
                        emailEmpty = false;
                        if (!passEmpty) {
                            signInButton.setEnabled(true);
                        } else {
                            signInButton.setEnabled(false);
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        passEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0) {
                    passEmpty = true;
                    signInButton.setEnabled(false);
                } else {
                    passEmpty = false;
                    if (!emailEmpty) {
                        signInButton.setEnabled(true);
                    } else {
                        signInButton.setEnabled(false);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Forgot password activity launch textview
        forgotPass.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, ForgotPassword.class);
            startActivity(intent);
        });

        // No account signup activity launch textview
        signUpText.setTextColor(Color.BLUE);

        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, SignUpActivity.class);
            startActivity(intent);
            finish();
        });

        mGoogleSignIn.setOnClickListener(v -> {
            if (!checkConnection()) {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle("No Internet Connection")
                        .setMessage("You don't have an internet connection. Please reconnect and try to Sign In again.")
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
    }

    // Sends user information to Firebase
    private void sendDataToFirebase(String userId, String name, String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("name", name);
        data.put("email", email);
        db.collection("Users").whereEqualTo("userId", userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().getDocuments().size() == 0) {
                            db.collection("Users").document(userId).set(data);
                        }
                    }
                });
    }



    // Google Sign In information and Methods -----------------------------------------------------------------------------------------------------------------

    private void googleSignIn() {
        Intent intent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("logged", true);
                editor.putString("userId", account.getId());
                editor.putString("name", account.getDisplayName());
                editor.putString("email", account.getEmail());
                editor.apply();
                userId = account.getId();
                name = account.getDisplayName();
                email = account.getEmail();
                Toast.makeText(mContext, "Successfully Signed In", Toast.LENGTH_SHORT).show();
                finish();
                sendDataToFirebase(userId, name, email);
            }
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
        }
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
                            //updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(mContext, "Authentication Failed", Toast.LENGTH_SHORT).show();
                            //updateUI(null);
                        }

                        // ...
                    }
                });
    }

    // --------------------------------------------------------------------------------------------------------------------------------------------------------

    // On Activity Result
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    //boolean that returns true if you are connected to internet and false if not
    private boolean checkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
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
