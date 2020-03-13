package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageButton;

// About activity that lists information about Scavenger

public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ImageButton backButton = findViewById(R.id.about_back);

        // Back Arrow button to close the activity
        backButton.setOnClickListener(v -> {
            finish();
        });
    }
}
