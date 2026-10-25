package com.example.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite helper managing local persistent storage for users and user-isolated tasks.
 * Enforces foreign keys, indexing, and cascade deletion.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "taskflow.db";
    public static final int DATABASE_VERSION = 1;

    // Users table
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "id";
    public static final String COL_USER_USERNAME = "username";
    public static final String COL_USER_NORMALIZED_EMAIL = "normalized_email";
    public static final String COL_USER_PASSWORD_HASH = "password_hash";
    public static final String COL_USER_PASSWORD_SALT = "password_salt";
    public static final String COL_USER_HASH_ITERATIONS = "hash_iterations";
    public static final String COL_USER_CREATED_AT = "created_at";

    // Tasks table
    public static final String TABLE_TASKS = "tasks";
    public static final String COL_TASK_ID = "id";
    public static final String COL_TASK_USER_ID = "user_id";
    public static final String COL_TASK_TITLE = "title";
    public static final String COL_TASK_DESCRIPTION = "description";
    public static final String COL_TASK_IS_COMPLETED = "is_completed";
    public static final String COL_TASK_PRIORITY = "priority";
    public static final String COL_TASK_DUE_DATE = "due_date";
    public static final String COL_TASK_CREATED_AT = "created_at";
    public static final String COL_TASK_UPDATED_AT = "updated_at";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public DatabaseHelper(Context context, String name) {
        super(context, name, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String createUsersSql = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USER_USERNAME + " TEXT NOT NULL UNIQUE, "
                + COL_USER_NORMALIZED_EMAIL + " TEXT NOT NULL UNIQUE, "
                + COL_USER_PASSWORD_HASH + " TEXT NOT NULL, "
                + COL_USER_PASSWORD_SALT + " TEXT NOT NULL, "
                + COL_USER_HASH_ITERATIONS + " INTEGER NOT NULL, "
                + COL_USER_CREATED_AT + " INTEGER NOT NULL"
                + ");";
        db.execSQL(createUsersSql);

        // Create tasks table with foreign key reference to users
        String createTasksSql = "CREATE TABLE " + TABLE_TASKS + " ("
                + COL_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TASK_USER_ID + " INTEGER NOT NULL, "
                + COL_TASK_TITLE + " TEXT NOT NULL, "
                + COL_TASK_DESCRIPTION + " TEXT, "
                + COL_TASK_IS_COMPLETED + " INTEGER NOT NULL DEFAULT 0, "
                + COL_TASK_PRIORITY + " TEXT NOT NULL DEFAULT 'Medium', "
                + COL_TASK_DUE_DATE + " INTEGER NOT NULL DEFAULT 0, "
                + COL_TASK_CREATED_AT + " INTEGER NOT NULL, "
                + COL_TASK_UPDATED_AT + " INTEGER NOT NULL, "
                + "FOREIGN KEY (" + COL_TASK_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ") ON DELETE CASCADE"
                + ");";
        db.execSQL(createTasksSql);

        // Indices for fast user task queries and status filtering
        db.execSQL("CREATE INDEX idx_tasks_user_id ON " + TABLE_TASKS + " (" + COL_TASK_USER_ID + ");");
        db.execSQL("CREATE INDEX idx_tasks_user_completed ON " + TABLE_TASKS + " (" + COL_TASK_USER_ID + ", " + COL_TASK_IS_COMPLETED + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
}
