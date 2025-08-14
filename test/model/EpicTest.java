package model;

import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EpicTest {

    /**
     * Проверяет, что конструктор создаёт эпик с пустым списком подзадач.
     */
    @Test
    void constructor_shouldCreateEpicWithEmptySubtaskList() {
        // given
        String name = "Тестовый эпик";
        String description = "Тестовое описание";

        // when
        Epic epic = new Epic(name, description);

        // then
        assertEquals(name, epic.getName());
        assertEquals(description, epic.getDescription());
        assertEquals(TaskStatus.NEW, epic.getStatus());
        assertTrue(epic.getSubtaskIds().isEmpty());
    }

    /**
     * Проверяет, что getSubtaskIds возвращает копию списка, а не сам внутренний
     * список.
     */
    @Test
    void getSubtaskIds_shouldReturnCopyOfList() {
        // given
        Epic epic = new Epic("Эпик", "Описание");

        // when
        List<Integer> subtaskIds1 = epic.getSubtaskIds();
        List<Integer> subtaskIds2 = epic.getSubtaskIds();

        // then
        assertNotSame(subtaskIds1, subtaskIds2);

        // Изменение возвращённого списка не должно влиять на внутренний список
        subtaskIds1.add(1);
        assertEquals(0, epic.getSubtaskIds().size());
    }

    /**
     * Проверяет, что addSubtaskId корректно добавляет ID подзадачи в список.
     */
    @Test
    void addSubtaskId_shouldAddIdToList() {
        // given
        Epic epic = new Epic("Эпик", "Описание");

        // when
        epic.addSubtaskId(1);
        epic.addSubtaskId(2);

        // then
        List<Integer> subtaskIds = epic.getSubtaskIds();
        assertEquals(2, subtaskIds.size());
        assertTrue(subtaskIds.contains(1));
        assertTrue(subtaskIds.contains(2));
    }

    /**
     * Проверяет, что removeSubtaskId корректно удаляет ID подзадачи из списка.
     */
    @Test
    void removeSubtaskId_shouldRemoveIdFromList() {
        // given
        Epic epic = new Epic("Эпик", "Описание");
        epic.addSubtaskId(1);
        epic.addSubtaskId(2);
        epic.addSubtaskId(3);

        // when
        epic.removeSubtaskId(2);

        // then
        List<Integer> subtaskIds = epic.getSubtaskIds();
        assertEquals(2, subtaskIds.size());
        assertTrue(subtaskIds.contains(1));
        assertFalse(subtaskIds.contains(2));
        assertTrue(subtaskIds.contains(3));
    }

    /**
     * Проверяет, что попытка удалить несуществующий ID не влияет на список
     * подзадач.
     */
    @Test
    void removeSubtaskId_shouldHandleNonExistentId() {
        // given
        Epic epic = new Epic("Эпик", "Описание");
        epic.addSubtaskId(1);

        // when
        epic.removeSubtaskId(999);

        // then
        List<Integer> subtaskIds = epic.getSubtaskIds();
        assertEquals(1, subtaskIds.size());
        assertTrue(subtaskIds.contains(1));
    }

    /**
     * Проверяет, что метод toString возвращает строку с полями, специфичными для
     * эпика.
     */
    @Test
    void toString_shouldContainEpicSpecificFields() {
        // given
        Epic epic = new Epic("Тестовый эпик", "Тестовое описание");
        epic.setId(5);
        epic.addSubtaskId(1);
        epic.addSubtaskId(2);

        // when
        String result = epic.toString();

        // then
        assertTrue(result.contains("Epic"));
        assertTrue(result.contains("5"));
        assertTrue(result.contains("Тестовый эпик"));
        assertTrue(result.contains("Тестовое описание"));
        assertTrue(result.contains("subtaskIds"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
    }
}
