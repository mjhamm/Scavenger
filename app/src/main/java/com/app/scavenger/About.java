package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageButton;

public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ImageButton backButton = findViewById(R.id.about_back);

        backButton.setOnClickListener(v -> {
            finish();
        });
    }
}
