import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskManager {
	private final Map<Integer, Task> tasks;
	private final Map<Integer, Epic> epics;
	private final Map<Integer, Subtask> subtasks;
	private int nextId;

	public TaskManager() {
		tasks = new HashMap<>();
		epics = new HashMap<>();
		subtasks = new HashMap<>();
		nextId = 1;
	}

	// Методы для обычных задач
	public Task createTask(String name, String description) {
		Task task = new Task(name, description);
		task.setId(nextId++);
		tasks.put(task.getId(), task);
		return task;
	}

	public List<Task> getAllTasks() {
		return new ArrayList<>(tasks.values());
	}

	public Task getTask(int id) {
		return tasks.get(id);
	}

	public void updateTask(Task task) {
		tasks.put(task.getId(), task);
	}

	public void deleteTask(int id) {
		tasks.remove(id);
	}

	public void deleteAllTasks() {
		tasks.clear();
	}

	// Методы для эпиков
	public Epic createEpic(String name, String description) {
		Epic epic = new Epic(name, description);
		epic.setId(nextId++);
		epics.put(epic.getId(), epic);
		return epic;
	}

	public List<Epic> getAllEpics() {
		return new ArrayList<>(epics.values());
	}

	public Epic getEpic(int id) {
		return epics.get(id);
	}

	public void updateEpic(Epic epic) {
		Epic savedEpic = epics.get(epic.getId());
		if (savedEpic != null) {
			// Сохраняем список подзадач из старого эпика
			epic.setSubtaskIds(savedEpic.getSubtaskIds());
			// ИГНОРИРУЕМ статус от пользователя - эпик не управляет своим статусом
			// Полная замена объекта
			epics.put(epic.getId(), epic);
			// Пересчитываем статус на основе подзадач (перезаписываем пользовательский)
			updateEpicStatus(epic);
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
	public Subtask createSubtask(String name, String description, int epicId) {
		Epic epic = epics.get(epicId);
		if (epic == null) {
			return null;
		}

		Subtask subtask = new Subtask(name, description, epicId);
		subtask.setId(nextId++);
		subtasks.put(subtask.getId(), subtask);
		epic.addSubtaskId(subtask.getId());
		updateEpicStatus(epic);
		return subtask;
	}

	public List<Subtask> getAllSubtasks() {
		return new ArrayList<>(subtasks.values());
	}

	public Subtask getSubtask(int id) {
		return subtasks.get(id);
	}

	public void updateSubtask(Subtask subtask) {
		subtasks.put(subtask.getId(), subtask);
		Epic epic = epics.get(subtask.getEpicId());
		if (epic != null) {
			updateEpicStatus(epic);
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