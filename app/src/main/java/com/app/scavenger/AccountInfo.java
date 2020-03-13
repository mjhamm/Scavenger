package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountInfo extends AppCompatActivity {

    private static final String TAG = "LOG: ";

    private Context mContext;
    private String mUserId = null;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_info);
        mContext = this;

        MaterialButton mDeleteAccountButton = findViewById(R.id.delete_account);
        ImageButton mBackButton = findViewById(R.id.account_info_back);

        mDeleteAccountButton.setOnClickListener(v -> {
            deleteAccountFirst();
        });

        mBackButton.setOnClickListener(v -> {
            finish();
        });

        Intent intent = getIntent();
        mUserId = intent.getStringExtra("userId");
    }

    private void deleteAccountFromFirebase() {
        db.collection("Users").document(mUserId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully removed account from Firebase");
                    finish();
                })
                .addOnFailureListener(e -> Log.d(TAG, "Failure to delete account from Firebase " + e.toString()));
    }

    private void deleteAccountFirst() {
        new MaterialAlertDialogBuilder(mContext)
                .setTitle("Are you sure you want to delete your account?")
                .setMessage("If you delete your account, you will lose all of your favorites that you have saved.")
                .setCancelable(false)
                .setPositiveButton("I'm Sure!", (dialog, which) -> {
                    deleteAccountSecond();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.cancel();
                })
                .create()
                .show();
    }

    private void deleteAccountSecond() {
        new MaterialAlertDialogBuilder(mContext)
                .setTitle("Just making sure. Do you really want to delete your account?")
                .setMessage("Just double checking that you want to delete your account and lose all of your favorites?")
                .setCancelable(false)
                .setPositiveButton("Yep! Delete it!", (dialog, which) -> {
                    //DELETE ACCOUNT
                    deleteAccountFromFirebase();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.cancel();
                })
                .create()
                .show();
    }
}
