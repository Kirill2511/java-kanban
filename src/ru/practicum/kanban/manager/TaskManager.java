package ru.practicum.kanban.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.practicum.kanban.model.Epic;
import ru.practicum.kanban.model.Subtask;
import ru.practicum.kanban.model.Task;
import ru.practicum.kanban.model.TaskStatus;

public class TaskManager {
	private final Map<Integer, Task> tasks = new HashMap<>();
	private final Map<Integer, Epic> epics = new HashMap<>();
	private final Map<Integer, Subtask> subtasks = new HashMap<>();
	private int nextId = 1;

	private int generateNextId() {
		if (nextId == Integer.MAX_VALUE) {
			throw new IllegalStateException("Достигнут максимальный ID");
		}
		return nextId++;
	}

	// Методы для обычных задач
	public int createTask(String name, String description) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Название задачи не может быть пустым");
		}
		Task task = new Task(name, description);
		task.setId(generateNextId());
		tasks.put(task.getId(), task);
		return task.getId();
	}

	public List<Task> getAllTasks() {
		return new ArrayList<>(tasks.values());
	}

	public Task getTask(int id) {
		return tasks.get(id);
	}

	public void updateTask(Task task) {
		if (task == null) {
			throw new IllegalArgumentException("Задача не может быть пустой");
		}
		if (tasks.containsKey(task.getId())) {
			tasks.put(task.getId(), task);
		}
	}

	public void deleteTask(int id) {
		tasks.remove(id);
	}

	public void deleteAllTasks() {
		tasks.clear();
	}

	// Методы для эпиков
	public int createEpic(String name, String description) {
		if (name == null || name.trim().isEmpty() || description == null) {
			throw new IllegalArgumentException("Название и описание эпика не могут быть пустыми");
		}
		Epic epic = new Epic(name, description);
		epic.setId(generateNextId());
		epics.put(epic.getId(), epic);
		return epic.getId();
	}

	public List<Epic> getAllEpics() {
		return new ArrayList<>(epics.values());
	}

	public Epic getEpic(int id) {
		return epics.get(id);
	}

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

	public void deleteAllEpics() {
		epics.clear();
		subtasks.clear();
	}

	// Методы для подзадач
	public void createSubtask(String name, String description, int epicId) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Название подзадачи не может быть пустым");
		}
		Epic epic = epics.get(epicId);
		if (epic == null) {
			throw new IllegalArgumentException("Эпик с ID " + epicId + " не найден");
		}

		Subtask subtask = new Subtask(name, description, epicId);
		subtask.setId(generateNextId());
		subtasks.put(subtask.getId(), subtask);
		epic.addSubtaskId(subtask.getId());
		updateEpicStatus(epic);
	}

	public List<Subtask> getAllSubtasks() {
		return new ArrayList<>(subtasks.values());
	}

	public Subtask getSubtask(int id) {
		return subtasks.get(id);
	}

	public void updateSubtask(Subtask subtask) {
		if (subtask == null) {
			throw new IllegalArgumentException("Подзадача не может быть пустой");
		}
		if (subtasks.containsKey(subtask.getId())) {
			Epic epic = epics.get(subtask.getEpicId());
			if (epic != null) {
				subtasks.put(subtask.getId(), subtask);
				updateEpicStatus(epic);
			}
		}
	}

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

	public void deleteAllSubtasks() {
		subtasks.clear();
		for (Epic epic : epics.values()) {
			epic.getSubtaskIds().clear();
			updateEpicStatus(epic);
		}
	}

	// Получение подзадач эпика
	public List<Subtask> getEpicSubtasks(int epicId) {
		Epic epic = epics.get(epicId);
		if (epic == null) {
			return new ArrayList<>();
		}

		List<Subtask> epicSubtasks = new ArrayList<>();
		for (int subtaskId : epic.getSubtaskIds()) {
			Subtask subtask = subtasks.get(subtaskId);
			if (subtask != null) {
				epicSubtasks.add(subtask);
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
}