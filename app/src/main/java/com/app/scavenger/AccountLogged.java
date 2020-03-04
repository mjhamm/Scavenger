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
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AccountLogged extends Fragment {

    Context mContext;
    ImageButton settingsButton;
    MaterialCardView mLogoutButton;
    TextView mName, mEmail, mAccountName;

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
        mName = view.findViewById(R.id.name_mainText);
        mEmail = view.findViewById(R.id.email_mainText);
        mAccountName = view.findViewById(R.id.account_name);

        mAccountName.setOnClickListener(v -> {
            startActivity(new Intent(mContext, AccountInfo.class));
        });

        settingsButton.setOnClickListener(v -> startActivity(new Intent(mContext, SettingsActivity.class)));

        mLogoutButton.setOnClickListener(v -> {
            logoutDialog();
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


}
