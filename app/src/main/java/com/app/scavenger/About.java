package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        aboutRecycler = findViewById(R.id.about_list);

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

    }

    @Override
    public void onItemClick(View view, int position) {
        Toast.makeText(this, "You clicked " + adapter.getItem(position) + " on row number " + position, Toast.LENGTH_SHORT).show();
    }
}
