package model;

import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SubtaskTest {

    /**
     * Проверяет, что конструктор подзадачи корректно инициализирует все поля.
     */
    @Test
    void constructor_shouldCreateSubtaskWithCorrectFields() {
        // given
        String name = "Тестовая подзадача";
        String description = "Тестовое описание";
        int epicId = 5;

        // when
        Subtask subtask = new Subtask(name, description, epicId);

        // then
        assertEquals(name, subtask.getName());
        assertEquals(description, subtask.getDescription());
        assertEquals(TaskStatus.NEW, subtask.getStatus());
        assertEquals(epicId, subtask.getEpicId());
    }

    /**
     * Проверяет, что конструктор с параметрами создаёт подзадачу с заданными
     * значениями.
     */
    @Test
    void constructorWithAllFields_shouldCreateSubtaskWithSpecifiedValues() {
        // given
        int id = 10;
        String name = "Тестовая подзадача";
        String description = "Тестовое описание";
        TaskStatus status = TaskStatus.DONE;
        int epicId = 5;

        // when
        Subtask subtask = new Subtask(id, name, description, status, epicId);

        // then
        assertEquals(id, subtask.getId());
        assertEquals(name, subtask.getName());
        assertEquals(description, subtask.getDescription());
        assertEquals(status, subtask.getStatus());
        assertEquals(epicId, subtask.getEpicId());
    }

    /**
     * Проверяет, что getEpicId возвращает корректный ID эпика.
     */
    @Test
    void getEpicId_shouldReturnEpicId() {
        // given
        int epicId = 42;
        Subtask subtask = new Subtask("Подзадача", "Описание", epicId);

        // when
        int result = subtask.getEpicId();

        // then
        assertEquals(epicId, result);
    }

    /**
     * Проверяет, что метод toString возвращает строку с полями, специфичными для
     * подзадачи.
     */
    @Test
    void toString_shouldContainSubtaskSpecificFields() {
        // given
        Subtask subtask = new Subtask("Тестовая подзадача", "Тестовое описание", 5);
        subtask.setId(10);
        subtask.setStatus(TaskStatus.IN_PROGRESS);

        // when
        String result = subtask.toString();

        // then
        assertTrue(result.contains("Subtask"));
        assertTrue(result.contains("10"));
        assertTrue(result.contains("Тестовая подзадача"));
        assertTrue(result.contains("Тестовое описание"));
        assertTrue(result.contains("IN_PROGRESS"));
        assertTrue(result.contains("epicId"));
        assertTrue(result.contains("5"));
    }

    /**
     * Проверяет, что Subtask наследуется от Task.
     */
    @Test
    void inheritance_shouldInheritFromTask() {
        // given
        Subtask subtask = new Subtask("Подзадача", "Описание", 1);

        // when и then
        assertInstanceOf(main.ru.practicum.kanban.model.Task.class, subtask);
    }
}
