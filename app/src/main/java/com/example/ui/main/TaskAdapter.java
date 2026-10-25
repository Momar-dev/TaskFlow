package com.example.ui.main;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter presenting the authenticated user's tasks in a high-contrast dark slate list.
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskActionListener {
        void onTaskCompletionToggled(Task task, boolean isCompleted);
        void onTaskEditRequested(Task task);
        void onTaskDeleteRequested(Task task);
    }

    private final Context context;
    private final List<Task> tasks = new ArrayList<>();
    private final OnTaskActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public TaskAdapter(Context context, OnTaskActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setTasks(List<Task> newTasks) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return tasks.size();
            }

            @Override
            public int getNewListSize() {
                return newTasks != null ? newTasks.size() : 0;
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return tasks.get(oldItemPosition).getId() == newTasks.get(newItemPosition).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Task oldItem = tasks.get(oldItemPosition);
                Task newItem = newTasks.get(newItemPosition);
                return oldItem.isCompleted() == newItem.isCompleted()
                        && oldItem.getTitle().equals(newItem.getTitle())
                        && oldItem.getDescription().equals(newItem.getDescription())
                        && oldItem.getPriority().equals(newItem.getPriority())
                        && oldItem.getDueDate() == newItem.getDueDate();
            }
        });

        tasks.clear();
        if (newTasks != null) {
            tasks.addAll(newTasks);
        }
        diffResult.dispatchUpdatesTo(this);
    }

    public List<Task> getTasks() {
        return tasks;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox cbCompletion;
        private final TextView tvTitle;
        private final TextView tvDesc;
        private final TextView tvPriority;
        private final LinearLayout llDueDate;
        private final TextView tvDueDate;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;
        private final LinearLayout llContent;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cbCompletion = itemView.findViewById(R.id.cb_task_completion);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDesc = itemView.findViewById(R.id.tv_task_desc);
            tvPriority = itemView.findViewById(R.id.tv_task_priority);
            llDueDate = itemView.findViewById(R.id.ll_task_due_date);
            tvDueDate = itemView.findViewById(R.id.tv_task_due_date);
            btnEdit = itemView.findViewById(R.id.btn_task_edit);
            btnDelete = itemView.findViewById(R.id.btn_task_delete);
            llContent = itemView.findViewById(R.id.ll_task_content);
        }

        void bind(Task task) {
            tvTitle.setText(task.getTitle());

            // Description visibility
            if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
                tvDesc.setText(task.getDescription());
                tvDesc.setVisibility(View.VISIBLE);
            } else {
                tvDesc.setVisibility(View.GONE);
            }

            // Completion strike-through and visual styling
            // Reset listener before changing checked state to avoid cyclic triggers
            cbCompletion.setOnCheckedChangeListener(null);
            cbCompletion.setChecked(task.isCompleted());

            if (task.isCompleted()) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
                tvDesc.setTextColor(ContextCompat.getColor(context, R.color.text_disabled));
                itemView.setAlpha(0.72f);
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                tvDesc.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                itemView.setAlpha(1.0f);
            }

            cbCompletion.setOnClickListener(v -> {
                boolean checked = cbCompletion.isChecked();
                listener.onTaskCompletionToggled(task, checked);
            });

            // Priority styling
            String priority = task.getPriority();
            tvPriority.setText(priority.toUpperCase(Locale.ROOT));
            if (Task.PRIORITY_HIGH.equalsIgnoreCase(priority)) {
                tvPriority.setBackgroundResource(R.drawable.bg_priority_high);
                tvPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_high));
            } else if (Task.PRIORITY_LOW.equalsIgnoreCase(priority)) {
                tvPriority.setBackgroundResource(R.drawable.bg_priority_low);
                tvPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_low));
            } else {
                tvPriority.setBackgroundResource(R.drawable.bg_priority_medium);
                tvPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_medium));
            }

            // Due Date visibility
            if (task.getDueDate() > 0) {
                llDueDate.setVisibility(View.VISIBLE);
                tvDueDate.setText(dateFormat.format(new Date(task.getDueDate())));
            } else {
                llDueDate.setVisibility(View.GONE);
            }

            // Click actions
            llContent.setOnClickListener(v -> listener.onTaskEditRequested(task));
            btnEdit.setOnClickListener(v -> listener.onTaskEditRequested(task));
            btnDelete.setOnClickListener(v -> listener.onTaskDeleteRequested(task));
        }
    }
}
