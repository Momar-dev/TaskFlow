package com.example.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.model.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository managing Task CRUD operations.
 * Mandatory User Isolation: Every single operation enforces the owner userId.
 */
public class TaskRepository {

    public enum Filter {
        ALL,
        ACTIVE,
        COMPLETED
    }

    public static class TaskCounts {
        public final int active;
        public final int completed;
        public final int total;

        public TaskCounts(int active, int completed, int total) {
            this.active = active;
            this.completed = completed;
            this.total = total;
        }
    }

    private final DatabaseHelper dbHelper;

    public TaskRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Inserts a new task owned by the specified user.
     */
    public Task addTask(long userId, String title, String description, String priority, long dueDate) {
        if (userId <= 0 || title == null || title.trim().isEmpty()) {
            return null;
        }

        long now = System.currentTimeMillis();
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_TASK_USER_ID, userId);
        values.put(DatabaseHelper.COL_TASK_TITLE, title.trim());
        values.put(DatabaseHelper.COL_TASK_DESCRIPTION, description != null ? description.trim() : "");
        values.put(DatabaseHelper.COL_TASK_IS_COMPLETED, 0);
        values.put(DatabaseHelper.COL_TASK_PRIORITY, priority != null ? priority : Task.PRIORITY_MEDIUM);
        values.put(DatabaseHelper.COL_TASK_DUE_DATE, dueDate);
        values.put(DatabaseHelper.COL_TASK_CREATED_AT, now);
        values.put(DatabaseHelper.COL_TASK_UPDATED_AT, now);

        long newId = db.insert(DatabaseHelper.TABLE_TASKS, null, values);
        if (newId == -1) {
            return null;
        }

