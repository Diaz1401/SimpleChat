package com.diaz1401.chat.activities

import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.diaz1401.chat.R
import com.diaz1401.chat.databinding.ActivityFileManagement2Binding
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class ReadFromFile : AppCompatActivity() {
    private var showText: TextView? = null
    private lateinit var binding: ActivityFileManagement2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileManagement2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        showText = findViewById<View>(R.id.getText) as TextView
        binding.button2.setOnClickListener {
            getPrivate(it)
        }
        binding.button3.setOnClickListener {
            getPublic(it)
        }
        binding.button5.setOnClickListener {
            back(it)
        }
    }

    private fun back(view: View?) {
        finish()
    }

    private fun getPublic(view: View?) {
        val folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val myFile = File(folder, "DataPublic.txt")
        val text = getData(myFile)
        if (text != null) {
            showText!!.text = text
        } else {
            showText!!.text = "ERROR"
        }
    }

    private fun getPrivate(view: View?) {
        val folder = getExternalFilesDir("arvita")
        val myFile = File(folder, "DataPrivate.txt")
        val text = getData(myFile)
        if (text != null) {
            showText!!.text = text
        } else {
            showText!!.text = "ERROR"
        }
    }

    private fun getData(myFile: File): String? {
        var fileInputStream: FileInputStream? = null
        try {
            fileInputStream = FileInputStream(myFile)
            var data: Int
            val stringBuilder = StringBuilder()
            while ((fileInputStream.read().also { data = it }) != -1) {
                stringBuilder.append(data.toChar())
            }
            return stringBuilder.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }
}
