package io.github.karino2.textdeck

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import io.github.karino2.listtextview.ListTextView
import java.io.FileInputStream
import java.io.FileOutputStream


class EditorActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_EDIT_CELL_CODE=1
        const val  LAST_URI_KEY = "last_uri_path"

        fun lastUriStr(ctx: Context) = sharedPreferences(ctx).getString(LAST_URI_KEY, null)
        fun writeLastUriStr(ctx: Context, path : String) = sharedPreferences(ctx).edit()
            .putString(LAST_URI_KEY, path)
            .commit()

        private fun sharedPreferences(ctx: Context) = ctx.getSharedPreferences("TEXT_DECK_PREFS", Context.MODE_PRIVATE)

        fun showMessage(ctx: Context, msg : String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    val listTextView: ListTextView by lazy {
        findViewById<ListTextView>(R.id.listTextView).apply {
            this.editActivityRequestCode = REQUEST_EDIT_CELL_CODE
            this.owner = this@EditorActivity
            this.onDatasetChangedListener = { saveFile() }
        }
    }

    val lastUri : Uri
       get() = Uri.parse(lastUriStr(this))!!

    fun saveFile() {
        val desc = contentResolver.openFileDescriptor(lastUri, "w")!!
        val fos = FileOutputStream(desc.fileDescriptor)
        fos.use {
            it.write(listTextView.text.toByteArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        intent?.let {
            if (it.action == Intent.ACTION_VIEW)
            {
                it.data?.let { uri ->
                    writeLastUriStr(this, uri.toString())
                }
            }
        }

        val urlstr = lastUriStr(this)

        if(urlstr == null)
        {
            Toast.makeText(this, "Use via ACTION_VIEW for the first time.", Toast.LENGTH_SHORT).show()
            finish()
        }

        setContentView(R.layout.activity_editor)
        openUri(lastUri)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        data?.let {
            if(listTextView.handleOnActivityResult(requestCode, resultCode, it)) {
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun openUri(uri: Uri) {
        val desc = contentResolver.openFileDescriptor(uri, "r")!!
        val fis = FileInputStream(desc.fileDescriptor)
        val text = fis.bufferedReader().use { it.readText() }
        listTextView.text = text
    }
}