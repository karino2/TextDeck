package io.github.karino2.textdeck

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView

class SetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        //             webView.loadUrl("file:///android_asset/fretboard.html")
        val webView = findViewById<WebView>(R.id.webView)!!
        val html = getString(R.string.html_name)
        webView.loadUrl("file:///android_asset/$html")
    }
}