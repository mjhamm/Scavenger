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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;


public class SignUpFragment extends Fragment {

    private static final int RC_SIGN_UP = 102;
    public static final String TAG = "LOG: ";

    private Context mContext;
    private GoogleSignInClient mGoogleSignUpClient;
    private OnSignUpFragmentInteractionListener mListener;
    private String mName = null;
    private String mEmail = null;
    private boolean isLogged = false;

    public SignUpFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignUpClient = GoogleSignIn.getClient(mContext, gso);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);

        AppCompatButton mGoogleSignUp = view.findViewById(R.id.google_signUp);

        mGoogleSignUp.setOnClickListener(v -> {
            googleSignUp();
        });

        return view;
    }

    private void googleSignUp() {
        Intent signInIntent = mGoogleSignUpClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_UP);
    }

    private void handleSignUpResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            if (account != null) {
                mName = account.getDisplayName();
                mEmail = account.getEmail();
                isLogged = true;
            }
            mListener.messageFromSignUpToParent(mName, mEmail,isLogged);
            //Sign in successful
        } catch (ApiException e) {
            Log.w(TAG, "signUpResult:failed code=" + e.getStatusCode());
        }
    }

    public interface OnSignUpFragmentInteractionListener {
        void messageFromSignUpToParent(String name, String email, boolean isLogged);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_UP) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignUpResult(task);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // check if parent Fragment implements listener
        if (getParentFragment() instanceof OnSignUpFragmentInteractionListener) {
            mListener = (OnSignUpFragmentInteractionListener) getParentFragment();
        } else {
            throw new RuntimeException("The parent fragment must implement OnSignUpFragmentInteractionListener");
        }
    }
}
