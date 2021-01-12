package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class SendFeedback extends AppCompatActivity {

    public static final String TAG = "SendFeedback";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private EditText feedbackEditText;
    private TextView submit_textButton;
    private ConnectionDetector con;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_feedback);

        feedbackEditText = findViewById(R.id.feedback_editText);
        submit_textButton = findViewById(R.id.submitFeedback_textButton);

        TopToolbar topToolbar = findViewById(R.id.sendFeedback_toolbar);
        topToolbar.setTitle("Send Feedback");

        submit_textButton.setEnabled(false);
        submit_textButton.setTextColor(Color.GRAY);
        submit_textButton.setBackgroundColor(Color.WHITE);

        mAuth = FirebaseAuth.getInstance();

        con = new ConnectionDetector(this);

        // Checks for whether or not the edit text is empty or not and changes the appearance of the submit button
        feedbackEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() != 0) {
                    submit_textButton.setEnabled(true);
                    submit_textButton.setTextColor(Color.BLUE);
                } else {
                    submit_textButton.setEnabled(false);
                    submit_textButton.setTextColor(Color.GRAY);
                }
                submit_textButton.setBackgroundColor(Color.WHITE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Submits Feedback
        submit_textButton.setOnClickListener(v -> {
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(Constants.noInternetTitle)
                        .setMessage(Constants.noInternetMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                sendFeedback();

                finish();
            }
        });
    }

    // Send feedback to Server under feedback with Phone information
    private void sendFeedback() {

        // Information specific to the device that is sending the feedback
        String manufacturer = Build.MANUFACTURER;
        String display = Build.DISPLAY;
        String board = Build.BOARD;
        String device = Build.DEVICE;
        String brand = Build.BRAND;
        String model = Build.MODEL;
        String product = Build.PRODUCT;
        int sdk = Build.VERSION.SDK_INT;
        String osVersion = Build.VERSION.RELEASE;

        String userId = null;

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        Calendar calendar = Calendar.getInstance();

        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int year = calendar.get(Calendar.YEAR);

        calendar.clear();
        calendar.set(year, month, day);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());
        String strDate = simpleDateFormat.format(calendar.getTime());


        Date now = new Date();
        Timestamp timestamp = new Timestamp(now);

        HashMap<String, Object> feedbackInfo = new HashMap<>();

        CollectionReference feedbackReference = db.collection("Feedback").document(strDate).collection("feedback");

        feedbackInfo.put("Feedback", getFeedbackText());
        feedbackInfo.put("Timestamp", timestamp);

        feedbackInfo.put("Board", board);
        feedbackInfo.put("Brand", brand);
        feedbackInfo.put("Device", device);
        feedbackInfo.put("Display", display);
        feedbackInfo.put("Manufacturer", manufacturer);
        feedbackInfo.put("Model", model);
        feedbackInfo.put("Product", product);
        feedbackInfo.put("SDK", sdk);
        feedbackInfo.put("OS", osVersion);
        feedbackInfo.put("User Id", userId);


        feedbackReference.document().set(feedbackInfo)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG,"feedback saved to Firebase");
                    toastMessage("Thank you for your feedback");
                })
                .addOnFailureListener(e -> {
                    toastMessage("Error sending feedback");
                    Log.d(TAG, e.toString());
                });
    }

    //method for creating a Toast
    private void toastMessage(String message) {
        Toast.makeText(this.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Gets report text
    private String getFeedbackText() {
        return feedbackEditText.getText().toString();
    }
}
