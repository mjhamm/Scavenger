package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ForgotPassword extends AppCompatActivity {

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
                    .setMessage("If there is an account with the email entered, you will receive reset instructions in 5 - 10 minutes.")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            hideSoftKeyboard(getCurrentFocus());
                            finish();
                        }
                    })
                    .create()
                    .show();
        });
    }

    public void hideSoftKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
