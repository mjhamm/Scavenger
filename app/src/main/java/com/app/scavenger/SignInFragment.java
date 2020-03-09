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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInFragment extends Fragment {

    private static final int RC_SIGN_IN = 101;
    private static final String TAG = "LOG: ";

    private Context mContext;
    private GoogleSignInClient mGoogleSignInClient;
    private String mUserId = null;
    private String mName = null;
    private String mEmail = null;
    private boolean isLogged = false;
    private OnChildFragmentInteractionListener mParentListener;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

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
        mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sign_in, container, false);

        AppCompatButton mGoogleSignIn = view.findViewById(R.id.google_signIn);

        mGoogleSignIn.setOnClickListener(v -> googleSignIn());

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

    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            if (account != null) {
                mUserId = account.getId();
                mName = account.getDisplayName();
                mEmail = account.getEmail();
                isLogged = true;
            }
            DocumentReference signInRef = db.collection("Users").document(mUserId);
            mParentListener.messageFromChildToParent(mUserId, mName, mEmail,isLogged);
            //Sign in successful
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
        }
    }

    public interface OnChildFragmentInteractionListener {
        void messageFromChildToParent(String userId, String name, String email, boolean isLogged);
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

    public void sendDataToFirestore() {

    }
}
