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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Controller for User Registration.
 * Performs rigorous client-side and database validations before saving credentials.
 */
public class RegisterActivity extends AppCompatActivity {

    private UserRepository userRepository;

    private TextInputLayout tilUsername;
    private TextInputEditText etUsername;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etConfirmPassword;
    private TextView tvRegisterError;
    private MaterialButton btnRegister;
    private TextView tvGoToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        userRepository = new UserRepository(dbHelper);

        tilUsername = findViewById(R.id.til_username);
        etUsername = findViewById(R.id.et_username);
        tilEmail = findViewById(R.id.til_email);
        etEmail = findViewById(R.id.et_email);
        tilPassword = findViewById(R.id.til_password);
        etPassword = findViewById(R.id.et_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        tvRegisterError = findViewById(R.id.tv_register_error);
        btnRegister = findViewById(R.id.btn_register);
        tvGoToLogin = findViewById(R.id.tv_go_to_login);

        btnRegister.setOnClickListener(v -> handleRegistration());
        tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void handleRegistration() {
        tvRegisterError.setVisibility(View.GONE);
        tilUsername.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String passwordStr = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirmPasswordStr = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        boolean hasError = false;

        // Username validation
        if (username.isEmpty()) {
            tilUsername.setError(getString(R.string.error_required_field));
            hasError = true;
        } else if (username.length() < 3) {
            tilUsername.setError(getString(R.string.error_invalid_username));
            hasError = true;
        }

        // Email validation
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_required_field));
            hasError = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            hasError = true;
        }

        // Password validation
        if (passwordStr.isEmpty()) {
            tilPassword.setError(getString(R.string.error_required_field));
            hasError = true;
        } else if (passwordStr.length() < 8) {
            tilPassword.setError(getString(R.string.error_short_password));
            hasError = true;
        }

        // Password confirmation
        if (confirmPasswordStr.isEmpty()) {
            tilConfirmPassword.setError(getString(R.string.error_required_field));
            hasError = true;
        } else if (!passwordStr.equals(confirmPasswordStr)) {
            tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            hasError = true;
        }

        if (hasError) {
            return;
        }

        char[] passwordChars = passwordStr.toCharArray();
        UserRepository.RegisterStatus status = userRepository.registerUser(username, email, passwordChars);

        // Wipe password from memory immediately
        java.util.Arrays.fill(passwordChars, '\0');

        switch (status) {
            case SUCCESS:
                Toast.makeText(this, R.string.registration_success, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.putExtra(LoginActivity.EXTRA_PREFILL_IDENTIFIER, username);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                break;
            case USERNAME_TAKEN:
                tilUsername.setError(getString(R.string.error_username_taken));
                tilUsername.requestFocus();
                break;
            case EMAIL_TAKEN:
                tilEmail.setError(getString(R.string.error_email_taken));
                tilEmail.requestFocus();
                break;
            case SHORT_PASSWORD:
                tilPassword.setError(getString(R.string.error_short_password));
                tilPassword.requestFocus();
                break;
            case INVALID_EMAIL:
                tilEmail.setError(getString(R.string.error_invalid_email));
                tilEmail.requestFocus();
                break;
            case INVALID_USERNAME:
                tilUsername.setError(getString(R.string.error_invalid_username));
                tilUsername.requestFocus();
                break;
            default:
                tvRegisterError.setText(R.string.error_required_field);
                tvRegisterError.setVisibility(View.VISIBLE);
                break;
        }
    }
}
