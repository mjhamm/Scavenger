package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.ImageButton;

public class DietPreferences extends AppCompatActivity {

    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_preferences);

        mRecyclerView = findViewById(R.id.diet_recyclerView);

        ImageButton backButton = findViewById(R.id.diets_back);
        backButton.setOnClickListener(v -> {
            finish();
        });
    }
}