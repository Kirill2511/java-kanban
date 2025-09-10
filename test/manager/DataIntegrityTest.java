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
 * Тесты для демонстрации проблем с сеттерами и проверки защитных механизмов.
 */
public class DataIntegrityTest {

    private TaskManager taskManager;

    @BeforeEach
    void setUp() {
        taskManager = new InMemoryTaskManager();
    }

    /**
     * Демонстрирует проблему: изменение задачи через сеттеры влияет на данные
     * менеджера, если менеджер возвращает ссылки вместо копий.
     */
    @Test
    void problem_taskModificationThroughSetters() {
        // given
        int taskId = taskManager.createTask("Оригинальная задача", "Оригинальное описание");

        // when - получаем задачу и изменяем её
        var taskOpt = taskManager.getTask(taskId);
        assertTrue(taskOpt.isPresent());
        Task task = taskOpt.get();
        String originalName = task.getName();
        String originalDescription = task.getDescription();
        TaskStatus originalStatus = task.getStatus();

        // Модифицируем полученную задачу
        task.setName("ИЗМЕНЕННОЕ НАЗВАНИЕ");
        task.setDescription("ИЗМЕНЕННОЕ ОПИСАНИЕ");
        task.setStatus(TaskStatus.DONE);

        // then - проверяем, что внутренние данные НЕ изменились
        var freshTaskOpt = taskManager.getTask(taskId);
        assertTrue(freshTaskOpt.isPresent());
        Task freshTask = freshTaskOpt.get();

        assertEquals(originalName, freshTask.getName(),
                "InMemoryTaskManager должен возвращать копии, а не ссылки");
        assertEquals(originalDescription, freshTask.getDescription(),
                "InMemoryTaskManager должен возвращать копии, а не ссылки");
        assertEquals(originalStatus, freshTask.getStatus(),
                "InMemoryTaskManager должен возвращать копии, а не ссылки");
    }

    /**
     * Демонстрирует защиту от изменения списка подзадач через геттер.
     */
    @Test
    void protection_epicSubtaskListModification() {
        // given
        int epicId = taskManager.createEpic("Тестовый эпик", "Описание");
        taskManager.createSubtask("Подзадача 1", "Описание 1", epicId);
        taskManager.createSubtask("Подзадача 2", "Описание 2", epicId);

        var epicOpt = taskManager.getEpic(epicId);
        assertTrue(epicOpt.isPresent());
        Epic epic = epicOpt.get();
        int originalSubtaskCount = epic.getSubtaskIds().size();

        // when - пытаемся изменить список подзадач через геттер
        List<Integer> subtaskIds = epic.getSubtaskIds();
        subtaskIds.clear(); // Пытаемся очистить список
        subtaskIds.add(999); // Пытаемся добавить невалидный ID

        // then - исходный эпик не должен измениться
        var freshEpicOpt = taskManager.getEpic(epicId);
        assertTrue(freshEpicOpt.isPresent());
        Epic freshEpic = freshEpicOpt.get();
        assertEquals(originalSubtaskCount, freshEpic.getSubtaskIds().size(),
                "Список подзадач не должен изменяться через геттер");
        assertFalse(freshEpic.getSubtaskIds().contains(999),
                "Невалидный ID не должен появиться в списке подзадач");
    }

    /**
     * Демонстрирует проблему изменения ID задачи и её влияние на целостность
     * данных.
     */
    @Test
    void problem_taskIdModification() {
        // given
        int originalTaskId = taskManager.createTask("Тестовая задача", "Описание");
        var taskOpt = taskManager.getTask(originalTaskId);
        assertTrue(taskOpt.isPresent());
        Task task = taskOpt.get();

        // when - изменяем ID задачи
        int newTaskId = 999;
        task.setId(newTaskId);

        // then - задача должна быть доступна по исходному ID, а не по новому
        assertTrue(taskManager.getTask(originalTaskId).isPresent(),
                "Задача должна быть доступна по исходному ID");
        assertTrue(taskManager.getTask(newTaskId).isEmpty(),
                "Задача НЕ должна быть доступна по измененному ID");

        // Проверяем, что ID в возвращаемой копии не влияет на внутреннее состояние
        var freshTaskOpt = taskManager.getTask(originalTaskId);
        assertTrue(freshTaskOpt.isPresent());
        Task freshTask = freshTaskOpt.get();
        assertEquals(originalTaskId, freshTask.getId(),
                "ID внутренней задачи не должен измениться");
    }


