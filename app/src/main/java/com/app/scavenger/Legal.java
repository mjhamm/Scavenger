package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageButton;

public class Legal extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);

        ImageButton backButton = findViewById(R.id.legal_back);

        backButton.setOnClickListener(v -> finish());
    }
}
