package com.diaz1401.chat.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.fragment.app.Fragment
import com.diaz1401.chat.activities.SaveToFile
import com.diaz1401.chat.database.DatabaseHelper
import com.diaz1401.chat.database.ProfileDAO
import com.diaz1401.chat.databinding.FragmentAccountBinding
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var profileDAO: ProfileDAO
    private var encodedImage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profileDAO = ProfileDAO(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val setProfileImage: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                if (result.data != null) {
                    val imageUri = result.data?.data
                    try {
                        val inputStream = imageUri?.let { requireContext().contentResolver.openInputStream(it) }
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

    private fun encodeImage(bitmap: Bitmap): String {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun decodeImage(base64Str: String): Bitmap {
        val byteArray = Base64.decode(base64Str, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.layoutProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setProfileImage.launch(intent)
        }

        binding.btnSave.setOnClickListener {
            val name = binding.inputName.text.toString()
            val email = binding.inputEmail.text.toString()
            val image = encodedImage

            val id = profileDAO.insertProfile(name, email, image.toString())
            if (id > 0) {
                Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show()
                displayAccounts()
            } else {
                Toast.makeText(requireContext(), "Error saving profile", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnUpdate.setOnClickListener {
            val id = binding.inputID.text.toString().toLong()
            val name = binding.inputName.text.toString()
            val email = binding.inputEmail.text.toString()
            val image = encodedImage

            val rows = profileDAO.updateProfile(id, name, email, image.toString())
            if (rows > 0) {
                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                displayAccounts()
            } else {
                Toast.makeText(requireContext(), "Error updating profile", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDelete.setOnClickListener {
            val id = binding.inputID.text.toString().toLongOrNull()
            if (id == null) {
                Toast.makeText(requireContext(), "Invalid ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val rows = profileDAO.deleteProfile(id)
            if (rows > 0) {
                Toast.makeText(requireContext(), "Profile deleted", Toast.LENGTH_SHORT).show()
                displayAccounts()
            } else {
                Toast.makeText(requireContext(), "Error deleting profile", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLoad.setOnClickListener {
            val id = binding.inputID.text.toString().toLong()
            val cursor = profileDAO.getProfile(id)
            if (cursor.moveToFirst()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
                val email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMAIL))
                val image = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IMAGE))

                binding.inputID.setText(id.toString())
                binding.inputName.setText(name)
                binding.inputEmail.setText(email)
                binding.imgProfile.setImageBitmap(decodeImage(image))
            } else {
                Toast.makeText(requireContext(), "Profile not found", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnFile.setOnClickListener {
            val intent = Intent(requireContext(), SaveToFile::class.java)
            startActivity(intent)
        }

        displayAccounts()
    }

    private fun displayAccounts() {
        val cursor = profileDAO.getAllProfiles()
        val accounts = StringBuilder()
        while (cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
            val email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMAIL))
            accounts.append("ID: $id, Name: $name, Email: $email\n")
        }
        binding.tvAccounts.text = accounts.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}