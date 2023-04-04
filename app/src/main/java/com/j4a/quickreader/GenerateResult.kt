package com.j4a.quickreader

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class GenerateResult : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_result)

        val returngenresbutton = findViewById<Button>(R.id.returngenresbutton)
        returngenresbutton.setOnClickListener {
            finish()
        }

        val savebutton = findViewById<Button>(R.id.savebutton)
        savebutton.setOnClickListener {
            saveImageToStorage()
        }
    }

    public fun saveImageToStorage(){
        val externalStorageState = Environment.getExternalStorageState()
        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)){
            val storageDirectory = Environment.getExternalStorageDirectory().toString()
            val file = File(storageDirectory, "test_image.jpg")
            try {
                val stream: OutputStream = FileOutputStream(file)
                bitmap: Bitmap
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                stream.flush()
                stream.close()
                Toast.makeText(this, "image saved succesfully ${Uri.parse(file.absolutePath)}",
                        Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "Unable to access storage", Toast.LENGTH_SHORT).show()
        }
    }
}