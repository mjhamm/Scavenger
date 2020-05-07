package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;
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
        reportSubmitButton.setTextColor(Color.GRAY);
        reportSubmitButton.setBackgroundColor(Color.WHITE);

        // Checks for whether or not the edit text is empty or not and changes the appearance of the submit button
        reportEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() != 0) {
                    reportSubmitButton.setEnabled(true);
                    reportSubmitButton.setTextColor(Color.BLUE);
                    reportSubmitButton.setBackgroundColor(Color.WHITE);
                } else {
                    reportSubmitButton.setEnabled(false);
                    reportSubmitButton.setTextColor(Color.GRAY);
                    reportSubmitButton.setBackgroundColor(Color.WHITE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Submits Report
        reportSubmitButton.setOnClickListener(v -> {
            Toast.makeText(this, "Thank you for your report", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
