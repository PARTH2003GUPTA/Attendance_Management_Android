package com.example.attendance

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class UploadAttendanceActivity : AppCompatActivity() {
    private lateinit var db: SQLiteDatabase
    private lateinit var listViewAttendance: ListView
    private val client = OkHttpClient()
    private val REQUEST_WRITE_PERMISSION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_attendance)

        db = openOrCreateDatabase("AttendanceDB", MODE_PRIVATE, null)
        listViewAttendance = findViewById(R.id.listViewAttendance)

        loadAttendanceData()
    }

    private fun loadAttendanceData() {
        val cursor = db.rawQuery(
            "SELECT DISTINCT date_time, course_id, lecture_type FROM Attendance ORDER BY date_time DESC",
            null
        )
        val attendanceList = ArrayList<HashMap<String, String>>()

        while (cursor.moveToNext()) {
            val data = HashMap<String, String>()
            data["date_time"] = cursor.getString(0)
            data["course_id"] = cursor.getString(1)
            data["lecture_type"] = cursor.getString(2)
            attendanceList.add(data)
        }
        cursor.close()

        val adapter = SimpleAdapter(
            this,
            attendanceList,
            android.R.layout.simple_list_item_2,
            arrayOf("date_time", "course_id"),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )
        listViewAttendance.adapter = adapter

        listViewAttendance.setOnItemClickListener { _, _, position, _ ->
            val data = attendanceList[position]
            val dateTime = data["date_time"]!!
            val courseId = data["course_id"]!!
            val lectureType = data["lecture_type"]!!

            showAttendanceOptions(dateTime, courseId, lectureType)
        }
    }

    private fun showAttendanceOptions(dateTime: String, courseId: String, lectureType: String) {
        val options = arrayOf("View Attendance", "Edit Attendance", "Download Excel", "Upload Excel")
        AlertDialog.Builder(this)
            .setTitle("Options for $courseId on $dateTime")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewAttendance(dateTime, courseId)
                    1 -> editAttendance(dateTime, courseId)
                    2 -> downloadAttendanceAsExcel(dateTime, courseId, lectureType)
                    3 -> uploadExcel(dateTime, courseId)
                }
            }
            .create()
            .show()
    }

    private fun viewAttendance(dateTime: String, courseId: String) {
        val cursor = db.rawQuery(
            "SELECT enrollment_no, name, status FROM Attendance WHERE date_time = ? AND course_id = ?",
            arrayOf(dateTime, courseId)
        )

        val attendanceRecords = ArrayList<String>()
        while (cursor.moveToNext()) {
            val enrollmentNo = cursor.getString(0)
            val name = cursor.getString(1)
            val status = if (cursor.getInt(2) == 1) "Present" else "Absent"
            attendanceRecords.add("$enrollmentNo - $name: $status")
        }
        cursor.close()

        AlertDialog.Builder(this)
            .setTitle("Attendance Records")
            .setItems(attendanceRecords.toTypedArray(), null)
            .setPositiveButton("OK", null)
            .create()
            .show()
    }

    private fun editAttendance(dateTime: String, courseId: String) {
        val cursor = db.rawQuery(
            "SELECT record_id, enrollment_no, name, status FROM Attendance WHERE date_time = ? AND course_id = ?",
            arrayOf(dateTime, courseId)
        )

        val attendanceList = mutableListOf<Pair<Int, String>>()
        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        while (cursor.moveToNext()) {
            val recordId = cursor.getInt(0)
            val enrollmentNo = cursor.getString(1)
            val name = cursor.getString(2)
            val status = if (cursor.getInt(3) == 1) "Present" else "Absent"
            val record = "$enrollmentNo - $name: $status"
            attendanceList.add(recordId to record)

            val editText = EditText(this).apply {
                setText(record)
                setPadding(0, 8, 0, 8)
            }
            linearLayout.addView(editText)

            val saveButton = Button(this).apply {
                text = "Save"
                setOnClickListener {
                    val newStatus = if (editText.text.toString().contains("Present")) 1 else 0
                    updateAttendanceStatus(recordId, newStatus)
                }
            }
            linearLayout.addView(saveButton)
        }
        cursor.close()

        ScrollView(this).apply {
            addView(linearLayout)
            AlertDialog.Builder(this@UploadAttendanceActivity)
                .setTitle("Edit Attendance")
                .setView(this)
                .setPositiveButton("OK", null)
                .create()
                .show()
        }
    }

    private fun updateAttendanceStatus(recordId: Int, status: Int) {
        val contentValues = ContentValues().apply { put("status", status) }
        db.update("Attendance", contentValues, "record_id = ?", arrayOf(recordId.toString()))
        Toast.makeText(this, "Attendance updated!", Toast.LENGTH_SHORT).show()
    }

    private fun downloadAttendanceAsExcel(dateTime: String, courseId: String, lectureID: String) {
        val cursor = db.rawQuery(
            "SELECT * FROM Attendance WHERE date_time = ? AND course_id = ?",
            arrayOf(dateTime, courseId)
        )

        if (cursor.count > 0) {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Attendance")

            // Create header
            val headerRow = sheet.createRow(0)
            val headers = arrayOf(
                "Record ID", "Enrollment Number", "Name", "Date Time", "Status", "Course ID", "Start Time" , "End Time","Day","Lecture Type","Faculty ID"
            )
            headers.forEachIndexed { index, header -> headerRow.createCell(index).setCellValue(header) }

            // Fill data
            var rowIndex = 1
            while (cursor.moveToNext()) {
                val row = sheet.createRow(rowIndex++)
                for (i in 0 until cursor.columnCount) {
                    row.createCell(i).setCellValue(cursor.getString(i))
                }
            }
            cursor.close()

            val path = "${getExternalFilesDir(null)}/${courseId}_$dateTime.xlsx"
            val file = File(path)
            try {
                FileOutputStream(file).use { workbook.write(it) }
                Toast.makeText(this, "Excel file saved at $path", Toast.LENGTH_SHORT).show()
                openFile(file)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "No data to download", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFile(file: File) {
        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    private fun uploadExcel(dateTime: String, courseId: String) {
        val path = "${getExternalFilesDir(null)}/${courseId}_$dateTime.xlsx"
        val file = File(path)

        if (file.exists()) {
            val requestBody = RequestBody.create(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".toMediaTypeOrNull(),
                file
            )
            val request = Request.Builder()
                .url("YOUR_UPLOAD_URL")
                .post(requestBody)
                .build()

            Thread {
                val response = client.newCall(request).execute()
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this, "File uploaded successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "File upload failed.", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        } else {
            Toast.makeText(this, "Excel file not found to upload", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_WRITE_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
        }
    }
}
