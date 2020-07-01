package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
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
        setContentView(R.layout.activity_about);

        RecyclerView aboutRecycler = findViewById(R.id.about_list);
        ImageButton backButton = findViewById(R.id.about_back);

        ArrayList<String> options = new ArrayList<>();
        options.add("Terms & Conditions");
        options.add("Privacy Policy");
        options.add("Open Source Libraries");

        aboutRecycler.setLayoutManager(new LinearLayoutManager(this));
        AboutAdapter adapter = new AboutAdapter(this, options);
        adapter.setClickListener(this);
        aboutRecycler.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        aboutRecycler.addItemDecoration(dividerItemDecoration);

        backButton.setOnClickListener(v -> finish());

    }

    @Override
    public void onItemClick(View view, int position) {

        switch (position) {
            case 0:
                toastMessage("Terms & Conditions");
                break;
            case 1:
                toastMessage("Privacy Policy");
                break;
            case 2:
                openOSL();
                break;
        }
    }

    //method for creating a Toast
    private void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void openOSL() {
        Intent intent = new Intent(this, OpenSourceLibraries.class);
        startActivity(intent);
    }
}
