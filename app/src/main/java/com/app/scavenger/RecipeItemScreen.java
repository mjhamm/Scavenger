package com.app.scavenger;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

public class RecipeItemScreen extends AppCompatActivity {

    private TextView recipeName, recipeSource, recipeNameCopy, recipeSourceCopy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update to the status bar on lower SDK's
        // Makes bar on lower SDK's black with white icons
       /* if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

            Window window = this.getWindow();
            window.setStatusBarColor(getResources().getColor(R.color.transparent));
        }*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.getWindow().getDecorView().setSystemUiVisibility( View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            // edited here

        }

        setContentView(R.layout.activity_recipe_item_screen);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());
        //toolBarLayout.setStatusBarScrimColor(getResources().getColor(R.color.com_facebook_blue));

        recipeName = findViewById(R.id.recipe_name);
        //recipeNameCopy = findViewById(R.id.recipe_name1);
        recipeSource = findViewById(R.id.recipe_source);
        //recipeSourceCopy = findViewById(R.id.recipe_source1);

        AppBarLayout appBar = findViewById(R.id.app_bar);

        /*appBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    //toolBarLayout.setContentScrimColor(Color.parseColor("#50000000"));
                    recipeName.setVisibility(View.VISIBLE);
                    recipeSource.setVisibility(View.VISIBLE);
                    recipeNameCopy.setVisibility(View.INVISIBLE);
                    recipeSourceCopy.setVisibility(View.INVISIBLE);
                    isShow = true;
                } else if(isShow) {
                    toolBarLayout.setContentScrimColor(getResources().getColor(R.color.transparent));
                    recipeName.setVisibility(View.INVISIBLE);
                    recipeSource.setVisibility(View.INVISIBLE);
                    recipeNameCopy.setVisibility(View.VISIBLE);
                    recipeSourceCopy.setVisibility(View.VISIBLE);
                    isShow = false;
                }
            }
        });*/

        ImageButton mBackButton = findViewById(R.id.item_screen_back);
        mBackButton.setOnClickListener(v -> {
            finish();
        });


    }
}