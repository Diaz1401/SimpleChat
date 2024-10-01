package com.diaz1401.chat.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.diaz1401.chat.R
import com.diaz1401.chat.database.ProfileDAO
import com.diaz1401.chat.databinding.FragmentAccountBinding
import com.diaz1401.chat.utilities.LocalConstants
import com.diaz1401.chat.utilities.PreferenceManager

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var profileDAO: ProfileDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profileDAO = ProfileDAO(requireContext())
        preferenceManager = PreferenceManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayProfile()
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

    private fun displayProfile() {
        val name = preferenceManager.getString(LocalConstants.KEY_NAME)
        val email = preferenceManager.getString(LocalConstants.KEY_EMAIL)
        val image = preferenceManager.getString(LocalConstants.KEY_IMAGE)
        val id = preferenceManager.getString(LocalConstants.KEY_USER_ID)

        binding.txtName.text = name
        binding.txtEmail.text = email
        binding.txtId.text = id
        val bitmap = decodeImage(image)
        if (bitmap != null) {
            binding.imgProfile.setImageBitmap(bitmap)
        } else {
            binding.imgProfile.setImageResource(R.drawable.logo) // Use a default image
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}