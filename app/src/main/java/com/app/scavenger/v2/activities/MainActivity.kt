package com.app.scavenger.v2.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.app.scavenger.LikesFragment
import com.app.scavenger.R
import com.app.scavenger.SearchFragment
import com.app.scavenger.SettingsFragment
import com.app.scavenger.databinding.ActivityMainBinding

class MainActivity: AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fragment1: SearchFragment
    private lateinit var fragment2: LikesFragment
    private lateinit var fragment3: SettingsFragment
    private lateinit var activeFragment: Fragment
    private val fm: FragmentManager = supportFragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fragment1 = SearchFragment.newInstance()
        fragment2 = LikesFragment.newInstance()
        fragment3 = SettingsFragment()
        activeFragment = fragment1

        fm.beginTransaction().add(R.id.fragment_container, fragment3).hide(fragment3).commit()
        fm.beginTransaction().add(R.id.fragment_container, fragment2).hide(fragment2).commit()
        fm.beginTransaction().add(R.id.fragment_container, fragment1).commit()

        binding.bottomNavView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.action_search -> {
                    fm.beginTransaction().hide(activeFragment).show(fragment1).commit()
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.action_likes -> {
                    fm.beginTransaction().hide(activeFragment).show(fragment2).commit()
                    return@setOnNavigationItemSelectedListener true
                }
                else -> {
                    fm.beginTransaction().hide(activeFragment).show(fragment3).commit()
                    activeFragment = fragment3
                    return@setOnNavigationItemSelectedListener true
                }
            }
        }
    }
}