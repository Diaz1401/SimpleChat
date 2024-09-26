package com.diaz1401.chat.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.diaz1401.chat.R
import com.diaz1401.chat.databinding.ActivityFileManagementBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SaveToFile : AppCompatActivity() {
    private var editText: EditText? = null

    private lateinit var binding: ActivityFileManagementBinding

    private val STORAGE_PERMISSION_CODE = 23

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        editText = findViewById<View>(R.id.edit_text) as EditText
        binding.savePublicButton.setOnClickListener {
            savePublic(it)
        }
        binding.savePrivateButton.setOnClickListener {
            savePrivate(it)
        }
        binding.viewButton.setOnClickListener {
            view(it)
        }
    }

    private fun view(view: View?) {
        val intent = Intent(
            this@SaveToFile,
            ReadFromFile::class.java
        )
        startActivity(intent)
    }

    private fun savePublic(view: View?) {
        //Permission to access external storage
        ActivityCompat.requestPermissions(
            this,
            arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
        val info = editText!!.text.toString()
        val folder =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) // Folder Name
        val myFile = File(folder, "DataPublic.txt") // Filename
        writeData(myFile, info)
        editText!!.setText("")
    }

    private fun savePrivate(view: View?) {
        val info = editText!!.text.toString()
        val folder = getExternalFilesDir("arvita")
        val myFile = File(folder, "DataPrivate.txt") // Filename
        writeData(myFile, info)
        editText!!.setText("")
    }

    private fun writeData(myFile: File, data: String) {
        var fileOutputStream: FileOutputStream? = null
        try {
            println("TEs")
            fileOutputStream = FileOutputStream(myFile)
            fileOutputStream.write(data.toByteArray())
            Toast.makeText(this, "Done" + myFile.absolutePath, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
