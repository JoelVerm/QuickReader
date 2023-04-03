package com.j4a.quickreader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class Settings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val returnsettingsbutton = findViewById<Button>(R.id.returnsettingbutton)
        returnsettingsbutton.setOnClickListener {
            finish()
        }
    }
}