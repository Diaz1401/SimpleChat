package com.diaz1401.chat.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.diaz1401.chat.database.DatabaseHelper
import com.diaz1401.chat.database.ProfileDAO
import com.diaz1401.chat.databinding.ActivitySignInBinding
import com.diaz1401.chat.utilities.LocalConstants
import com.diaz1401.chat.utilities.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var preferenceManager: PreferenceManager
    private var profileDAO = ProfileDAO(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        preferenceManager = PreferenceManager(applicationContext)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
    }

    private fun setListeners() {
        binding.txtSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        binding.btnSignIn.setOnClickListener {
            if (isValidInput()) {
                signInSQLite()
            }
        }
    }

    private fun signInSQLite() {
        val email = binding.inputEmailSignIn.text.toString()
        val password = binding.inputPasswordSignIn.text.toString()
        val cursor = profileDAO.getSignIn(email, password)
        if (cursor.count > 0) {
            if (cursor.moveToFirst()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
                val image = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IMAGE))
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                preferenceManager.putBoolean(LocalConstants.KEY_IS_SIGNED_IN, true)
                preferenceManager.putString(LocalConstants.KEY_USER_ID, id.toString())
                preferenceManager.putString(LocalConstants.KEY_NAME, name)
                preferenceManager.putString(LocalConstants.KEY_EMAIL, email)
                preferenceManager.putString(LocalConstants.KEY_IMAGE, image)
                startActivity(Intent(this, MainActivity::class.java))
            }
        } else {
            showToast("Unable to sign in")
        }
        cursor.close()
    }

    private fun signIn() {
        val db = FirebaseFirestore.getInstance()
        db.collection(LocalConstants.KEY_COLLECTION_USERS)
            .whereEqualTo(LocalConstants.KEY_EMAIL, binding.inputEmailSignIn.text.toString())
            .whereEqualTo(LocalConstants.KEY_PASSWORD, binding.inputPasswordSignIn.text.toString())
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null && task.result!!.documents.size > 0) {
                    val documentSnapshot = task.result!!.documents[0]
                    preferenceManager.putBoolean(LocalConstants.KEY_IS_SIGNED_IN, true)
                    preferenceManager.putString(LocalConstants.KEY_USER_ID, documentSnapshot.id)
                    preferenceManager.putString(LocalConstants.KEY_NAME, documentSnapshot.getString(LocalConstants.KEY_NAME))
                    preferenceManager.putString(LocalConstants.KEY_IMAGE, documentSnapshot.getString(LocalConstants.KEY_IMAGE))
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    showToast("Unable to sign in")
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidInput(): Boolean {
        if (binding.inputEmailSignIn.text.toString().trim().isEmpty()) {
            showToast("Enter email")
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmailSignIn.text.toString().trim())
                .matches()
        ) {
            showToast("Enter valid email")
            return false
        } else if (binding.inputPasswordSignIn.text.toString().trim().isEmpty()) {
            showToast("Enter password")
            return false
        }

        return true
    }

}
