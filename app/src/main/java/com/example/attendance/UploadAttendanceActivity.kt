package com.example.attendance

import android.Manifest
import android.content.ContentValues
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
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
        val options = arrayOf("View Attendance", "Edit Attendance", "Download Excel", "Upload Attendance","Delete Attendance")
        AlertDialog.Builder(this)
            .setTitle("Options for $courseId on $dateTime")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewAttendance(dateTime, courseId)
                    1 -> editAttendance(dateTime, courseId)
                    2 -> downloadAttendanceAsExcel(dateTime, courseId, lectureType)
                    3 -> uploadAttendance(dateTime, courseId)
                    4 -> deleteAttendance(dateTime, courseId)
                }
            }
            .create()
            .show()
    }
    private fun deleteAttendance(dateTime: String, courseId: String) {
        // Confirm deletion
        AlertDialog.Builder(this)
            .setTitle("Delete Attendance")
            .setMessage("Are you sure you want to delete the attendance for $courseId on $dateTime?")
            .setPositiveButton("Yes") { _, _ ->
                val deleteQuery = "DELETE FROM Attendance WHERE date_time = ? AND course_id = ?"
                db.execSQL(deleteQuery, arrayOf(dateTime, courseId))
                Toast.makeText(this, "Attendance deleted successfully!", Toast.LENGTH_SHORT).show()
                loadAttendanceData() // Refresh the attendance list
            }
            .setNegativeButton("No", null)
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

        val attendanceMap = mutableListOf<Pair<Int, CheckBox>>()
        val scrollView = ScrollView(this)
        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        while (cursor.moveToNext()) {
            val recordId = cursor.getInt(0)
            val enrollmentNo = cursor.getString(1)
            val name = cursor.getString(2)
            val status = cursor.getInt(3)

            // Create a TextView for displaying the student's details
            val textView = TextView(this).apply {
                text = "$enrollmentNo - $name"
                setPadding(0, 8, 0, 8)
            }
            linearLayout.addView(textView)

            // Create a CheckBox for marking attendance
            val checkBox = CheckBox(this).apply {
                text = "Absent"
                isChecked = status == 0 // Checked if Absent
            }
            attendanceMap.add(recordId to checkBox)
            linearLayout.addView(checkBox)
        }
        cursor.close()

        // Add a single Save Attendance button at the bottom
        val saveButton = Button(this).apply {
            text = "Save Attendance"
            setOnClickListener {
                // Loop through the attendanceMap and update the database for each record
                for ((recordId, checkBox) in attendanceMap) {
                    val newStatus = if (checkBox.isChecked) 0 else 1
                    updateAttendanceStatus(recordId, newStatus)
                }
                Toast.makeText(this@UploadAttendanceActivity, "Attendance updated!", Toast.LENGTH_SHORT).show()
            }
        }
        linearLayout.addView(saveButton)

        scrollView.addView(linearLayout)

        // Display everything in an AlertDialog
        AlertDialog.Builder(this)
            .setTitle("Edit Attendance for $courseId on $dateTime")
            .setView(scrollView)
            .setPositiveButton("OK") { _, _ ->
                // Optionally refresh the attendance list after closing
                loadAttendanceData()
            }
            .create()
            .show()
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
                "Record ID", "Enrollment Number", "Name", "Date Time", "Status", "Course ID", "Start Time" , "End Time","Lecture Type","Lecture Count","Faculty ID"
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



    private fun uploadAttendance(dateTime: String, courseId: String) {
        val cursor = db.rawQuery(
            """
        SELECT enrollment_no, name, status, date_time, course_id, start_time, end_time, lecture_type, faculty_id, lecture_count 
        FROM Attendance WHERE date_time = ? AND course_id = ?
        """, arrayOf(dateTime, courseId)
        )

        val attendanceList = ArrayList<Map<String, Any>>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) // Current format
        val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) // ISO format

        // Dynamically fetch column indices
        val enrollmentNoIndex = cursor.getColumnIndex("enrollment_no")
        val statusIndex = cursor.getColumnIndex("status")
        val dateTimeIndex = cursor.getColumnIndex("date_time")
        val lectureTypeIndex = cursor.getColumnIndex("lecture_type")
        val facultyIdIndex = cursor.getColumnIndex("faculty_id")
        val startTimeIndex = cursor.getColumnIndex("start_time")
        val endTimeIndex = cursor.getColumnIndex("end_time")
        val lectureCountIndex = cursor.getColumnIndex("lecture_count")
        val courseIdIndex = cursor.getColumnIndex("course_id")

        // Read data from the cursor
        while (cursor.moveToNext()) {
            try {
                val record = mapOf(
                    "attendance" to (cursor.getInt(statusIndex) == 1), // Convert 1 or 0 to true/false
                    "classDate" to isoDateFormat.format(dateFormat.parse(cursor.getString(dateTimeIndex)!!)),
                    "classType" to cursor.getString(lectureTypeIndex),
                    "createdBy" to cursor.getString(facultyIdIndex),
                    "enrollmentId" to cursor.getString(enrollmentNoIndex),
                    "fromTime" to cursor.getString(startTimeIndex),
                    "lectureCount" to cursor.getInt(lectureCountIndex),
                    "subjectCode" to cursor.getString(courseIdIndex),
                    "toTime" to cursor.getString(endTimeIndex)
                )
                attendanceList.add(record)
            } catch (e: Exception) {
                Log.e("UploadAttendance", "Error processing record: ${e.message}")
            }
        }
        cursor.close()

        // Convert the attendance list directly to JSON
        val jsonBody = attendanceList.toJson()  // We need to ensure the JSON is correctly formatted
        Log.d("AttendanceUpload", "Sending attendance data: $jsonBody")

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)
        val request = Request.Builder()
            .url("http://10.0.2.2:8081/api/student/attendance")  // Adjust the URL accordingly
            .post(requestBody)
            .build()

        Thread {
            val response = client.newCall(request).execute()
            runOnUiThread {
                if (response.isSuccessful) {
                    deleteAttendanceFromDatabase(dateTime, courseId)
                    Toast.makeText(this, "Attendance uploaded successfully!", Toast.LENGTH_SHORT).show()
                    loadAttendanceData()
                } else {
                    Toast.makeText(this, "Failed to upload attendance: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    private fun deleteAttendanceFromDatabase(dateTime: String, courseId: String) {
        val deleteQuery = "DELETE FROM Attendance WHERE date_time = ? AND course_id = ?"
        db.execSQL(deleteQuery, arrayOf(dateTime, courseId))
    }

    // Helper function to convert list to JSON format
    private fun List<Map<String, Any>>.toJson(): String {
        val sb = StringBuilder("[")
        forEachIndexed { index, map ->
            sb.append("{")
            map.entries.joinToString(", ") { "\"${it.key}\":\"${it.value}\"" }.let {
                sb.append(it)
            }
            sb.append("}")
            if (index < size - 1) sb.append(",")  // Only add commas between objects
        }
        sb.append("]")
        return sb.toString()
    }
    private fun updateAttendanceStatus(recordId: Int, status: Int) {
        val contentValues = ContentValues().apply { put("status", status) }
        db.update("Attendance", contentValues, "record_id = ?", arrayOf(recordId.toString()))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_WRITE_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
        }
    }
}
