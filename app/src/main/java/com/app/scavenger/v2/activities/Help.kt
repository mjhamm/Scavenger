package com.app.scavenger.v2.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.scavenger.HelpAdapter
import com.app.scavenger.R
import com.app.scavenger.databinding.ActivityHelpBinding
import com.app.scavenger.dialog.DialogBuilder

// TODO: UPDATE TO KOTLIN
class Help: AppCompatActivity(), HelpAdapter.ItemClickListener {

    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.helpToolbar.setTitle("Help")
        val options = mutableListOf<String>()
        options.add(getString(R.string.report_a_problem))
        options.add(getString(R.string.help_center))

        binding.helpList.layoutManager = LinearLayoutManager(this)
        binding.helpList.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
        val helpAdapter = HelpAdapter(this, options)
        binding.helpList.adapter = helpAdapter

    }

    override fun onItemClick(view: View?, position: Int) {

        when (position) {
            0 -> {
                val items = mutableListOf(
                    "Report a Problem",
                    "Send Feedback"
                )
            }
        }
    }
}