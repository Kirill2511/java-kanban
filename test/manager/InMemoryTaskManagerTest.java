package test.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.manager.InMemoryTaskManager;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest {

    private TaskManager taskManager;

    @BeforeEach
    void setUp() {
        taskManager = new InMemoryTaskManager();
    }

    /**
     * Проверяет, что создаётся задача с уникальным ID и корректными полями.
     */
    @Test
    void createTask_shouldCreateTaskWithUniqueId() {
        // given
        String name = "Тестовая задача";
        String description = "Тестовое описание";

        // when
        int taskId = taskManager.createTask(name, description);

        // then
        assertTrue(taskId > 0);
        Task createdTask = taskManager.getTask(taskId);
        assertNotNull(createdTask);
        assertEquals(name, createdTask.getName());
        assertEquals(description, createdTask.getDescription());
        assertEquals(TaskStatus.NEW, createdTask.getStatus());
    }

    /**
     * Проверяет, что при пустом имени задачи выбрасывается исключение.
     */
    @Test
    void createTask_shouldThrowExceptionForEmptyName() {
        // when и then
        assertThrows(IllegalArgumentException.class, () -> taskManager.createTask("", "Description"));
    }

    /**
     * Проверяет, что при null в имени задачи выбрасывается исключение.
     */
    @Test
    void createTask_shouldThrowExceptionForNullName() {
        // when и then
        assertThrows(IllegalArgumentException.class, () -> taskManager.createTask(null, "Description"));
    }

    /**
     * Проверяет, что если задач нет, возвращается пустой список.
     */
    @Test
    void getAllTasks_shouldReturnEmptyListWhenNoTasks() {
        // when
        List<Task> tasks = taskManager.getAllTasks();

        // then
        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
    }

    /**
     * Проверяет, что возвращаются все созданные задачи.
     */
    @Test
    void getAllTasks_shouldReturnAllCreatedTasks() {
        // given
        int task1Id = taskManager.createTask("Задача 1", "Описание 1");
        int task2Id = taskManager.createTask("Задача 2", "Описание 2");

        // when
        List<Task> tasks = taskManager.getAllTasks();

        // then
        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().anyMatch(task -> task.getId() == task1Id));
        assertTrue(tasks.stream().anyMatch(task -> task.getId() == task2Id));
    }

    /**
     * Проверяет, что для несуществующей задачи возвращается null.
     */
    @Test
    void getTask_shouldReturnNullForNonExistentTask() {
        // when
        Task task = taskManager.getTask(999);

        // then
        assertNull(task);
    }

    /**
     * Проверяет, что обновление задачи меняет её статус.
     */
    @Test
    void updateTask_shouldUpdateExistingTask() {
        // given
        int taskId = taskManager.createTask("Исходная задача", "Исходное описание");
        Task task = taskManager.getTask(taskId);
        task.setStatus(TaskStatus.IN_PROGRESS);

        // when
        taskManager.updateTask(task);

        // then
        Task updatedTask = taskManager.getTask(taskId);
        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.getStatus());
    }

    /**
     * Проверяет, что при попытке обновить null-задачу выбрасывается исключение.
     */
    @Test
    void updateTask_shouldThrowExceptionForNullTask() {
        // when и then
        assertThrows(IllegalArgumentException.class, () -> taskManager.updateTask(null));
    }

    /**
     * Проверяет, что задача удаляется корректно.
     */
    @Test
    void deleteTask_shouldRemoveTask() {
        // given
        int taskId = taskManager.createTask("Задача для удаления", "Описание");

        // when
        taskManager.deleteTask(taskId);

        // then
        assertNull(taskManager.getTask(taskId));
        assertTrue(taskManager.getAllTasks().isEmpty());
    }

    /**
     * Проверяет, что удаляются все задачи.
     */
    @Test
    void deleteAllTasks_shouldRemoveAllTasks() {
        // given
        taskManager.createTask("Задача 1", "Описание 1");
        taskManager.createTask("Задача 2", "Описание 2");

        // when
        taskManager.deleteAllTasks();

        // then
        assertTrue(taskManager.getAllTasks().isEmpty());
    }

    /**
     * Проверяет, что создаётся эпик с уникальным ID и корректными полями.
     */
    @Test
    void createEpic_shouldCreateEpicWithUniqueId() {
        // given
        String name = "Тестовый эпик";
        String description = "Описание тестового эпика";

        // when
        int epicId = taskManager.createEpic(name, description);

        // then
        assertTrue(epicId > 0);
        Epic createdEpic = taskManager.getEpic(epicId);
        assertNotNull(createdEpic);
        assertEquals(name, createdEpic.getName());
        assertEquals(description, createdEpic.getDescription());
        assertEquals(TaskStatus.NEW, createdEpic.getStatus());
        assertTrue(createdEpic.getSubtaskIds().isEmpty());
    }

    /**
     * Проверяет, что создаётся подзадача, связанная с эпиком.
     */
    @Test
    void createSubtask_shouldCreateSubtaskLinkedToEpic() {
        // given
        int epicId = taskManager.createEpic("Эпик", "Описание эпика");

        // when
        taskManager.createSubtask("Подзадача", "Описание подзадачи", epicId);

        // then
        List<Subtask> subtasks = taskManager.getAllSubtasks();
        assertEquals(1, subtasks.size());
        Subtask subtask = subtasks.getFirst();
        assertEquals(epicId, subtask.getEpicId());

        Epic epic = taskManager.getEpic(epicId);
        assertEquals(1, epic.getSubtaskIds().size());
        assertTrue(epic.getSubtaskIds().contains(subtask.getId()));
    }

    /**
     * Проверяет, что при попытке создать подзадачу для несуществующего эпика
     * выбрасывается исключение.
     */
    @Test
    void createSubtask_shouldThrowExceptionForNonExistentEpic() {
        // when и then
        assertThrows(IllegalArgumentException.class, () -> taskManager.createSubtask("Subtask", "Description", 999));
    }

    /**
     * Проверяет, что удаление эпика удаляет и все его подзадачи.
     */
    @Test
    void deleteEpic_shouldDeleteEpicAndAllSubtasks() {
        // given
        int epicId = taskManager.createEpic("Эпик", "Описание эпика");
        taskManager.createSubtask("Подзадача 1", "Описание 1", epicId);
        taskManager.createSubtask("Подзадача 2", "Описание 2", epicId);

        // when
        taskManager.deleteEpic(epicId);

        // then
        assertNull(taskManager.getEpic(epicId));
        assertTrue(taskManager.getAllSubtasks().isEmpty());
    }

    /**
     * Проверяет, что история изначально пуста.
     */
    @Test
    void getHistory_shouldReturnEmptyListInitially() {
        // when
        List<Task> history = taskManager.getHistory();

        // then
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    /**
     * Проверяет, что история отслеживает просмотры задач и эпиков.
     */
    @Test
    void getHistory_shouldTrackTaskViews() {
        // given
        int taskId = taskManager.createTask("Задача", "Описание");
        int epicId = taskManager.createEpic("Эпик", "Описание эпика");

        // when
        taskManager.getTask(taskId);
        taskManager.getEpic(epicId);

        // then
        List<Task> history = taskManager.getHistory();
        assertEquals(2, history.size());
        assertEquals(taskId, history.get(0).getId());
        assertEquals(epicId, history.get(1).getId());
    }

    /**
     * Проверяет, что история корректно хранит множество уникальных задач.
     */
    @Test
    void getHistory_shouldTrackMultipleUniqueTasks() {
        // given
        int task1Id = taskManager.createTask("Задача 1", "Описание 1");
        int task2Id = taskManager.createTask("Задача 2", "Описание 2");
        int task3Id = taskManager.createTask("Задача 3", "Описание 3");
        int epicId = taskManager.createEpic("Эпик", "Описание эпика");

        // when - просматриваем задачи в разном порядке
        taskManager.getTask(task1Id);
        taskManager.getTask(task2Id);
        taskManager.getEpic(epicId);
        taskManager.getTask(task3Id);
        taskManager.getTask(task1Id); // повторный просмотр - должен переместиться в конец

        // then
        List<Task> history = taskManager.getHistory();
        assertEquals(4, history.size(), "История должна содержать 4 уникальные задачи");

        // Проверяем порядок: task2, epic, task3, task1 (task1 переместилась в конец)
        assertEquals(task2Id, history.get(0).getId());
        assertEquals(epicId, history.get(1).getId());
        assertEquals(task3Id, history.get(2).getId());
        assertEquals(task1Id, history.get(3).getId());
    }
}
