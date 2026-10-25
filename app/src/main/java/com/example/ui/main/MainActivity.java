package com.example.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.db.DatabaseHelper;
import com.example.db.TaskRepository;
import com.example.db.UserRepository;
import com.example.model.Task;
import com.example.session.SessionManager;
import com.example.ui.auth.LoginActivity;
import com.example.ui.task.TaskEditBottomSheet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Main Task Dashboard Activity.
 * Provides statistics overview, filter tabs, task list, progress bar, and user session management.
 */
public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskActionListener {

    private static final String KEY_CURRENT_FILTER = "key_current_filter";

    private UserRepository userRepository;
    private TaskRepository taskRepository;
    private SessionManager sessionManager;
    private long currentUserId;

    private TaskAdapter taskAdapter;
    private TaskRepository.Filter currentFilter = TaskRepository.Filter.ALL;

    private TextView tvGreeting;
    private TextView tvDateSubtitle;
    private MaterialButton btnLogout;
    private TextView tvStatActive;
    private TextView tvStatCompleted;
    private TextView tvStatTotal;
    private TextView tvProgressPercentage;
    private ProgressBar pbCompletion;
    private TextView tvProgressRatio;
    private ChipGroup chipGroupFilters;
    private Chip chipFilterAll;
    private Chip chipFilterActive;
    private Chip chipFilterCompleted;
    private RecyclerView rvTasks;
    private LinearLayout llEmptyState;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private MaterialButton btnEmptyAction;
    private FloatingActionButton fabAddTask;

    private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        userRepository = new UserRepository(dbHelper);
        taskRepository = new TaskRepository(dbHelper);
        sessionManager = new SessionManager(this);

        // Security check: Guard unauthenticated access
        if (!sessionManager.isLoggedIn(userRepository)) {
            redirectToLogin();
            return;
        }

        currentUserId = sessionManager.getUserId();

        if (savedInstanceState != null) {
            String savedFilter = savedInstanceState.getString(KEY_CURRENT_FILTER, TaskRepository.Filter.ALL.name());
            try {
                currentFilter = TaskRepository.Filter.valueOf(savedFilter);
            } catch (Exception ignored) {
                currentFilter = TaskRepository.Filter.ALL;
            }
        }

        setContentView(R.layout.activity_main);
        initViews();
        setupRecyclerView();
        setupListeners();
        loadDashboardData();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_FILTER, currentFilter.name());
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvDateSubtitle = findViewById(R.id.tv_date_subtitle);
        btnLogout = findViewById(R.id.btn_logout);
        tvStatActive = findViewById(R.id.tv_stat_active_count);
        tvStatCompleted = findViewById(R.id.tv_stat_completed_count);
        tvStatTotal = findViewById(R.id.tv_stat_total_count);
        tvProgressPercentage = findViewById(R.id.tv_progress_percentage);
        pbCompletion = findViewById(R.id.pb_completion);
        tvProgressRatio = findViewById(R.id.tv_progress_ratio);
        chipGroupFilters = findViewById(R.id.chip_group_filters);
        chipFilterAll = findViewById(R.id.chip_filter_all);
        chipFilterActive = findViewById(R.id.chip_filter_active);
        chipFilterCompleted = findViewById(R.id.chip_filter_completed);
        rvTasks = findViewById(R.id.rv_tasks);
        llEmptyState = findViewById(R.id.ll_empty_state);
        ivEmptyIcon = findViewById(R.id.iv_empty_icon);
        tvEmptyTitle = findViewById(R.id.tv_empty_title);
        tvEmptyDesc = findViewById(R.id.tv_empty_desc);
        btnEmptyAction = findViewById(R.id.btn_empty_action);
        fabAddTask = findViewById(R.id.fab_add_task);

        // Header Greeting
        String username = sessionManager.getUsername();
        if (username.isEmpty()) {
            username = "Friend";
        }
        tvGreeting.setText(getString(R.string.greeting_format, username));
        tvDateSubtitle.setText(dayFormat.format(new Date()));

        // Sync filter chip selection
        switch (currentFilter) {
            case ACTIVE:
                chipFilterActive.setChecked(true);
                break;
            case COMPLETED:
                chipFilterCompleted.setChecked(true);
                break;
            default:
                chipFilterAll.setChecked(true);
                break;
        }
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(this, this);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);
    }

    private void setupListeners() {
        // Logout action with confirmation dialog
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        // Filter chips
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_filter_active) {
                currentFilter = TaskRepository.Filter.ACTIVE;
            } else if (checkedId == R.id.chip_filter_completed) {
                currentFilter = TaskRepository.Filter.COMPLETED;
            } else {
                currentFilter = TaskRepository.Filter.ALL;
            }
            loadTasksOnly();
        });

        // FAB add task
        fabAddTask.setOnClickListener(v -> openTaskEditor(null));
        btnEmptyAction.setOnClickListener(v -> openTaskEditor(null));
    }

    public void loadDashboardData() {
        TaskRepository.TaskCounts counts = taskRepository.getTaskCounts(currentUserId);

        // Update statistics cards
        tvStatActive.setText(String.valueOf(counts.active));
        tvStatCompleted.setText(String.valueOf(counts.completed));
        tvStatTotal.setText(String.valueOf(counts.total));

        // Update completion percentage and progress bar
        int progress = 0;
        if (counts.total > 0) {
            progress = (int) Math.round(((double) counts.completed / counts.total) * 100);
        }
        pbCompletion.setProgress(progress);
        tvProgressPercentage.setText(getString(R.string.progress_percentage_format, progress));
        tvProgressRatio.setText(getString(R.string.tasks_ratio_format, counts.completed, counts.total));

        loadTasksOnly();
    }

    private void loadTasksOnly() {
        List<Task> tasks = taskRepository.getTasks(currentUserId, currentFilter);
        taskAdapter.setTasks(tasks);

        if (tasks.isEmpty()) {
            rvTasks.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
            updateEmptyStateText();
        } else {
            rvTasks.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }
    }

    private void updateEmptyStateText() {
        switch (currentFilter) {
            case ACTIVE:
                tvEmptyTitle.setText(R.string.empty_active_tasks_title);
                tvEmptyDesc.setText(R.string.empty_active_tasks_desc);
                btnEmptyAction.setVisibility(View.GONE);
                break;
            case COMPLETED:
                tvEmptyTitle.setText(R.string.empty_completed_tasks_title);
                tvEmptyDesc.setText(R.string.empty_completed_tasks_desc);
                btnEmptyAction.setVisibility(View.GONE);
                break;
            default:
                tvEmptyTitle.setText(R.string.empty_all_tasks_title);
                tvEmptyDesc.setText(R.string.empty_all_tasks_desc);
                btnEmptyAction.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void openTaskEditor(Task task) {
        TaskEditBottomSheet bottomSheet = TaskEditBottomSheet.newInstance(task);
        bottomSheet.setOnTaskSavedListener((existingTask, title, desc, priority, dueDate) -> {
            if (existingTask == null) {
                Task created = taskRepository.addTask(currentUserId, title, desc, priority, dueDate);
                if (created != null) {
                    Toast.makeText(this, R.string.task_saved_success, Toast.LENGTH_SHORT).show();
                }
            } else {
                boolean updated = taskRepository.updateTask(currentUserId, existingTask.getId(), title, desc, priority, dueDate);
                if (updated) {
                    Toast.makeText(this, R.string.task_saved_success, Toast.LENGTH_SHORT).show();
                }
            }
            loadDashboardData();
        });
        bottomSheet.show(getSupportFragmentManager(), "TaskEditBottomSheet");
    }

    @Override
    public void onTaskCompletionToggled(Task task, boolean isCompleted) {
        boolean updated = taskRepository.setTaskCompleted(currentUserId, task.getId(), isCompleted);
        if (updated) {
            loadDashboardData();
        }
    }

    @Override
    public void onTaskEditRequested(Task task) {
        openTaskEditor(task);
    }

    @Override
    public void onTaskDeleteRequested(Task task) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(R.string.delete_dialog_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_confirm_delete, (dialog, which) -> {
                    boolean deleted = taskRepository.deleteTask(currentUserId, task.getId());
                    if (deleted) {
                        Toast.makeText(this, R.string.task_deleted_success, Toast.LENGTH_SHORT).show();
                        loadDashboardData();
                    }
                })
                .show();
    }

    private void showLogoutConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_dialog_title)
                .setMessage(R.string.logout_dialog_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_confirm_logout, (dialog, which) -> {
                    sessionManager.logout();
                    redirectToLogin();
                })
                .show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
