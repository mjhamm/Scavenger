package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

public class Help extends AppCompatActivity implements HelpAdapter.ItemClickListener {

    private SharedPreferences sharedPreferences;

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

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // List of options for recyclerview
        ArrayList<String> options = new ArrayList<>();
        options.add(getResources().getString(R.string.report_a_problem));
        options.add(getResources().getString(R.string.help_center));

        helpRecycler.setLayoutManager(new LinearLayoutManager(this));
        HelpAdapter adapter = new HelpAdapter(this, options);
        adapter.setClickListener(this);
        helpRecycler.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        helpRecycler.addItemDecoration(dividerItemDecoration);
        helpRecycler.setLayoutManager(layoutManager);

        // Close activity on button click
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    public void onItemClick(View view, int position) {

        switch (position) {
            // Open new alert to choose report problem or feedback
            case 0:
                final CharSequence[] items = {"Report a Problem", "Send Feedback"};

                new MaterialAlertDialogBuilder(this, R.style.ReportAlertTheme)
                        .setTitle(getResources().getString(R.string.report_a_problem))
                        .setCancelable(true)
                        .setItems(items, (dialog, which) -> {
                            switch (which) {
                                // Opens report problem activity
                                case 0:
                                    Intent reportProblem = new Intent(this, ReportProblem.class);
                                    startActivity(reportProblem);
                                    break;
                                    // Opens send feedback activity
                                case 1:
                                    Intent sendFeedback = new Intent(this, SendFeedback.class);
                                    startActivity(sendFeedback);
                                    break;
                            }
                        })
                        .create()
                        .show();
                break;
                // Open help center
            case 1:
                if (sharedPreferences.getBoolean("inAppBrowser", true)) {
                    openURLInChromeCustomTab(this, Constants.scavengerHelpURL);
                } else {
                    openInDefaultBrowser(this, Constants.scavengerHelpURL);
                }
                break;
        }
    }

    // opens the recipe in the users default browser
    private void openURLInChromeCustomTab(Context context, String url) {
        try {
            CustomTabsIntent.Builder builder1 = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder1.build();
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            builder1.setInstantAppsEnabled(true);
            customTabsIntent.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.getPackageName()));
            builder1.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
            customTabsIntent.launchUrl(context, Uri.parse(url));
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e("ChromeCustomTabError: ", "Activity Error");
        }
    }

    // open the recipe in the App Browser
    private void openInDefaultBrowser(Context context, String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(browserIntent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e("DefaultBrowserError: ", "Activity Error");
        }
    }
}
