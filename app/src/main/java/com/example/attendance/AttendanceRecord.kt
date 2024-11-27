package com.example.attendance

data class AttendanceRecord(
    val enrollmentNo: String,
    val name: String,
    val dateTime: String,
    var status: Boolean,
    val courseId: String,
    val startTime: String,
    val endTime: String,
    val day: String,
    val lectureType: String,
    val facultyID: String
)
