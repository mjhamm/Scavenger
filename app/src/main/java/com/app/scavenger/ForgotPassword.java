package com.app.scavenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {

    private static final String TAG = "FORGOT_PASSWORD";

    private EditText forgot_editText;
    private MaterialButton forgot_pass_button;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        forgot_editText = findViewById(R.id.forgot_editText);
        forgot_pass_button = findViewById(R.id.forgot_pass_button);
        backButton = findViewById(R.id.forgotPass_back);


        backButton.setOnClickListener(v -> {
            finish();
        });

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

            new MaterialAlertDialogBuilder(this, R.style.ReportAlertTheme)
                    .setTitle("Reset instructions have been sent.")
                    .setMessage("If there is an account with the Email Address provided, you will receive reset instructions in 2 - 5 minutes.")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            hideKeyboard(ForgotPassword.this);
                            FirebaseAuth.getInstance().sendPasswordResetEmail(forgot_editText.getText().toString().trim())
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "Reset password email sent.");
                                            }
                                        }
                                    });
                            finish();
                        }
                    })
                    .create()
                    .show();
        });
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        view.clearFocus();
    }
}