        return new Task(newId, userId, title.trim(), description != null ? description.trim() : "",
                false, priority != null ? priority : Task.PRIORITY_MEDIUM, dueDate, now, now);
    }

    /**
     * Updates an existing task after verifying that the task belongs to userId.
     */
    public boolean updateTask(long userId, long taskId, String title, String description, String priority, long dueDate) {
        if (userId <= 0 || taskId <= 0 || title == null || title.trim().isEmpty()) {
            return false;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_TASK_TITLE, title.trim());
        values.put(DatabaseHelper.COL_TASK_DESCRIPTION, description != null ? description.trim() : "");
        values.put(DatabaseHelper.COL_TASK_PRIORITY, priority != null ? priority : Task.PRIORITY_MEDIUM);
        values.put(DatabaseHelper.COL_TASK_DUE_DATE, dueDate);
        values.put(DatabaseHelper.COL_TASK_UPDATED_AT, System.currentTimeMillis());

        int rowsAffected = db.update(
                DatabaseHelper.TABLE_TASKS,
                values,
                DatabaseHelper.COL_TASK_ID + " = ? AND " + DatabaseHelper.COL_TASK_USER_ID + " = ?",
                new String[]{String.valueOf(taskId), String.valueOf(userId)}
        );

        return rowsAffected > 0;
    }

    /**
     * Toggles the completion status of a task owned by userId.
     */
    public boolean setTaskCompleted(long userId, long taskId, boolean isCompleted) {
        if (userId <= 0 || taskId <= 0) {
            return false;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_TASK_IS_COMPLETED, isCompleted ? 1 : 0);
        values.put(DatabaseHelper.COL_TASK_UPDATED_AT, System.currentTimeMillis());

        int rowsAffected = db.update(
                DatabaseHelper.TABLE_TASKS,
                values,
                DatabaseHelper.COL_TASK_ID + " = ? AND " + DatabaseHelper.COL_TASK_USER_ID + " = ?",
                new String[]{String.valueOf(taskId), String.valueOf(userId)}
        );

        return rowsAffected > 0;
    }

    /**
     * Deletes a task after verifying that the authenticated user owns it.
     */
    public boolean deleteTask(long userId, long taskId) {
        if (userId <= 0 || taskId <= 0) {
            return false;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete(
                DatabaseHelper.TABLE_TASKS,
                DatabaseHelper.COL_TASK_ID + " = ? AND " + DatabaseHelper.COL_TASK_USER_ID + " = ?",
                new String[]{String.valueOf(taskId), String.valueOf(userId)}
        );

        return rowsDeleted > 0;
    }

    /**
     * Retrieves a task by id only if owned by userId.
     */
    public Task getTaskById(long userId, long taskId) {
        if (userId <= 0 || taskId <= 0) {
            return null;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COL_TASK_ID + " = ? AND " + DatabaseHelper.COL_TASK_USER_ID + " = ?";
        String[] selectionArgs = new String[]{String.valueOf(taskId), String.valueOf(userId)};

        try (Cursor cursor = db.query(
                DatabaseHelper.TABLE_TASKS,
                null,
                selection,
                selectionArgs,
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                return parseTaskFromCursor(cursor);
            }
        }
        return null;
    }

    /**
     * Retrieves all tasks for the given user, filtered by status, and ordered consistently:
     * Incomplete tasks first, then sorted by created_at descending.
     */
    public List<Task> getTasks(long userId, Filter filter) {
        List<Task> result = new ArrayList<>();
        if (userId <= 0) {
            return result;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection;
        String[] selectionArgs;

        if (filter == Filter.ACTIVE) {
            selection = DatabaseHelper.COL_TASK_USER_ID + " = ? AND " + DatabaseHelper.COL_TASK_IS_COMPLETED + " = 0";
            selectionArgs = new String[]{String.valueOf(userId)};
        } else if (filter == Filter.COMPLETED) {
            selection = DatabaseHelper.COL_TASK_USER_ID + " = ? AND " + DatabaseHelper.COL_TASK_IS_COMPLETED + " = 1";
            selectionArgs = new String[]{String.valueOf(userId)};
        } else {
            selection = DatabaseHelper.COL_TASK_USER_ID + " = ?";
            selectionArgs = new String[]{String.valueOf(userId)};
        }

        // Incomplete tasks first, then most recently created/updated
        String orderBy = DatabaseHelper.COL_TASK_IS_COMPLETED + " ASC, "
                + DatabaseHelper.COL_TASK_CREATED_AT + " DESC";

        try (Cursor cursor = db.query(
                DatabaseHelper.TABLE_TASKS,
                null,
                selection,
                selectionArgs,
                null, null,
                orderBy)) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    result.add(parseTaskFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        }

        return result;
    }

    /**
     * Returns task count statistics for the given user.
     */
    public TaskCounts getTaskCounts(long userId) {
        if (userId <= 0) {
            return new TaskCounts(0, 0, 0);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int active = 0;
        int completed = 0;

        // Query active tasks count
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TASKS
                        + " WHERE " + DatabaseHelper.COL_TASK_USER_ID + " = ? AND " + DatabaseHelper.COL_TASK_IS_COMPLETED + " = 0",
                new String[]{String.valueOf(userId)})) {
            if (c != null && c.moveToFirst()) {
                active = c.getInt(0);
            }
        }

        // Query completed tasks count
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TASKS
                        + " WHERE " + DatabaseHelper.COL_TASK_USER_ID + " = ? AND " + DatabaseHelper.COL_TASK_IS_COMPLETED + " = 1",
                new String[]{String.valueOf(userId)})) {
            if (c != null && c.moveToFirst()) {
                completed = c.getInt(0);
            }
        }

        return new TaskCounts(active, completed, active + completed);
    }

    private Task parseTaskFromCursor(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_ID));
        long userId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_USER_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_TITLE));
        String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_DESCRIPTION));
        boolean isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_IS_COMPLETED)) == 1;
        String priority = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_PRIORITY));
        long dueDate = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_DUE_DATE));
        long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_CREATED_AT));
        long updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_UPDATED_AT));

        return new Task(id, userId, title, description, isCompleted, priority, dueDate, createdAt, updatedAt);
    }
}
