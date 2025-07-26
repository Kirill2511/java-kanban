package main.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.model.TaskStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryTaskManager implements TaskManager {
    private final Map<Integer, Task> tasks = new HashMap<>();
    private final Map<Integer, Epic> epics = new HashMap<>();
    private final Map<Integer, Subtask> subtasks = new HashMap<>();
    private final HistoryManager historyManager = Managers.getDefaultHistory();
    private int nextId = 1;

    private int generateNextId() {
        if (nextId == Integer.MAX_VALUE) {
            throw new IllegalStateException("Достигнут максимальный ID");
        }
        return nextId++;
    }

    // Методы для обычных задач
    @Override
    public int createTask(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название задачи не может быть пустым");
        }
        Task task = new Task(name, description);
        task.setId(generateNextId());
        tasks.put(task.getId(), task);
        return task.getId();
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
    public Task getTask(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
            return new Task(task);
        }
        return null;
    }

    @Override
    public void updateTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Задача не может быть пустой");
        }
        if (tasks.containsKey(task.getId())) {
            tasks.put(task.getId(), new Task(task));
        }
    }

    @Override
    public void deleteTask(int id) {
        tasks.remove(id);
    }

    @Override
    public void deleteAllTasks() {
        tasks.clear();
    }

    // Методы для эпиков
    @Override
    public int createEpic(String name, String description) {
        if (name == null || name.trim().isEmpty() || description == null) {
            throw new IllegalArgumentException("Название и описание эпика не могут быть пустыми");
        }
        Epic epic = new Epic(name, description);
        epic.setId(generateNextId());
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
    public Epic getEpic(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
            return new Epic(epic);
        }
        return null;
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
            }
            epics.remove(id);
        }
    }

    @Override
    public void deleteAllEpics() {
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
        // Дополнительная проверка на случай переполнения ID или других проблем
        if (subtaskId == epicId) {
            subtaskId = generateNextId(); // Получаем следующий ID
        }
        subtask.setId(subtaskId);
        subtasks.put(subtask.getId(), subtask);
        epic.addSubtaskId(subtask.getId());
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
    public Subtask getSubtask(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) {
            historyManager.add(subtask);
            return new Subtask(subtask);
        }
        return null;
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (subtask == null) {
            throw new IllegalArgumentException("Подзадача не может быть пустой");
        }
        if (subtasks.containsKey(subtask.getId())) {
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                subtasks.put(subtask.getId(), new Subtask(subtask));
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
        }
    }

    @Override
    public void deleteAllSubtasks() {
        subtasks.clear();
        for (Epic epic : epics.values()) {
            epic.getSubtaskIds().forEach(subtasks::remove);
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

        List<Subtask> epicSubtasks = new ArrayList<>();
        for (int subtaskId : epic.getSubtaskIds()) {
            Subtask subtask = subtasks.get(subtaskId);
            if (subtask != null) {
                epicSubtasks.add(new Subtask(subtask));
            }
        }
        return epicSubtasks;
    }

    // Обновление статуса эпика на основе подзадач
    private void updateEpicStatus(Epic epic) {
        List<Subtask> epicSubtasks = getEpicSubtasks(epic.getId());

        if (epicSubtasks.isEmpty()) {
            epic.setStatus(TaskStatus.NEW);
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
    }

    // История просмотров задач
    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }
}