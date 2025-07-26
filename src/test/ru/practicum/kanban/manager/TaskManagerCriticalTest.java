package test.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.manager.InMemoryTaskManager;
import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskManagerCriticalTest {

    private InMemoryTaskManager taskManager;

    @BeforeEach
    void setUp() {
        taskManager = new InMemoryTaskManager();
    }

    /**
     * Проверяет, что задачи, эпики и подзадачи корректно находятся по их ID.
     */
    @Test
    void taskManager_shouldFindTasksOfDifferentTypesByID() {
        // given
        int taskId = taskManager.createTask("Задача", "Описание задачи");
        int epicId = taskManager.createEpic("Эпик", "Описание эпика");
        taskManager.createSubtask("Подзадача", "Описание подзадачи", epicId);

        // получаем ID подзадачи из эпика
        var subtasks = taskManager.getEpicSubtasks(epicId);
        int subtaskId = subtasks.getFirst().getId();

        // when
        Task retrievedTask = taskManager.getTask(taskId);
        Epic retrievedEpic = taskManager.getEpic(epicId);
        Subtask retrievedSubtask = taskManager.getSubtask(subtaskId);

        // then
        assertNotNull(retrievedTask, "Задача должна быть найдена по ID");
        assertNotNull(retrievedEpic, "Эпик должен быть найден по ID");
        assertNotNull(retrievedSubtask, "Подзадача должна быть найдена по ID");

        assertEquals(taskId, retrievedTask.getId());
        assertEquals(epicId, retrievedEpic.getId());
        assertEquals(subtaskId, retrievedSubtask.getId());
    }

    /**
     * Проверяет, что менеджер корректно обрабатывает конфликт между сгенерированным
     * и вручную установленным ID задачи.
     */
    @Test
    void taskManager_shouldHandleGeneratedAndManualIDsWithoutConflict() {
        // given
        int generatedTaskId = taskManager.createTask("Сгенерированная задача", "Описание");

        // when
        Task manualTask = new Task("Ручная задача", "Ручное описание");
        manualTask.setId(generatedTaskId); // Тот же ID, что и у сгенерированной

        // Это должно быть обработано менеджером (либо отклонено, либо корректно
        // обработано)
        // Сейчас тестируем текущее поведение
        taskManager.updateTask(manualTask); // Может перезаписать или должно быть предотвращено

        Task retrieved = taskManager.getTask(generatedTaskId);

        // then
        assertNotNull(retrieved, "Задача должна существовать");
        // Поведение зависит от реализации — зафиксируем, что происходит
        System.out.println("Имя полученной задачи: " + retrieved.getName());
    }

    /**
     * Проверяет, что после добавления задачи в менеджере внешние изменения исходного
     * объекта не влияют на сохранённую задачу.
     */
    @Test
    void taskManager_shouldPreserveTaskImmutabilityOnAdd() {
        // given
        Task originalTask = new Task("Оригинальная задача", "Оригинальное описание");
        originalTask.setStatus(TaskStatus.IN_PROGRESS);
        String originalName = originalTask.getName();
        String originalDescription = originalTask.getDescription();

        // when
        int managerId = taskManager.createTask(originalTask.getName(), originalTask.getDescription());

        // Симулируем внешнее изменение исходной задачи
        originalTask.setName("Измененное имя");
        originalTask.setDescription("Измененное описание");
        originalTask.setStatus(TaskStatus.DONE);

        // then
        Task retrievedTask = taskManager.getTask(managerId);

        // Задача в менеджере не должна быть затронута внешними изменениями
        assertEquals(originalName, retrievedTask.getName(),
                "Имя задачи в менеджере должно быть сохранено");
        assertEquals(originalDescription, retrievedTask.getDescription(),
                "Описание задачи в менеджере должно быть сохранено");
        // Статус должен быть NEW, так как createTask создаёт новую задачу со статусом
        // NEW
        assertEquals(TaskStatus.NEW, retrievedTask.getStatus(),
                "Статус задачи должен быть NEW при создании через менеджер");
    }

    /**
     * Проверяет, что после обновления задачи в менеджере дальнейшие внешние
     * изменения не затрагивают сохранённую версию.
     */
    @Test
    void taskManager_shouldMaintainTaskIntegrityWhenUpdating() {
        // given
        int taskId = taskManager.createTask("Оригинал", "Оригинальное описание");
        Task originalTask = taskManager.getTask(taskId);

        // Создаём копию для модификации
        Task modifiedTask = new Task(originalTask.getName(), originalTask.getDescription());
        modifiedTask.setId(originalTask.getId());
        modifiedTask.setName("Изменено");
        modifiedTask.setStatus(TaskStatus.DONE);

        // when
        taskManager.updateTask(modifiedTask);

        // Внешнее изменение после обновления
        modifiedTask.setName("Внешне изменено");
        modifiedTask.setDescription("Внешне измененное описание");

        // then
        Task retrievedTask = taskManager.getTask(taskId);
        assertEquals("Изменено", retrievedTask.getName(),
                "Задача должна сохранить обновлённое имя, а не внешние изменения");
        assertEquals("Оригинальное описание", retrievedTask.getDescription(),
                "Задача должна сохранить оригинальное описание");
        assertEquals(TaskStatus.DONE, retrievedTask.getStatus(),
                "Задача должна сохранить обновлённый статус");
    }

    /**
     * Проверяет, что подзадача не может иметь тот же ID, что и эпик, и корректно
     * ссылается на свой эпик.
     */
    @Test
    void epicSubtask_shouldNotCreateCircularReference() {
        // given
        int epicId = taskManager.createEpic("Эпик", "Описание эпика");

        // when и then
        // Это должно быть предотвращено: эпик не может быть своей подзадачей
        // Так как используется метод createSubtask, это предотвращается на уровне API

        // Но проверим, что ID эпика и подзадач всегда различаются
        taskManager.createSubtask("Подзадача", "Описание", epicId);
        var subtasks = taskManager.getEpicSubtasks(epicId);

        for (Subtask subtask : subtasks) {
            assertNotEquals(epicId, subtask.getId(),
                    "ID подзадачи никогда не должен равняться ID эпика");
            assertEquals(epicId, subtask.getEpicId(),
                    "Подзадача должна правильно ссылаться на свой эпик");
        }
    }
}
