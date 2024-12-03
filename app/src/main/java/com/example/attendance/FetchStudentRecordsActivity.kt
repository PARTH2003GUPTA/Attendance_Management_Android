package com.example.attendance

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class FetchStudentRecordsActivity : AppCompatActivity() {
    private lateinit var spinnerCourse: Spinner
    private lateinit var spinnerSemester: Spinner
    private lateinit var spinnerSection: Spinner
    private lateinit var buttonFetchStudents: Button
    private lateinit var buttonSaveStudents: Button
    private lateinit var listViewStudents: ListView
    private lateinit var editTextCourseId: EditText
    private lateinit var dbHelper: DatabaseHelper
    private var studentList: MutableList<Student> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fetch_student_records)

        // Initialize views
        spinnerCourse = findViewById(R.id.spinnerCourse)
        spinnerSemester = findViewById(R.id.spinnerSemester)
        spinnerSection = findViewById(R.id.spinnerSection)
        buttonFetchStudents = findViewById(R.id.buttonFetchStudents)
        buttonSaveStudents = findViewById(R.id.buttonSaveStudents)
        listViewStudents = findViewById(R.id.listViewStudents)
        editTextCourseId = findViewById(R.id.editTextCourseId)

        // Initialize database helper
        dbHelper = DatabaseHelper(this)

        // Populate spinners
        spinnerCourse.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("BTech", "MTech"))
        spinnerSemester.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII"))
        spinnerSection.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("A", "B"))

        // Fetch students when button is clicked
        buttonFetchStudents.setOnClickListener {
            val course = spinnerCourse.selectedItem?.toString()
            val semester = spinnerSemester.selectedItem?.toString()
            val section = spinnerSection.selectedItem?.toString()

            if (validateInputs(course, semester, section, isCourseIdRequired = false)) {
                fetchStudents(course!!, semester!!, section!!)
            }
        }

        // Save students to database when button is clicked
        buttonSaveStudents.setOnClickListener {
            val courseId = editTextCourseId.text.toString()
            if (validateInputs(null, null, null, isCourseIdRequired = true, courseId = courseId)) {
                saveStudentsToDatabase(courseId)
            }
        }
    }

    private fun fetchStudents(course: String, semester: String, section: String) {
        thread {
            try {
                val url = URL("http://10.0.2.2:8081/api/student?course=$course&semester=$semester&section=$section")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val students = JSONArray(response)

                    studentList.clear()
                    for (i in 0 until students.length()) {
                        val student = students.getJSONObject(i)
                        val enrollmentNo = student.getString("enrollment_no")
                        val name = student.getString("name")

                        studentList.add(Student(enrollmentNo, name, semester, section, course))
                    }

                    runOnUiThread {
                        val adapter = ArrayAdapter(
                            this,
                            android.R.layout.simple_list_item_1,
                            studentList.map { "${it.enrollmentNo} - ${it.name}" }
                        )
                        listViewStudents.adapter = adapter
                    }
                } else {
                    showToast("Failed to fetch students")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun saveStudentsToDatabase(courseId: String) {
        val db = dbHelper.writableDatabase

        try {
            db.beginTransaction()
            for (student in studentList) {
                val values = ContentValues().apply {
                    put("enrollment_no", student.enrollmentNo)
                    put("name", student.name)
                    put("course_id", courseId)
                    put("semester", student.semester)
                    put("section", student.section)
                    put("course", student.course)
                }
                db.insert("Students", null, values)
            }
            db.setTransactionSuccessful()
            showToast("Students saved successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error saving students: ${e.message}")
        } finally {
            db.endTransaction()
        }
    }

    private fun validateInputs(
        course: String?,
        semester: String?,
        section: String?,
        isCourseIdRequired: Boolean = false,
        courseId: String? = null
    ): Boolean {
        if (course.isNullOrEmpty() && semester.isNullOrEmpty() && section.isNullOrEmpty() && !isCourseIdRequired) {
            showToast("Please select all fields (Course, Semester, and Section)")
            return false
        }

        if (isCourseIdRequired && courseId.isNullOrEmpty()) {
            showToast("Please enter a valid Course ID")
            return false
        }

        return true
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}

data class Student(
    val enrollmentNo: String,
    val name: String,
    val semester: String,
    val section: String,
    val course: String
)

class DatabaseHelper(context: android.content.Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS Students(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                enrollment_no VARCHAR,
                name VARCHAR,
                course_id VARCHAR,
                semester VARCHAR,
                section VARCHAR,
                course VARCHAR
            );
            """
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrade if necessary
    }

    companion object {
        private const val DATABASE_NAME = "Attendance.db"
        private const val DATABASE_VERSION = 1
    }
}
