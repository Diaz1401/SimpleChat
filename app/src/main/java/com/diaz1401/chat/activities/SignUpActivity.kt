package com.diaz1401.chat.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.diaz1401.chat.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
    }

    private fun setListeners() {
        binding.txtSignIn.setOnClickListener {
            finish()
        }
    }
}
