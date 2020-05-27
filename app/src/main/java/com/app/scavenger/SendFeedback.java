package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SendFeedback extends AppCompatActivity {

    private EditText feedbackEditText;
    private MaterialButton feedbackSubmitButton;
    private ImageButton backButton;
    private ConnectionDetector con;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_feedback);

        feedbackEditText = findViewById(R.id.feedback_editText);
        feedbackSubmitButton = findViewById(R.id.feedback_submit);
        backButton = findViewById(R.id.feedback_back);

        feedbackSubmitButton.setEnabled(false);
        feedbackSubmitButton.setTextColor(Color.GRAY);
        feedbackSubmitButton.setBackgroundColor(Color.WHITE);

        con = new ConnectionDetector(this);

        backButton.setOnClickListener(v -> {
            finish();
        });

        // Checks for whether or not the edit text is empty or not and changes the appearance of the submit button
        feedbackEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() != 0) {
                    feedbackSubmitButton.setEnabled(true);
                    feedbackSubmitButton.setTextColor(Color.BLUE);
                    feedbackSubmitButton.setBackgroundColor(Color.WHITE);
                } else {
                    feedbackSubmitButton.setEnabled(false);
                    feedbackSubmitButton.setTextColor(Color.GRAY);
                    feedbackSubmitButton.setBackgroundColor(Color.WHITE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Submits Feedback
        feedbackSubmitButton.setOnClickListener(v -> {
            if (!con.isConnectingToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("No Internet connection found")
                        .setMessage("You don't have an Internet connection. Please reconnect in order to Submit Feedback.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
            } else {
                Toast.makeText(this, "Thank you for your feedback", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
