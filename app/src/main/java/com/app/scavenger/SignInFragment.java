package com.app.scavenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;

public class SignInFragment extends Fragment implements GoogleApiClient.OnConnectionFailedListener {

    private static final int RC_SIGN_IN = 101;
    private static final String TAG = "LOG: ";

    private Context mContext;
    private AppCompatButton mGoogleSignIn;
    private GoogleSignInClient mGooleSignInClient;
    private GoogleApiClient mGoogleApiClient;
    private String mName = null;
    private String mEmail = null;
    private boolean isLogged = false;
    private OnChildFragmentInteractionListener mParentListener;

    public SignInFragment() {
        // Required empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGooleSignInClient = GoogleSignIn.getClient(mContext, gso);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sign_in, container, false);

        mGoogleSignIn = view.findViewById(R.id.google_signIn);

        mGoogleSignIn.setOnClickListener(v -> {
            googleSignIn();
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void googleSignIn() {
        Intent signInIntent = mGooleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            if (account != null) {
                mName = account.getDisplayName();
                mEmail = account.getEmail();
                isLogged = true;
            }

            Log.e(TAG, "Name: " + mName + " Email: " + mEmail + " Logged: " + isLogged);
            mParentListener.messageFromChildToParent(isLogged, mName, mEmail);
            //Sign in successful
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
        }
    }

    public interface OnChildFragmentInteractionListener {
        void messageFromChildToParent(boolean isLogged, String name, String email);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // check if parent Fragment implements listener
        if (getParentFragment() instanceof OnChildFragmentInteractionListener) {
            mParentListener = (OnChildFragmentInteractionListener) getParentFragment();
        } else {
            throw new RuntimeException("The parent fragment must implement OnChildFragmentInteractionListener");
        }
    }
}
