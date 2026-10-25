package com.example.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.R;
import com.example.db.DatabaseHelper;
import com.example.db.UserRepository;
import com.example.model.User;
import com.example.session.SessionManager;
import com.example.ui.main.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Controller for User Authentication.
 * Validates against SQLite user database using PBKDF2 hash comparison.
 */
public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_PREFILL_IDENTIFIER = "extra_prefill_identifier";

    private UserRepository userRepository;
    private SessionManager sessionManager;

    private TextInputLayout tilIdentifier;
    private TextInputEditText etIdentifier;
    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private TextView tvLoginError;
    private MaterialButton btnLogin;
    private TextView tvGoToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        userRepository = new UserRepository(dbHelper);
        sessionManager = new SessionManager(this);

        // Auto-login if valid existing session
        if (sessionManager.isLoggedIn(userRepository)) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        tilIdentifier = findViewById(R.id.til_identifier);
        etIdentifier = findViewById(R.id.et_identifier);
        tilPassword = findViewById(R.id.til_password);
        etPassword = findViewById(R.id.et_password);
        tvLoginError = findViewById(R.id.tv_login_error);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register);

        // Check if prefill identifier passed from registration
        String prefill = getIntent().getStringExtra(EXTRA_PREFILL_IDENTIFIER);
        if (prefill != null && !prefill.isEmpty()) {
            etIdentifier.setText(prefill);
            etPassword.requestFocus();
        }

        btnLogin.setOnClickListener(v -> handleLogin());
        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        tvLoginError.setVisibility(View.GONE);
        tilIdentifier.setError(null);
        tilPassword.setError(null);

        String identifier = etIdentifier.getText() != null ? etIdentifier.getText().toString().trim() : "";
        String passwordStr = etPassword.getText() != null ? etPassword.getText().toString() : "";

        boolean hasError = false;
        if (identifier.isEmpty()) {
            tilIdentifier.setError(getString(R.string.error_required_field));
            hasError = true;
        }

        if (passwordStr.isEmpty()) {
            tilPassword.setError(getString(R.string.error_required_field));
            hasError = true;
        }

        if (hasError) {
            return;
        }

        char[] passwordChars = passwordStr.toCharArray();
        User authenticatedUser = userRepository.authenticate(identifier, passwordChars);

        // Overwrite plaintext char array immediately from memory
        java.util.Arrays.fill(passwordChars, '\0');

        if (authenticatedUser != null) {
            sessionManager.createSession(
                    authenticatedUser.getId(),
                    authenticatedUser.getUsername(),
                    authenticatedUser.getEmail()
            );
            Toast.makeText(this, getString(R.string.greeting_format, authenticatedUser.getUsername()), Toast.LENGTH_SHORT).show();
            navigateToMain();
        } else {
            // Generic security-compliant authentication failure message
            tvLoginError.setText(R.string.error_auth_failed);
            tvLoginError.setVisibility(View.VISIBLE);
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
