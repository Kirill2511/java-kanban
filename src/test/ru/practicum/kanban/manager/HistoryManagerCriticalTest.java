package test.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.manager.HistoryManager;
import main.ru.practicum.kanban.manager.InMemoryHistoryManager;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class HistoryManagerCriticalTest {

    private HistoryManager historyManager;

    @BeforeEach
    void setUp() {
        historyManager = new InMemoryHistoryManager();
    }

    /**
     * Проверяет, что история сохраняет предыдущую версию задачи при изменении
     * исходного объекта.
     */
    @Test
    void historyManager_shouldPreservePreviousVersionOfTask() {
        // given
        Task task = new Task("Оригинальное имя", "Оригинальное описание");
        task.setId(1);
        task.setStatus(TaskStatus.NEW);

        // when
        historyManager.add(task);

        // изменяем задачу после добавления в историю
        task.setName("Измененное имя");
        task.setDescription("Измененное описание");
        task.setStatus(TaskStatus.DONE);

        // then
        var history = historyManager.getHistory();
        assertEquals(1, history.size());

        Task historicalTask = history.getFirst();
        assertEquals("Оригинальное имя", historicalTask.getName(),
                "История должна сохранить оригинальное имя");
        assertEquals("Оригинальное описание", historicalTask.getDescription(),
                "История должна сохранить оригинальное описание");
        assertEquals(TaskStatus.NEW, historicalTask.getStatus(),
                "История должна сохранить оригинальный статус");
        assertEquals(1, historicalTask.getId(),
                "История должна сохранить оригинальный ID");
    }

    /**
     * Проверяет, что история хранит все версии одной и той же задачи.
     */
    @Test
    void historyManager_shouldStoreMultipleVersionsOfSameTask() {
        // given
        Task task = new Task("Версия 1", "Описание 1");
        task.setId(1);

        // when
        historyManager.add(task);

        // изменяем и добавляем снова
        task.setName("Версия 2");
        task.setDescription("Описание 2");
        task.setStatus(TaskStatus.IN_PROGRESS);
        historyManager.add(task);

        // изменяем и добавляем в третий раз
        task.setName("Версия 3");
        task.setStatus(TaskStatus.DONE);
        historyManager.add(task);

        // then
        var history = historyManager.getHistory();
        assertEquals(3, history.size(), "История должна содержать все версии");

        // проверяем, что каждая версия сохранена правильно
        Task firstVersion = history.get(0);
        Task secondVersion = history.get(1);
        Task thirdVersion = history.get(2);

        assertEquals("Версия 1", firstVersion.getName());
        assertEquals("Версия 2", secondVersion.getName());
        assertEquals("Версия 3", thirdVersion.getName());

        assertEquals(TaskStatus.NEW, firstVersion.getStatus());
        assertEquals(TaskStatus.IN_PROGRESS, secondVersion.getStatus());
        assertEquals(TaskStatus.DONE, thirdVersion.getStatus());
    }

    /**
     * Проверяет, что история не изменяется при внешних изменениях задачи после
     * добавления.
     */
    @Test
    void historyManager_shouldNotBeAffectedByExternalTaskModifications() {
        // даны задачи
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание 2");
        task2.setId(2);

        // when
        historyManager.add(task1);
        historyManager.add(task2);

        // внешние изменения
        task1.setName("Измененная задача 1");
        task2.setDescription("Измененное описание 2");

        // then
        var history = historyManager.getHistory();
        assertEquals("Задача 1", history.get(0).getName(),
                "История не должна быть затронута внешними изменениями задачи 1");
        assertEquals("Описание 2", history.get(1).getDescription(),
                "История не должна быть затронута внешними изменениями задачи 2");
    }

    /**
     * Проверяет, что история не превышает установленный лимит.
     */
    @Test
    void historyManager_shouldMaintainHistoryLimit() {
        // дан лимит истории
        final int HISTORY_LIMIT = 10;

        // when
        for (int i = 1; i <= 15; i++) {
            Task task = new Task("Задача " + i, "Описание " + i);
            task.setId(i);
            historyManager.add(task);
        }

        // then
        var history = historyManager.getHistory();
        assertEquals(HISTORY_LIMIT, history.size(),
                "История не должна превышать лимит");

        // должна содержать последние 10 задач (6-15)
        assertEquals("Задача 6", history.get(0).getName(),
                "История должна содержать задачу 6 как самую старую после очистки");
        assertEquals("Задача 15", history.get(9).getName(),
                "История должна содержать задачу 15 как самую новую");
    }

    /**
     * Проверяет, что история корректно хранит задачи с одинаковыми ID как отдельные
     * экземпляры.
     */
    @Test
    void historyManager_shouldHandleIdenticalTasksCorrectly() {
        // даны одинаковые задачи
        Task task1 = new Task("Одинаковая задача", "Одинаковое описание");
        task1.setId(1);
        Task task2 = new Task("Одинаковая задача", "Одинаковое описание");
        task2.setId(1); // тот же ID

        // when
        historyManager.add(task1);
        historyManager.add(task2);

        // then
        var history = historyManager.getHistory();
        assertEquals(2, history.size(),
                "История должна хранить оба экземпляра, даже если у них одинаковый ID");

        // проверяем, что это отдельные экземпляры
        assertNotSame(history.get(0), history.get(1),
                "История должна хранить отдельные экземпляры");
    }
}
