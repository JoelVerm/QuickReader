package com.j4a.quickreader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ReaderResult : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_result)

        val extras = intent.extras
        if (extras != null) {
            val resultText = extras.getString("QRResult")
            val textBox = findViewById<TextView>(R.id.resultText)
            textBox.text = resultText
        }

        val returnmainbutton = findViewById<Button>(R.id.returnmainbutton)
        returnmainbutton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val copybutton = findViewById<Button>(R.id.copybutton)
        copybutton.setOnClickListener {
            copyTextToClipboard()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val browsebutton = findViewById<Button>(R.id.browsebutton)
        browsebutton.setOnClickListener {
            val textBox = findViewById<TextView>(R.id.resultText)
            val url = textBox.text.toString()
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
    }

    private fun copyTextToClipboard() {
        val textBox = findViewById<TextView>(R.id.resultText)
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", textBox.text)
        clipboardManager.setPrimaryClip(clipData)

        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_LONG).show()
    }
}