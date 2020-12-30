package io.github.karino2.textdeck

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import io.github.karino2.listtextview.ListTextView
import kotlinx.coroutines.*
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import kotlin.coroutines.CoroutineContext


class EditorActivity : AppCompatActivity(), CoroutineScope {
    companion object {
        const val REQUEST_EDIT_CELL_CODE=1
        const val  LAST_URI_KEY = "last_uri_path"
        const val  LAST_READ_TIME_KEY = "last_read_time"
        const val SHORT_ENOUGH = 1*60*1000

        fun lastUriStr(ctx: Context) = sharedPreferences(ctx).getString(LAST_URI_KEY, null)
        fun writeLastUriStr(ctx: Context, path : String) = sharedPreferences(ctx).edit()
            .putString(LAST_URI_KEY, path)
            .commit()

        fun lastReadTime(ctx: Context) = sharedPreferences(ctx).getLong(LAST_READ_TIME_KEY, 0)
        fun writeLastReadTime(ctx: Context, time : Long) = sharedPreferences(ctx).edit()
            .putLong(LAST_READ_TIME_KEY, time)
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
        checkUpdate()
    }

    private fun checkUpdate() {
        val now = (Date()).time
        val last = lastReadTime(this)

        if (last != 0L && (now-last) < SHORT_ENOUGH )
            return

        lastUriStr(this) ?: return

        writeLastReadTime(this, now)
        launch(Dispatchers.IO) {
            val desc = contentResolver.openFileDescriptor(lastUri, "r")!!
            val fis = FileInputStream(desc.fileDescriptor)
            val text = fis.bufferedReader().use { it.readText() }
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
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
            finish()
            return
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
        writeLastReadTime(this, (Date()).time)
        val desc = contentResolver.openFileDescriptor(uri, "r")!!
        val fis = FileInputStream(desc.fileDescriptor)
        val text = fis.bufferedReader().use { it.readText() }
        listTextView.text = text
        lastReadContent = text
    }
}