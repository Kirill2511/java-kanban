package main.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.exception.TaskValidationException;
import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.model.TaskStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Оптимизированная версия InMemoryTaskManager с проверкой пересечений за O(1).
 * Использует временную сетку для быстрой проверки конфликтов задач.
 */
public class OptimizedInMemoryTaskManager implements TaskManager {
    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected final Map<Integer, Subtask> subtasks = new HashMap<>();
    protected final HistoryManager historyManager = Managers.getDefaultHistory();
    protected int nextId = 1;

    // TreeSet для сортировки задач по приоритету (время начала)
    protected final TreeSet<Task> prioritizedTasks = new TreeSet<>((task1, task2) -> {
        if (task1.getStartTime() == null && task2.getStartTime() == null) {
            return Integer.compare(task1.getId(), task2.getId());
        }
        if (task1.getStartTime() == null)
            return 1;
        if (task2.getStartTime() == null)
            return -1;

        int timeComparison = task1.getStartTime().compareTo(task2.getStartTime());
        return timeComparison != 0 ? timeComparison : Integer.compare(task1.getId(), task2.getId());
    });

    // Оптимизированный менеджер временных слотов
    private final OptimizedTimeSlotManager timeSlotManager;

    public OptimizedInMemoryTaskManager() {
        // Инициализируем базовое время как начало текущего года
        LocalDateTime baseTime = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0)
                .withNano(0);
        this.timeSlotManager = new OptimizedTimeSlotManager(baseTime);
    }

    public OptimizedInMemoryTaskManager(LocalDateTime baseTime) {
        this.timeSlotManager = new OptimizedTimeSlotManager(baseTime);
    }

    protected int generateNextId() {
        return nextId++;
    }

    // Методы для обычных задач
    @Override
    public int createTask(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название задачи не может быть пустым");
        }

        Task task = new Task(name, description);
        int taskId = generateNextId();
        task.setId(taskId);

        tasks.put(taskId, task);
        prioritizedTasks.add(task);

        return taskId;
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public Optional<Task> getTask(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
            return Optional.of(new Task(task));
        }
        return Optional.empty();
    }

    @Override
    public void updateTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Задача не может быть null");
        }

        Task existingTask = tasks.get(task.getId());
        if (existingTask == null) {
            throw new IllegalArgumentException("Задача с ID " + task.getId() + " не найдена");
        }

        // ОПТИМИЗИРОВАННАЯ ПРОВЕРКА ПЕРЕСЕЧЕНИЙ O(1)
        if (hasTimeConflictOptimized(task)) {
            throw new TaskValidationException("Задача пересекается по времени с существующими задачами");
        }

        // Обновляем временную сетку
        timeSlotManager.removeTask(existingTask);
        timeSlotManager.addTask(task);

        // Обновляем в коллекциях
        prioritizedTasks.remove(existingTask);
        tasks.put(task.getId(), new Task(task));
        prioritizedTasks.add(tasks.get(task.getId()));
    }

    @Override
    public void deleteTask(int id) {
        Task task = tasks.remove(id);
        if (task != null) {
            prioritizedTasks.remove(task);
            timeSlotManager.removeTask(task);
            historyManager.remove(id);
        }
    }

    @Override
    public void deleteAllTasks() {
        for (Task task : tasks.values()) {
            timeSlotManager.removeTask(task);
        }
        tasks.clear();
        prioritizedTasks.removeIf(task -> !(task instanceof Epic) && !(task instanceof Subtask));
    }

    // Методы для эпиков
    @Override
    public int createEpic(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название эпика не может быть пустым");
        }

        Epic epic = new Epic(name, description);
        int epicId = generateNextId();
        epic.setId(epicId);

        epics.put(epicId, epic);
        return epicId;
    }

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public Optional<Epic> getEpic(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
            return Optional.of(new Epic(epic));
        }
        return Optional.empty();
    }

    @Override
    public void updateEpic(Epic epic) {
        if (epic == null) {
            throw new IllegalArgumentException("Эпик не может быть null");
        }

        if (!epics.containsKey(epic.getId())) {
            throw new IllegalArgumentException("Эпик с ID " + epic.getId() + " не найден");
        }

        Epic existingEpic = epics.get(epic.getId());
        existingEpic.setName(epic.getName());
        existingEpic.setDescription(epic.getDescription());

        updateEpicStatus(existingEpic);
    }

    @Override
    public void deleteEpic(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            // Удаляем все подзадачи эпика
            List<Subtask> epicSubtasks = getEpicSubtasks(id);
            for (Subtask subtask : epicSubtasks) {
                deleteSubtask(subtask.getId());
            }
            historyManager.remove(id);
        }
    }

    @Override
    public void deleteAllEpics() {
        epics.clear();
        deleteAllSubtasks();
    }

    // Методы для подзадач
    @Override
    public void createSubtask(String name, String description, int epicId) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название подзадачи не может быть пустым");
        }
        Epic epic = epics.get(epicId);
        if (epic == null) {
            throw new IllegalArgumentException("Эпик с ID " + epicId + " не найден");
        }

        Subtask subtask = new Subtask(name, description, epicId);
        int subtaskId = generateNextId();
        subtask.setId(subtaskId);

        subtasks.put(subtaskId, subtask);
        prioritizedTasks.add(subtask);

        updateEpicStatus(epic);
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public Optional<Subtask> getSubtask(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) {
            historyManager.add(subtask);
            return Optional.of(new Subtask(subtask));
        }
        return Optional.empty();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (subtask == null) {
            throw new IllegalArgumentException("Подзадача не может быть null");
        }

        Subtask existingSubtask = subtasks.get(subtask.getId());
        if (existingSubtask == null) {
            throw new IllegalArgumentException("Подзадача с ID " + subtask.getId() + " не найдена");
        }

        // ОПТИМИЗИРОВАННАЯ ПРОВЕРКА ПЕРЕСЕЧЕНИЙ O(1)
        if (hasTimeConflictOptimized(subtask)) {
            throw new TaskValidationException("Подзадача пересекается по времени с существующими задачами");
        }

        // Обновляем временную сетку
        timeSlotManager.removeTask(existingSubtask);
        timeSlotManager.addTask(subtask);

        // Обновляем в коллекциях
        prioritizedTasks.remove(existingSubtask);
        subtasks.put(subtask.getId(), new Subtask(subtask));
        prioritizedTasks.add(subtasks.get(subtask.getId()));

        // Обновляем статус родительского эпика
        Epic epic = epics.get(subtask.getEpicId());
        if (epic != null) {
            updateEpicStatus(epic);
        }
    }

    @Override
    public void deleteSubtask(int id) {
        Subtask subtask = subtasks.remove(id);
        if (subtask != null) {
            prioritizedTasks.remove(subtask);
            timeSlotManager.removeTask(subtask);
            historyManager.remove(id);

            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                updateEpicStatus(epic);
            }
        }
    }

    @Override
    public void deleteAllSubtasks() {
        for (Subtask subtask : subtasks.values()) {
            timeSlotManager.removeTask(subtask);
        }
        subtasks.clear();
        prioritizedTasks.removeIf(task -> task instanceof Subtask);

        // Обновляем статусы всех эпиков
        for (Epic epic : epics.values()) {
            updateEpicStatus(epic);
        }
    }

    // Получение подзадач эпика с использованием Stream API
    @Override
    public List<Subtask> getEpicSubtasks(int epicId) {
        return subtasks.values().stream()
                .filter(subtask -> subtask.getEpicId() == epicId)
                .collect(Collectors.toList());
    }

    // История просмотров задач
    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    // Получение приоритизированных задач
    public List<Task> getPrioritizedTasks() {
        return new ArrayList<>(prioritizedTasks);
    }

    // ОПТИМИЗИРОВАННЫЕ МЕТОДЫ ПРОВЕРКИ ПЕРЕСЕЧЕНИЙ O(1)

    /**
     * Проверяет пересечения с помощью оптимизированного алгоритма O(1).
     */
    private boolean hasTimeConflictOptimized(Task task) {
        return timeSlotManager.hasTimeConflict(task);
    }

    @Override
    public boolean isTasksOverlapping(Task task1, Task task2) {
        // Для совместимости с интерфейсом, но рекомендуется использовать
        // оптимизированный метод
        if (task1 == null || task2 == null) {
            return false;
        }

        if (task1.getStartTime() == null || task1.getDuration() == null ||
                task2.getStartTime() == null || task2.getDuration() == null) {
            return false;
        }

        if (task1.getId() == task2.getId()) {
            return false;
        }

        var end1 = task1.getEndTime();
        var end2 = task2.getEndTime();

        return !(end1.isBefore(task2.getStartTime()) || end1.isEqual(task2.getStartTime()) ||
                end2.isBefore(task1.getStartTime()) || end2.isEqual(task1.getStartTime()));
    }

    @Override
    public boolean hasTimeConflict(Task task) {
        // Используем оптимизированный алгоритм
        return hasTimeConflictOptimized(task);
    }

    // Вспомогательные методы

    private void updateEpicStatus(Epic epic) {
        List<Subtask> epicSubtasks = getEpicSubtasks(epic.getId());

        if (epicSubtasks.isEmpty()) {
            epic.setStatus(TaskStatus.NEW);
            return;
        }

        boolean allNew = epicSubtasks.stream().allMatch(subtask -> subtask.getStatus() == TaskStatus.NEW);
        boolean allDone = epicSubtasks.stream().allMatch(subtask -> subtask.getStatus() == TaskStatus.DONE);

        if (allNew) {
            epic.setStatus(TaskStatus.NEW);
        } else if (allDone) {
            epic.setStatus(TaskStatus.DONE);
        } else {
            epic.setStatus(TaskStatus.IN_PROGRESS);
        }
    }

    /**
     * Возвращает статистику использования временных слотов.
     */
    public String getTimeSlotStatistics() {
        return timeSlotManager.getStatistics();
    }

    /**
     * Находит ближайший свободный временной слот для задачи заданной длительности.
     */
    public LocalDateTime findNextFreeSlot(int durationMinutes, LocalDateTime searchStart) {
        return timeSlotManager.findNextFreeSlot(durationMinutes, searchStart);
    }
}
