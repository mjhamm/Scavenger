package com.app.scavenger.v2.activities

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.scavenger.AboutAdapter
import com.app.scavenger.Constants
import com.app.scavenger.OpenSourceLibraries
import com.app.scavenger.R
import com.app.scavenger.databinding.ActivityAboutBinding

class About: AppCompatActivity(), AboutAdapter.ItemClickListener {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_about)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        binding.aboutToolbar.setTitle("About")

        val options = ArrayList<String>()
        options.add(getString(R.string.terms_and_conditions))
        options.add(getString(R.string.privacy_policy))
        options.add(getString(R.string.open_source_libraries))
        options.add(getString(R.string.scavenger_base_url))

        val aboutAdapter = AboutAdapter(this, options)
        aboutAdapter.setClickListener(this)
        binding.aboutList.adapter = aboutAdapter
        binding.aboutList.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
        binding.aboutList.layoutManager = LinearLayoutManager(this)
    }

    override fun onItemClick(position: Int) {

        when (position) {
            0 -> open(this, Constants.scavengerTermsURL)
            1 -> open(this, Constants.scavengerPrivacyURL)
            2 -> openOSL()
            3 -> open(this, Constants.scavengerBaseURL)
            else -> open(this, Constants.scavengerBaseURL)
        }
    }

    private fun openOSL() {
        val intent = Intent(this, OpenSourceLibraries::class.java)
        startActivity(intent)
    }

    private fun open(context: Context, url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        } catch (e : ActivityNotFoundException) {
            e.printStackTrace()
        }
    }
}