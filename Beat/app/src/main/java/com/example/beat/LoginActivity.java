package com.example.beat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.dao.MusicDao;
import com.example.beat.data.entities.User;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button signUpButton;
    private MusicDao musicDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize database
        AppDatabase database = AppDatabase.getInstance(this);
        musicDao = database.musicDao();

        // Initialize views
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        signUpButton = findViewById(R.id.sign_up_button);

        // Set up login button click listener
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate credentials on a background thread
            new Thread(() -> {
                User user = musicDao.getUserByEmailAndPassword(email, password);
                if (user != null) {
                    runOnUiThread(() -> {
                        // Store user ID and email in shared preferences
                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        prefs.edit()
                                .putInt("userId", user.userId)
                                .putString("user_email", email)  // Store email
                                .apply();
                        
                        // Start MainActivity
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    });
                } else {
                    runOnUiThread(() -> 
                        Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                    );
                }
            }).start();
        });

        // Set up sign up button click listener
        signUpButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });
    }
}
