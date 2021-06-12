package io.github.karino2.textdeck

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import io.github.karino2.listtextview.ListTextView
import kotlinx.coroutines.*
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.*
import kotlin.coroutines.CoroutineContext


class EditorActivity : AppCompatActivity(), CoroutineScope {
    companion object {
        const val REQUEST_EDIT_CELL_CODE=1
        const val REQUEST_SETTING_CODE=2
        const val  LAST_URI_KEY = "last_uri_path"

        fun lastUriStr(ctx: Context) = sharedPreferences(ctx).getString(LAST_URI_KEY, null)
        fun writeLastUriStr(ctx: Context, path : String) = sharedPreferences(ctx).edit()
            .putString(LAST_URI_KEY, path)
            .commit()

        private fun sharedPreferences(ctx: Context) = ctx.getSharedPreferences("TEXT_DECK_PREFS", Context.MODE_PRIVATE)

        fun showMessage(ctx: Context, msg : String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onStart() {
        job = Job()
        super.onStart()
    }

    private fun checkUpdate() {
        launch(Dispatchers.IO) {
            val text = contentResolver.openFileDescriptor(lastUri, "r")!!.use { desc ->
                val fis = FileInputStream(desc.fileDescriptor)
                fis.bufferedReader().use { it.readText() }
            }
            if (text == lastReadContent)
                return@launch

            lastReadContent = text
            withContext(Dispatchers.Main)
            {
                listTextView.text = text
            }
        }
    }

    override fun onStop() {
        super.onStop()
        job.cancel()
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

    var lastReadContent : String = ""

    fun saveFile() {
        contentResolver.openFileDescriptor(lastUri, "w")!!.use {desc->
            val fos = FileOutputStream(desc.fileDescriptor)
            fos.use {
                it.write(listTextView.text.toByteArray())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

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
            gotoSettings()
            return
        }

        try {
            openUri(lastUri)
        } catch( e: FileNotFoundException) {
            gotoSettings()
        } catch( _:  SecurityException) {
            showMessage(this, "Can't open file...")
            gotoSettings()
        }

    }

    private fun gotoSettings() {
        val intent = Intent(this, SetupActivity::class.java)
        startActivityForResult(intent, REQUEST_SETTING_CODE)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId)
        {
            R.id.menu_item_refresh-> {
                showMessage(this, "check update")
                checkUpdate()
                return true
            }
            R.id.menu_item_preferences -> {
                gotoSettings()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            REQUEST_EDIT_CELL_CODE -> {
                data?.let {
                    if(listTextView.handleOnActivityResult(requestCode, resultCode, it)) {
                        return
                    }
                }
            }
            REQUEST_SETTING_CODE -> {
                if (resultCode == RESULT_OK)
                {
                    openUri(lastUri)
                    return
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun openUri(uri: Uri) {
        val text = contentResolver.openFileDescriptor(uri, "r")!!.use {desc->
            val fis = FileInputStream(desc.fileDescriptor)
            fis.bufferedReader().use { it.readText() }
        }
        listTextView.text = text
        lastReadContent = text
    }
}