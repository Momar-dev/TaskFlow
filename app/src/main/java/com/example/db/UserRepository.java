package com.example.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.model.User;
import com.example.security.PasswordHasher;

import java.util.Locale;

/**
 * Repository managing User entity persistence and secure authentication against SQLite.
 * Uses parameterized queries to prevent SQL injection.
 */
public class UserRepository {

    public enum RegisterStatus {
        SUCCESS,
        EMPTY_FIELDS,
        INVALID_USERNAME,
        INVALID_EMAIL,
        SHORT_PASSWORD,
        USERNAME_TAKEN,
        EMAIL_TAKEN,
        DATABASE_ERROR
    }

    private final DatabaseHelper dbHelper;
    private final PasswordHasher passwordHasher;

    public UserRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
        this.passwordHasher = new PasswordHasher();
    }

    public static String normalizeEmail(String rawEmail) {
        if (rawEmail == null) return "";
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isUsernameTaken(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_ID},
                DatabaseHelper.COL_USER_USERNAME + " = ? COLLATE NOCASE",
                new String[]{username.trim()},
                null, null, null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    public boolean isEmailTaken(String email) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty()) return false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_ID},
                DatabaseHelper.COL_USER_NORMALIZED_EMAIL + " = ?",
                new String[]{normalized},
                null, null, null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    /**
     * Registers a new user after strict validation.
     */
    public RegisterStatus registerUser(String username, String rawEmail, char[] password) {
        if (username == null || username.trim().isEmpty() ||
                rawEmail == null || rawEmail.trim().isEmpty() ||
                password == null || password.length == 0) {
            return RegisterStatus.EMPTY_FIELDS;
        }

        String cleanUsername = username.trim();
        if (cleanUsername.length() < 3) {
            return RegisterStatus.INVALID_USERNAME;
        }

        String normalizedEmail = normalizeEmail(rawEmail);
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return RegisterStatus.INVALID_EMAIL;
        }

        if (password.length < 8) {
            return RegisterStatus.SHORT_PASSWORD;
        }

        if (isUsernameTaken(cleanUsername)) {
            return RegisterStatus.USERNAME_TAKEN;
        }

        if (isEmailTaken(normalizedEmail)) {
            return RegisterStatus.EMAIL_TAKEN;
        }

        PasswordHasher.HashResult hashResult = passwordHasher.hashPassword(password);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_USER_USERNAME, cleanUsername);
        values.put(DatabaseHelper.COL_USER_NORMALIZED_EMAIL, normalizedEmail);
        values.put(DatabaseHelper.COL_USER_PASSWORD_HASH, hashResult.getHashBase64());
        values.put(DatabaseHelper.COL_USER_PASSWORD_SALT, hashResult.getSaltBase64());
        values.put(DatabaseHelper.COL_USER_HASH_ITERATIONS, hashResult.getIterations());
        values.put(DatabaseHelper.COL_USER_CREATED_AT, System.currentTimeMillis());

        long insertedId = db.insert(DatabaseHelper.TABLE_USERS, null, values);
        if (insertedId == -1) {
            return RegisterStatus.DATABASE_ERROR;
        }

        return RegisterStatus.SUCCESS;
    }

    /**
     * Authenticates against SQLite by username or normalized email and verifies the hash.
     * Returns User on success, or null on failed verification.
     */
    public User authenticate(String identifier, char[] password) {
        if (identifier == null || identifier.trim().isEmpty() || password == null || password.length == 0) {
            return null;
        }

        String cleanIdentifier = identifier.trim();
        String normalizedEmail = normalizeEmail(cleanIdentifier);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COL_USER_USERNAME + " = ? COLLATE NOCASE OR "
                + DatabaseHelper.COL_USER_NORMALIZED_EMAIL + " = ?";
        String[] selectionArgs = new String[]{cleanIdentifier, normalizedEmail};

        try (Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                null,
                selection,
                selectionArgs,
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_USERNAME));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_NORMALIZED_EMAIL));
                String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_PASSWORD_HASH));
                String storedSalt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_PASSWORD_SALT));
                int iterations = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_HASH_ITERATIONS));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_CREATED_AT));

                boolean valid = passwordHasher.verifyPassword(password, storedHash, storedSalt, iterations);
                if (valid) {
                    return new User(id, username, email, storedHash, storedSalt, iterations, createdAt);
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    /**
     * Fetches a User by their ID.
     */
    public User getUserById(long userId) {
        if (userId <= 0) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                null,
                DatabaseHelper.COL_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_USERNAME));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_NORMALIZED_EMAIL));
                String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_PASSWORD_HASH));
                String storedSalt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_PASSWORD_SALT));
                int iterations = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_HASH_ITERATIONS));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_CREATED_AT));
                return new User(id, username, email, storedHash, storedSalt, iterations, createdAt);
            }
        }
        return null;
    }
}
