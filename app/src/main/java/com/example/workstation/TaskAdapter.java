package com.example.workstation;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private Context context;
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task, int position);
        void onDeleteClick(Task task, int position);
    }

    public TaskAdapter(Context context, List<Task> taskList) {
        this.context = context;
        this.taskList = taskList;
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.taskName.setText(task.getName());

        // Definir cor de fundo baseado na prioridade
        int backgroundColor;
        switch (task.getPriority()) {
            case 1: // Azul
                backgroundColor = Color.parseColor("#4A90E2");
                break;
            case 2: // Amarelo
                backgroundColor = Color.parseColor("#F5A623");
                break;
            case 3: // Vermelho
                backgroundColor = Color.parseColor("#D0021B");
                break;
            default:
                backgroundColor = Color.parseColor("#4A90E2");
                break;
        }

        // Se a task está finalizada, usar cor verde
        if (task.isFinished()) {
            backgroundColor = Color.parseColor("#7ED321");
            holder.taskName.setPaintFlags(holder.taskName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.checkIcon.setVisibility(View.VISIBLE);
        } else {
            holder.taskName.setPaintFlags(holder.taskName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.checkIcon.setVisibility(View.INVISIBLE);
        }

        holder.itemView.setBackgroundColor(backgroundColor);

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task, position);
            }
        });

        holder.deleteIcon.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(task, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskName;
        ImageView checkIcon;
        ImageView deleteIcon;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskName = itemView.findViewById(R.id.taskName);
            checkIcon = itemView.findViewById(R.id.checkIcon);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }
    }
}