package com.example.attendance

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.TableRow.LayoutParams
import java.util.Calendar

class AttendanceActivity : AppCompatActivity() {
    private lateinit var db: SQLiteDatabase
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var spinnerCourse: Spinner
    private lateinit var spinnerSemester: Spinner
    private lateinit var spinnerSection: Spinner
    private lateinit var spinnerDay: Spinner
    private lateinit var spinnerLectureType: Spinner
    private lateinit var editTextStartTime: EditText
    private lateinit var editTextEndTime: EditText
    private lateinit var editTextDate: EditText
    private lateinit var editTextCourseID: EditText
    private lateinit var editTextFacultyID:EditText
    private lateinit var buttonGenerateSheet: Button
    private lateinit var buttonSaveAttendance: Button
    private lateinit var tableLayoutAttendance: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

//        sharedPreferences = getSharedPreferences("LoginPreferences", Context.MODE_PRIVATE)

        db = openOrCreateDatabase("AttendanceDB", MODE_PRIVATE, null)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS Students(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                enrollment_no VARCHAR,
                name VARCHAR,
                course_id VARCHAR,
                semester INTEGER,
                section VARCHAR
            );
        """
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS Attendance(
                record_id INTEGER PRIMARY KEY AUTOINCREMENT,
                enrollment_no VARCHAR,
                name VARCHAR,
                date_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                status INTEGER,
                course_id VARCHAR,
                start_time VARCHAR,
                end_time VARCHAR,
                day VARCHAR,
                lecture_type VARCHAR,
                faculty_id VARCHAR
            );
        """
        )

        insertDummyData()

        spinnerCourse = findViewById(R.id.spinnerCourse)
        spinnerSemester = findViewById(R.id.spinnerSemester)
        spinnerSection = findViewById(R.id.spinnerSection)
        spinnerDay = findViewById(R.id.spinnerDay)
        spinnerLectureType = findViewById(R.id.spinnerLectureType)
        editTextStartTime = findViewById(R.id.editTextStartTime)
        editTextEndTime = findViewById(R.id.editTextEndTime)
        editTextDate = findViewById(R.id.editTextDate)
        editTextCourseID = findViewById(R.id.editTextCourseID)
        editTextFacultyID=findViewById(R.id.editTextFacultyID)
        buttonGenerateSheet = findViewById(R.id.buttonGenerateSheet)
        buttonSaveAttendance = findViewById(R.id.buttonSaveAttendance)
        tableLayoutAttendance = findViewById(R.id.tableLayoutAttendance)

        buttonSaveAttendance.visibility = Button.GONE

        setupSpinners()

        buttonGenerateSheet.setOnClickListener {
            if (validateInputs()) {
                generateAttendanceSheet()
            }
        }

        buttonSaveAttendance.setOnClickListener {
            saveAttendance()
        }

        editTextStartTime.setOnClickListener {
            showTimePicker(editTextStartTime)
        }

        editTextEndTime.setOnClickListener {
            showTimePicker(editTextEndTime)
        }

        editTextDate.setOnClickListener {
            showDatePicker(editTextDate)
        }
    }

    private fun setupSpinners() {
        val courseList = listOf("Select Course", "BTech", "MTech")
        ArrayAdapter(this, android.R.layout.simple_spinner_item, courseList).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCourse.adapter = adapter
        }

        ArrayAdapter.createFromResource(this, R.array.semesters, android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSemester.adapter = adapter
        }

        ArrayAdapter.createFromResource(this, R.array.sections, android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSection.adapter = adapter
        }

        val days = listOf("Select Day", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        ArrayAdapter(this, android.R.layout.simple_spinner_item, days).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDay.adapter = adapter
        }

        val lectureTypes = listOf("Select Lecture Type", "Theory", "Lab")
        ArrayAdapter(this, android.R.layout.simple_spinner_item, lectureTypes).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerLectureType.adapter = adapter
        }
    }

    private fun insertDummyData() {
        val students = listOf(
            Pair("0801CS21101", "Prashant Tripathi"),
            Pair("0801CS21102", "Ramkrishna Patidar"),
            Pair("0801CS21103", "Shrajan Gupta"),
            Pair("0801CS21104", "Tanmay Sharma"),
            Pair("0801CS21105", "Vinay Patidar"),
            Pair("0801CS21106", "Ajay Kumar Sahani"),
            Pair("0801CS21107", "Harsh Agrawal"),
            Pair("0801CS21108", "Pranjal Shrivastava")
        )

        students.forEach { (enrollmentNo, name) ->
            val student = ContentValues().apply {
                put("enrollment_no", enrollmentNo)
                put("name", name)
                put("course_id", "CO2589")
                put("semester", 1)
                put("section", "A")
            }
            db.insertWithOnConflict("Students", null, student, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    private fun validateInputs(): Boolean {
        if (spinnerCourse.selectedItem.toString() == "Select Course" ||
            spinnerSemester.selectedItem.toString() == "Select Semester" ||
            spinnerSection.selectedItem.toString() == "Select Section" ||
            spinnerDay.selectedItem.toString() == "Select Day" ||
            spinnerLectureType.selectedItem.toString() == "Select Lecture Type" ||
            editTextStartTime.text.isEmpty() ||
            editTextEndTime.text.isEmpty() ||
            editTextDate.text.isEmpty() ||
            editTextCourseID.text.isEmpty() ||
            editTextFacultyID.text.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all fields correctly.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun generateAttendanceSheet() {
        val courseID = editTextCourseID.text.toString()
        val semester = spinnerSemester.selectedItem.toString()
        val section = spinnerSection.selectedItem.toString()

        val cursor: Cursor = db.rawQuery(
            "SELECT enrollment_no, name FROM Students WHERE course_id=? AND semester=? AND section=?",
            arrayOf(courseID, semester, section)
        )

        tableLayoutAttendance.removeAllViews()

        if (cursor.count > 0) {
            val headerRow = TableRow(this).apply {
                addView(createHeaderTextView("Enrollment No", 2f))
                addView(createHeaderTextView("Name", 3f))
                addView(createHeaderTextView("Attendance", 1f))
            }
            tableLayoutAttendance.addView(headerRow)

            while (cursor.moveToNext()) {
                val row = TableRow(this).apply {
                    addView(createTextView(cursor.getString(0), 2f))
                    addView(createTextView(cursor.getString(1), 3f))
                    addView(createCheckBox(1f))
                }
                tableLayoutAttendance.addView(row)
            }

            buttonSaveAttendance.visibility = Button.VISIBLE
        } else {
            Toast.makeText(this, "No students found", Toast.LENGTH_SHORT).show()
        }
        cursor.close()
    }

    private fun saveAttendance() {
//        val facultyName = sharedPreferences.getString("username", "Unknown") ?: "Unknown"
        val facultyID=editTextFacultyID.text.toString()
        val courseID = editTextCourseID.text.toString()
        val startTime = editTextStartTime.text.toString()
        val endTime = editTextEndTime.text.toString()
        val day = spinnerDay.selectedItem.toString()
        val lectureType = spinnerLectureType.selectedItem.toString()

        for (i in 1 until tableLayoutAttendance.childCount) {
            val row = tableLayoutAttendance.getChildAt(i) as TableRow
            val enrollmentNo = (row.getChildAt(0) as TextView).text.toString()
            val name=(row.getChildAt(1) as TextView).text.toString()
            val isPresent = !(row.getChildAt(2) as CheckBox).isChecked

            val values = ContentValues().apply {
                put("enrollment_no", enrollmentNo)
                put("name",name)
                put("course_id", courseID)
                put("status", if (isPresent) 1 else 0)
                put("start_time", startTime)
                put("end_time", endTime)
                put("day", day)
                put("lecture_type", lectureType)
                put("faculty_id", facultyID)
            }
            db.insert("Attendance", null, values)
        }

        Toast.makeText(this, "Attendance saved!", Toast.LENGTH_SHORT).show()
        finish()
        startActivity(intent)
    }

    private fun showTimePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            editText.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
        }, hour, minute, true).show()
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            editText.setText(String.format("%02d-%02d-%04d", selectedDay, selectedMonth + 1, selectedYear))
        }, year, month, day).show()
    }

    private fun createHeaderTextView(text: String, weight: Float): TextView {
        return TextView(this).apply {
            setText(text)
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
        }
    }

    private fun createTextView(text: String, weight: Float): TextView {
        return TextView(this).apply {
            setText(text)
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
        }
    }

    private fun createCheckBox(weight: Float): CheckBox {
        return CheckBox(this).apply {
            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
        }
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }
}
