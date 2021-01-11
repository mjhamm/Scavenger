package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {

    //private static final String TAG = "FORGOT_PASSWORD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_forgot_password);

        EditText forgot_editText = findViewById(R.id.forgot_editText);
        MaterialButton forgot_pass_button = findViewById(R.id.forgot_pass_button);
        ConnectionDetector con = new ConnectionDetector(this);

        TopToolbar topToolbar = findViewById(R.id.forgot_toolbar);
        topToolbar.setTitle("Forgot Password");

        // Disable the button on start
        forgot_pass_button.setEnabled(false);

        // Check if text is empty or not
        forgot_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() != 0) {
                    forgot_pass_button.setEnabled(s.toString().trim().contains("@"));
                } else {
                    forgot_pass_button.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        forgot_pass_button.setOnClickListener(v -> {
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(Constants.noInternetTitle)
                        .setMessage(Constants.noInternetMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                FirebaseAuth.getInstance().sendPasswordResetEmail(forgot_editText.getText().toString().trim())
                        .addOnFailureListener(e -> {
                            //Log.d(TAG, "Email send failure");
                            new MaterialAlertDialogBuilder(ForgotPassword.this, R.style.ReportAlertTheme)
                                    .setTitle(Constants.accountNotFound)
                                    .setMessage(Constants.accountNotFoundMessage)
                                    .setCancelable(false)
                                    .setPositiveButton("Ok", (dialog, which) -> dialog.dismiss())
                                    .create()
                                    .show();
                        })
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                //Log.d(TAG, "Reset password email sent");
                                new MaterialAlertDialogBuilder(ForgotPassword.this, R.style.ReportAlertTheme)
                                        .setTitle(Constants.resetPassTitle)
                                        .setMessage(Constants.resetPassMessage)
                                        .setCancelable(false)
                                        .setPositiveButton("OK", (dialog, which) -> {
                                            dialog.dismiss();
                                            finish();
                                        })
                                        .create()
                                        .show();
                            }
                        });
            }
        });
    }
}
