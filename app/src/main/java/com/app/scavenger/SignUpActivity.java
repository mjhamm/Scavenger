package com.app.scavenger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private static final int RC_SIGN_UP = 101;
    public static final String TAG = "LOG: ";

    private Context mContext;
    private GoogleSignInClient mGoogleSignUpClient;
    private SharedPreferences sharedPreferences;
    private EditText fullName, emailEdit, passEdit, passConfirmEdit;
    private TextView passNoMatch;
    private MaterialCheckBox termsCheck;
    private FirebaseAuth mAuth;
    private MaterialButton signUpButton, googleSignUpButton, facebookSignUpButton;
    private boolean nameEmpty = true, emailEmpty = true, passEmpty = true, passConfirmEmpty = true;

    // Shared Preferences Data
    //-----------------------------------------
    private String userId = null;
    private boolean logged = false;
    private String name = null;
    private String email = null;
    //------------------------------------------
    private String pass = null;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Toast.makeText(mContext, "Firebase User Logged In" + currentUser.getEmail(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mContext = getApplicationContext();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        getInfoFromSharedPrefs();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.clientId_web_googleSignIn))
                .requestProfile()
                .requestEmail()
                .build();
        mGoogleSignUpClient = GoogleSignIn.getClient(mContext, gso);
        
        mAuth = FirebaseAuth.getInstance();

        fullName = findViewById(R.id.fullName_editText);
        emailEdit = findViewById(R.id.email_editText);
        passEdit = findViewById(R.id.password_editText);
        passConfirmEdit = findViewById(R.id.passwordConfirm_editText);
        passNoMatch = findViewById(R.id.passNoMatch);
        termsCheck = findViewById(R.id.signUpTerms);
        signUpButton = findViewById(R.id.signUp_Button);
        facebookSignUpButton = findViewById(R.id.facebook_signUp);
        googleSignUpButton = findViewById(R.id.google_signUp);

        // Edit Text fields for sign up
        // ------------------------------------------------------------------------------------------------------
        fullName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0) {
                    nameEmpty = true;
                } else {
                    nameEmpty = false;
                }
                checkFieldsForEmpty();
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkFieldsForEmpty();
            }
        });

        emailEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0) {
                    emailEmpty = true;
                } else {
                    emailEmpty = false;
//                    if (!s.toString().trim().contains("@")) {
//                        emailEmpty = true;
//                    } else {
//                        emailEmpty = false;
//
//                    }
                }
                checkFieldsForEmpty();
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
                } else {
                    if (!passEdit.getText().toString().equals(passConfirmEdit.getText().toString())) {
                        passEmpty = true;
                        passNoMatch.setVisibility(View.VISIBLE);
                    } else {
                        passEmpty = false;
                        passNoMatch.setVisibility(View.GONE);
                    }
                }
                checkFieldsForEmpty();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        passConfirmEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0) {
                    passConfirmEmpty = true;
                } else {
                    if (!passEdit.getText().toString().equals(passConfirmEdit.getText().toString())) {
                        passConfirmEmpty = true;
                        passNoMatch.setVisibility(View.VISIBLE);
                    } else {
                        passConfirmEmpty = false;
                        passNoMatch.setVisibility(View.GONE);
                    }
                }
                checkFieldsForEmpty();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // ------------------------------------------------------------------------------------------------------


        googleSignUpButton.setOnClickListener(v -> {
            googleSignUp();
        });
        
        signUpButton.setOnClickListener(v -> {
            name = fullName.getText().toString();
            email = emailEdit.getText().toString();
            pass = passEdit.getText().toString();
            if (!email.isEmpty() && !pass.isEmpty()) {
                mAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Map<String, Object> data = new HashMap<>();
                                    //data.put("userId", mAuth.getCurrentUser().getIdToken(true));
                                    data.put("name", name);
                                    data.put("email", email);
                                    // Sign in success
                                } else {
                                    // Sign in failed
                                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                    Toast.makeText(SignUpActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }

        });

    }

    private void googleSignUp() {
        Intent signInIntent = mGoogleSignUpClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_UP);
    }

    private void handleSignUpResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            if (account != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("logged", true);
                editor.putString("userId", account.getId());
                editor.putString("name", account.getDisplayName());
                editor.putString("email", account.getEmail());
                editor.apply();
                userId = account.getId();
                name = account.getDisplayName();
                email = account.getEmail();
                finish();
                sendDataToFirestore(userId, name, email);
            }

            //Sign in successful
        } catch (ApiException e) {
            Log.w(TAG, "signUpResult:failed code=" + e.getStatusCode());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_UP) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignUpResult(task);
        }
    }

    private void sendDataToFirestore(String userId, String name, String email) {
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

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", null);
        email = sharedPreferences.getString("email", null);
        name = sharedPreferences.getString("name", null);
    }

    private void checkFieldsForEmpty() {
        if (!nameEmpty && !emailEmpty && !passEmpty && !passConfirmEmpty) {
            if (!emailEdit.getText().toString().contains("@")) {
                signUpButton.setEnabled(false);
            } else {
                signUpButton.setEnabled(true);
            }
//            if (!passEdit.getText().toString().equals(passConfirmEdit.getText().toString())) {
//                signUpButton.setEnabled(false);
//            } else {
//                signUpButton.setEnabled(true);
//            }
        } else {
            signUpButton.setEnabled(false);
        }
    }
}
