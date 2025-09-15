package manager;

import main.ru.practicum.kanban.manager.FileBackedTaskManager;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.model.Subtask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PrioritizedTasksTest {

    private FileBackedTaskManager manager;
    private File tempFile;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("priority-test", ".csv").toFile();
        manager = new FileBackedTaskManager(tempFile);
    }

    @AfterEach
    void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    void getPrioritizedTasks_shouldReturnTasksSortedByStartTime() {
        // given
        // Задача без времени начала (не должна попасть в приоритизированный список)
        int task1Id = manager.createTask("Задача без времени", "Описание 1");

        // Задача с временем 15:00
        int task2Id = manager.createTask("Задача на 15:00", "Описание 2");
        var task2Opt = manager.getTask(task2Id);
        assertTrue(task2Opt.isPresent());
        Task task2 = task2Opt.get();
        Task updatedTask2 = new Task(task2.getId(), task2.getName(), task2.getDescription(), task2.getStatus(),
                                   Duration.ofHours(1), LocalDateTime.of(2024, 1, 15, 15, 0));
        manager.updateTask(updatedTask2);

        // Задача с временем 10:00 (должна быть первой)
        int task3Id = manager.createTask("Задача на 10:00", "Описание 3");
        var task3Opt = manager.getTask(task3Id);
        assertTrue(task3Opt.isPresent());
        Task task3 = task3Opt.get();
        Task updatedTask3 = new Task(task3.getId(), task3.getName(), task3.getDescription(), task3.getStatus(),
                                   Duration.ofMinutes(30), LocalDateTime.of(2024, 1, 15, 10, 0));
        manager.updateTask(updatedTask3);

        // when
        List<Task> prioritizedTasks = manager.getPrioritizedTasks();

        // then
        assertEquals(2, prioritizedTasks.size(), "Должно быть 2 задачи с временем начала");
        assertEquals("Задача на 10:00", prioritizedTasks.get(0).getName(), 
                    "Первой должна быть задача на 10:00");
        assertEquals("Задача на 15:00", prioritizedTasks.get(1).getName(), 
                    "Второй должна быть задача на 15:00");
    }

    @Test
    void getPrioritizedTasks_shouldIncludeSubtasksWithTime() {
        // given
        int epicId = manager.createEpic("Эпик", "Эпик с подзадачами");
        manager.createSubtask("Подзадача на 12:00", "Описание подзадачи", epicId);

        // Получаем подзадачу и устанавливаем время
        List<Subtask> subtasks = manager.getAllSubtasks();
        assertEquals(1, subtasks.size());
        Subtask subtask = subtasks.get(0);
        Subtask updatedSubtask = new Subtask(subtask.getId(), subtask.getName(), subtask.getDescription(), 
                                           subtask.getStatus(), Duration.ofMinutes(45),
                                           LocalDateTime.of(2024, 1, 15, 12, 0), subtask.getEpicId());
        manager.updateSubtask(updatedSubtask);

        // when
        List<Task> prioritizedTasks = manager.getPrioritizedTasks();

        // then
        assertEquals(1, prioritizedTasks.size(), "Должна быть 1 подзадача с временем");
        assertEquals("Подзадача на 12:00", prioritizedTasks.get(0).getName());
        assertEquals(LocalDateTime.of(2024, 1, 15, 12, 0), prioritizedTasks.get(0).getStartTime());
    }

    @Test
    void getPrioritizedTasks_shouldMaintainOrderWithMultipleTasks() {
        // given
        // Создаем задачи в неупорядоченном временном порядке
        int task1Id = manager.createTask("Задача на 15:00", "Описание");
        var task1Opt = manager.getTask(task1Id);
        assertTrue(task1Opt.isPresent());
        Task task1 = task1Opt.get();
        Task updatedTask1 = new Task(task1.getId(), task1.getName(), task1.getDescription(), task1.getStatus(),
                                   Duration.ofHours(1), LocalDateTime.of(2024, 1, 15, 15, 0));
        manager.updateTask(updatedTask1);

        int task2Id = manager.createTask("Задача на 10:00", "Описание");
        var task2Opt = manager.getTask(task2Id);
        assertTrue(task2Opt.isPresent());
        Task task2 = task2Opt.get();
        Task updatedTask2 = new Task(task2.getId(), task2.getName(), task2.getDescription(), task2.getStatus(),
                                   Duration.ofMinutes(30), LocalDateTime.of(2024, 1, 15, 10, 0));
        manager.updateTask(updatedTask2);

        int epicId = manager.createEpic("Эпик", "Описание");
        manager.createSubtask("Подзадача на 12:00", "Описание", epicId);
        List<Subtask> subtasks = manager.getAllSubtasks();
        Subtask subtask = subtasks.get(0);
        Subtask updatedSubtask = new Subtask(subtask.getId(), subtask.getName(), subtask.getDescription(), 
                                           subtask.getStatus(), Duration.ofMinutes(45),
                                           LocalDateTime.of(2024, 1, 15, 12, 0), subtask.getEpicId());
        manager.updateSubtask(updatedSubtask);

        // when
        List<Task> prioritizedTasks = manager.getPrioritizedTasks();

        // then
        assertEquals(3, prioritizedTasks.size());
        assertEquals("Задача на 10:00", prioritizedTasks.get(0).getName());
        assertEquals("Подзадача на 12:00", prioritizedTasks.get(1).getName());
        assertEquals("Задача на 15:00", prioritizedTasks.get(2).getName());
    }

    @Test
    void getPrioritizedTasks_shouldPersistAfterFileReload() throws IOException {
        // given
        int task1Id = manager.createTask("Задача на 15:00", "Описание");
        var task1Opt = manager.getTask(task1Id);
        assertTrue(task1Opt.isPresent());
        Task task1 = task1Opt.get();
        Task updatedTask1 = new Task(task1.getId(), task1.getName(), task1.getDescription(), task1.getStatus(),
                                   Duration.ofHours(1), LocalDateTime.of(2024, 1, 15, 15, 0));
        manager.updateTask(updatedTask1);

        int task2Id = manager.createTask("Задача на 10:00", "Описание");
        var task2Opt = manager.getTask(task2Id);
        assertTrue(task2Opt.isPresent());
        Task task2 = task2Opt.get();
        Task updatedTask2 = new Task(task2.getId(), task2.getName(), task2.getDescription(), task2.getStatus(),
                                   Duration.ofMinutes(30), LocalDateTime.of(2024, 1, 15, 10, 0));
        manager.updateTask(updatedTask2);

        // when
        FileBackedTaskManager newManager = FileBackedTaskManager.loadFromFile(tempFile);
        List<Task> loadedPrioritizedTasks = newManager.getPrioritizedTasks();

        // then
        assertEquals(2, loadedPrioritizedTasks.size());
        assertEquals("Задача на 10:00", loadedPrioritizedTasks.get(0).getName());
        assertEquals("Задача на 15:00", loadedPrioritizedTasks.get(1).getName());
    }

    @Test
    void getPrioritizedTasks_shouldExcludeTasksWithoutTime() {
        // given
        manager.createTask("Задача без времени 1", "Описание");
        manager.createTask("Задача без времени 2", "Описание");

        int taskWithTimeId = manager.createTask("Задача с временем", "Описание");
        var taskOpt = manager.getTask(taskWithTimeId);
        assertTrue(taskOpt.isPresent());
        Task task = taskOpt.get();
        Task updatedTask = new Task(task.getId(), task.getName(), task.getDescription(), task.getStatus(),
                                  Duration.ofHours(1), LocalDateTime.of(2024, 1, 15, 10, 0));
        manager.updateTask(updatedTask);

        // when
        List<Task> prioritizedTasks = manager.getPrioritizedTasks();

        // then
        assertEquals(1, prioritizedTasks.size(), "Только задачи с временем должны быть в приоритизированном списке");
        assertEquals("Задача с временем", prioritizedTasks.get(0).getName());
    }
}
