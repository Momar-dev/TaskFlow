package com.example.session;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.db.UserRepository;
import com.example.model.User;

/**
 * Manages authenticated user session using private SharedPreferences.
 * Stores only minimal identifying state (never passwords or sensitive keys).
 */
public class SessionManager {

    private static final String PREF_NAME = "taskflow_session_pref";
    private static final String KEY_USER_ID = "session_user_id";
    private static final String KEY_USERNAME = "session_username";
    private static final String KEY_EMAIL = "session_email";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Initializes an authenticated user session.
     */
    public void createSession(long userId, String username, String email) {
        prefs.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    /**
     * Validates whether a valid session exists and the corresponding user exists in SQLite.
     */
    public boolean isLoggedIn(UserRepository userRepository) {
        long userId = getUserId();
        if (userId <= 0) {
            return false;
        }
        User user = userRepository.getUserById(userId);
        return user != null;
    }

    /**
     * Clears all session credentials from private storage.
     */
    public void logout() {
        prefs.edit().clear().apply();
    }
}
