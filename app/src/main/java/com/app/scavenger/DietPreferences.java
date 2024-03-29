package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import java.util.ArrayList;

public class DietPreferences extends AppCompatActivity implements DietsAdapter.ItemClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_preferences);

        RecyclerView mRecyclerView = findViewById(R.id.diet_recyclerView);
        TopToolbar topToolbar = findViewById(R.id.diets_toolbar);
        topToolbar.setTitle("Dietary Preferences");

        //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        ArrayList<Object> options = new ArrayList<>();

        // Diet Header
        options.add(new DietHeader("Diets"));

        // Diets
        options.add(new DietItem("Gluten Free"));
        options.add(new DietItem("Ketogenic"));
        options.add(new DietItem("Vegetarian"));
        options.add(new DietItem("Lacto-Vegetarian"));
        options.add(new DietItem("Ovo-Vegetarian"));
        options.add(new DietItem("Vegan"));
        options.add(new DietItem("Pescetarian"));
        options.add(new DietItem("Paleo"));
        options.add(new DietItem("Primal"));
        options.add(new DietItem("Whole30"));

        // Allergy Header
        options.add(new DietHeader("Allergies"));

        // Allergies
        options.add(new DietItem("Dairy"));
        options.add(new DietItem("Egg"));
        options.add(new DietItem("Gluten"));
        options.add(new DietItem("Grain"));
        options.add(new DietItem("Peanut"));
        options.add(new DietItem("Seafood"));
        options.add(new DietItem("Sesame"));
        options.add(new DietItem("Shellfish"));
        options.add(new DietItem("Soy"));
        options.add(new DietItem("Sulfite"));
        options.add(new DietItem("Tree Nut"));
        options.add(new DietItem("Wheat"));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        mRecyclerView.addItemDecoration(dividerItemDecoration);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DietsAdapter adapter = new DietsAdapter(this, options);
        adapter.setClickListener(this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(int position) {
    }
}