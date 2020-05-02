package com.app.scavenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

// Activity that houses the information about when a user signs in or signs up inside of Scavenger

public class Account extends Fragment implements SignInFragment.OnChildFragmentInteractionListener, SignUpFragment.OnSignUpFragmentInteractionListener {

    private static final String TAG = "LOG: ";

    private Context mContext;
    private TextView mName, mEmail, mAccountName, mTextSignInUp;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private FrameLayout accountContainer;
    private FragmentManager fm;
    private Fragment fragment_signIn;
    private Fragment fragment_signUp;
    private GoogleSignInClient mGoogleSignInClient;
    private ImageButton backButton;
    private String mUserId = null;
    private String mNameText = null;
    private String mEmailText = null;
    private boolean isLogged = false;
    private RelativeLayout mAccountRL;
    private SendDataToMain sendDataToMain;

    // Creates a new instance of the Account Fragment and passes arguments to the fragment when it is initialized
    static Account newInstance(String userId, String name, String email, boolean logged) {
        Account account = new Account();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putString("name", name);
        args.putString("email", email);
        args.putBoolean("logged", logged);
        account.setArguments(args);
        return account;
    }

    // Interface that sends login data from the Account Activity to Parent Activity (Main)
    public interface SendDataToMain {
        void getLoginData(String userId, boolean logged);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        //viewPager = view.findViewById(R.id.login_viewPager);
        //tabLayout = view.findViewById(R.id.login_tabLayout);
        accountContainer = view.findViewById(R.id.account_fragment_container);
        ImageButton settingsButton = view.findViewById(R.id.settings_button);
        MaterialButton mLogoutButton = view.findViewById(R.id.logout_button);
        mName = view.findViewById(R.id.name_mainText);
        mEmail = view.findViewById(R.id.email_mainText);
        backButton = view.findViewById(R.id.account_backButton);
        mTextSignInUp = view.findViewById(R.id.text_signInUp);
        mAccountName = view.findViewById(R.id.account_name);
        mAccountRL = view.findViewById(R.id.account_relativeLayout);

        fm = getChildFragmentManager();
        fragment_signIn = new SignInFragment();
        fragment_signUp = new SignUpFragment();

        //GOOGLE INFORMATION -------------------------------------------------------
        // Requests the information from Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.clientId_web_googleSignIn))
                .requestProfile()
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso);

        //--------------------------------------------------------------------------

        // Checks to see if a user has logged in with Google prior to this session
        try {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mContext);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        // Checks information sent to Account from Sign In / Sign Up Activities
        // If user is logged in, show information related to signed in user
        // If user is not logged in, show information to allow user to sign in / sign up
        getDataFromSignIn();

        // Setup view pager with sign in / sign up fragments
        setUpContainer();
        //setupViewPager(viewPager);
        //tabLayout.setupWithViewPager(viewPager);

        // Settings button that opens up Preferences Fragment
        settingsButton.setOnClickListener(v -> startActivity(new Intent(mContext, SettingsActivity.class)));

        backButton.setOnClickListener(v -> {
            if (!fragment_signIn.isHidden()) {
                backButton.setVisibility(View.VISIBLE);
                fm.beginTransaction().hide(fragment_signIn).addToBackStack(null).commit();
                fm.beginTransaction().show(fragment_signUp).commit();
            } else {
                backButton.setVisibility(View.GONE);
                fm.beginTransaction().hide(fragment_signUp).commit();
                fm.beginTransaction().show(fragment_signIn).commit();
            }
        });

        // Click listener on Account Name TextView that opens to show account information
        mAccountName.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, AccountInfo.class);
            intent.putExtra("userId", mUserId);
            startActivity(intent);
        });

        // Log Out button that is shown when user is logged in
        mLogoutButton.setOnClickListener(v -> {
            // Prompts the user to determine if they really wanted to logout
            logoutDialog();
        });

        return view;
    }

    public void replaceContainer() {
        Log.e(TAG, "" + fragment_signIn.isHidden());
        if (!fragment_signIn.isHidden()) {
            backButton.setVisibility(View.VISIBLE);
            fm.beginTransaction().hide(fragment_signIn).addToBackStack(null).commit();
            fm.beginTransaction().show(fragment_signUp).commit();
        } else {
            backButton.setVisibility(View.GONE);
            fm.beginTransaction().hide(fragment_signUp).commit();
            fm.beginTransaction().show(fragment_signIn).commit();
        }
    }

    // On Resume will check to see if the user is logged in and will hide / show the corresponding information
    @Override
    public void onResume() {
        super.onResume();
        if (isLogged) {
            hideForLogin();
        } else {
            showForLogin();
        }
        // Sends the information to the Parent Activity (Main)
        sendDataToMain.getLoginData(mUserId, isLogged);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getActivity() instanceof SendDataToMain) {
            sendDataToMain = (SendDataToMain) getActivity();
        } else {
            throw new RuntimeException("The parent fragment must implement SendDataToMain");
        }
    }

    private void hideForLogin() {
        //viewPager.setVisibility(View.GONE);
        //tabLayout.setVisibility(View.GONE);
        accountContainer.setVisibility(View.GONE);
        mAccountRL.setVisibility(View.VISIBLE);
        mName.setText(mNameText);
        mEmail.setText(mEmailText);
        mTextSignInUp.setText("My Account");
    }

    private void showForLogin() {
        //viewPager.setVisibility(View.VISIBLE);
        //tabLayout.setVisibility(View.VISIBLE);
        accountContainer.setVisibility(View.VISIBLE);
        mAccountRL.setVisibility(View.GONE);
        mTextSignInUp.setText("Sign In");
    }

    private void getDataFromSignIn() {
        try {
            if (getArguments() != null) {
                mUserId = getArguments().getString("userId");
                mNameText = getArguments().getString("name");
                mEmailText = getArguments().getString("email");
                isLogged = getArguments().getBoolean("logged");
            }
            if (isLogged) {
                hideForLogin();
            } else {
                showForLogin();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void setUpContainer() {
        fm.beginTransaction().replace(R.id.account_fragment_container, fragment_signIn, "signIn").commit();
        fm.beginTransaction().add(R.id.account_fragment_container, fragment_signUp, "signUp").hide(fragment_signUp).commit();
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
                    isLogged = false;
                    mNameText = null;
                    mEmailText = null;
                    showForLogin();
                    sendDataToMain.getLoginData(mUserId, isLogged);
                });
    }

    @Override
    public void messageFromChildToParent(String userId, String name, String email, boolean isLogged) {
        this.mUserId = userId;
        this.mNameText = name;
        this.mEmailText = email;
        this.isLogged = isLogged;
    }

    @Override
    public void messageFromSignUpToParent(String userId, String name, String email, boolean isLogged) {
        this.mUserId = userId;
        this.mNameText = name;
        this.mEmailText = email;
        this.isLogged = isLogged;
    }
}