    /**
     * Проверяет корректность обновления статуса эпика при изменении подзадач.
     */
    @Test
    void integrity_epicStatusConsistency() {
        // given
        int epicId = taskManager.createEpic("Эпик для проверки статуса", "Описание");
        taskManager.createSubtask("Подзадача 1", "Описание 1", epicId);
        taskManager.createSubtask("Подзадача 2", "Описание 2", epicId);

        List<Subtask> subtasks = taskManager.getEpicSubtasks(epicId);
        Subtask subtask1 = subtasks.get(0);
        Subtask subtask2 = subtasks.get(1);

        // Проверяем начальный статус
        var epicOpt = taskManager.getEpic(epicId);
        assertTrue(epicOpt.isPresent());
        Epic epic = epicOpt.get();
        assertEquals(TaskStatus.NEW, epic.getStatus(), "Изначально эпик должен быть NEW");

        // when - одну подзадачу делаем IN_PROGRESS
        subtask1.setStatus(TaskStatus.IN_PROGRESS);
        taskManager.updateSubtask(subtask1);

        // then
        var epicOpt2 = taskManager.getEpic(epicId);
        assertTrue(epicOpt2.isPresent());
        epic = epicOpt2.get();
        assertEquals(TaskStatus.IN_PROGRESS, epic.getStatus(),
                "Эпик должен стать IN_PROGRESS когда хотя бы одна подзадача IN_PROGRESS");

        // when - обе подзадачи делаем DONE
        subtask1.setStatus(TaskStatus.DONE);
        subtask2.setStatus(TaskStatus.DONE);
        taskManager.updateSubtask(subtask1);
        taskManager.updateSubtask(subtask2);

        // then
        var epicOpt3 = taskManager.getEpic(epicId);
        assertTrue(epicOpt3.isPresent());
        epic = epicOpt3.get();
        assertEquals(TaskStatus.DONE, epic.getStatus(),
                "Эпик должен стать DONE когда все подзадачи DONE");
    }

    /**
     * Проверяет защиту от создания циклических ссылок.
     */
    @Test
    void protection_preventCircularReferences() {
        // given
        int epicId = taskManager.createEpic("Эпик", "Описание");
        taskManager.createSubtask("Подзадача", "Описание", epicId);

        List<Subtask> subtasks = taskManager.getEpicSubtasks(epicId);
        Subtask subtask = subtasks.get(0);

        // when/then - попытка установить ID подзадачи равным ID эпика должна вызвать
        // исключение
        assertThrows(IllegalArgumentException.class, () -> subtask.setId(epicId), "Подзадача не должна иметь ID равный ID эпика");
    }

    /**
     * Тест производительности связного списка истории.
     */
    @Test
    void performance_historyLinkedListOperations() {
        // given
        final int OPERATION_COUNT = 10000;

        // Создаем много задач
        for (int i = 0; i < 100; i++) {
            taskManager.createTask("Задача " + i, "Описание " + i);
        }

        // when - выполняем много операций с историей
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < OPERATION_COUNT; i++) {
            int taskId = (i % 100) + 1; // Циклически обращаемся к задачам
            taskManager.getTask(taskId); // Добавляем в историю
        }

        long endTime = System.currentTimeMillis();
        long operationTime = endTime - startTime;

        // then
        List<Task> history = taskManager.getHistory();
        assertEquals(100, history.size(), "В истории должно быть 100 уникальных задач");

        // Операции должны выполняться быстро благодаря O(1) алгоритму
        assertTrue(operationTime < 1000,
                "Операции с историей должны выполняться быстро: " + operationTime + "ms");

        System.out.println("Время выполнения " + OPERATION_COUNT + " операций: " + operationTime + "ms");
    }

    /**
     * Проверяет правильность работы истории при сложных сценариях.
     */
    @Test
    void history_complexScenarioCorrectness() {
        // given
        int task1Id = taskManager.createTask("Задача 1", "Описание 1");
        int epicId = taskManager.createEpic("Эпик", "Описание эпика");
        taskManager.createSubtask("Подзадача", "Описание подзадачи", epicId);

        List<Subtask> subtasks = taskManager.getEpicSubtasks(epicId);
        int subtaskId = subtasks.get(0).getId();

        // when - сложная последовательность просмотров
        taskManager.getTask(task1Id); // [Task1]
        taskManager.getEpic(epicId); // [Task1, Epic]
        taskManager.getSubtask(subtaskId); // [Task1, Epic, Subtask]
        taskManager.getTask(task1Id); // [Epic, Subtask, Task1] - Task1 перемещается в конец
        taskManager.getEpic(epicId); // [Subtask, Task1, Epic] - Epic перемещается в конец

        // then
        List<Task> history = taskManager.getHistory();
        assertEquals(3, history.size());
        assertInstanceOf(Subtask.class, history.get(0));
        assertTrue(history.get(1) != null && !(history.get(1) instanceof Epic));
        assertInstanceOf(Epic.class, history.get(2));

        // when - удаляем подзадачу
        taskManager.deleteSubtask(subtaskId);

        // then - подзадача должна исчезнуть из истории
        history = taskManager.getHistory();
        assertEquals(2, history.size());
        assertFalse(history.stream().anyMatch(task -> task.getId() == subtaskId));
    }
}
