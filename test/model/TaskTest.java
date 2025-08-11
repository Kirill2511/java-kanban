package test.ru.practicum.kanban.model;

import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.model.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    /**
     * Проверяет, что конструктор задачи корректно инициализирует все поля.
     */
    @Test
    void constructor_shouldCreateTaskWithCorrectFields() {
        // given
        String name = "Тестовая задача";
        String description = "Тестовое описание";

        // when
        Task task = new Task(name, description);

        // then
        assertEquals(name, task.getName());
        assertEquals(description, task.getDescription());
        assertEquals(TaskStatus.NEW, task.getStatus());
        assertEquals(0, task.getId()); // ID по умолчанию
    }

    /**
     * Проверяет, что конструктор выбрасывает исключение при null в имени.
     */
    @Test
    void constructor_shouldThrowExceptionForNullName() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> new Task(null, "Описание"));
    }

    /**
     * Проверяет, что конструктор выбрасывает исключение при пустом имени.
     */
    @Test
    void constructor_shouldThrowExceptionForEmptyName() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> new Task("", "Описание"));
    }

    /**
     * Проверяет, что конструктор выбрасывает исключение, если имя состоит только из
     * пробелов.
     */
    @Test
    void constructor_shouldThrowExceptionForWhitespaceOnlyName() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> new Task("   ", "Описание"));
    }

    /**
     * Проверяет, что конструктор выбрасывает исключение при null в описании.
     */
    @Test
    void constructor_shouldThrowExceptionForNullDescription() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> new Task("Имя", null));
    }

    /**
     * Проверяет, что конструктор с параметрами создаёт задачу с заданными
     * значениями.
     */
    @Test
    void constructorWithAllFields_shouldCreateTaskWithSpecifiedValues() {
        // given
        int id = 5;
        String name = "Тестовая задача";
        String description = "Тестовое описание";
        TaskStatus status = TaskStatus.IN_PROGRESS;

        // when
        Task task = new Task(id, name, description, status);

        // then
        assertEquals(id, task.getId());
        assertEquals(name, task.getName());
        assertEquals(description, task.getDescription());
        assertEquals(status, task.getStatus());
    }

    /**
     * Проверяет, что setId корректно обновляет ID задачи.
     */
    @Test
    void setId_shouldUpdateId() {
        // given
        Task task = new Task("Тест", "Описание");
        int newId = 10;

        // when
        task.setId(newId);

        // then
        assertEquals(newId, task.getId());
    }

    /**
     * Проверяет, что setName корректно обновляет имя задачи.
     */
    @Test
    void setName_shouldUpdateName() {
        // given
        Task task = new Task("Оригинал", "Описание");
        String newName = "Обновленное имя";

        // when
        task.setName(newName);

        // then
        assertEquals(newName, task.getName());
    }

    /**
     * Проверяет, что setDescription корректно обновляет описание задачи.
     */
    @Test
    void setDescription_shouldUpdateDescription() {
        // given
        Task task = new Task("Имя", "Оригинальное описание");
        String newDescription = "Обновленное описание";

        // when
        task.setDescription(newDescription);

        // then
        assertEquals(newDescription, task.getDescription());
    }

    /**
     * Проверяет, что setStatus корректно обновляет статус задачи.
     */
    @Test
    void setStatus_shouldUpdateStatus() {
        // given
        Task task = new Task("Имя", "Описание");
        TaskStatus newStatus = TaskStatus.DONE;

        // when
        task.setStatus(newStatus);

        // then
        assertEquals(newStatus, task.getStatus());
    }

    /**
     * Проверяет, что задачи с одинаковым ID считаются равными.
     */
    @Test
    void equals_shouldReturnTrueForSameId() {
        // given
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание 2");
        task2.setId(1);

        // when и then
        assertEquals(task1, task2);
    }

    /**
     * Проверяет, что задачи с разными ID не равны.
     */
    @Test
    void equals_shouldReturnFalseForDifferentId() {
        // given
        Task task1 = new Task("Задача", "Описание");
        task1.setId(1);
        Task task2 = new Task("Задача", "Описание");
        task2.setId(2);

        // when и then
        assertNotEquals(task1, task2);
    }

    /**
     * Проверяет, что hashCode совпадает для задач с одинаковым ID.
     */
    @Test
    void hashCode_shouldBeSameForSameId() {
        // given
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание 2");
        task2.setId(1);

        // when и then
        assertEquals(task1.hashCode(), task2.hashCode());
    }

    /**
     * Проверяет, что метод toString возвращает строку со всеми полями задачи.
     */
    @Test
    void toString_shouldContainAllFields() {
        // given
        Task task = new Task("Тестовая задача", "Тестовое описание");
        task.setId(5);
        task.setStatus(TaskStatus.IN_PROGRESS);

        // when
        String result = task.toString();

        // then
        assertTrue(result.contains("5"));
        assertTrue(result.contains("Тестовая задача"));
        assertTrue(result.contains("Тестовое описание"));
        assertTrue(result.contains("IN_PROGRESS"));
    }

    /**
     * Проверяет, что Task и Epic с одинаковым ID считаются равными.
     */
    @Test
    void equals_shouldReturnTrueForTaskAndEpicWithSameId() {
        // given
        Task task = new Task("Задача", "Описание");
        task.setId(1);
        Epic epic = new Epic("Эпик", "Описание эпика");
        epic.setId(1);

        // when и then
        assertEquals(task, epic, "Задача и Эпик с одинаковым ID должны быть равны");
        assertEquals(epic, task, "Эпик и Задача с одинаковым ID должны быть равны");
    }

    /**
     * Проверяет, что Task и Subtask с одинаковым ID считаются равными.
     */
    @Test
    void equals_shouldReturnTrueForTaskAndSubtaskWithSameId() {
        // given
        Task task = new Task("Задача", "Описание");
        task.setId(1);
        Subtask subtask = new Subtask("Подзадача", "Описание подзадачи", 2);
        subtask.setId(1);

        // when и then
        assertEquals(task, subtask, "Задача и Подзадача с одинаковым ID должны быть равны");
        assertEquals(subtask, task, "Подзадача и Задача с одинаковым ID должны быть равны");
    }

    /**
     * Проверяет, что Epic и Subtask с одинаковым ID считаются равными.
     */
    @Test
    void equals_shouldReturnTrueForEpicAndSubtaskWithSameId() {
        // given
        Epic epic = new Epic("Эпик", "Описание эпика");
        epic.setId(1);
        Subtask subtask = new Subtask("Подзадача", "Описание подзадачи", 2);
        subtask.setId(1);

        // when и then
        assertEquals(epic, subtask, "Эпик и Подзадача с одинаковым ID должны быть равны");
        assertEquals(subtask, epic, "Подзадача и Эпик с одинаковым ID должны быть равны");
    }
}
