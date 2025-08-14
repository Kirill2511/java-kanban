package manager;

import main.ru.practicum.kanban.manager.HistoryManager;
import main.ru.practicum.kanban.manager.Managers;
import main.ru.practicum.kanban.manager.TaskManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ManagersCriticalTest {

    /**
     * Проверяет, что Managers.getDefault() всегда возвращает инициализированный
     * TaskManager,
     * готовый к работе с задачами и историей.
     */
    @Test
    void managers_shouldAlwaysReturnInitializedTaskManager() {
        // when
        TaskManager manager = Managers.getDefault();

        // then
        assertNotNull(manager, "TaskManager не должен быть null");

        // Проверяем, что менеджер готов к работе
        int taskId = manager.createTask("Тестовая задача", "Тестовое описание");
        assertTrue(taskId > 0, "TaskManager должен уметь создавать задачи");

        assertNotNull(manager.getTask(taskId), "TaskManager должен уметь получать задачи");
        assertNotNull(manager.getAllTasks(), "TaskManager должен уметь возвращать все задачи");
        assertNotNull(manager.getHistory(), "TaskManager должен иметь рабочую историю");
    }

    /**
     * Проверяет, что Managers.getDefaultHistory() всегда возвращает
     * инициализированный HistoryManager,
     * готовый к работе с историей задач.
     */
    @Test
    void managers_shouldAlwaysReturnInitializedHistoryManager() {
        // when
        HistoryManager historyManager = Managers.getDefaultHistory();

        // then
        assertNotNull(historyManager, "HistoryManager не должен быть null");

        // Проверяем, что история инициализирована
        assertNotNull(historyManager.getHistory(),
                "HistoryManager должен возвращать инициализированный список истории");

        // Проверяем, что можно добавить задачу
        main.ru.practicum.kanban.model.Task task = new main.ru.practicum.kanban.model.Task("Тест", "Описание");
        task.setId(1);

        assertDoesNotThrow(() -> historyManager.add(task),
                "HistoryManager должен добавлять задачи без ошибок");

        assertEquals(1, historyManager.getHistory().size(),
                "HistoryManager должен корректно хранить добавленные задачи");
    }

    /**
     * Проверяет, что каждый вызов Managers.getDefault() и
     * Managers.getDefaultHistory()
     * возвращает рабочие экземпляры TaskManager и HistoryManager.
     */
    @Test
    void managers_shouldReturnWorkingInstancesEveryTime() {
        // when
        TaskManager manager1 = Managers.getDefault();
        TaskManager manager2 = Managers.getDefault();
        HistoryManager history1 = Managers.getDefaultHistory();
        HistoryManager history2 = Managers.getDefaultHistory();

        // then
        assertNotNull(manager1, "Первый TaskManager не должен быть null");
        assertNotNull(manager2, "Второй TaskManager не должен быть null");
        assertNotNull(history1, "Первый HistoryManager не должен быть null");
        assertNotNull(history2, "Второй HistoryManager не должен быть null");

        // Проверяем, что все экземпляры работают
        assertDoesNotThrow(() -> {
            manager1.createTask("Задача 1", "Описание 1");
            manager2.createTask("Задача 2", "Описание 2");
        }, "Все экземпляры TaskManager должны работать");

        assertDoesNotThrow(() -> {
            main.ru.practicum.kanban.model.Task task1 = new main.ru.practicum.kanban.model.Task("Тест1", "Описание1");
            task1.setId(1);
            main.ru.practicum.kanban.model.Task task2 = new main.ru.practicum.kanban.model.Task("Тест2", "Описание2");
            task2.setId(2);

            history1.add(task1);
            history2.add(task2);
        }, "Все экземпляры HistoryManager должны работать");
    }

    /**
     * Проверяет, что TaskManager и HistoryManager корректно интегрированы:
     * просмотр задачи добавляет её в историю.
     */
    @Test
    void managers_shouldReturnInstancesWithProperIntegration() {
        // given
        TaskManager manager = Managers.getDefault();

        // when
        int taskId = manager.createTask("Интеграционный тест", "Описание теста");
        manager.getTask(taskId); // Это должно добавить задачу в историю

        // then
        var history = manager.getHistory();
        assertNotNull(history, "Менеджер должен иметь рабочую интеграцию с историей");
        assertFalse(history.isEmpty(), "История должна содержать просмотренную задачу");
        assertEquals("Интеграционный тест", history.getFirst().getName(),
                "История должна содержать корректную задачу");
    }

    /**
     * Проверяет, что экземпляры TaskManager и HistoryManager сразу работоспособны
     * без дополнительной настройки.
     */
    @Test
    void managers_shouldReturnFunctionalInstancesImmediately() {
        // Проверяем, что экземпляры сразу готовы к работе без дополнительной настройки

        // when и then
        assertDoesNotThrow(() -> {
            TaskManager manager = Managers.getDefault();

            // Должен быть готов к использованию сразу
            int taskId = manager.createTask("Мгновенный тест", "Описание");
            int epicId = manager.createEpic("Мгновенный эпик", "Описание эпика");
            manager.createSubtask("Мгновенная подзадача", "Описание подзадачи", epicId);

            // Все операции должны работать без какой-либо настройки
            assertNotNull(manager.getTask(taskId));
            assertNotNull(manager.getEpic(epicId));
            assertFalse(manager.getEpicSubtasks(epicId).isEmpty());
            assertFalse(manager.getHistory().isEmpty());

        }, "TaskManager должен быть сразу работоспособен");

        assertDoesNotThrow(() -> {
            HistoryManager historyManager = Managers.getDefaultHistory();

            // Должен быть готов к использованию сразу
            main.ru.practicum.kanban.model.Task task = new main.ru.practicum.kanban.model.Task("Тест", "Описание");
            task.setId(1);
            historyManager.add(task);

            assertFalse(historyManager.getHistory().isEmpty());

        }, "HistoryManager должен быть сразу работоспособен");
    }
}
