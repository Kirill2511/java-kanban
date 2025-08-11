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
     * Проверяет, что история хранит все уникальные задачи без ограничений.
     * В новой реализации нет лимита на количество элементов.
     */
    @Test
    void add_shouldStoreAllUniqueTasks() {
        // given
        for (int i = 1; i <= 15; i++) {
            Task task = new Task("Задача " + i, "Описание " + i);
            task.setId(i);
            historyManager.add(task);
        }

        // when
        List<Task> history = historyManager.getHistory();

        // then
        assertEquals(15, history.size(), "История должна содержать все уникальные задачи");
        // Проверяем порядок - первая и последняя задачи
        assertEquals(1, history.getFirst().getId(), "Первая задача должна иметь ID 1");
        assertEquals(15, history.get(14).getId(), "Последняя задача должна иметь ID 15");
    }

    /**
     * Проверяет, что история заменяет задачи с одинаковым ID.
     * В новой реализации дубликаты по ID не допускаются.
     */
    @Test
    void add_shouldReplaceDuplicateTasksById() {
        // given
        Task task = new Task("Оригинальная задача", "Оригинальное описание");
        task.setId(1);
        historyManager.add(task);

        // when - изменяем и добавляем ту же задачу
        task.setName("Измененная задача");
        historyManager.add(task);

        // then
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size(), "История должна содержать только одну задачу с уникальным ID");
        assertEquals("Измененная задача", history.getFirst().getName(),
                "Должна сохраниться последняя версия задачи");
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

    /**
     * Проверяет функциональность удаления задач из истории.
     */
    @Test
    void remove_shouldRemoveTaskFromHistory() {
        // given
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание 2");
        task2.setId(2);
        Task task3 = new Task("Задача 3", "Описание 3");
        task3.setId(3);

        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        // when
        historyManager.remove(2);

        // then
        List<Task> history = historyManager.getHistory();
        assertEquals(2, history.size());
        assertFalse(history.stream().anyMatch(task -> task.getId() == 2));
        assertTrue(history.stream().anyMatch(task -> task.getId() == 1));
        assertTrue(history.stream().anyMatch(task -> task.getId() == 3));
    }

    /**
     * Проверяет комплексную функциональность: добавление, порядок, замену
     * дубликатов.
     */
    @Test
    void add_shouldHandleComplexScenarios() {
        // given
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание 2");
        task2.setId(2);
        Task task3 = new Task("Задача 3", "Описание 3");
        task3.setId(3);

        // when - добавляем задачи
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        // Изменяем задачу 1 и добавляем снова (должна заменить и переместиться в конец)
        task1.setName("Измененная задача 1");
        historyManager.add(task1);

        // then
        List<Task> history = historyManager.getHistory();
        assertEquals(3, history.size(), "История должна содержать 3 уникальные задачи");

        // Проверяем порядок: task2, task3, task1 (task1 переместилась в конец)
        assertEquals(2, history.get(0).getId());
        assertEquals(3, history.get(1).getId());
        assertEquals(1, history.get(2).getId());

        // Проверяем, что сохранена измененная версия task1
        assertEquals("Измененная задача 1", history.get(2).getName());
    }
}
