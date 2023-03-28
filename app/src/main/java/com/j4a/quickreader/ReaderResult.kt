package com.j4a.quickreader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class ReaderResult : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_result)

        val returnmainbutton = findViewById<Button>(R.id.returnmainbutton)
        returnmainbutton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val copybutton = findViewById<Button>(R.id.copybutton)
        copybutton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}