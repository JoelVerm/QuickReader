package com.j4a.quickreader

import android.content.ContentValues
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GenerateResult : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_result)

        val extras = intent.extras
        if (extras == null) {
            finish()
            return
        }
        val text = extras.getString("Text")
        if (text == null) {
            finish()
            return
        }
        val QREncoder = QREncoder()
        val QRData = QREncoder.createQR(text)
        val imageBitWriter = ImageBitWriter()
        var bitmap = imageBitWriter.write(QRData)
        val imageView = findViewById<ImageView>(R.id.resultImage)
        val size = Resources.getSystem().getDisplayMetrics().widthPixels
        Log.d("QR width", size.toString())
        bitmap = bitmap.scale(size, size, false)
        imageView.setImageBitmap(bitmap)

        val savebutton = findViewById<Button>(R.id.savebutton)
        savebutton.setOnClickListener {
            saveImageToStorage(bitmap)
        }

        val returngenresbutton = findViewById<Button>(R.id.returngenresbutton)
        returngenresbutton.setOnClickListener {
            finish()
        }
    }

    private fun saveImageToStorage(bitmap: Bitmap){
        val date = Calendar.getInstance().time
        val formatter = SimpleDateFormat.getDateTimeInstance()
        val formattedDate = formatter.format(date)
        val datetime = formattedDate.toString()
        val relativeLocation = Environment.DIRECTORY_DCIM + File.separator + "qr_$datetime.jpg";

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "qr_$datetime.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
        }

        var uri: Uri = Uri.parse(relativeLocation)

        runCatching {
            with(contentResolver) {
                insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also {
                    uri = it // Keep uri reference so it can be removed on failure

                    openOutputStream(it)?.use { stream ->
                        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream))
                            throw Exception("Failed to save bitmap.")
                    } ?: throw Exception("Failed to open output stream.")

                } ?: throw Exception("Failed to create new MediaStore record.")
            }
            Toast.makeText(this, "QR code saved", Toast.LENGTH_LONG)
        }.getOrElse {
            contentResolver.delete(uri, null, null)
            throw it
        }
    }
}