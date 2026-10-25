package com.example.ui.task;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.R;
import com.example.model.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * BottomSheetDialogFragment facilitating task creation and editing.
 */
public class TaskEditBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TASK = "arg_task";

    public interface OnTaskSavedListener {
        void onTaskSaved(@Nullable Task existingTask, String title, String description, String priority, long dueDate);
    }

    private Task existingTask;
    private OnTaskSavedListener listener;

    private TextView tvFormTitle;
    private TextInputLayout tilTitle;
    private TextInputEditText etTitle;
    private TextInputEditText etDesc;
    private ChipGroup chipGroupPriority;
    private Chip chipLow;
    private Chip chipMedium;
    private Chip chipHigh;
    private MaterialCardView cardSelectDueDate;
    private TextView tvSelectedDueDate;
    private ImageButton btnClearDueDate;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private ImageButton btnClose;

    private long selectedDueDate = 0;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public static TaskEditBottomSheet newInstance(@Nullable Task task) {
        TaskEditBottomSheet fragment = new TaskEditBottomSheet();
        if (task != null) {
            Bundle args = new Bundle();
            args.putSerializable(ARG_TASK, task);
            fragment.setArguments(args);
        }
        return fragment;
    }

    public void setOnTaskSavedListener(OnTaskSavedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_task_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            existingTask = (Task) getArguments().getSerializable(ARG_TASK);
        }

        tvFormTitle = view.findViewById(R.id.tv_form_title);
        tilTitle = view.findViewById(R.id.til_task_title);
        etTitle = view.findViewById(R.id.et_task_title);
        etDesc = view.findViewById(R.id.et_task_desc);
        chipGroupPriority = view.findViewById(R.id.chip_group_priority);
        chipLow = view.findViewById(R.id.chip_priority_low);
        chipMedium = view.findViewById(R.id.chip_priority_medium);
        chipHigh = view.findViewById(R.id.chip_priority_high);
        cardSelectDueDate = view.findViewById(R.id.card_select_due_date);
        tvSelectedDueDate = view.findViewById(R.id.tv_selected_due_date);
        btnClearDueDate = view.findViewById(R.id.btn_clear_due_date);
        btnSave = view.findViewById(R.id.btn_save_task);
        btnCancel = view.findViewById(R.id.btn_cancel_task);
        btnClose = view.findViewById(R.id.btn_form_close);

        // Populate existing task if in edit mode
        if (existingTask != null) {
            tvFormTitle.setText(R.string.dialog_edit_task_title);
            etTitle.setText(existingTask.getTitle());
            etDesc.setText(existingTask.getDescription());
            selectedDueDate = existingTask.getDueDate();
            setPriorityChecked(existingTask.getPriority());
        } else {
            tvFormTitle.setText(R.string.dialog_add_task_title);
            chipMedium.setChecked(true);
            selectedDueDate = 0;
        }

        updateDueDateDisplay();

        // Due date picker dialog
        cardSelectDueDate.setOnClickListener(v -> showDatePicker());
        btnClearDueDate.setOnClickListener(v -> {
            selectedDueDate = 0;
            updateDueDateDisplay();
        });

        // Cancel / Close buttons
        btnCancel.setOnClickListener(v -> dismiss());
        btnClose.setOnClickListener(v -> dismiss());

        // Save action
        btnSave.setOnClickListener(v -> handleSave());
    }

    private void setPriorityChecked(String priority) {
        if (Task.PRIORITY_HIGH.equalsIgnoreCase(priority)) {
            chipHigh.setChecked(true);
        } else if (Task.PRIORITY_LOW.equalsIgnoreCase(priority)) {
            chipLow.setChecked(true);
        } else {
            chipMedium.setChecked(true);
        }
    }

    private String getSelectedPriority() {
        int checkedId = chipGroupPriority.getCheckedChipId();
        if (checkedId == R.id.chip_priority_high) {
            return Task.PRIORITY_HIGH;
        } else if (checkedId == R.id.chip_priority_low) {
            return Task.PRIORITY_LOW;
        }
        return Task.PRIORITY_MEDIUM;
    }

    private void updateDueDateDisplay() {
        if (selectedDueDate > 0) {
            tvSelectedDueDate.setText(dateFormat.format(new Date(selectedDueDate)));
            tvSelectedDueDate.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary));
            btnClearDueDate.setVisibility(View.VISIBLE);
        } else {
            tvSelectedDueDate.setText(R.string.select_due_date);
            tvSelectedDueDate.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary));
            btnClearDueDate.setVisibility(View.GONE);
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        if (selectedDueDate > 0) {
            cal.setTimeInMillis(selectedDueDate);
        }
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, y, m, d) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(Calendar.YEAR, y);
            picked.set(Calendar.MONTH, m);
            picked.set(Calendar.DAY_OF_MONTH, d);
            picked.set(Calendar.HOUR_OF_DAY, 23);
            picked.set(Calendar.MINUTE, 59);
            picked.set(Calendar.SECOND, 59);
            selectedDueDate = picked.getTimeInMillis();
            updateDueDateDisplay();
        }, year, month, day);

        dialog.show();
    }

    private void handleSave() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";

        if (title.isEmpty()) {
            tilTitle.setError(getString(R.string.error_empty_title));
            return;
        }
        tilTitle.setError(null);

        if (listener != null) {
            listener.onTaskSaved(existingTask, title, desc, getSelectedPriority(), selectedDueDate);
        }
        dismiss();
    }
}
