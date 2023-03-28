package com.j4a.quickreader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class GenerateQr : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_qr)

        var returngenbutton = findViewById<Button>(R.id.returngenbutton)
        returngenbutton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        var generatebutton = findViewById<Button>(R.id.generatebutton)
        generatebutton.setOnClickListener {
            val intent = Intent(this, GenerateResult::class.java)
            startActivity(intent)
        }
    }
}