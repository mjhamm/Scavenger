package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;

// About activity that lists information about Scavenger

public class About extends AppCompatActivity implements AboutAdapter.ItemClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update to the status bar on lower SDK's
        // Makes bar on lower SDK's black with white icons
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            this.getWindow().setStatusBarColor(getResources().getColor(android.R.color.black));
        }

        setContentView(R.layout.activity_about);

        RecyclerView aboutRecycler = findViewById(R.id.about_list);
        ImageButton backButton = findViewById(R.id.about_back);

        // Add options inside of the recyclerview
        ArrayList<String> options = new ArrayList<>();
        options.add(getResources().getString(R.string.terms_and_conditions));
        options.add(getResources().getString(R.string.privacy_policy));
        options.add(getResources().getString(R.string.open_source_libraries));

        aboutRecycler.setLayoutManager(new LinearLayoutManager(this));
        AboutAdapter adapter = new AboutAdapter(this, options);
        adapter.setClickListener(this);
        aboutRecycler.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        aboutRecycler.addItemDecoration(dividerItemDecoration);
        aboutRecycler.setLayoutManager(layoutManager);

        // Close activity through back button
        backButton.setOnClickListener(v -> finish());

    }

    @Override
    public void onItemClick(View view, int position) {

        switch (position) {
            // Terms and Conditions
            case 0:
                toastMessage(getResources().getString(R.string.terms_and_conditions));
                break;
                // Privacy Policy
            case 1:
                toastMessage(getResources().getString(R.string.privacy_policy));
                break;
                // Open Source Libraries
            case 2:
                openOSL();
                break;
        }
    }

    // Method for creating a Toast
    private void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Method for opening the Open Source Libraries activity
    private void openOSL() {
        Intent intent = new Intent(this, OpenSourceLibraries.class);
        startActivity(intent);
    }
}
