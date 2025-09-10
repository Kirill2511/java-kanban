package manager;

import main.ru.practicum.kanban.exception.TaskValidationException;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TaskManagerTest<T extends TaskManager> {

    protected T manager;

    @BeforeEach
    void setUp() {
        manager = createTaskManager();
    }

    protected abstract T createTaskManager();

    @Test
    void testCreateTask() {
        int taskId = manager.createTask("Test Task", "Description");
        var taskOpt = manager.getTask(taskId);

        assertTrue(taskOpt.isPresent());
        Task task = taskOpt.get();
        assertEquals("Test Task", task.getName());
        assertEquals("Description", task.getDescription());
        assertEquals(TaskStatus.NEW, task.getStatus());
    }

    @Test
    void testCreateTaskWithEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            manager.createTask("", "Description");
        });
    }

    @Test
    void testCreateTaskWithNullName() {
        assertThrows(IllegalArgumentException.class, () -> {
            manager.createTask(null, "Description");
        });
    }

    @Test
    void testUpdateTask() {
        int taskId = manager.createTask("Task", "Description");
        var taskOpt = manager.getTask(taskId);
        assertTrue(taskOpt.isPresent());
        Task task = taskOpt.get();

        Task updatedTask = new Task(task.getId(), "Updated Task", "Updated Description", TaskStatus.IN_PROGRESS);
        manager.updateTask(updatedTask);

        var retrievedTaskOpt = manager.getTask(taskId);
        assertTrue(retrievedTaskOpt.isPresent());
        Task retrievedTask = retrievedTaskOpt.get();
        assertEquals("Updated Task", retrievedTask.getName());
        assertEquals("Updated Description", retrievedTask.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, retrievedTask.getStatus());
    }

    @Test
    void testDeleteTask() {
        int taskId = manager.createTask("Task", "Description");
        assertTrue(manager.getTask(taskId).isPresent());

        manager.deleteTask(taskId);
        assertTrue(manager.getTask(taskId).isEmpty());
    }

    @Test
    void testCreateEpic() {
        int epicId = manager.createEpic("Epic", "Epic Description");
        var epicOpt = manager.getEpic(epicId);

        assertTrue(epicOpt.isPresent());
        Epic epic = epicOpt.get();
        assertEquals("Epic", epic.getName());
        assertEquals("Epic Description", epic.getDescription());
        assertEquals(TaskStatus.NEW, epic.getStatus());
    }

    @Test
    void testCreateSubtaskForEpic() {
        int epicId = manager.createEpic("Epic", "Description");
        manager.createSubtask("Subtask", "Subtask Description", epicId);

        List<Subtask> subtasks = manager.getAllSubtasks();
        assertEquals(1, subtasks.size());

        Subtask subtask = subtasks.getFirst();
        assertEquals("Subtask", subtask.getName());
        assertEquals("Subtask Description", subtask.getDescription());
        assertEquals(epicId, subtask.getEpicId());
        assertEquals(TaskStatus.NEW, subtask.getStatus());
    }

    @Test
    void testGetEpicSubtasks() {
        int epicId = manager.createEpic("Epic", "Description");
        manager.createSubtask("Subtask1", "Description1", epicId);
        manager.createSubtask("Subtask2", "Description2", epicId);

        List<Subtask> epicSubtasks = manager.getEpicSubtasks(epicId);
        assertEquals(2, epicSubtasks.size());

        // Проверяем что все подзадачи принадлежат эпику
        for (Subtask subtask : epicSubtasks) {
            assertEquals(epicId, subtask.getEpicId());
        }
    }

    @Test
    void testDeleteEpicRemovesSubtasks() {
        int epicId = manager.createEpic("Epic", "Description");
        manager.createSubtask("Subtask1", "Description1", epicId);
        manager.createSubtask("Subtask2", "Description2", epicId);

        assertEquals(2, manager.getAllSubtasks().size());

        manager.deleteEpic(epicId);

        assertEquals(0, manager.getAllSubtasks().size());
        assertTrue(manager.getEpic(epicId).isEmpty());
    }

    @Test
    void testGetAllTasks() {
        manager.createTask("Task1", "Description1");
        manager.createTask("Task2", "Description2");

        List<Task> tasks = manager.getAllTasks();
        assertEquals(2, tasks.size());
    }

    @Test
    void testGetAllEpics() {
        manager.createEpic("Epic1", "Description1");
        manager.createEpic("Epic2", "Description2");

        List<Epic> epics = manager.getAllEpics();
        assertEquals(2, epics.size());
    }

    @Test
    void testGetAllSubtasks() {
        int epicId = manager.createEpic("Epic", "Description");
        manager.createSubtask("Subtask1", "Description1", epicId);
        manager.createSubtask("Subtask2", "Description2", epicId);

        List<Subtask> subtasks = manager.getAllSubtasks();
        assertEquals(2, subtasks.size());
    }

    @Test
    void testTaskPrioritization() {
        // Создаем задачи с разным временем
        int task1Id = manager.createTask("Task1", "Description");
        int task2Id = manager.createTask("Task2", "Description");
        int task3Id = manager.createTask("Task3", "Description");

        // Задача без времени
        // Первая задача остается без времени

        // Задача с поздним временем
        var task2Opt = manager.getTask(task2Id);
        assertTrue(task2Opt.isPresent());
        Task task2 = task2Opt.get();
        Task lateTask = new Task(task2.getId(), task2.getName(), task2.getDescription(), task2.getStatus(),
                Duration.ofHours(1), LocalDateTime.of(2024, 1, 15, 15, 0));
        manager.updateTask(lateTask);

        // Задача с ранним временем
        var task3Opt = manager.getTask(task3Id);
        assertTrue(task3Opt.isPresent());
        Task task3 = task3Opt.get();
        Task earlyTask = new Task(task3.getId(), task3.getName(), task3.getDescription(), task3.getStatus(),
                Duration.ofHours(1), LocalDateTime.of(2024, 1, 15, 10, 0));
        manager.updateTask(earlyTask);

        List<Task> prioritized = manager.getPrioritizedTasks();

        // Проверяем порядок: сначала задачи по времени, потом без времени
        assertTrue(prioritized.size() >= 3);
        assertEquals(task3Id, prioritized.get(0).getId()); // Самая ранняя
        assertEquals(task2Id, prioritized.get(1).getId()); // Поздняя
        // Задача без времени должна быть в конце
    }

    @Test
    void testOverlapDetection() {
        Task task1 = new Task(1, "Task1", "Description", TaskStatus.NEW,
                Duration.ofHours(2), LocalDateTime.of(2024, 1, 15, 10, 0)); // 10:00-12:00
        Task task2 = new Task(2, "Task2", "Description", TaskStatus.NEW,
                Duration.ofHours(2), LocalDateTime.of(2024, 1, 15, 11, 0)); // 11:00-13:00 (пересекается)
        Task task3 = new Task(3, "Task3", "Description", TaskStatus.NEW,
                Duration.ofHours(1), LocalDateTime.of(2024, 1, 15, 12, 0)); // 12:00-13:00 (граница)

        assertTrue(manager.isTasksOverlapping(task1, task2)); // Пересекаются
        assertFalse(manager.isTasksOverlapping(task1, task3)); // Не пересекаются (граница)
        assertTrue(manager.isTasksOverlapping(task2, task3)); // Пересекаются
    }

    @Test
    void testTaskTimeConflictValidation() {
        // Создаем первую задачу с временем
        int taskId1 = manager.createTask("Task1", "Description");
        var task1Opt = manager.getTask(taskId1);
        assertTrue(task1Opt.isPresent());
        Task task1 = task1Opt.get();
        Task timedTask1 = new Task(task1.getId(), task1.getName(), task1.getDescription(), task1.getStatus(),
                Duration.ofHours(2), LocalDateTime.of(2024, 1, 15, 10, 0));
        manager.updateTask(timedTask1);

        // Пытаемся создать пересекающуюся задачу
        int taskId2 = manager.createTask("Task2", "Description");
        var task2Opt = manager.getTask(taskId2);
        assertTrue(task2Opt.isPresent());
        Task task2 = task2Opt.get();
        Task conflictingTask = new Task(task2.getId(), task2.getName(), task2.getDescription(), task2.getStatus(),
                Duration.ofHours(2), LocalDateTime.of(2024, 1, 15, 11, 0));

        assertThrows(TaskValidationException.class, () -> {
            manager.updateTask(conflictingTask);
        });
    }

    @Test
    void testHistory() {
        int taskId1 = manager.createTask("Task1", "Description");
        int taskId2 = manager.createTask("Task2", "Description");

        // Просматриваем задачи
        manager.getTask(taskId1);
        manager.getTask(taskId2);
        manager.getTask(taskId1); // Повторный доступ

        List<Task> history = manager.getHistory();
        assertEquals(2, history.size()); // В истории должно быть 2 уникальные задачи
        assertEquals(taskId2, history.get(0).getId()); // Последняя просмотренная
        assertEquals(taskId1, history.get(1).getId()); // Предпоследняя
    }

    @Test
    void testNonExistentTaskRetrieval() {
        assertTrue(manager.getTask(999).isEmpty());
        assertTrue(manager.getEpic(999).isEmpty());
        assertTrue(manager.getSubtask(999).isEmpty());
    }
}
