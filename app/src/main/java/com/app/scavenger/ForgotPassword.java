package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {

    private static final String TAG = "FORGOT_PASSWORD";

    private EditText forgot_editText;
    private MaterialButton forgot_pass_button;
    private ConnectionDetector con;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update to the status bar on lower SDK's
        // Makes bar on lower SDK's black with white icons
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            this.getWindow().setStatusBarColor(getResources().getColor(android.R.color.black));
        }

        setContentView(R.layout.activity_forgot_password);

        forgot_editText = findViewById(R.id.forgot_editText);
        forgot_pass_button = findViewById(R.id.forgot_pass_button);
        ImageButton backButton = findViewById(R.id.forgotPass_back);
        con = new ConnectionDetector(this);

        backButton.setOnClickListener(v -> finish());

        forgot_pass_button.setEnabled(false);

        forgot_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() != 0) {
                    if (s.toString().trim().contains("@")) {
                        forgot_pass_button.setEnabled(true);
                    } else {
                        forgot_pass_button.setEnabled(false);
                    }
                } else {
                    forgot_pass_button.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        forgot_pass_button.setOnClickListener(v -> {
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("No Internet Connection")
                        .setMessage("You don't have an internet connection. Please reconnect and try to Sign In again.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                FirebaseAuth.getInstance().sendPasswordResetEmail(forgot_editText.getText().toString().trim())
                        .addOnFailureListener(e -> {
                            Log.d(TAG, "Email send failure");
                            new MaterialAlertDialogBuilder(ForgotPassword.this, R.style.ReportAlertTheme)
                                    .setTitle("Account not found")
                                    .setMessage("The email that you have entered does not belong to an account. If this issue persists, please contact support at support@theScavengerApp.com")
                                    .setCancelable(false)
                                    .setPositiveButton("Ok", (dialog, which) -> dialog.dismiss())
                                    .create()
                                    .show();
                        })
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Reset password email sent");
                                new MaterialAlertDialogBuilder(ForgotPassword.this, R.style.ReportAlertTheme)
                                        .setTitle("Reset instructions have been sent.")
                                        .setMessage("You will receive reset instructions in 2-5 minutes.")
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

// --Commented out by Inspection START (7/2/2020 12:42 PM):
//    public static void hideKeyboard(Activity activity) {
//        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
//        //Find the currently focused view, so we can grab the correct window token from it.
//        View view = activity.getCurrentFocus();
//        //If no view currently has focus, create a new one, just so we can grab a window token from it
//        if (view == null) {
//            view = new View(activity);
//        }
//        if (imm != null) {
//            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
//        }
//        view.clearFocus();
//    }
// --Commented out by Inspection STOP (7/2/2020 12:42 PM)
}
