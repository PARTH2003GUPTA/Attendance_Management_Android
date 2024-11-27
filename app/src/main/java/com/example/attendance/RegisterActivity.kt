package com.example.attendance

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.attendance.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var db: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable foreign keys
        db = openOrCreateDatabase("AttendanceDB", MODE_PRIVATE, null)
        db.execSQL("PRAGMA foreign_keys = ON;")

        // Create Faculty table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS Faculty(
                username VARCHAR PRIMARY KEY, 
                password VARCHAR
            );
        """)

        // Create Courses table with foreign key constraint
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS Courses(
                course_id VARCHAR PRIMARY KEY, 
                course_name VARCHAR, 
                teacher_username VARCHAR,
                FOREIGN KEY (teacher_username) REFERENCES Faculty(username) 
                    ON DELETE CASCADE ON UPDATE CASCADE
            );
        """)

        setupPasswordVisibilityToggle()

        // Set up listener for the course count input
        binding.editTextCoursesCount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed here
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val countText = charSequence.toString()
                val courseCount = countText.toIntOrNull()

                if (courseCount != null && courseCount > 0) {
                    binding.coursesLayout.removeAllViews()

                    for (i in 1..courseCount) {
                        val editText = EditText(this@RegisterActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            hint = "Course $i"
                        }
                        binding.coursesLayout.addView(editText)
                    }
                    binding.coursesLayout.visibility = View.VISIBLE
                } else {
                    binding.coursesLayout.visibility = View.GONE
                }
            }

            override fun afterTextChanged(editable: Editable?) {
                // No action needed here
            }
        })

        // Set up register button click listener
        binding.buttonRegister.setOnClickListener {
            val username = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()
            val courses = ArrayList<String>()

            // Validations
            if (username.isEmpty()) {
                showError(binding.usernameLayout, "Username cannot be empty!")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                showError(binding.passwordLayout, "Password cannot be empty!")
                return@setOnClickListener
            }
            if (password.length < 6) {
                showError(binding.passwordLayout, "Password must be at least 6 characters!")
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty() || password != confirmPassword) {
                showError(binding.confirmPasswordLayout, "Passwords do not match!")
                return@setOnClickListener
            }

            // Collect course names
            for (i in 0 until binding.coursesLayout.childCount) {
                val courseEditText = binding.coursesLayout.getChildAt(i) as EditText
                val courseName = courseEditText.text.toString().trim()
                if (courseName.isNotEmpty()) {
                    courses.add(courseName)
                }
            }

            // Save user and courses to the database
            val facultyValues = ContentValues().apply {
                put("username", username)
                put("password", password)
            }
            db.insert("Faculty", null, facultyValues)

            for (course in courses) {
                val courseValues = ContentValues().apply {
                    put("course_name", course)
                    put("teacher_username", username)
                }
                db.insert("Courses", null, courseValues)
            }

            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupPasswordVisibilityToggle() {
        binding.passwordToggle.setOnClickListener {
            if (binding.editTextPassword.transformationMethod == PasswordTransformationMethod.getInstance()) {
                binding.editTextPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.passwordToggle.setImageResource(R.drawable.ic_visibility_on)
            } else {
                binding.editTextPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.passwordToggle.setImageResource(R.drawable.ic_visibility_off)
            }
        }

        binding.confirmPasswordToggle.setOnClickListener {
            if (binding.editTextConfirmPassword.transformationMethod == PasswordTransformationMethod.getInstance()) {
                binding.editTextConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.confirmPasswordToggle.setImageResource(R.drawable.ic_visibility_on)
            } else {
                binding.editTextConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.confirmPasswordToggle.setImageResource(R.drawable.ic_visibility_off)
            }
        }
    }

    private fun showError(view: View, errorMessage: String) {
        // Display error messages as toast
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }
}
