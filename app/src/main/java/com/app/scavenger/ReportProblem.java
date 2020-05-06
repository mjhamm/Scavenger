package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.button.MaterialButton;

public class ReportProblem extends AppCompatActivity {

    private EditText reportEditText;
    private MaterialButton reportSubmitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_problem);

        reportEditText = findViewById(R.id.report_editText);
        reportSubmitButton = findViewById(R.id.report_submit);

        reportSubmitButton.setEnabled(false);
        reportSubmitButton.setTextColor(getResources().getColor(R.color.dark_gray, null));
        reportSubmitButton.setBackgroundColor(getResources().getColor(android.R.color.white, null));

        reportEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() != 0) {
                    reportSubmitButton.setEnabled(true);
                    reportSubmitButton.setTextColor(getResources().getColor(android.R.color.white, null));
                    reportSubmitButton.setBackgroundColor(getResources().getColor(R.color.buttonRed, null));
                } else {
                    reportSubmitButton.setEnabled(false);
                    reportSubmitButton.setTextColor(getResources().getColor(R.color.dark_gray, null));
                    reportSubmitButton.setBackgroundColor(getResources().getColor(android.R.color.white, null));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
}
