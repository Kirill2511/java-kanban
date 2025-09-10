package main.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.exception.TaskValidationException;
import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.model.TaskStatus;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryTaskManager implements TaskManager {
    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected final Map<Integer, Subtask> subtasks = new HashMap<>();
    protected final HistoryManager historyManager = Managers.getDefaultHistory();
    // TreeSet для хранения задач, отсортированных по времени начала
    protected final Set<Task> prioritizedTasks = new TreeSet<>((t1, t2) -> {
        // Если у одной из задач нет startTime, она не должна учитываться в приоритете
        if (t1.getStartTime() == null && t2.getStartTime() == null) {
            return Integer.compare(t1.getId(), t2.getId()); // Сортируем по ID для стабильности
        }
        if (t1.getStartTime() == null) {
            return 1; // Задачи без времени идут в конец
        }
        if (t2.getStartTime() == null) {
            return -1; // Задачи без времени идут в конец
        }

        int timeComparison = t1.getStartTime().compareTo(t2.getStartTime());
        if (timeComparison != 0) {
            return timeComparison;
        }

        // Если время одинаковое, сравниваем по ID для стабильности
        return Integer.compare(t1.getId(), t2.getId());
    });
    protected int nextId = 1;

    // Методы для обычных задач
    @Override
    public int createTask(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название задачи не может быть пустым");
        }
        Task task = new Task(name, description);
        int taskId = generateNextId();
        // Проверяем, что ID уникален среди всех задач
        while (tasks.containsKey(taskId) || epics.containsKey(taskId) || subtasks.containsKey(taskId)) {
            taskId = generateNextId();
        }
        task.setId(taskId);
        tasks.put(task.getId(), task);
        addToPrioritizedTasks(task); // Добавляем в приоритизированный список
        return task.getId();
    }

    private int generateNextId() {
        if (nextId >= Integer.MAX_VALUE) {
            throw new IllegalStateException("Достигнут максимальный ID");
        }
        return nextId++;
    }

    // Добавление задачи в приоритизированный список
    protected void addToPrioritizedTasks(Task task) {
        if (task != null && task.getStartTime() != null) {
            prioritizedTasks.add(task);
        }
    }

    @Override
    public List<Task> getAllTasks() {
        List<Task> taskCopies = new ArrayList<>();
        for (Task task : tasks.values()) {
            taskCopies.add(new Task(task));
        }
        return taskCopies;
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
            throw new IllegalArgumentException("Задача не может быть пустой");
        }
        if (tasks.containsKey(task.getId())) {
            // Проверяем на пересечение времени с другими задачами
            if (hasTimeConflict(task)) {
                throw new TaskValidationException("Задача пересекается по времени с существующими задачами");
            }

            Task oldTask = tasks.get(task.getId());
            Task newTask = new Task(task);
            tasks.put(task.getId(), newTask);
            updateInPrioritizedTasks(oldTask, newTask); // Обновляем в приоритизированном списке
        }
    }

    @Override
    public void deleteTask(int id) {
        Task task = tasks.get(id);
        tasks.remove(id);
        removeFromPrioritizedTasks(task); // Удаляем из приоритизированного списка
        historyManager.remove(id);
    }

    @Override
    public void deleteAllTasks() {
        for (int taskId : tasks.keySet()) {
            Task task = tasks.get(taskId);
            removeFromPrioritizedTasks(task); // Удаляем из приоритизированного списка
            historyManager.remove(taskId);
        }
        tasks.clear();
    }

    // Методы для эпиков
    @Override
    public int createEpic(String name, String description) {
        if (name == null || name.trim().isEmpty() || description == null) {
            throw new IllegalArgumentException("Название и описание эпика не могут быть пустыми");
        }
        Epic epic = new Epic(name, description);
        int epicId = generateNextId();
        // Проверяем, что ID уникален среди всех задач
        while (tasks.containsKey(epicId) || epics.containsKey(epicId) || subtasks.containsKey(epicId)) {
            epicId = generateNextId();
        }
        epic.setId(epicId);
        epics.put(epic.getId(), epic);
        return epic.getId();
    }

    @Override
    public List<Epic> getAllEpics() {
        List<Epic> epicCopies = new ArrayList<>();
        for (Epic epic : epics.values()) {
            epicCopies.add(new Epic(epic));
        }
        return epicCopies;
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
            throw new IllegalArgumentException("Эпик не может быть пустым");
        }
        if (epics.containsKey(epic.getId())) {
            Epic savedEpic = epics.get(epic.getId());
            savedEpic.setName(epic.getName());
            savedEpic.setDescription(epic.getDescription());
        }
    }

    @Override
    public void deleteEpic(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            // Удаляем все подзадачи эпика
            for (int subtaskId : epic.getSubtaskIds()) {
                subtasks.remove(subtaskId);
                historyManager.remove(subtaskId);
            }
            epics.remove(id);
            historyManager.remove(id);
        }
    }

    @Override
    public void deleteAllEpics() {
        for (int epicId : epics.keySet()) {
            historyManager.remove(epicId);
        }
        for (int subtaskId : subtasks.keySet()) {
            historyManager.remove(subtaskId);
        }
        epics.clear();
        subtasks.clear();
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
        // Проверяем, что ID уникален среди всех задач
        while (tasks.containsKey(subtaskId) || epics.containsKey(subtaskId) || subtasks.containsKey(subtaskId)) {
            subtaskId = generateNextId();
        }
        subtask.setId(subtaskId);
        subtasks.put(subtask.getId(), subtask);
        epic.addSubtaskId(subtask.getId());
        addToPrioritizedTasks(subtask); // Добавляем в приоритизированный список
        updateEpicStatus(epic);
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        List<Subtask> subtaskCopies = new ArrayList<>();
        for (Subtask subtask : subtasks.values()) {
            subtaskCopies.add(new Subtask(subtask));
        }
        return subtaskCopies;
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
            throw new IllegalArgumentException("Подзадача не может быть пустой");
        }
        if (subtasks.containsKey(subtask.getId())) {
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                // Проверяем на пересечение времени с другими задачами
                if (hasTimeConflict(subtask)) {
                    throw new TaskValidationException("Подзадача пересекается по времени с существующими задачами");
                }

                Subtask oldSubtask = subtasks.get(subtask.getId());
                Subtask newSubtask = new Subtask(subtask);
                subtasks.put(subtask.getId(), newSubtask);
                updateInPrioritizedTasks(oldSubtask, newSubtask); // Обновляем в приоритизированном списке
                updateEpicStatus(epic);
            }
        }
    }

    @Override
    public void deleteSubtask(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) {
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.removeSubtaskId(id);
                updateEpicStatus(epic);
            }
            subtasks.remove(id);
            removeFromPrioritizedTasks(subtask); // Удаляем из приоритизированного списка
            historyManager.remove(id);
        }
    }

    @Override
    public void deleteAllSubtasks() {
        for (int subtaskId : subtasks.keySet()) {
            Subtask subtask = subtasks.get(subtaskId);
            removeFromPrioritizedTasks(subtask); // Удаляем из приоритизированного списка
            historyManager.remove(subtaskId);
        }
        subtasks.clear();
        for (Epic epic : epics.values()) {
            epic.clearSubtaskIds();
            updateEpicStatus(epic);
        }
    }

    // Получение подзадач эпика
    @Override
    public List<Subtask> getEpicSubtasks(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) {
            return new ArrayList<>();
        }

        return epic.getSubtaskIds().stream()
                .map(subtasks::get)
                .filter(Objects::nonNull)
                .map(Subtask::new)
                .collect(Collectors.toList());
    }

    // История просмотров задач
    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    // Получение задач по приоритету (отсортированных по startTime)
    @Override
    public List<Task> getPrioritizedTasks() {
        return new ArrayList<>(prioritizedTasks);
    }

    // Проверка пересечения временных интервалов двух задач
    @Override
    public boolean isTasksOverlapping(Task task1, Task task2) {
        if (task1 == null || task2 == null) {
            return false;
        }

        // Если у задач нет времени начала или продолжительности, пересечения нет
        if (task1.getStartTime() == null || task1.getDuration() == null ||
                task2.getStartTime() == null || task2.getDuration() == null) {
            return false;
        }

        // Если это одна и та же задача, пересечения нет
        if (task1.getId() == task2.getId()) {
            return false;
        }

        // Рассчитываем время окончания задач
        var end1 = task1.getEndTime();
        var end2 = task2.getEndTime();

        // Проверяем пересечение отрезков [start1, end1] и [start2, end2]
        // Отрезки НЕ пересекаются, если один заканчивается до или ровно когда
        // начинается другой
        // Пересекаются в противном случае (если есть реальное наложение времени)
        return !(end1.isBefore(task2.getStartTime()) || end1.isEqual(task2.getStartTime()) ||
                end2.isBefore(task1.getStartTime()) || end2.isEqual(task1.getStartTime()));
    }

    // Проверка пересечения задачи с любой другой задачей в менеджере
    @Override
    public boolean hasTimeConflict(Task task) {
        if (task == null || task.getStartTime() == null || task.getDuration() == null) {
            return false;
        }

        // Используем Stream API для проверки пересечения с приоритизированными задачами
        return getPrioritizedTasks().stream()
                .filter(existingTask -> existingTask.getId() != task.getId()) // Исключаем саму задачу
                .anyMatch(existingTask -> isTasksOverlapping(task, existingTask));
    }

    // Обновление статуса эпика на основе подзадач
    protected void updateEpicStatus(Epic epic) {
        List<Subtask> epicSubtasks = getEpicSubtasks(epic.getId());

        if (epicSubtasks.isEmpty()) {
            epic.setStatus(TaskStatus.NEW);
            epic.updateCalculatedFields(epicSubtasks);
            return;
        }

        boolean allDone = true;
        boolean anyInProgress = false;

        for (Subtask subtask : epicSubtasks) {
            if (subtask.getStatus() != TaskStatus.DONE) {
                allDone = false;
            }
            if (subtask.getStatus() == TaskStatus.IN_PROGRESS) {
                anyInProgress = true;
            }
        }

        if (allDone) {
            epic.setStatus(TaskStatus.DONE);
        } else if (anyInProgress) {
            epic.setStatus(TaskStatus.IN_PROGRESS);
        } else {
            epic.setStatus(TaskStatus.NEW);
        }

        // Обновляем расчетные поля эпика
        epic.updateCalculatedFields(epicSubtasks);
    }

    // Удаление задачи из приоритизированного списка
    protected void removeFromPrioritizedTasks(Task task) {
        if (task != null) {
            prioritizedTasks.remove(task);
        }
    }

    // Обновление задачи в приоритизированном списке
    protected void updateInPrioritizedTasks(Task oldTask, Task newTask) {
        if (oldTask != null) {
            prioritizedTasks.remove(oldTask);
        }
        if (newTask != null && newTask.getStartTime() != null) {
            prioritizedTasks.add(newTask);
        }
    }
}
