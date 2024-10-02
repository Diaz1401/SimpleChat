package com.diaz1401.chat.activities

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.diaz1401.chat.R
import com.diaz1401.chat.database.DatabaseHelper
import com.diaz1401.chat.database.ProfileDAO
import com.diaz1401.chat.databinding.ActivityEditProfileBinding
import com.diaz1401.chat.utilities.LocalConstants
import com.diaz1401.chat.utilities.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

class EditProfileActivity : AppCompatActivity() {

    private var _binding: ActivityEditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var profileDAO: ProfileDAO
    private var encodedImage: String? = null
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        preferenceManager = PreferenceManager(this)
        profileDAO = ProfileDAO(this)
        _binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        loadProfile()
    }

    private fun loadProfile() {
        val name = preferenceManager.getString(LocalConstants.KEY_NAME)
        val email = preferenceManager.getString(LocalConstants.KEY_EMAIL)
        encodedImage = preferenceManager.getString(LocalConstants.KEY_IMAGE)
        val bio = preferenceManager.getString(LocalConstants.KEY_BIO)

        if (!bio.isNullOrEmpty()) {
            binding.inputBio.setText(bio)
        }

        binding.inputName.setText(name)
        binding.inputEmail.setText(email)
        val bitmap = decodeImage(encodedImage)
        if (bitmap != null) {
            binding.imgProfile.setImageBitmap(bitmap)
        } else {
            binding.imgProfile.setImageResource(R.drawable.person) // Use a default image
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidInput(): Boolean {
        if (binding.inputName.text.toString().trim().isEmpty()) {
            showToast("Enter name")
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.text.toString()).matches()) {
            showToast("Enter valid email")
            return false
        } else if (binding.inputPW.text.toString().trim().isEmpty()) {
            showToast("Enter password")
            return false
        } else if (binding.inputPW.text.toString() != preferenceManager.getString(LocalConstants.KEY_PASSWORD)) {
            showToast("Invalid password")
            return false
        } else {
            return true
        }
    }

    private fun updateDataFirebase() {
        val db = FirebaseFirestore.getInstance()
        val user = hashMapOf(
            LocalConstants.KEY_NAME to binding.inputName.text.toString(),
            LocalConstants.KEY_EMAIL to binding.inputEmail.text.toString(),
            LocalConstants.KEY_IMAGE to encodedImage,
            LocalConstants.KEY_PASSWORD to binding.inputPW.text.toString(),
            LocalConstants.KEY_BIO to binding.inputBio.text.toString()
        )
        val id = preferenceManager.getString(LocalConstants.KEY_USER_ID)
        if (id != null) {
            db.collection(LocalConstants.KEY_COLLECTION_USERS)
                .document(id)
                .set(user)
                .addOnSuccessListener {
                    showToast("Profile updated")
                }
                .addOnFailureListener {
                    showToast("Error updating profile")
                }
        }
    }

    private fun updateDataSQLite() {
        val id = preferenceManager.getString(LocalConstants.KEY_USER_ID)?.toLongOrNull()
        val name = binding.inputName.text.toString()
        val email = binding.inputEmail.text.toString()
        val image = encodedImage
        val password = binding.inputPW.text.toString()
        if (id != null) {
            val rows = profileDAO.updateProfile(id, name, email, image.toString(), password)
            if (rows > 0) {
                showToast("Profile updated")
            } else {
                showToast("Error updating profile")
            }
        }
    }

    private fun deleteDataFirebase() {
        val db = FirebaseFirestore.getInstance()
        val id = preferenceManager.getString(LocalConstants.KEY_USER_ID)
        if (id != null) {
            db.collection(LocalConstants.KEY_COLLECTION_USERS)
                .document(id)
                .delete()
                .addOnSuccessListener {
                    showToast("Profile deleted")
                }
                .addOnFailureListener {
                    showToast("Error deleting profile")
                }
        }
    }

    private fun deleteDataSQLite() {
        val id = preferenceManager.getString(LocalConstants.KEY_USER_ID)?.toLongOrNull()
        if (id != null) {
            val rows = profileDAO.deleteProfile(id)
            if (rows > 0) {
                showToast("Profile deleted")
            } else {
                showToast("Error deleting profile")
            }
        }
    }

    private fun setListeners() {
        binding.layoutProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setProfileImage.launch(intent)
        }

        binding.btnUpdate.setOnClickListener {
            if (!isValidInput()) {
                return@setOnClickListener
            }
//            updateDataSQLite()
            updateDataFirebase()
        }

        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete this account?")
                .setPositiveButton("Yes") { _, _ ->
//                    deleteDataSQLite()
                    deleteDataFirebase()
                    preferenceManager.clear()
                    val intent = Intent(this, SignInActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                }
                .setNegativeButton("No", null)
                .show()
        }
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
                        encodedImage = encodeImage(bitmap)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
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

    private fun decodeImage(base64Str: String?): Bitmap? {
        return if (!base64Str.isNullOrEmpty()) {
            try {
                val byteArray = Base64.decode(base64Str, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}