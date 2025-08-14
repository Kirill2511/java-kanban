package manager;

import main.ru.practicum.kanban.manager.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ManagersTest {

    /**
     * Проверяет, что Managers.getDefault() возвращает экземпляр
     * InMemoryTaskManager.
     */
    @Test
    void getDefault_shouldReturnInMemoryTaskManagerInstance() {
        // when
        TaskManager taskManager = Managers.getDefault();

        // then
        assertNotNull(taskManager);
        assertInstanceOf(InMemoryTaskManager.class, taskManager);
    }

    /**
     * Проверяет, что Managers.getDefault() возвращает новый экземпляр при каждом
     * вызове.
     */
    @Test
    void getDefault_shouldReturnNewInstanceEachTime() {
        // when
        TaskManager taskManager1 = Managers.getDefault();
        TaskManager taskManager2 = Managers.getDefault();

        // then
        assertNotSame(taskManager1, taskManager2);
    }

    /**
     * Проверяет, что Managers.getDefaultHistory() возвращает экземпляр
     * InMemoryHistoryManager.
     */
    @Test
    void getDefaultHistory_shouldReturnInMemoryHistoryManagerInstance() {
        // when
        HistoryManager historyManager = Managers.getDefaultHistory();

        // then
        assertNotNull(historyManager);
        assertInstanceOf(InMemoryHistoryManager.class, historyManager);
    }

    /**
     * Проверяет, что Managers.getDefaultHistory() возвращает новый экземпляр при
     * каждом вызове.
     */
    @Test
    void getDefaultHistory_shouldReturnNewInstanceEachTime() {
        // when
        HistoryManager historyManager1 = Managers.getDefaultHistory();
        HistoryManager historyManager2 = Managers.getDefaultHistory();

        // then
        assertNotSame(historyManager1, historyManager2);
    }
}
