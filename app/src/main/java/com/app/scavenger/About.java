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
import android.widget.ImageButton;
import java.util.ArrayList;

// About activity that lists information about Scavenger

public class About extends AppCompatActivity implements AboutAdapter.ItemClickListener {

    private boolean inAppBrowsingOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        RecyclerView aboutRecycler = findViewById(R.id.about_list);

        TopToolbar topToolbar = findViewById(R.id.about_toolbar);
        topToolbar.setTitle("About");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Add options inside of the recyclerview
        ArrayList<String> options = new ArrayList<>();
        options.add(getResources().getString(R.string.terms_and_conditions));
        options.add(getResources().getString(R.string.privacy_policy));
        options.add(getResources().getString(R.string.open_source_libraries));

        //aboutRecycler.setLayoutManager(new LinearLayoutManager(this));
        AboutAdapter adapter = new AboutAdapter(this, options);
        adapter.setClickListener(this);
        aboutRecycler.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        aboutRecycler.addItemDecoration(dividerItemDecoration);
        aboutRecycler.setLayoutManager(layoutManager);

        inAppBrowsingOn = sharedPreferences.getBoolean("inAppBrowser", true);


        // Close activity through back button
        //backButton.setOnClickListener(v -> finish());

    }

    @Override
    public void onItemClick(int position) {

        switch (position) {
            // Terms and Conditions
            case 0:
                if (inAppBrowsingOn) {
                    openURLInChromeCustomTab(this, Constants.scavengerTermsURL);
                } else {
                    openInDefaultBrowser(this, Constants.scavengerTermsURL);
                }
                break;
                // Privacy Policy
            case 1:
                if (inAppBrowsingOn) {
                    openURLInChromeCustomTab(this, Constants.scavengerPrivacyURL);
                } else {
                    openInDefaultBrowser(this, Constants.scavengerPrivacyURL);
                }
                break;
                // Open Source Libraries
            case 2:
                openOSL();
                break;
        }
    }

    // Method for opening the Open Source Libraries activity
    private void openOSL() {
        Intent intent = new Intent(this, OpenSourceLibraries.class);
        startActivity(intent);
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
