package com.example.beat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.dao.MusicDao;
import com.example.beat.data.entities.User;

public class SignUpActivity extends AppCompatActivity {
    private EditText usernameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button signUpButton;
    private Button backButton;
    private MusicDao musicDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize database
        AppDatabase database = AppDatabase.getInstance(this);
        musicDao = database.musicDao();

        // Initialize views
        usernameEditText = findViewById(R.id.username_edit_text);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        signUpButton = findViewById(R.id.sign_up_button);
        backButton = findViewById(R.id.back_button);

        // Set up sign up button click listener
        signUpButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Register user on a background thread
            new Thread(() -> {
                User existingUser = musicDao.getUserByEmailAndPassword(email, null);
                if (existingUser != null) {
                    runOnUiThread(() -> 
                        Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // Create new user
                User newUser = new User();
                newUser.username = username;
                newUser.email = email;
                newUser.password = password; // In a real app, you should hash the password

                // Insert user and get generated ID
                long userId = musicDao.insertUser(newUser);
                if (userId != -1) {
                    runOnUiThread(() -> {
                        // Store user ID in shared preferences
                        getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                .edit()
                                .putInt("userId", (int) userId)
                                .apply();
                        
                        // Start MainActivity
                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                        finish();
                    });
                } else {
                    runOnUiThread(() -> 
                        Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
                    );
                }
            }).start();
        });

        // Set up back button click listener
        backButton.setOnClickListener(v -> {
            finish();
        });
    }
}
