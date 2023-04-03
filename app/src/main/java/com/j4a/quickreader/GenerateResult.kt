package com.j4a.quickreader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

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
        val bitmap = imageBitWriter.write(QRData)
        val imageView = findViewById<ImageView>(R.id.resultImage)
        imageView.setImageBitmap(bitmap)

        val returngenresbutton = findViewById<Button>(R.id.returngenresbutton)
        returngenresbutton.setOnClickListener {
            finish()
        }
    }
}