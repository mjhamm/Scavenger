package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageButton;

public class OpenSourceLibraries extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_osl);

        ImageButton backButton = findViewById(R.id.osl_back);

        backButton.setOnClickListener(v -> finish());
    }
}
