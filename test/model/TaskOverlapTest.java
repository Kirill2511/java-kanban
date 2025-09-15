package model;

import main.ru.practicum.kanban.exception.TaskValidationException;
import main.ru.practicum.kanban.manager.InMemoryTaskManager;
import main.ru.practicum.kanban.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class TaskOverlapTest {

    private InMemoryTaskManager manager;

    @BeforeEach
    void setUp() {
        manager = new InMemoryTaskManager();
    }

    @Test
    void isTasksOverlapping_shouldDetectOverlap() {
        // given
        Task task1 = new Task("Задача 1", "Описание", Duration.ofHours(2), 
                             LocalDateTime.of(2024, 1, 15, 10, 0)); // 10:00-12:00
        task1.setId(1);
        
        Task task2 = new Task("Задача 2", "Описание", Duration.ofHours(2), 
                             LocalDateTime.of(2024, 1, 15, 11, 0)); // 11:00-13:00
        task2.setId(2);

        // when & then
        assertTrue(manager.isTasksOverlapping(task1, task2), 
                  "Задачи должны пересекаться");
    }

    @Test
    void isTasksOverlapping_shouldNotDetectOverlapForNonOverlappingTasks() {
        // given
        Task task1 = new Task("Задача 1", "Описание", Duration.ofHours(2), 
                             LocalDateTime.of(2024, 1, 15, 10, 0)); // 10:00-12:00
        task1.setId(1);
        
        Task task3 = new Task("Задача 3", "Описание", Duration.ofHours(2), 
                             LocalDateTime.of(2024, 1, 15, 13, 0)); // 13:00-15:00
        task3.setId(3);

        // when & then
        assertFalse(manager.isTasksOverlapping(task1, task3), 
                   "Задачи не должны пересекаться");
    }

    @Test
    void isTasksOverlapping_shouldNotDetectOverlapForAdjacentTasks() {
        // given - граничный случай: задачи касаются границами
        Task task1 = new Task("Задача 1", "Описание", Duration.ofHours(2), 
                             LocalDateTime.of(2024, 1, 15, 10, 0)); // 10:00-12:00
        task1.setId(1);
        
        Task task4 = new Task("Задача 4", "Описание", Duration.ofHours(2), 
                             LocalDateTime.of(2024, 1, 15, 12, 0)); // 12:00-14:00
        task4.setId(4);

        // when & then
        assertFalse(manager.isTasksOverlapping(task1, task4), 
                   "Смежные задачи не должны считаться пересекающимися");
    }

    @Test
    void updateTask_shouldThrowExceptionForOverlappingTasks() {
        // given
        int taskId1 = manager.createTask("Первая задача", "Описание");
        var firstTaskOpt = manager.getTask(taskId1);
        assertTrue(firstTaskOpt.isPresent());
        Task firstTask = firstTaskOpt.get();
        
        Task updatedFirstTask = new Task(firstTask.getId(), firstTask.getName(), firstTask.getDescription(),
                                        firstTask.getStatus(), Duration.ofHours(2), 
                                        LocalDateTime.of(2024, 1, 15, 14, 0)); // 14:00-16:00
        manager.updateTask(updatedFirstTask);

        // when
        int taskId2 = manager.createTask("Вторая задача", "Описание");
        var secondTaskOpt = manager.getTask(taskId2);
        assertTrue(secondTaskOpt.isPresent());
        Task secondTask = secondTaskOpt.get();
        
        Task conflictingTask = new Task(secondTask.getId(), secondTask.getName(), secondTask.getDescription(),
                                       secondTask.getStatus(), Duration.ofHours(2), 
                                       LocalDateTime.of(2024, 1, 15, 15, 0)); // 15:00-17:00 - пересекается!

        // then
        assertThrows(TaskValidationException.class, () -> manager.updateTask(conflictingTask),
                    "Должно выброситься исключение при попытке создать пересекающуюся задачу");
    }

    @Test
    void updateTask_shouldAllowNonOverlappingTasks() {
        // given
        int taskId1 = manager.createTask("Первая задача", "Описание");
        var firstTaskOpt = manager.getTask(taskId1);
        assertTrue(firstTaskOpt.isPresent());
        Task firstTask = firstTaskOpt.get();
        
        Task updatedFirstTask = new Task(firstTask.getId(), firstTask.getName(), firstTask.getDescription(),
                                        firstTask.getStatus(), Duration.ofHours(2), 
                                        LocalDateTime.of(2024, 1, 15, 14, 0)); // 14:00-16:00
        manager.updateTask(updatedFirstTask);

        int taskId2 = manager.createTask("Вторая задача", "Описание");
        var secondTaskOpt = manager.getTask(taskId2);
        assertTrue(secondTaskOpt.isPresent());
        Task secondTask = secondTaskOpt.get();
        
        Task nonConflictingTask = new Task(secondTask.getId(), secondTask.getName(), secondTask.getDescription(),
                                          secondTask.getStatus(), Duration.ofHours(2), 
                                          LocalDateTime.of(2024, 1, 15, 16, 0)); // 16:00-18:00 - не пересекается

        // when & then
        assertDoesNotThrow(() -> manager.updateTask(nonConflictingTask),
                          "Непересекающаяся задача должна создаваться без ошибок");
    }

    @Test
    void isTasksOverlapping_shouldNotDetectOverlapForTasksWithoutTime() {
        // given
        Task taskWithoutTime = new Task("Задача без времени", "Описание");
        taskWithoutTime.setId(99);
        
        Task taskWithTime = new Task("Задача с временем", "Описание", Duration.ofHours(2), 
                                   LocalDateTime.of(2024, 1, 15, 14, 0));
        taskWithTime.setId(100);

        // when & then
        assertFalse(manager.isTasksOverlapping(taskWithoutTime, taskWithTime),
                   "Задача без времени не должна пересекаться с задачей с временем");
        assertFalse(manager.hasTimeConflict(taskWithoutTime),
                   "Задача без времени не должна иметь временных конфликтов");
    }
}
