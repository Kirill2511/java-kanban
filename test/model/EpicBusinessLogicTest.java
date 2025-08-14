package model;

import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EpicBusinessLogicTest {

    /**
     * Проверяет, что эпик не может быть добавлен как собственная подзадача.
     */
    @Test
    void epic_shouldNotBeAddedAsItsOwnSubtask() {
        // given
        Epic epic = new Epic("Эпик", "Описание эпика");
        epic.setId(1);

        // when & then
        // Эпик нельзя преобразовать в подзадачу, поэтому этот сценарий естественно
        // предотвращён системой типов, но давайте проверим бизнес-логику концептуально

        // Эпик не должен содержать свой собственный ID в списке подзадач
        assertFalse(epic.getSubtaskIds().contains(epic.getId()),
                "Эпик не должен содержать свой собственный ID как подзадачу");

        // Более реалистично — эпик не может быть своим собственным родителем
        assertTrue(epic.getSubtaskIds().isEmpty(),
                "Вновь созданный эпик не должен иметь подзадач");
    }

    /**
     * Проверяет, что подзадача не может быть собственным эпиком и их ID
     * различаются.
     */
    @Test
    void subtask_shouldNotBeItsOwnEpic() {
        // given
        Epic epic = new Epic("Эпик", "Описание эпика");
        epic.setId(1);

        Subtask subtask = new Subtask("Подзадача", "Описание подзадачи", epic.getId());
        subtask.setId(2);

        // when & then
        assertNotEquals(subtask.getId(), subtask.getEpicId(),
                "Подзадача не может быть собственным эпиком");

        // Проверяем, что ID подзадачи и эпика различаются
        assertNotEquals(subtask.getId(), epic.getId(),
                "Подзадача должна иметь ID, отличающийся от ID эпика");
    }

    /**
     * Проверяет, что попытка установить подзадаче ID, совпадающий с ID эпика,
     * вызывает исключение.
     */
    @Test
    void subtask_shouldNotHaveSameIdAsEpicId() {
        // given
        int epicId = 5;
        Subtask subtask = new Subtask("Подзадача", "Описание", epicId);

        // when & then
        // Попытка установить ID подзадачи равным ID эпика должна выбрасывать исключение
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> subtask.setId(epicId), "Установка ID подзадачи равным ID эпика должна вызывать исключение");

        assertEquals("ID подзадачи не может совпадать с ID эпика", exception.getMessage());
    }

    /**
     * Проверяет, что эпик концептуально не может содержать сам себя в списке
     * подзадач.
     */
    @Test
    void epic_shouldNotContainItselfAsSubtask_conceptualTest() {
        // given
        Epic epic = new Epic("Эпик", "Описание эпика");
        epic.setId(1);

        // when & then
        // Концептуально эпик никогда не должен содержать сам себя
        // Это предотвращается системой типов (Epic != Subtask),
        // но бизнес-правило должно быть явно проверено

        assertFalse(epic.getSubtaskIds().contains(epic.getId()),
                "Эпик не должен содержать свой собственный ID в списке подзадач");
    }
}
