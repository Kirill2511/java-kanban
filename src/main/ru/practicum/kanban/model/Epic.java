package main.ru.practicum.kanban.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Epic extends Task {
    private final List<Integer> subtaskIds;
    private LocalDateTime endTime;

    public Epic(String name, String description) {
        super(name, description);
        this.subtaskIds = new ArrayList<>();
        this.endTime = null;
    }

    public Epic(int id, String name, String description, TaskStatus status) {
        super(id, name, description, status);
        this.subtaskIds = new ArrayList<>();
        this.endTime = null;
    }

    public Epic(int id, String name, String description, TaskStatus status, Duration duration, LocalDateTime startTime,
                LocalDateTime endTime) {
        super(id, name, description, status, duration, startTime);
        this.subtaskIds = new ArrayList<>();
        this.endTime = endTime;
    }

    // Конструктор копирования
    public Epic(Epic other) {
        super(other);
        this.subtaskIds = new ArrayList<>(other.subtaskIds);
        this.endTime = other.endTime;
    }

    public List<Integer> getSubtaskIds() {
        return new ArrayList<>(subtaskIds);
    }

    public void addSubtaskId(int subtaskId) {
        subtaskIds.add(subtaskId);
    }

    public void removeSubtaskId(int subtaskId) {
        subtaskIds.remove(Integer.valueOf(subtaskId));
    }

    public void clearSubtaskIds() {
        subtaskIds.clear();
    }

    // Переопределяем геттеры для расчетных полей
    @Override
    public Duration getDuration() {
        return duration;
    }

    @Override
    public LocalDateTime getStartTime() {
        return startTime;
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return "Epic{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", duration=" + duration +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", subtaskIds=" + subtaskIds +
                '}';
    }

    // Методы для обновления расчетных полей на основе подзадач
    public void updateCalculatedFields(List<Subtask> subtasks) {
        if (subtasks == null || subtasks.isEmpty()) {
            this.duration = Duration.ofMinutes(0);
            this.startTime = null;
            this.endTime = null;
            return;
        }

        // Рассчитываем общую продолжительность
        long totalMinutes = 0;
        LocalDateTime earliestStart = null;
        LocalDateTime latestEnd = null;

        for (Subtask subtask : subtasks) {
            // Суммируем продолжительность
            if (subtask.getDuration() != null) {
                totalMinutes += subtask.getDuration().toMinutes();
            }

            // Находим самое раннее время начала
            if (subtask.getStartTime() != null) {
                if (earliestStart == null || subtask.getStartTime().isBefore(earliestStart)) {
                    earliestStart = subtask.getStartTime();
                }
            }

            // Находим самое позднее время завершения
            if (subtask.getEndTime() != null) {
                if (latestEnd == null || subtask.getEndTime().isAfter(latestEnd)) {
                    latestEnd = subtask.getEndTime();
                }
            }
        }

        this.duration = Duration.ofMinutes(totalMinutes);
        this.startTime = earliestStart;
        this.endTime = latestEnd;
    }
}
