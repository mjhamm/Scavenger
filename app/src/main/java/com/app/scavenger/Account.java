package com.app.scavenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import static android.view.View.GONE;

// Activity that houses the information about when a user signs in or signs up inside of Scavenger

public class Account extends Fragment implements SignInFragment.OnChildFragmentInteractionListener, SignUpFragment.OnSignUpFragmentInteractionListener {

    private static final String TAG = "LOG: ";

    private Context mContext;
    private ImageButton settingsButton;
    private MaterialButton mLogoutButton;
    private TextView mName, mEmail, mAccountName, mTextSignInUp;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private GoogleSignInAccount account;
    private GoogleSignInClient mGoogleSignInClient;
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

        // Checks to see if a user has logged in with Google prior to this session
        try {
            account = GoogleSignIn.getLastSignedInAccount(mContext);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        viewPager = view.findViewById(R.id.login_viewPager);
        tabLayout = view.findViewById(R.id.login_tabLayout);
        settingsButton = view.findViewById(R.id.settings_button);
        mLogoutButton = view.findViewById(R.id.logout_button);
        mName = view.findViewById(R.id.name_mainText);
        mEmail = view.findViewById(R.id.email_mainText);
        mTextSignInUp = view.findViewById(R.id.text_signInUp);
        mAccountName = view.findViewById(R.id.account_name);
        mAccountRL = view.findViewById(R.id.account_relativeLayout);

        //GOOGLE INFORMATION -------------------------------------------------------
        // Requests the information from Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.clientId_web_googleSignIn))
                .requestProfile()
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso);

        //--------------------------------------------------------------------------

        // Checks information sent to Account from Sign In / Sign Up Activities
        // If user is logged in, show information related to signed in user
        // If user is not logged in, show information to allow user to sign in / sign up
        getDataFromSignIn();

        // Setup view pager with sign in / sign up fragments
        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager);

        // Settings button that opens up Preferences Fragment
        settingsButton.setOnClickListener(v -> startActivity(new Intent(mContext, SettingsActivity.class)));

        // Click listener on Account Name TextView that opens to show account information
        mAccountName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, AccountInfo.class);
                intent.putExtra("userId", mUserId);
                startActivity(intent);
            }
        });

        // Log Out button that is shown when user is logged in
        mLogoutButton.setOnClickListener(v -> {
            // Prompts the user to determine if they really wanted to logout
            logoutDialog();
        });

        return view;
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
        viewPager.setVisibility(GONE);
        tabLayout.setVisibility(GONE);
        mTextSignInUp.setVisibility(GONE);
        mAccountRL.setVisibility(View.VISIBLE);
        mAccountName.setVisibility(View.VISIBLE);
        mAccountName.setText(mNameText);
        mName.setText(mNameText);
        mEmail.setText(mEmailText);
    }

    private void showForLogin() {
        viewPager.setVisibility(View.VISIBLE);
        tabLayout.setVisibility(View.VISIBLE);
        mTextSignInUp.setVisibility(View.VISIBLE);
        mAccountRL.setVisibility(GONE);
        mAccountName.setVisibility(GONE);
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

    private void setupViewPager(ViewPager viewPager) {
        AccountPagerAdapter adapter = new AccountPagerAdapter(getChildFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        adapter.addFragment(new SignInFragment(), "Sign In");
        adapter.addFragment(new SignUpFragment(), "Sign Up");
        viewPager.setAdapter(adapter);
    }

    private void logoutDialog() {
        new MaterialAlertDialogBuilder(mContext)
                .setTitle("Are you sure you want to Log Out?")
                .setCancelable(false)
                .setPositiveButton("Log Out", (dialog, which) -> {
                    //LOG OUT
                    signOut();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.cancel();
                })
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
