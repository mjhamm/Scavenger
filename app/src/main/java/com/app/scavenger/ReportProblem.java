package com.app.scavenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.SharedPreferences;
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
import com.instabug.library.model.Report;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReportProblem extends AppCompatActivity {

    public static final String TAG = "ReportProblem";

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private EditText reportEditText;
    private ImageButton backButton;
    private TextView submit_buttonText;
    private ConnectionDetector con;
    private String reportText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_problem);

        reportEditText = findViewById(R.id.report_editText);
        submit_buttonText = findViewById(R.id.report_textButton);
        backButton = findViewById(R.id.reportProblem_back);

        submit_buttonText.setEnabled(false);
        submit_buttonText.setTextColor(Color.GRAY);
        submit_buttonText.setBackgroundColor(Color.WHITE);

        con = new ConnectionDetector(this);

        backButton.setOnClickListener(v -> {
            finish();
        });

        // Checks for whether or not the edit text is empty or not and changes the appearance of the submit button
        reportEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() != 0) {
                    submit_buttonText.setEnabled(true);
                    submit_buttonText.setTextColor(Color.BLUE);
                } else {
                    submit_buttonText.setEnabled(false);
                    submit_buttonText.setTextColor(Color.GRAY);
                }
                submit_buttonText.setBackgroundColor(Color.WHITE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Submits Report
        submit_buttonText.setOnClickListener(v -> {
            if (!con.connectedToInternet()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("No Internet connection found")
                        .setMessage("You don't have an Internet connection. Please reconnect in order to Report a Problem.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
            } else {
                // Send problem to server
                sendReport();
                finish();
            }
        });
    }

    // Send report to Server under reports with Phone information
    private void sendReport() {

        // Information specific to the device that is sending the report
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

        Map<String, Object> reportInfo = new HashMap<>();

        CollectionReference reportingReference = db.collection("Reports").document(strDate).collection("reports");

        reportInfo.put("Report", getReportText());
        reportInfo.put("Timestamp", timestamp);

        reportInfo.put("Board", board);
        reportInfo.put("Brand", brand);
        reportInfo.put("Device", device);
        reportInfo.put("Display", display);
        reportInfo.put("Manufacturer", manufacturer);
        reportInfo.put("Model", model);
        reportInfo.put("Product", product);
        reportInfo.put("SDK", sdk);
        reportInfo.put("OS", osVersion);


        reportingReference.document().set(reportInfo)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG,"Report saved to Firebase");
                        toastMessage("Thank you for your report");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        toastMessage("Error sending report");
                        Log.d(TAG, e.toString());
                    }
                });
    }

    //method for creating a Toast
    private void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Gets report text
    private String getReportText() {
        return reportEditText.getText().toString();
    }
}
