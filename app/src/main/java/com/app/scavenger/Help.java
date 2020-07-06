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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

public class Help extends AppCompatActivity implements HelpAdapter.ItemClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update to the status bar on lower SDK's
        // Makes bar on lower SDK's black with white icons
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            this.getWindow().setStatusBarColor(getResources().getColor(android.R.color.black));
        }

        setContentView(R.layout.activity_help);

        RecyclerView helpRecycler = findViewById(R.id.help_list);
        ImageButton backButton = findViewById(R.id.help_back);

        ArrayList<String> options = new ArrayList<>();
        options.add("Report a Problem");
        options.add("Help Center");

        helpRecycler.setLayoutManager(new LinearLayoutManager(this));
        HelpAdapter adapter = new HelpAdapter(this, options);
        adapter.setClickListener(this);
        helpRecycler.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        helpRecycler.addItemDecoration(dividerItemDecoration);

        backButton.setOnClickListener(v -> finish());
    }

    @Override
    public void onItemClick(View view, int position) {

        switch (position) {
            case 0:
                final CharSequence[] items = {"Report a Problem", "Send Feedback"};

                new MaterialAlertDialogBuilder(this, R.style.ReportAlertTheme)
                        .setTitle("Report a Problem")
                        .setCancelable(true)
                        .setItems(items, (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    Intent reportProblem = new Intent(this, ReportProblem.class);
                                    startActivity(reportProblem);
                                    break;
                                case 1:
                                    Intent sendFeedback = new Intent(this, SendFeedback.class);
                                    startActivity(sendFeedback);
                                    break;
                            }
                        })
                        .create()
                        .show();
                break;
            case 1:
                break;
        }
    }
}
