package manager;

import main.ru.practicum.kanban.manager.InMemoryTaskManager;
import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TaskManagerCriticalTest {

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
     * Проверяет, что менеджер корректно обрабатывает обновление существующей задачи
     * и не создает конфликтов ID.
     */
    @Test
    void taskManager_shouldUpdateExistingTaskCorrectly() {
        // given
        int taskId = taskManager.createTask("Оригинальная задача", "Оригинальное описание");

        // when
        Task updatedTask = new Task("Обновленная задача", "Обновленное описание");
        updatedTask.setId(taskId);
        updatedTask.setStatus(TaskStatus.IN_PROGRESS);

        taskManager.updateTask(updatedTask);
        Task retrieved = taskManager.getTask(taskId);

        // then
        assertNotNull(retrieved, "Задача должна существовать");
        assertEquals("Обновленная задача", retrieved.getName(),
                "Название задачи должно быть обновлено");
        assertEquals("Обновленное описание", retrieved.getDescription(),
                "Описание задачи должно быть обновлено");
        assertEquals(TaskStatus.IN_PROGRESS, retrieved.getStatus(),
                "Статус задачи должен быть обновлен");
        assertEquals(taskId, retrieved.getId(),
                "ID задачи должен остаться неизменным");
    }

    /**
     * Проверяет, что менеджер возвращает независимые копии задач,
     * и изменения в одной копии не влияют на другие.
     */
    @Test
    void taskManager_shouldReturnImmutableCopies() {
        // given
        int taskId = taskManager.createTask("Оригинальная задача", "Оригинальное описание");

        // when
        Task firstCopy = taskManager.getTask(taskId);
        Task secondCopy = taskManager.getTask(taskId);

        // Изменяем первую копию
        firstCopy.setName("Измененное имя");
        firstCopy.setDescription("Измененное описание");
        firstCopy.setStatus(TaskStatus.DONE);

        // then
        // Вторая копия не должна быть затронута изменениями первой
        assertEquals("Оригинальная задача", secondCopy.getName(),
                "Изменение одной копии не должно влиять на имя другой");
        assertEquals("Оригинальное описание", secondCopy.getDescription(),
                "Изменение одной копии не должно влиять на описание другой");
        assertEquals(TaskStatus.NEW, secondCopy.getStatus(),
                "Изменение одной копии не должно влиять на статус другой");

        // Проверяем, что задача в менеджере тоже не изменилась
        Task fromManager = taskManager.getTask(taskId);
        assertEquals("Оригинальная задача", fromManager.getName(),
                "Задача в менеджере должна сохранить оригинальное имя");
        assertEquals("Оригинальное описание", fromManager.getDescription(),
                "Задача в менеджере должна сохранить оригинальное описание");
        assertEquals(TaskStatus.NEW, fromManager.getStatus(),
                "Задача в менеджере должна сохранить оригинальный статус");
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
     * ссылается на свой эпик. Также проверяет предотвращение циклических ссылок.
     */
    @Test
    void epicSubtask_shouldPreventCircularReferencesAndMaintainIntegrity() {
        // given
        int epicId = taskManager.createEpic("Эпик", "Описание эпика");

        // when
        taskManager.createSubtask("Подзадача 1", "Описание 1", epicId);
        taskManager.createSubtask("Подзадача 2", "Описание 2", epicId);

        var subtasks = taskManager.getEpicSubtasks(epicId);

        // then
        // Проверяем, что ID эпика и подзадач всегда различаются
        for (Subtask subtask : subtasks) {
            assertNotEquals(epicId, subtask.getId(),
                    "ID подзадачи никогда не должен равняться ID эпика");
            assertEquals(epicId, subtask.getEpicId(),
                    "Подзадача должна правильно ссылаться на свой эпик");
        }

        // Проверяем, что все ID уникальны между собой
        assertEquals(2, subtasks.size(), "Должно быть создано 2 подзадачи");
        assertNotEquals(subtasks.get(0).getId(), subtasks.get(1).getId(),
                "ID подзадач должны быть уникальными");

        // Проверяем, что эпик содержит ссылки на все свои подзадачи
        Epic epic = taskManager.getEpic(epicId);
        assertEquals(2, epic.getSubtaskIds().size(),
                "Эпик должен содержать ссылки на все подзадачи");

        for (Subtask subtask : subtasks) {
            assertTrue(epic.getSubtaskIds().contains(subtask.getId()),
                    "Эпик должен содержать ID подзадачи: " + subtask.getId());
        }
    }


    /**
     * Проверяет поведение при обновлении несуществующих задач.
     */
    @Test
    void taskManager_shouldHandleNonExistentTaskUpdates() {
        // given
        Task nonExistentTask = new Task("Несуществующая задача", "Описание");
        nonExistentTask.setId(999);

        Epic nonExistentEpic = new Epic("Несуществующий эпик", "Описание");
        nonExistentEpic.setId(998);

        Subtask nonExistentSubtask = new Subtask("Несуществующая подзадача", "Описание", 1);
        nonExistentSubtask.setId(997);

        // when & then
        // Обновление несуществующих объектов не должно бросать исключения,
        // но и не должно создавать новые объекты
        taskManager.updateTask(nonExistentTask);
        taskManager.updateEpic(nonExistentEpic);
        taskManager.updateSubtask(nonExistentSubtask);

        assertNull(taskManager.getTask(999),
                "Несуществующая задача не должна быть создана при обновлении");
        assertNull(taskManager.getEpic(998),
                "Несуществующий эпик не должен быть создан при обновлении");
        assertNull(taskManager.getSubtask(997),
                "Несуществующая подзадача не должна быть создана при обновлении");
    }

    /**
     * Проверяет целостность данных при удалении эпиков с подзадачами.
     */
    @Test
    void taskManager_shouldMaintainDataIntegrityOnEpicDeletion() {
        // given
        int epicId = taskManager.createEpic("Эпик", "Описание эпика");
        taskManager.createSubtask("Подзадача 1", "Описание 1", epicId);
        taskManager.createSubtask("Подзадача 2", "Описание 2", epicId);

        List<Subtask> subtasks = taskManager.getEpicSubtasks(epicId);
        int subtask1Id = subtasks.get(0).getId();
        int subtask2Id = subtasks.get(1).getId();

        // when
        taskManager.deleteEpic(epicId);

        // then
        assertNull(taskManager.getEpic(epicId),
                "Эпик должен быть удален");
        assertNull(taskManager.getSubtask(subtask1Id),
                "Подзадача 1 должна быть удалена вместе с эпиком");
        assertNull(taskManager.getSubtask(subtask2Id),
                "Подзадача 2 должна быть удалена вместе с эпиком");

        // Проверяем, что подзадачи не остались в общем списке
        List<Subtask> allSubtasks = taskManager.getAllSubtasks();
        assertTrue(allSubtasks.isEmpty(),
                "Все подзадачи должны быть удалены");
    }


    /**
     * Проверяет общую консистентность данных между эпиками и подзадачами
     * при различных операциях.
     */
    @Test
    void taskManager_shouldMaintainOverallDataConsistency() {
        // given
        int epicId = taskManager.createEpic("Эпик", "Описание эпика");
        taskManager.createSubtask("Подзадача 1", "Описание 1", epicId);
        taskManager.createSubtask("Подзадача 2", "Описание 2", epicId);

        // when
        Epic epic = taskManager.getEpic(epicId);
        List<Subtask> subtasks = taskManager.getEpicSubtasks(epicId);

        // then
        // Проверяем двустороннюю консистентность
        assertEquals(2, epic.getSubtaskIds().size(),
                "Эпик должен содержать 2 подзадачи");
        assertEquals(2, subtasks.size(),
                "Должно быть получено 2 подзадачи");

        for (Subtask subtask : subtasks) {
            assertTrue(epic.getSubtaskIds().contains(subtask.getId()),
                    "Эпик должен содержать ID подзадачи: " + subtask.getId());
            assertEquals(epicId, subtask.getEpicId(),
                    "Подзадача должна ссылаться на правильный эпик");
        }

        // when - удаляем одну подзадачу
        int firstSubtaskId = subtasks.getFirst().getId();
        taskManager.deleteSubtask(firstSubtaskId);

        // then - проверяем обновление связей
        Epic updatedEpic = taskManager.getEpic(epicId);
        List<Subtask> remainingSubtasks = taskManager.getEpicSubtasks(epicId);

        assertEquals(1, updatedEpic.getSubtaskIds().size(),
                "После удаления должна остаться 1 подзадача в эпике");
        assertEquals(1, remainingSubtasks.size(),
                "Должна быть получена 1 оставшаяся подзадача");
        assertFalse(updatedEpic.getSubtaskIds().contains(firstSubtaskId),
                "Эпик не должен содержать ID удаленной подзадачи");
        assertNull(taskManager.getSubtask(firstSubtaskId),
                "Удаленная подзадача не должна быть доступна");
    }
}
