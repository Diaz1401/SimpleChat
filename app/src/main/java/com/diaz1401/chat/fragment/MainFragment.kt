package com.diaz1401.chat.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.diaz1401.chat.R
import com.diaz1401.chat.databinding.FragmentMainBinding
import com.diaz1401.chat.utilities.LocalConstants
import com.diaz1401.chat.utilities.PreferenceManager

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserDetails()
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

    private fun loadUserDetails() {
        val name = preferenceManager.getString(LocalConstants.KEY_NAME)
        val image = preferenceManager.getString(LocalConstants.KEY_IMAGE)
        binding.txtProfile.text = name

        val bitmap = decodeImage(image)
        if (bitmap != null) {
            binding.imgProfile.setImageBitmap(bitmap)
        } else {
            binding.imgProfile.setImageResource(R.drawable.person) // Use a default image
        }
    }
}