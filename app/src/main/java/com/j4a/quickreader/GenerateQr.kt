package com.j4a.quickreader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class GenerateQr : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_qr)

        val returngenbutton = findViewById<Button>(R.id.returngenbutton)
        returngenbutton.setOnClickListener {
            finish()
        }

        val generatebutton = findViewById<Button>(R.id.generatebutton)
        generatebutton.setOnClickListener {
            val textBox = findViewById<EditText>(R.id.editText)
            val text = textBox.text.toString()
            val intent = Intent(this, GenerateResult::class.java)
            intent.putExtra("Text", text)
            startActivity(intent)
        }
    }
}