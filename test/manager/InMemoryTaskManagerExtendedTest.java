package manager;

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

/**
 * Расширенные тесты для InMemoryTaskManager с проверкой целостности данных.
 */
public class InMemoryTaskManagerExtendedTest {

    private TaskManager taskManager;

    @BeforeEach
    void setUp() {
        taskManager = new InMemoryTaskManager();
    }

    // === ТЕСТЫ ЦЕЛОСТНОСТИ ДАННЫХ ДЛЯ ЭПИКОВ ===

    /**
     * Проверяет, что при удалении подзадачи её ID удаляется из эпика.
     */
    @Test
    void deleteSubtask_shouldRemoveSubtaskIdFromEpic() {
        // given
        int epicId = taskManager.createEpic("Эпик", "Описание эпика");
        taskManager.createSubtask("Подзадача", "Описание подзадачи", epicId);

        // Получаем ID подзадачи из списка подзадач эпика
        List<Subtask> epicSubtasks = taskManager.getEpicSubtasks(epicId);
        assertEquals(1, epicSubtasks.size());
        int subtaskId = epicSubtasks.getFirst().getId();

        // Проверяем, что подзадача добавлена в эпик
        Epic epic = taskManager.getEpic(epicId);
        assertTrue(epic.getSubtaskIds().contains(subtaskId));

        // when
        taskManager.deleteSubtask(subtaskId);

        // then
        epic = taskManager.getEpic(epicId);
        assertFalse(epic.getSubtaskIds().contains(subtaskId),
                "ID удаленной подзадачи не должен оставаться в эпике");
        assertTrue(epic.getSubtaskIds().isEmpty(),
                "Список подзадач эпика должен быть пустым");
    }


    /**
     * Проверяет, что статус эпика пересчитывается при изменении статуса подзадач.
     */
    @Test
    void updateSubtaskStatus_shouldRecalculateEpicStatus() {
        // given
        int epicId = taskManager.createEpic("Эпик", "Описание эпика");
        taskManager.createSubtask("Подзадача 1", "Описание 1", epicId);
        taskManager.createSubtask("Подзадача 2", "Описание 2", epicId);

        List<Subtask> epicSubtasks = taskManager.getEpicSubtasks(epicId);
        assertEquals(2, epicSubtasks.size());

        // Эпик должен быть NEW когда все подзадачи NEW
        Epic epic = taskManager.getEpic(epicId);
        assertEquals(TaskStatus.NEW, epic.getStatus());

        // when - одну подзадачу делаем IN_PROGRESS
        Subtask subtask1 = epicSubtasks.getFirst();
        subtask1.setStatus(TaskStatus.IN_PROGRESS);
        taskManager.updateSubtask(subtask1);

        // then - эпик должен стать IN_PROGRESS
        epic = taskManager.getEpic(epicId);
        assertEquals(TaskStatus.IN_PROGRESS, epic.getStatus());

        // when - обе подзадачи делаем DONE
        subtask1.setStatus(TaskStatus.DONE);
        taskManager.updateSubtask(subtask1);

        Subtask subtask2 = epicSubtasks.get(1);
        subtask2.setStatus(TaskStatus.DONE);
        taskManager.updateSubtask(subtask2);

        // then - эпик должен стать DONE
        epic = taskManager.getEpic(epicId);
        assertEquals(TaskStatus.DONE, epic.getStatus());
    }

    // === ТЕСТЫ ПРОБЛЕМ С СЕТТЕРАМИ ===


    /**
     * Проверяет проблему изменения ID задачи через сеттер.
     */
    @Test
    void taskIdSetter_shouldNotBreakInternalMapping() {
        // given
        int taskId = taskManager.createTask("Тестовая задача", "Описание");

        // when - получаем задачу и пытаемся изменить её ID
        Task retrievedTask = taskManager.getTask(taskId);
        int originalId = retrievedTask.getId();

        // Это потенциально опасная операция
        retrievedTask.setId(999);

        // then - задача должна по-прежнему быть доступна по исходному ID
        Task taskByOriginalId = taskManager.getTask(originalId);
        Task taskByNewId = taskManager.getTask(999);

        assertNotNull(taskByOriginalId, "Задача должна быть доступна по исходному ID");
        assertNull(taskByNewId, "Задача не должна быть доступна по новому ID");
    }

