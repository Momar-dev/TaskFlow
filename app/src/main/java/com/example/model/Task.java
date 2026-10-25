package com.example.model;

import java.io.Serializable;

/**
 * Domain entity representing a task authored by an authenticated user.
 * Isolated strictly by userId.
 */
public class Task implements Serializable {
    public static final String PRIORITY_LOW = "Low";
    public static final String PRIORITY_MEDIUM = "Medium";
    public static final String PRIORITY_HIGH = "High";

    private final long id;
    private final long userId;
    private String title;
    private String description;
    private boolean isCompleted;
    private String priority;
    private long dueDate;
    private final long createdAt;
    private long updatedAt;

    public Task(long id, long userId, String title, String description, boolean isCompleted,
                String priority, long dueDate, long createdAt, long updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.isCompleted = isCompleted;
        this.priority = priority != null ? priority : PRIORITY_MEDIUM;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getPriority() {
        return priority != null ? priority : PRIORITY_MEDIUM;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
