package com.app.scavenger

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.LifecycleObserver
import androidx.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class BaseApplication: Application(), LifecycleObserver {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var mAuth: FirebaseAuth
    private lateinit var firebaseDB: FirebaseFirestore

    override fun onCreate() {
        super.onCreate()
        instance = this
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mAuth = FirebaseAuth.getInstance()
        firebaseDB = FirebaseFirestore.getInstance()
    }

    fun getFirebaseDatabase(): FirebaseFirestore {
        return firebaseDB
    }

    fun getFirebaseAuth(): FirebaseAuth {
        return mAuth
    }

    fun getCurrentUser(): FirebaseUser? {
        return mAuth.currentUser
    }

    fun getUserId(): String {
        return mAuth.currentUser?.uid ?: ""
    }

    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("logged", false)
    }

    companion object {

        lateinit var instance: BaseApplication
            private set

        const val TAG = "BaseApplication"
        const val NO_APP_NAME = "EmptyApplicationName"

    }
}