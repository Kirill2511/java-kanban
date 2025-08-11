package test.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.manager.HistoryManager;
import main.ru.practicum.kanban.manager.InMemoryHistoryManager;
import main.ru.practicum.kanban.manager.InMemoryTaskManager;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для удаления из истории при операциях с TaskManager.
 * Объединяет тесты удаления задач из истории и интеграции с TaskManager.
 */
class HistoryRemoveTest {

    private HistoryManager historyManager;
    private TaskManager taskManager;

    @BeforeEach
    void setUp() {
        historyManager = new InMemoryHistoryManager();
        taskManager = new InMemoryTaskManager();
    }

    /**
     * Проверяет прямое удаление задач из HistoryManager.
     */
    @Test
    void testHistoryManagerRemove() {
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
        assertEquals(2, history.size(), "История должна содержать 2 задачи после удаления");

        // Проверяем, что задача с id = 2 удалена
        boolean found = history.stream().anyMatch(task -> task.getId() == 2);
        assertFalse(found, "Задача с ID 2 не должна быть в истории после удаления");

        // Проверяем, что остальные задачи остались
        assertTrue(history.stream().anyMatch(task -> task.getId() == 1),
                "Задача с ID 1 должна остаться в истории");
        assertTrue(history.stream().anyMatch(task -> task.getId() == 3),
                "Задача с ID 3 должна остаться в истории");
    }

    /**
     * Тестирует автоматическое удаление задач из истории при удалении из
     * TaskManager
     */
    @Test
    void testTaskDeletionRemovesFromHistory() {
        // given
        int taskId1 = taskManager.createTask("Задача 1", "Описание 1");
        int taskId2 = taskManager.createTask("Задача 2", "Описание 2");
        int epicId = taskManager.createEpic("Эпик 1", "Описание эпика");
        taskManager.createSubtask("Подзадача 1", "Описание подзадачи", epicId);

        // Просматриваем задачи (добавляем в историю)
        taskManager.getTask(taskId1);
        taskManager.getTask(taskId2);
        taskManager.getEpic(epicId);

        // Получаем подзадачу для добавления в историю
        List<Subtask> subtasks = taskManager.getAllSubtasks();
        if (!subtasks.isEmpty()) {
            taskManager.getSubtask(subtasks.getFirst().getId());
        }

        List<Task> historyBefore = taskManager.getHistory();
        assertTrue(historyBefore.size() >= 3, "История должна содержать минимум 3 элемента до удаления");

        // when - удаляем задачу
        taskManager.deleteTask(taskId1);

        // then
        List<Task> historyAfterTask = taskManager.getHistory();
        assertEquals(historyBefore.size() - 1, historyAfterTask.size(),
                "История должна содержать на 1 элемент меньше после удаления задачи");

        // Проверяем, что удаленная задача не в истории
        boolean taskFound = historyAfterTask.stream().anyMatch(task -> task.getId() == taskId1);
        assertFalse(taskFound, "Удаленная задача не должна быть в истории");
    }

    /**
     * Тестирует удаление эпика и автоматическое удаление его подзадач из истории
     */
    @Test
    void testEpicDeletionRemovesSubtasksFromHistory() {
        // given
        int epicId = taskManager.createEpic("Эпик 1", "Описание эпика");
        taskManager.createSubtask("Подзадача 1", "Описание 1", epicId);
        taskManager.createSubtask("Подзадача 2", "Описание 2", epicId);

        // Просматриваем все (добавляем в историю)
        taskManager.getEpic(epicId);

        // Получаем подзадачи для добавления в историю
        List<Subtask> subtasks = taskManager.getAllSubtasks();
        for (Subtask subtask : subtasks) {
            taskManager.getSubtask(subtask.getId());
        }

        List<Task> historyBefore = taskManager.getHistory();
        assertFalse(historyBefore.isEmpty(), "История должна содержать минимум 1 элемент до удаления");

        // when - удаляем эпик
        taskManager.deleteEpic(epicId);

        // then
        List<Task> historyAfter = taskManager.getHistory();

        // Проверяем, что эпик и все его подзадачи удалены из истории
        boolean epicFound = historyAfter.stream().anyMatch(task -> task.getId() == epicId);
        assertFalse(epicFound, "Эпик не должен быть в истории после удаления");

        // Проверяем, что подзадачи тоже удалены
        for (Subtask subtask : subtasks) {
            boolean subtaskFound = historyAfter.stream().anyMatch(task -> task.getId() == subtask.getId());
            assertFalse(subtaskFound,
                    "Подзадача " + subtask.getId() + " не должна быть в истории после удаления эпика");
        }
    }

    /**
     * Тестирует удаление несуществующего элемента из истории
     */
    @Test
    void testRemoveNonExistentTask() {
        // given
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setId(1);
        historyManager.add(task1);

        // when - удаляем несуществующий ID
        historyManager.remove(999);

        // then - история не должна измениться
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size(), "История не должна измениться при удалении несуществующего ID");
        assertEquals(1, history.getFirst().getId(), "Задача с ID 1 должна остаться в истории");
    }
}
