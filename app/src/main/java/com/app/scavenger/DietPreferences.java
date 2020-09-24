package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;

import com.google.firebase.database.core.utilities.Tree;

import java.util.ArrayList;

public class DietPreferences extends AppCompatActivity implements DietsAdapter.ItemClickListener {

    private RecyclerView mRecyclerView;
    private DietsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_preferences);

        mRecyclerView = findViewById(R.id.diet_recyclerView);
        TopToolbar topToolbar = findViewById(R.id.diets_toolbar);
        topToolbar.setTitle("Dietary Preferences");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        ArrayList<Object> options = new ArrayList<>();

        // Diet Header
        options.add(new DietHeader("Diets"));

        // Diets
        options.add(new DietItem("Gluten Free", false));
        options.add(new DietItem("Ketogenic", false));
        options.add(new DietItem("Vegetarian", false));
        options.add(new DietItem("Lacto-Vegetarian", false));
        options.add(new DietItem("Ovo-Vegetarian", false));
        options.add(new DietItem("Vegan", false));
        options.add(new DietItem("Pescetarian", false));
        options.add(new DietItem("Paleo", false));
        options.add(new DietItem("Primal", false));
        options.add(new DietItem("Whole30", false));

        // Allergy Header
        options.add(new DietHeader("Allergies"));

        // Allergies
        options.add(new DietItem("Dairy", false));
        options.add(new DietItem("Egg", false));
        options.add(new DietItem("Gluten", false));
        options.add(new DietItem("Grain", false));
        options.add(new DietItem("Peanut", false));
        options.add(new DietItem("Seafood", false));
        options.add(new DietItem("Sesame", false));
        options.add(new DietItem("Shellfish", false));
        options.add(new DietItem("Soy", false));
        options.add(new DietItem("Sulfite", false));
        options.add(new DietItem("Tree Nut", false));
        options.add(new DietItem("Wheat", false));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        mRecyclerView.addItemDecoration(dividerItemDecoration);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DietsAdapter(this, options);
        adapter.setClickListener(this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(int position) {
    }
}