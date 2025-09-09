package main.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskManager {

    // Методы для обычных задач
    int createTask(String name, String description);

    List<Task> getAllTasks();

    Optional<Task> getTask(int id);

    void updateTask(Task task);

    void deleteTask(int id);

    void deleteAllTasks();

    // Методы для эпиков
    int createEpic(String name, String description);

    List<Epic> getAllEpics();

    Optional<Epic> getEpic(int id);

    void updateEpic(Epic epic);

    void deleteEpic(int id);

    void deleteAllEpics();

    // Методы для подзадач
    void createSubtask(String name, String description, int epicId);

    List<Subtask> getAllSubtasks();

    Optional<Subtask> getSubtask(int id);

    void updateSubtask(Subtask subtask);

    void deleteSubtask(int id);

    void deleteAllSubtasks();

    // Получение подзадач эпика
    List<Subtask> getEpicSubtasks(int epicId);

    // История просмотров задач
    List<Task> getHistory();

    // Получение задач по приоритету (отсортированных по startTime)
    List<Task> getPrioritizedTasks();

    // Проверка пересечения временных интервалов двух задач
    boolean isTasksOverlapping(Task task1, Task task2);

    // Проверка пересечения задачи с любой другой задачей в менеджере
    boolean hasTimeConflict(Task task);
}
