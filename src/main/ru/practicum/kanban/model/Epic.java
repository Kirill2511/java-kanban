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
            resetCalculatedFields();
            return;
        }

        this.duration = calculateTotalDuration(subtasks);
        this.startTime = calculateEarliestStartTime(subtasks);
        this.endTime = calculateLatestEndTime(subtasks);
    }

    private void resetCalculatedFields() {
        this.duration = Duration.ZERO;
        this.startTime = null;
        this.endTime = null;
    }

    private Duration calculateTotalDuration(List<Subtask> subtasks) {
        long totalMinutes = 0;
        for (Subtask subtask : subtasks) {
            if (subtask.getDuration() != null) {
                totalMinutes += subtask.getDuration().toMinutes();
            }
        }
        return Duration.ofMinutes(totalMinutes);
    }

    private LocalDateTime calculateEarliestStartTime(List<Subtask> subtasks) {
        LocalDateTime earliestStart = null;
        for (Subtask subtask : subtasks) {
            if (subtask.getStartTime() != null) {
                if (earliestStart == null || subtask.getStartTime().isBefore(earliestStart)) {
                    earliestStart = subtask.getStartTime();
                }
            }
        }
        return earliestStart;
    }

    private LocalDateTime calculateLatestEndTime(List<Subtask> subtasks) {
        LocalDateTime latestEnd = null;
        for (Subtask subtask : subtasks) {
            if (subtask.getEndTime() != null) {
                if (latestEnd == null || subtask.getEndTime().isAfter(latestEnd)) {
                    latestEnd = subtask.getEndTime();
                }
            }
        }
        return latestEnd;
    }
}
