package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

// About activity that lists information about Scavenger

public class About extends AppCompatActivity implements AboutAdapter.ItemClickListener {

    private RecyclerView aboutRecycler;
    private AboutAdapter adapter;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        aboutRecycler = findViewById(R.id.about_list);
        backButton = findViewById(R.id.about_back);

        ArrayList<String> options = new ArrayList<>();
        options.add("Terms & Conditions");
        options.add("Privacy Policy");
        options.add("Open Source Libraries");

        aboutRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AboutAdapter(this, options);
        adapter.setClickListener(this);
        aboutRecycler.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(aboutRecycler.getContext(), layoutManager.getOrientation());
        aboutRecycler.addItemDecoration(dividerItemDecoration);

        backButton.setOnClickListener(v -> {
            finish();
        });

    }

    @Override
    public void onItemClick(View view, int position) {

        switch (position) {
            case 0:
                Toast.makeText(this, "Terms & Conditions", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                Toast.makeText(this, "Privacy Policy", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                openOSL();
                break;
        }
    }

    private void openOSL() {
        Intent intent = new Intent(this, OpenSourceLibraries.class);
        startActivity(intent);
    }
}
