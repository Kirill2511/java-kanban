package manager;

import main.ru.practicum.kanban.manager.InMemoryTaskManager;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EpicStatusCalculationTest {

    private TaskManager manager;

    @BeforeEach
    void setUp() {
        manager = new InMemoryTaskManager();
    }

    @Test
    void testEpicStatusWithEmptySubtaskList() {
        int epicId = manager.createEpic("Epic", "Description");

        Optional<Epic> epicOpt = manager.getEpic(epicId);
        assertTrue(epicOpt.isPresent());
        Epic epic = epicOpt.get();

        assertEquals(TaskStatus.NEW, epic.getStatus(),
                "Эпик без подзадач должен иметь статус NEW");
    }

    @Test
    void testEpicStatusWithAllNewSubtasks() {
        int epicId = manager.createEpic("Epic", "Description");

        manager.createSubtask("Subtask1", "Description1", epicId);
        manager.createSubtask("Subtask2", "Description2", epicId);
        manager.createSubtask("Subtask3", "Description3", epicId);

        Optional<Epic> epicOpt = manager.getEpic(epicId);
        assertTrue(epicOpt.isPresent());
        Epic epic = epicOpt.get();

        assertEquals(TaskStatus.NEW, epic.getStatus(),
                "Эпик с подзадачами только в статусе NEW должен иметь статус NEW");
    }

    @Test
    void testEpicStatusWithAllDoneSubtasks() {
        int epicId = manager.createEpic("Epic", "Description");

        manager.createSubtask("Subtask1", "Description1", epicId);
        manager.createSubtask("Subtask2", "Description2", epicId);
        manager.createSubtask("Subtask3", "Description3", epicId);

        var subtasks = manager.getEpicSubtasks(epicId);
        for (Subtask subtask : subtasks) {
            Subtask updatedSubtask = new Subtask(subtask.getId(), subtask.getName(),
                    subtask.getDescription(), TaskStatus.DONE, subtask.getEpicId());
            manager.updateSubtask(updatedSubtask);
        }

        Optional<Epic> epicOpt = manager.getEpic(epicId);
        assertTrue(epicOpt.isPresent());
        Epic epic = epicOpt.get();

        assertEquals(TaskStatus.DONE, epic.getStatus(),
                "Эпик с подзадачами только в статусе DONE должен иметь статус DONE");
    }

    @Test
    void testEpicStatusWithNewAndDoneSubtasks() {
        int epicId = manager.createEpic("Epic", "Description");

        manager.createSubtask("Subtask1", "Description1", epicId);
        manager.createSubtask("Subtask2", "Description2", epicId);
        manager.createSubtask("Subtask3", "Description3", epicId);

        var subtasks = manager.getEpicSubtasks(epicId);
        for (int i = 0; i < subtasks.size(); i++) {
            Subtask subtask = subtasks.get(i);
            if (i < 2) { // Первые две переводим в DONE
                Subtask updatedSubtask = new Subtask(subtask.getId(), subtask.getName(),
                        subtask.getDescription(), TaskStatus.DONE, subtask.getEpicId());
                manager.updateSubtask(updatedSubtask);
            }
        }

        Optional<Epic> epicOpt = manager.getEpic(epicId);
        assertTrue(epicOpt.isPresent());
        Epic epic = epicOpt.get();

        assertEquals(TaskStatus.IN_PROGRESS, epic.getStatus(),
                "Эпик с подзадачами в статусах NEW и DONE должен иметь статус IN_PROGRESS");
    }

    @Test
    void testEpicStatusWithInProgressSubtasks() {
        int epicId = manager.createEpic("Epic", "Description");

        manager.createSubtask("Subtask1", "Description1", epicId);
        manager.createSubtask("Subtask2", "Description2", epicId);
        manager.createSubtask("Subtask3", "Description3", epicId);

        var subtasks = manager.getEpicSubtasks(epicId);
        Subtask firstSubtask = subtasks.getFirst();
        Subtask updatedSubtask = new Subtask(firstSubtask.getId(), firstSubtask.getName(),
                firstSubtask.getDescription(), TaskStatus.IN_PROGRESS, firstSubtask.getEpicId());
        manager.updateSubtask(updatedSubtask);

        Optional<Epic> epicOpt = manager.getEpic(epicId);
        assertTrue(epicOpt.isPresent());
        Epic epic = epicOpt.get();

        assertEquals(TaskStatus.IN_PROGRESS, epic.getStatus(),
                "Эпик с хотя бы одной подзадачей в статусе IN_PROGRESS должен иметь статус IN_PROGRESS");
    }

    @Test
    void testEpicStatusWithMixedInProgressSubtasks() {
        int epicId = manager.createEpic("Epic", "Description");

        manager.createSubtask("Subtask1", "Description1", epicId);
        manager.createSubtask("Subtask2", "Description2", epicId);
        manager.createSubtask("Subtask3", "Description3", epicId);

        var subtasks = manager.getEpicSubtasks(epicId);

        Subtask secondSubtask = subtasks.get(1);
        Subtask inProgressSubtask = new Subtask(secondSubtask.getId(), secondSubtask.getName(),
                secondSubtask.getDescription(), TaskStatus.IN_PROGRESS, secondSubtask.getEpicId());
        manager.updateSubtask(inProgressSubtask);

        Subtask thirdSubtask = subtasks.get(2);
        Subtask doneSubtask = new Subtask(thirdSubtask.getId(), thirdSubtask.getName(),
                thirdSubtask.getDescription(), TaskStatus.DONE, thirdSubtask.getEpicId());
        manager.updateSubtask(doneSubtask);

        Optional<Epic> epicOpt = manager.getEpic(epicId);
        assertTrue(epicOpt.isPresent());
        Epic epic = epicOpt.get();

        assertEquals(TaskStatus.IN_PROGRESS, epic.getStatus(),
                "Эпик с подзадачами в разных статусах (включая IN_PROGRESS) должен иметь статус IN_PROGRESS");
    }

    @Test
    void testEpicStatusAfterSubtaskDeletion() {
        int epicId = manager.createEpic("Epic", "Description");

        manager.createSubtask("Subtask1", "Description1", epicId);
        manager.createSubtask("Subtask2", "Description2", epicId);

        var subtasks = manager.getEpicSubtasks(epicId);
        Subtask firstSubtask = subtasks.getFirst();
        Subtask doneSubtask = new Subtask(firstSubtask.getId(), firstSubtask.getName(),
                firstSubtask.getDescription(), TaskStatus.DONE, firstSubtask.getEpicId());
        manager.updateSubtask(doneSubtask);

        Optional<Epic> epicOpt = manager.getEpic(epicId);
        assertTrue(epicOpt.isPresent());
        assertEquals(TaskStatus.IN_PROGRESS, epicOpt.get().getStatus());

        Subtask secondSubtask = subtasks.get(1);
        manager.deleteSubtask(secondSubtask.getId());

        epicOpt = manager.getEpic(epicId);
        assertTrue(epicOpt.isPresent());
        assertEquals(TaskStatus.DONE, epicOpt.get().getStatus(),
                "После удаления подзадачи NEW эпик должен иметь статус DONE");
    }

    @Test
    void testEpicStatusAfterAllSubtasksDeletion() {
        int epicId = manager.createEpic("Epic", "Description");

        manager.createSubtask("Subtask1", "Description1", epicId);
        manager.createSubtask("Subtask2", "Description2", epicId);

        var subtasks = manager.getEpicSubtasks(epicId);
        for (Subtask subtask : subtasks) {
            manager.deleteSubtask(subtask.getId());
        }

        Optional<Epic> epicOpt = manager.getEpic(epicId);
        assertTrue(epicOpt.isPresent());
        assertEquals(TaskStatus.NEW, epicOpt.get().getStatus(),
                "После удаления всех подзадач эпик должен иметь статус NEW");
    }
}