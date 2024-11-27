package com.example.attendance

import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var db: SQLiteDatabase
    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonRegister: Button
    private lateinit var passwordToggle: ImageView
    private lateinit var sharedPreferences: SharedPreferences

    private var isPasswordVisible = false // To toggle visibility

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize database
        db = openOrCreateDatabase("AttendanceDB", MODE_PRIVATE, null)
        db.execSQL("CREATE TABLE IF NOT EXISTS Faculty(username VARCHAR PRIMARY KEY, password VARCHAR);")
        db.execSQL("INSERT OR IGNORE INTO Faculty VALUES('faculty1', 'password123');")  // Sample data

        // Initialize views
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonRegister = findViewById(R.id.buttonRegister)
        passwordToggle = findViewById(R.id.passwordToggle)

        // Initialize SharedPreferences for session management
        sharedPreferences = getSharedPreferences("SessionPrefs", MODE_PRIVATE)

        // Set initial state for password visibility
        passwordToggle.setOnClickListener {
            togglePasswordVisibility()
        }

        buttonLogin.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check user credentials in the Faculty table
            val cursor: Cursor = db.rawQuery(
                "SELECT * FROM Faculty WHERE username=? AND password=?",
                arrayOf(username, password)
            )
            if (cursor.count > 0) {
                cursor.close()

                // Save the username in SharedPreferences
                val editor = sharedPreferences.edit()
                editor.putString("username", username)
                editor.apply()

                // Navigate to the HomeActivity
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish() // Close login activity
            } else {
                Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show()
            }
        }

        buttonRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            editTextPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordToggle.setImageResource(R.drawable.ic_visibility_off)
        } else {
            editTextPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            passwordToggle.setImageResource(R.drawable.ic_visibility_on)
        }
        editTextPassword.setSelection(editTextPassword.text.length) // Keep cursor at the end
        isPasswordVisible = !isPasswordVisible
    }
}
