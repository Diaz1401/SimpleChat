package com.diaz1401.chat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

class ProfileDAO(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun insertProfile(name: String, email: String, image: String): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_NAME, name)
            put(DatabaseHelper.COLUMN_EMAIL, email)
            put(DatabaseHelper.COLUMN_IMAGE, image)
        }
        return db.insert(DatabaseHelper.TABLE_PROFILE, null, values)
    }

    fun getProfile(id: Long): Cursor {
        val db = dbHelper.readableDatabase
        return db.query(
            DatabaseHelper.TABLE_PROFILE,
            null,
            "${DatabaseHelper.COLUMN_ID}=?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
    }

    fun getAllProfiles(): Cursor {
        val db = dbHelper.readableDatabase
        return db.query(
            DatabaseHelper.TABLE_PROFILE,
            null,
            null,
            null,
            null,
            null,
            null
        )
    }

    fun updateProfile(id: Long, name: String, email: String, image: String): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_NAME, name)
            put(DatabaseHelper.COLUMN_EMAIL, email)
            put(DatabaseHelper.COLUMN_IMAGE, image)
        }
        return db.update(DatabaseHelper.TABLE_PROFILE, values, "${DatabaseHelper.COLUMN_ID}=?", arrayOf(id.toString()))
    }

    fun deleteProfile(id: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(DatabaseHelper.TABLE_PROFILE, "${DatabaseHelper.COLUMN_ID}=?", arrayOf(id.toString()))
    }
}