package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.Image;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AccountInfo extends AppCompatActivity {

    Context mContext;
    MaterialCardView mDeleteAccountButton, mUpdateInfoButton;
    EditText mName_editText, mEmail_editText;
    ImageButton mBackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_info);
        mContext = this;

        mDeleteAccountButton = findViewById(R.id.delete_account);
        mUpdateInfoButton = findViewById(R.id.updateInfo_button);
        mName_editText = findViewById(R.id.name_info_editText);
        mEmail_editText = findViewById(R.id.email_info_editText);
        mBackButton = findViewById(R.id.account_info_back);

        mDeleteAccountButton.setOnClickListener(v -> {
            deleteAccountFirst();
        });

        mBackButton.setOnClickListener(v -> {
            finish();
        });
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
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.cancel();
                })
                .create()
                .show();
    }
}
