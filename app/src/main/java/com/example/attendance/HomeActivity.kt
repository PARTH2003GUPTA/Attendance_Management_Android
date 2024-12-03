package com.example.attendance

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    private lateinit var imageViewTakeAttendance: ImageView
    private lateinit var imageViewUploadAttendance: ImageView
    private lateinit var buttonTakeAttendance: Button
    private lateinit var buttonUploadAttendance: Button
    private lateinit var imageViewFetchStudentRecords: ImageView
    private lateinit var buttonFetchStudentRecords: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Find views
        imageViewTakeAttendance = findViewById(R.id.imageViewTakeAttendance)
        imageViewUploadAttendance = findViewById(R.id.imageViewUploadAttendance)
        buttonTakeAttendance = findViewById(R.id.buttonTakeAttendance)
        buttonUploadAttendance = findViewById(R.id.buttonUploadAttendance)
        imageViewFetchStudentRecords = findViewById(R.id.imageViewFetchStudentRecords)
        buttonFetchStudentRecords = findViewById(R.id.buttonFetchStudentRecords)

        // Set click listeners
        buttonTakeAttendance.setOnClickListener {
            val intent = Intent(this, AttendanceActivity::class.java)
            startActivity(intent)
        }

        buttonUploadAttendance.setOnClickListener {
            val intent = Intent(this, UploadAttendanceActivity::class.java)
            startActivity(intent)
        }

        buttonFetchStudentRecords.setOnClickListener {
            val intent = Intent(this, FetchStudentRecordsActivity::class.java)
            startActivity(intent)
        }
    }
}
