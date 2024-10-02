package com.diaz1401.chat.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.diaz1401.chat.database.DatabaseHelper
import com.diaz1401.chat.database.ProfileDAO
import com.diaz1401.chat.databinding.ActivitySignUpBinding
import com.diaz1401.chat.utilities.LocalConstants
import com.diaz1401.chat.utilities.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException


class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private var preferenceManager: PreferenceManager? = null
    private var encodedImage: String? = null
    private val profileDAO = ProfileDAO(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)
        setListeners()
    }

    private fun setListeners() {
        binding.txtSignIn.setOnClickListener {
            onBackPressed()
        }
        binding.btnSignUp.setOnClickListener {
            if (isValidInput()) {
                signUp()
//                signUpSQLite()
            }
        }
        binding.layoutProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setProfileImage.launch(intent)
        }
    }

    private fun signUpSQLite() {
        val name = binding.inputNameSignUp.text.toString()
        val email = binding.inputEmailSignUp.text.toString()
        val password = binding.inputPasswordSignUp.text.toString()
        val id = profileDAO.insertProfile(name, email, encodedImage.toString(), password)
        if (id > 0) {
            preferenceManager?.putBoolean(LocalConstants.KEY_IS_SIGNED_IN, true)
            preferenceManager?.putString(LocalConstants.KEY_USER_ID, id.toString())
            preferenceManager?.putString(LocalConstants.KEY_NAME, name)
            preferenceManager?.putString(LocalConstants.KEY_EMAIL, email)
            preferenceManager?.putString(LocalConstants.KEY_IMAGE, encodedImage)
            showToast("User created successfully")
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else {
            showToast("Error: Unable to insert data")
        }
    }

    private fun signUp() {
        val db = FirebaseFirestore.getInstance()
        val name = binding.inputNameSignUp.text.toString()
        val email = binding.inputEmailSignUp.text.toString()
        val password = binding.inputPasswordSignUp.text.toString()
        val user = hashMapOf(
            LocalConstants.KEY_NAME to name,
            LocalConstants.KEY_EMAIL to email,
            LocalConstants.KEY_PASSWORD to password,
            LocalConstants.KEY_IMAGE to encodedImage,
            LocalConstants.KEY_BIO to null
        )
        db.collection(LocalConstants.KEY_COLLECTION_USERS)
            .add(user)
            .addOnSuccessListener {
                preferenceManager?.putBoolean(LocalConstants.KEY_IS_SIGNED_IN, true)
                preferenceManager?.putString(LocalConstants.KEY_USER_ID, it.id)
                preferenceManager?.putString(LocalConstants.KEY_NAME, name)
                preferenceManager?.putString(LocalConstants.KEY_EMAIL, email)
                preferenceManager?.putString(LocalConstants.KEY_IMAGE, encodedImage)
                preferenceManager?.putString(LocalConstants.KEY_PASSWORD, password)
                preferenceManager?.putString(LocalConstants.KEY_BIO, null)
                showToast("User created successfully")
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            .addOnFailureListener {
                showToast("Error: ${it.message}")
            }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private val setProfileImage: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                if (result.data != null) {
                    val imageUri = result.data?.data
                    try {
                        val inputStream = imageUri?.let { contentResolver.openInputStream(it) }
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.imgProfile.setImageBitmap(bitmap)
                        binding.txtProfile.visibility = View.GONE
                        encodedImage = encodeImage(bitmap)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }
        }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidInput(): Boolean {
        if (encodedImage == null) {
            showToast("Select profile image")
            return false
        } else if (binding.inputNameSignUp.text.toString().trim().isEmpty()) {
            showToast("Enter name")
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmailSignUp.text.toString()).matches()) {
            showToast("Enter valid email")
            return false
        } else if (binding.inputPasswordSignUp.text.toString().trim().isEmpty()) {
            showToast("Enter password")
            return false
        } else if (binding.inputPasswordSignUp.text.toString() != binding.inputConfirmPasswordSignUp.text.toString()
        ) {
            showToast("Password & confirm password must be same")
            return false
        } else {
            return true
        }
    }
}