    /**
     * Проверяет изменение эпика через сеттеры после получения из менеджера.
     */
    @Test
    void epicSetters_shouldNotAffectSubtaskList() {
        // given
        int epicId = taskManager.createEpic("Эпик", "Описание эпика");
        taskManager.createSubtask("Подзадача", "Описание подзадачи", epicId);

        List<Subtask> epicSubtasks = taskManager.getEpicSubtasks(epicId);
        int subtaskId = epicSubtasks.getFirst().getId();

        // when - получаем эпик и пытаемся изменить список подзадач
        Epic retrievedEpic = taskManager.getEpic(epicId);
        List<Integer> subtaskIds = retrievedEpic.getSubtaskIds();
        subtaskIds.clear(); // Пытаемся очистить список подзадач

        // then - внутренний список подзадач не должен измениться
        Epic internalEpic = taskManager.getEpic(epicId);
        assertFalse(internalEpic.getSubtaskIds().isEmpty(),
                "Список подзадач во внутреннем эпике не должен быть пустым");
        assertTrue(internalEpic.getSubtaskIds().contains(subtaskId),
                "Подзадача должна оставаться в списке эпика");
    }

    /**
     * Проверяет интеграцию с новым алгоритмом истории - удаление из истории при
     * удалении задач.
     */
    @Test
    void taskDeletion_shouldRemoveFromHistory() {
        // given
        int taskId = taskManager.createTask("Задача", "Описание");
        int epicId = taskManager.createEpic("Эпик", "Описание эпика");
        taskManager.createSubtask("Подзадача", "Описание", epicId);

        List<Subtask> subtasks = taskManager.getAllSubtasks();
        int subtaskId = subtasks.getFirst().getId();

        // Просматриваем все элементы (добавляем в историю)
        taskManager.getTask(taskId);
        taskManager.getEpic(epicId);
        taskManager.getSubtask(subtaskId);

        List<Task> historyBefore = taskManager.getHistory();
        assertEquals(3, historyBefore.size(), "История должна содержать 3 элемента");

        // when - удаляем эпик (должны удалиться эпик и подзадача из истории)
        taskManager.deleteEpic(epicId);

        // then
        List<Task> historyAfter = taskManager.getHistory();
        assertEquals(1, historyAfter.size(), "В истории должна остаться только обычная задача");
        assertEquals(taskId, historyAfter.getFirst().getId(), "В истории должна остаться только обычная задача");

        // Проверяем, что эпик и подзадача удалены из истории
        assertFalse(historyAfter.stream().anyMatch(task -> task.getId() == epicId),
                "Эпик не должен остаться в истории");
        assertFalse(historyAfter.stream().anyMatch(task -> task.getId() == subtaskId),
                "Подзадача не должна остаться в истории");
    }

    // === ТЕСТЫ ПРОИЗВОДИТЕЛЬНОСТИ И НАГРУЗКИ ===

    /**
     * Проверяет работу менеджера с большим количеством задач.
     */
    @Test
    void performance_shouldHandleManyTasks() {
        // given
        final int TASK_COUNT = 1000;

        // when - создаем много задач
        for (int i = 0; i < TASK_COUNT; i++) {
            taskManager.createTask("Задача " + i, "Описание " + i);
        }

        // then
        List<Task> allTasks = taskManager.getAllTasks();
        assertEquals(TASK_COUNT, allTasks.size());

        // Проверяем, что каждая задача имеет уникальный ID
        long uniqueIds = allTasks.stream().mapToInt(Task::getId).distinct().count();
        assertEquals(TASK_COUNT, uniqueIds);
    }

    /**
     * Проверяет корректность работы с эпиками, содержащими много подзадач.
     */
    @Test
    void performance_shouldHandleEpicWithManySubtasks() {
        // given
        int epicId = taskManager.createEpic("Большой эпик", "Эпик с множеством подзадач");
        final int SUBTASK_COUNT = 100;

        // when - создаем много подзадач
        for (int i = 0; i < SUBTASK_COUNT; i++) {
            taskManager.createSubtask("Подзадача " + i, "Описание " + i, epicId);
        }

        // then
        Epic epic = taskManager.getEpic(epicId);
        assertEquals(SUBTASK_COUNT, epic.getSubtaskIds().size());

        List<Subtask> allSubtasks = taskManager.getAllSubtasks();
        assertEquals(SUBTASK_COUNT, allSubtasks.size());

        // Все подзадачи должны ссылаться на правильный эпик
        assertTrue(allSubtasks.stream().allMatch(subtask -> subtask.getEpicId() == epicId));
    }

    // === ТЕСТЫ ГРАНИЧНЫХ СЛУЧАЕВ ===


    /**
     * Проверяет корректность работы при удалении несуществующих задач.
     */
    @Test
    void delete_shouldHandleNonExistentTasks() {
        // given - пустой менеджер

        // when - пытаемся удалить несуществующие задачи
        taskManager.deleteTask(999);
        taskManager.deleteEpic(999);
        taskManager.deleteSubtask(999);

        // then - никаких исключений не должно возникнуть
        assertTrue(taskManager.getAllTasks().isEmpty());
        assertTrue(taskManager.getAllEpics().isEmpty());
        assertTrue(taskManager.getAllSubtasks().isEmpty());
        assertTrue(taskManager.getHistory().isEmpty());
    }
}
