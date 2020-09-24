package com.app.scavenger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
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
import com.facebook.shimmer.ShimmerFrameLayout;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class GroceryListActivity extends AppCompatActivity {

    private final int RC_SIGN_IN = 101;
    public static final String TAG = "GroceryListActivity";

    private ConstraintLayout mSignInLayout;
    private MaterialButton mEmailSignIn, mGoogleSignIn, mFacebookSignIn;
    private TextView mSignUpText;
    private FrameLayout progressHolder, mBottomBar;
    private GroceryListAdapter adapter;
    private RecyclerView mGroceryRecyclerView;
    private SharedPreferences sharedPreferences;
    private TextView terms, mAddItemButton;
    private FirebaseAuth mAuth;
    private ConnectionDetector con;
    private ArrayList<GroceryListItem> groceryItemsList;
    private ArrayList<String> groceryItemsFromFB;
    private LinearLayoutManager mLayoutManager;
    private CallbackManager callbackManager;
    private GoogleSignInClient mGoogleSignInClient;
    private ImageButton mMoreButton, mDeleteSelectedItems, mBackButton;
    private ShimmerFrameLayout mShimmerLayout;
    private View mHorizontalBar;

    // Shared Preferences Data
    //-----------------------------------------
    private String userId = null;
    private boolean logged = false;
    //------------------------------------------

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onStart() {
        super.onStart();

        String termsText = "By Signing In, you agree to Scavenger's Terms & Conditions and Privacy Policy.";
        SpannableString termsSS = new SpannableString(termsText);
        termsSS.setSpan(new URLSpan(Constants.scavengerTermsURL), 40,58, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        termsSS.setSpan(new URLSpan(Constants.scavengerPrivacyURL), 63,77, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        terms.setText(termsSS);
        terms.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update to the status bar on lower SDK's
        // Makes bar on lower SDK's black with white icons
        /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            this.getWindow().setStatusBarColor(getResources().getColor(android.R.color.black));
        }*/

        setContentView(R.layout.activity_grocery_list);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        con = new ConnectionDetector(this);
        mAuth = FirebaseAuth.getInstance();

        mMoreButton = findViewById(R.id.list_clear);
        mAddItemButton = findViewById(R.id.addCustomItemRow);
        terms = findViewById(R.id.accept_terms_signin);
        mSignUpText = findViewById(R.id.signUp_text);
        mGroceryRecyclerView = findViewById(R.id.grocery_recyclerView);
        mEmailSignIn = findViewById(R.id.signIn_Button);
        mSignInLayout = findViewById(R.id.signIn_layout);
        progressHolder = findViewById(R.id.signIn_progressHolder);
        mShimmerLayout = findViewById(R.id.grocery_shimmerLayout);
        mDeleteSelectedItems = findViewById(R.id.delete_selectedItems_button);
        mBackButton = findViewById(R.id.list_back);
        mHorizontalBar = findViewById(R.id.horiz_bar);

        mShimmerLayout.setVisibility(View.GONE);

        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        // List of Grocery List items
        groceryItemsList = new ArrayList<>();

        groceryItemsFromFB = new ArrayList<>();

        getInfoFromSharedPrefs();

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
        closeButton.setOnClickListener(v -> {
            finish();
        });

        mBackButton.setOnClickListener(v -> {
            mDeleteSelectedItems.setVisibility(View.GONE);
            for (GroceryListItem item : groceryItemsList) {
                item.setmGroceryItemTapped(false);
                item.setShowSelectItems(false);
            }
            mBackButton.setVisibility(View.INVISIBLE);
            mBackButton.setClickable(false);
            closeButton.setClickable(true);
            closeButton.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        });

        // Open up sign up activity
        mSignUpText.setTextColor(Color.BLUE);
        mSignUpText.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));

        hideLayouts();

        mDeleteSelectedItems.setOnClickListener(v -> {
            Iterator<GroceryListItem> iterator = groceryItemsList.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getmGroceryItemSelected()) {
                    iterator.remove();
                }
            }
            mDeleteSelectedItems.setVisibility(View.GONE);

            for (GroceryListItem item : groceryItemsList) {
                item.setShowSelectItems(false);
            }
            mBackButton.setVisibility(View.INVISIBLE);
            mBackButton.setClickable(false);
            closeButton.setClickable(true);
            closeButton.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        });

        mAddItemButton.setOnClickListener(v -> {
            addCustomItem();
        });

        mMoreButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, mMoreButton);
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.action_sendlist:
                        Log.d(TAG, "SEND LIST");
                        break;
                    case R.id.action_selectitems:
                        Log.d(TAG, "SELECT ITEMS");

                        if (!groceryItemsList.isEmpty()) {
                            closeButton.setClickable(false);
                            closeButton.setVisibility(View.INVISIBLE);

                            mBackButton.setClickable(true);
                            mBackButton.setVisibility(View.VISIBLE);
                            mDeleteSelectedItems.setVisibility(View.VISIBLE);

                            for (GroceryListItem groceryListItem : groceryItemsList) {
                                groceryListItem.setmGroceryItemTapped(false);
                                groceryListItem.setShowSelectItems(true);
                            }
                            adapter.notifyDataSetChanged();
                        }
                        break;
                    case R.id.action_clearlist:
                        // clear list
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Clear your Grocery List?")
                                .setMessage("This will remove all items in your list. Are you sure you want to continue?")
                                .setCancelable(false)
                                .setPositiveButton("Clear", (dialog, which) -> clearList())
                                .setNegativeButton("Cancel", ((dialog, which) -> dialog.cancel()))
                                .create()
                                .show();
                        break;
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

        //hideLayouts();
    }

    // hide or show layouts based on whether the user is logged in or not
    private void hideLayouts() {
        // check shared preferences to see if the user is logged in or not and set variable "logged"
        //boolean logged = sharedPreferences.getBoolean("logged", false);

        // Set the visibility of layouts based on whether or not the user is logged in or not
        if (logged) {
            mAddItemButton.setVisibility(View.VISIBLE);
            mSignInLayout.setVisibility(View.GONE);
            mMoreButton.setVisibility(View.VISIBLE);
            mHorizontalBar.setVisibility(View.VISIBLE);
            if (mAuth.getCurrentUser() != null) {
                retrieveListFromFirebase(mAuth.getCurrentUser());
            }
        } else {
            if (adapter != null) {
                adapter = null;
            }
            groceryItemsList.clear();
            mGroceryRecyclerView.setVisibility(View.GONE);
            mAddItemButton.setVisibility(View.GONE);
            mSignInLayout.setVisibility(View.VISIBLE);
            mHorizontalBar.setVisibility(View.GONE);
            mMoreButton.setVisibility(View.GONE);
        }
    }

    private void clearList() {
        // clear all items in grocery list
        groceryItemsList.clear();
        if (adapter != null) {
            adapter = new GroceryListAdapter(this, groceryItemsList, groceryItemsFromFB, userId);
        }
        mGroceryRecyclerView.setAdapter(adapter);
    }

    private void addCustomItem() {
        // Add custom item to grocery list
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Add a Custom Item?");
        builder.setMessage("Add your own custom item to your Grocery List");
        final View customView = getLayoutInflater().inflate(R.layout.custom_grocery_item, null);
        builder.setView(customView);

        builder.setPositiveButton("Add Custom Item", (dialog, which) -> {
            EditText editText = customView.findViewById(R.id.grocery_item_custom);

            if (!editText.getText().toString().trim().isEmpty()) {
                GroceryListItem groceryListItem = new GroceryListItem();
                groceryListItem.setGroceryItemName(editText.getText().toString());
                groceryItemsList.add(0, groceryListItem);
                sendGroceryItemToFirebase(editText.getText().toString());
                Toast.makeText(this, "Added " + editText.getText().toString() + " to list", Toast.LENGTH_SHORT).show();
                adapter.notifyDataSetChanged();
            }
        });

        builder.setNegativeButton("Cancel", ((dialog, which) -> dialog.cancel()));
        builder.create();
        builder.show();
    }

    private void sendGroceryItemToFirebase(String customItem) {

        groceryItemsFromFB.add(0, customItem);

        HashMap<String, Object> itemMap = new HashMap<>();

        CollectionReference groceryRef = db.collection(Constants.firebaseUser).document(userId).collection("GroceryList");

        itemMap.put("items", groceryItemsFromFB);

        groceryRef.document(userId).set(itemMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Grocery item saved to Firebase");
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Error saving grocery item to Firebase : " + e.toString());
                });
    }

    private void retrieveListFromFirebase(FirebaseUser user) {

        mShimmerLayout.setVisibility(View.VISIBLE);

        CollectionReference groceryRef = db.collection(Constants.firebaseUser).document(user.getUid()).collection("GroceryList");
        groceryRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Grocery List is empty on Firebase");
                    } else {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {

                            if (documentSnapshot.exists()) {
                                groceryItemsFromFB = (ArrayList<String>) documentSnapshot.get("items");
                            }

                        }

                        for (String item : groceryItemsFromFB) {
                            GroceryListItem groceryListItem = new GroceryListItem();
                            groceryListItem.setGroceryItemName(item);
                            groceryItemsList.add(groceryListItem);
                        }
                        mShimmerLayout.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {

                });

        adapter = new GroceryListAdapter(this, groceryItemsList, groceryItemsFromFB, userId);
        mGroceryRecyclerView.setLayoutManager(mLayoutManager);
        mGroceryRecyclerView.setAdapter(adapter);

        mGroceryRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        mGroceryRecyclerView.setVisibility(View.VISIBLE);
    }

    private void updatePrefInfo(String userId) {

        logged = true;
        this.userId = userId;

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

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        logged = sharedPreferences.getBoolean("logged", false);
        userId = sharedPreferences.getString("userId", "");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        terms.setText("");
        terms.setMovementMethod(null);

        if (callbackManager != null) {
            callbackManager = null;
        }

        groceryItemsFromFB.clear();
        groceryItemsList.clear();
    }
}