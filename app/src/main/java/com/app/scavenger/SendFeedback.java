package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.google.android.material.button.MaterialButton;

public class SendFeedback extends AppCompatActivity {

    private EditText feedbackEditText;
    private MaterialButton feedbackSubmitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_feedback);

        feedbackEditText = findViewById(R.id.feedback_editText);
        feedbackSubmitButton = findViewById(R.id.feedback_submit);

        feedbackSubmitButton.setEnabled(false);
        feedbackSubmitButton.setTextColor(getResources().getColor(R.color.dark_gray, null));
        feedbackSubmitButton.setBackgroundColor(getResources().getColor(android.R.color.white, null));

        feedbackEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() != 0) {
                    feedbackSubmitButton.setEnabled(true);
                    feedbackSubmitButton.setTextColor(getResources().getColor(android.R.color.white, null));
                    feedbackSubmitButton.setBackgroundColor(getResources().getColor(R.color.buttonRed, null));
                } else {
                    feedbackSubmitButton.setEnabled(false);
                    feedbackSubmitButton.setTextColor(getResources().getColor(R.color.dark_gray, null));
                    feedbackSubmitButton.setBackgroundColor(getResources().getColor(android.R.color.white, null));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
}
