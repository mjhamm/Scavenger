package com.app.scavenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AccountLogged extends Fragment {

    Context mContext;
    ImageButton settingsButton;
    MaterialCardView mLogoutButton;
    MaterialCardView mDeleteAccountButton;

    public AccountLogged() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logged, container, false);

        settingsButton = view.findViewById(R.id.settings_button_logged);
        mLogoutButton = view.findViewById(R.id.logout_button);
        mDeleteAccountButton = view.findViewById(R.id.delete_account);

        settingsButton.setOnClickListener(v -> startActivity(new Intent(mContext, SettingsActivity.class)));

        mLogoutButton.setOnClickListener(v -> {
            logoutDialog();
        });

        mDeleteAccountButton.setOnClickListener(v -> {
            deleteAccountFirst();
        });

        return view;
    }

    private void logoutDialog() {
        new MaterialAlertDialogBuilder(mContext)
                .setTitle("Are you sure you want to Log Out?")
                .setCancelable(false)
                .setPositiveButton("Log Out", (dialog, which) -> {
                    //LOG OUT
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.cancel();
                })
                .create()
                .show();
    }

    private void deleteAccountFirst() {
        new MaterialAlertDialogBuilder(mContext)
                .setTitle("Are you sure you want to delete your account?")
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
