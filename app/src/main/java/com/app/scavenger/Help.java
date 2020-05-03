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

public class Help extends AppCompatActivity implements HelpAdapter.ItemClickListener {

    private RecyclerView helpRecycler;
    private HelpAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        helpRecycler = findViewById(R.id.help_list);

        ArrayList<String> options = new ArrayList<>();
        options.add("Report a Problem");
        options.add("Help Center");

        helpRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HelpAdapter(this, options);
        adapter.setClickListener(this);
        helpRecycler.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(helpRecycler.getContext(), layoutManager.getOrientation());
        helpRecycler.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public void onItemClick(View view, int position) {
        Toast.makeText(this, "You clicked " + adapter.getItem(position) + " on row number " + position, Toast.LENGTH_SHORT).show();
    }
}
