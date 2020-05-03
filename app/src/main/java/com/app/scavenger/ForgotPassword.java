package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.EditText;

import com.google.android.material.button.MaterialButton;

public class ForgotPassword extends AppCompatActivity {

    private EditText forgot_editText;
    private MaterialButton forgot_pass_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        forgot_editText = findViewById(R.id.forgot_editText);
        forgot_pass_button = findViewById(R.id.forgot_pass_button);
    }
}
