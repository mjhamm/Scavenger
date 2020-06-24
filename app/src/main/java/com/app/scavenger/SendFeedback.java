package com.app.scavenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SendFeedback extends AppCompatActivity {

    public static final String TAG = "SendFeedback";

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private EditText feedbackEditText;
    private TextView submit_textButton;
    private ImageButton backButton;
    private ConnectionDetector con;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_feedback);

        feedbackEditText = findViewById(R.id.feedback_editText);
        submit_textButton = findViewById(R.id.submitFeedback_textButton);
        backButton = findViewById(R.id.feedback_back);

        submit_textButton.setEnabled(false);
        submit_textButton.setTextColor(Color.GRAY);
        submit_textButton.setBackgroundColor(Color.WHITE);

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

        Map<String, Object> feedbackInfo = new HashMap<>();

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


        feedbackReference.document().set(feedbackInfo)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG,"feedback saved to Firebase");
                        Toast.makeText(SendFeedback.this, "Thank you for your feedback", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SendFeedback.this, "Error sending feedback", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, e.toString());
                    }
                });
    }

    // Gets report text
    private String getFeedbackText() {
        return feedbackEditText.getText().toString();
    }
}
