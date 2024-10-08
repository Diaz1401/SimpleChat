package com.diaz1401.chat.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.diaz1401.chat.R
import com.diaz1401.chat.databinding.ActivityMainBinding
import com.diaz1401.chat.utilities.LocalConstants
import com.diaz1401.chat.utilities.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)
        val navView: BottomNavigationView = binding.navbarView

        // Ensure the correct ID is used for the NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentView) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_main, R.id.navigation_account
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        getToken()
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateToken(token: String) {
        val db = FirebaseFirestore.getInstance()
        val documentReference = db.collection(LocalConstants.KEY_COLLECTION_USERS).document(preferenceManager.getString(
            LocalConstants.KEY_USER_ID)!!)
        documentReference.update(LocalConstants.KEY_FCM_TOKEN, token).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showToast("Token updated successfully")
            } else {
                showToast("Unable to update token")
            }
        }
    }

    private fun getToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (task.isSuccessful) {
                updateToken(task.result!!)
            }
        })
    }
}