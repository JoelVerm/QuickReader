package com.j4a.quickreader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.net.URL

class ReaderResult : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_result)

        val returnmainbutton = findViewById<Button>(R.id.returnmainbutton)
        returnmainbutton.setOnClickListener {
            finish()
        }

        val copybutton = findViewById<Button>(R.id.copybutton)
        copybutton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val browsebutton = findViewById<Button>(R.id.browsebutton)
        browsebutton.setOnClickListener {
            val textBox = findViewById<TextView>(R.id.resultText)
            val start = "http"
            val url = textBox.text.toString()
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            if (url.startsWith(start)){
                startActivity(i)
            } else {
                Toast.makeText(this, "Result is not a url", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun setQRResult(value:String) {
        val textBox = findViewById<TextView>(R.id.resultText)
        textBox.text = value
    }

    private fun copyTextToClipboard() {
        val textBox = findViewById<TextView>(R.id.resultText)
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", textBox.text)
        clipboardManager.setPrimaryClip(clipData)

        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_LONG).show()
    }
}