package com.j4a.quickreader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class GenerateResult : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_result)

        var returngenresbutton = findViewById<Button>(R.id.returngenresbutton)
        returngenresbutton.setOnClickListener {
            val intent = Intent(this, GenerateQr::class.java)
            startActivity(intent)
        }
    }
}