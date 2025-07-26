package test.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.manager.HistoryManager;
import main.ru.practicum.kanban.manager.InMemoryHistoryManager;
import main.ru.practicum.kanban.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryHistoryManagerTest {

    private HistoryManager historyManager;

    @BeforeEach
    void setUp() {
        historyManager = new InMemoryHistoryManager();
    }

    /**
     * Проверяет, что история изначально пуста.
     */
    @Test
    void getHistory_shouldReturnEmptyListInitially() {
        // when
        List<Task> history = historyManager.getHistory();

        // then
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    /**
     * Проверяет, что задача добавляется в историю.
     */
    @Test
    void add_shouldAddTaskToHistory() {
        // given
        Task task = new Task("Тестовая задача", "Тестовое описание");
        task.setId(1);

        // when
        historyManager.add(task);

        // then
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size());
        assertEquals(task.getId(), history.getFirst().getId());
    }

    /**
     * Проверяет, что null-задача не добавляется в историю.
     */
    @Test
    void add_shouldNotAddNullTask() {
        // when
        historyManager.add(null);

        // then
        List<Task> history = historyManager.getHistory();
        assertTrue(history.isEmpty());
    }

    /**
     * Проверяет, что порядок добавления задач в историю сохраняется.
     */
    @Test
    void add_shouldMaintainOrderOfAddition() {
        // given
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setId(1);
        Task task2 = new Task("Task 2", "Description 2");
        task2.setId(2);
        Task task3 = new Task("Task 3", "Description 3");
        task3.setId(3);

        // when
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        // then
        List<Task> history = historyManager.getHistory();
        assertEquals(3, history.size());
        assertEquals(1, history.get(0).getId());
        assertEquals(2, history.get(1).getId());
        assertEquals(3, history.get(2).getId());
    }

    /**
     * Проверяет, что история ограничивается 10 последними задачами.
     */
    @Test
    void add_shouldLimitHistoryToTenItems() {
        // given
        for (int i = 1; i <= 15; i++) {
            Task task = new Task("Задача " + i, "Описание " + i);
            task.setId(i);
            historyManager.add(task);
        }

        // when
        List<Task> history = historyManager.getHistory();

        // then
        assertEquals(10, history.size());
        // Должны содержаться задачи 6-15 (последние 10)
        assertEquals(6, history.get(0).getId());
        assertEquals(15, history.get(9).getId());
    }

    /**
     * Проверяет, что история допускает дублирование одной и той же задачи.
     */
    @Test
    void add_shouldAllowDuplicateTasks() {
        // given
        Task task = new Task("Тестовая задача", "Тестовое описание");
        task.setId(1);

        // when
        historyManager.add(task);
        historyManager.add(task);
        historyManager.add(task);

        // then
        List<Task> history = historyManager.getHistory();
        assertEquals(3, history.size());
        assertTrue(history.stream().allMatch(t -> t.getId() == 1));
    }

    /**
     * Проверяет, что getHistory возвращает копию истории, а не сам внутренний
     * список.
     */
    @Test
    void getHistory_shouldReturnCopyOfHistory() {
        // given
        Task task = new Task("Тестовая задача", "Тестовое описание");
        task.setId(1);
        historyManager.add(task);

        // when
        List<Task> history1 = historyManager.getHistory();
        List<Task> history2 = historyManager.getHistory();

        // then
        assertNotSame(history1, history2);
        assertEquals(history1.size(), history2.size());
        assertEquals(history1.getFirst().getId(), history2.getFirst().getId());

        // Изменение возвращённого списка не должно влиять на внутреннюю историю
        history1.clear();
        List<Task> history3 = historyManager.getHistory();
        assertEquals(1, history3.size());
    }
}
