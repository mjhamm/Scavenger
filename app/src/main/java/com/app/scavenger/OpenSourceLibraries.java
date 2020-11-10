package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class OpenSourceLibraries extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_osl);

        TopToolbar topToolbar = findViewById(R.id.osl_toolbar);
        topToolbar.setTitle("Open Source Libraries");
    }
}
