package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageButton;

public class Help extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        ImageButton backButton = findViewById(R.id.help_back);

        backButton.setOnClickListener(v -> {
            finish();
        });
    }
}
