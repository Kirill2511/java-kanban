package test.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.manager.HistoryManager;
import main.ru.practicum.kanban.manager.InMemoryHistoryManager;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
     * Проверяет, что история корректно обрабатывает много уникальных задач.
     * В новой реализации нет жесткого лимита, но проверяем базовую
     * функциональность.
     */
    @Test
    void historyManager_shouldMaintainHistoryLimit() {
        // when - добавляем много уникальных задач
        for (int i = 1; i <= 15; i++) {
            Task task = new Task("Задача " + i, "Описание " + i);
            task.setId(i);
            historyManager.add(task);
        }

        // then
        var history = historyManager.getHistory();
        assertEquals(15, history.size(), "История должна содержать все уникальные задачи");

        // должна содержать задачи в порядке добавления
        assertEquals("Задача 1", history.getFirst().getName());
        assertEquals("Задача 15", history.get(14).getName());
    }

    /**
     * Проверяет, что история корректно обрабатывает задачи с одинаковыми ID.
     * В новой реализации задачи с одинаковым ID заменяют друг друга.
     */
    @Test
    void historyManager_shouldHandleIdenticalTasksCorrectly() {
        // даны задачи с одинаковым ID но разными данными
        Task task1 = new Task("Первая версия", "Первое описание");
        task1.setId(1);
        task1.setStatus(TaskStatus.NEW);

        Task task2 = new Task("Вторая версия", "Второе описание");
        task2.setId(1); // тот же ID
        task2.setStatus(TaskStatus.DONE);

        // when
        historyManager.add(task1);
        historyManager.add(task2);

        // then
        var history = historyManager.getHistory();
        assertEquals(1, history.size(),
                "История должна содержать только одну задачу с уникальным ID");

        // проверяем, что сохранена последняя версия
        Task savedTask = history.getFirst();
        assertEquals("Вторая версия", savedTask.getName());
        assertEquals("Второе описание", savedTask.getDescription());
        assertEquals(TaskStatus.DONE, savedTask.getStatus());
        assertEquals(1, savedTask.getId());
    }

    /**
     * Проверяет, что при повторном добавлении задача перемещается в конец истории.
     */
    @Test
    void historyManager_shouldMoveTaskToEndOnReAdd() {
        // given
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание 2");
        task2.setId(2);
        Task task3 = new Task("Задача 3", "Описание 3");
        task3.setId(3);

        // when
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        // Повторно добавляем первую задачу
        historyManager.add(task1);

        // then
        var history = historyManager.getHistory();
        assertEquals(3, history.size(), "Размер истории не должен измениться");

        // Проверяем новый порядок: task2, task3, task1
        assertEquals(2, history.get(0).getId(), "Задача 2 должна быть первой");
        assertEquals(3, history.get(1).getId(), "Задача 3 должна быть второй");
        assertEquals(1, history.get(2).getId(), "Задача 1 должна переместиться в конец");
    }
}
