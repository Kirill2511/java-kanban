package manager;

import main.ru.practicum.kanban.exception.TaskValidationException;
import main.ru.practicum.kanban.manager.OptimizedInMemoryTaskManager;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OptimizedInMemoryTaskManagerTest {

    private OptimizedInMemoryTaskManager manager;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        manager = new OptimizedInMemoryTaskManager(baseTime);
    }

    @Test
    void createTask_shouldCreateTaskWithOptionalAPI() {
        // given
        String name = "Тестовая задача";
        String description = "Описание";

        // when
        int taskId = manager.createTask(name, description);

        // then
        assertTrue(taskId > 0);
        var taskOpt = manager.getTask(taskId);
        assertTrue(taskOpt.isPresent());
        Task task = taskOpt.get();
        assertEquals(name, task.getName());
        assertEquals(description, task.getDescription());
        assertEquals(TaskStatus.NEW, task.getStatus());
    }

    @Test
    void hasTimeConflict_shouldDetectConflictWithOptimizedAlgorithm() {
        // given - создаем задачу с временем
        int taskId1 = manager.createTask("Задача 1", "Описание");
        var task1Opt = manager.getTask(taskId1);
        assertTrue(task1Opt.isPresent());
        Task task1 = task1Opt.get();

        Task updatedTask1 = new Task(task1.getId(), task1.getName(), task1.getDescription(),
                task1.getStatus(), Duration.ofHours(2),
                baseTime.plusHours(10)); // 10:00-12:00
        manager.updateTask(updatedTask1);

        // when - создаем пересекающуюся задачу
        Task conflictingTask = new Task("Конфликтующая задача", "Описание",
                Duration.ofHours(2), baseTime.plusHours(11)); // 11:00-13:00

        // then
        assertTrue(manager.hasTimeConflict(conflictingTask),
                "Оптимизированный алгоритм должен обнаружить конфликт");
    }

    @Test
    void hasTimeConflict_shouldNotDetectConflictForNonOverlappingTasks() {
        // given
        int taskId1 = manager.createTask("Задача 1", "Описание");
        var task1Opt = manager.getTask(taskId1);
        assertTrue(task1Opt.isPresent());
        Task task1 = task1Opt.get();

        Task updatedTask1 = new Task(task1.getId(), task1.getName(), task1.getDescription(),
                task1.getStatus(), Duration.ofHours(2),
                baseTime.plusHours(10)); // 10:00-12:00
        manager.updateTask(updatedTask1);

        // when - создаем НЕ пересекающуюся задачу
        Task nonConflictingTask = new Task("Не конфликтующая задача", "Описание",
                Duration.ofHours(2), baseTime.plusHours(13)); // 13:00-15:00

        // then
        assertFalse(manager.hasTimeConflict(nonConflictingTask),
                "Не должно быть конфликта для непересекающихся задач");
    }

    @Test
    void updateTask_shouldThrowExceptionForTimeConflict() {
        // given
        int taskId1 = manager.createTask("Задача 1", "Описание");
        var task1Opt = manager.getTask(taskId1);
        assertTrue(task1Opt.isPresent());
        Task task1 = task1Opt.get();

        Task updatedTask1 = new Task(task1.getId(), task1.getName(), task1.getDescription(),
                task1.getStatus(), Duration.ofHours(2),
                baseTime.plusHours(10)); // 10:00-12:00
        manager.updateTask(updatedTask1);

        int taskId2 = manager.createTask("Задача 2", "Описание");
        var task2Opt = manager.getTask(taskId2);
        assertTrue(task2Opt.isPresent());
        Task task2 = task2Opt.get();

        // when & then
        Task conflictingTask = new Task(task2.getId(), task2.getName(), task2.getDescription(),
                task2.getStatus(), Duration.ofHours(2),
                baseTime.plusHours(11)); // 11:00-13:00 - пересекается!

        assertThrows(TaskValidationException.class, () -> manager.updateTask(conflictingTask),
                "Должно выброситься исключение при временном конфликте");
    }

    @Test
    void updateSubtask_shouldThrowExceptionForTimeConflict() {
        // given
        int epicId = manager.createEpic("Эпик", "Описание эпика");
        manager.createSubtask("Подзадача 1", "Описание 1", epicId);
        manager.createSubtask("Подзадача 2", "Описание 2", epicId);

        List<Subtask> subtasks = manager.getAllSubtasks();
        assertEquals(2, subtasks.size());

        // Устанавливаем время для первой подзадачи
        Subtask subtask1 = subtasks.get(0);
        Subtask updatedSubtask1 = new Subtask(subtask1.getId(), subtask1.getName(), subtask1.getDescription(),
                subtask1.getStatus(), Duration.ofHours(1),
                baseTime.plusHours(14), subtask1.getEpicId()); // 14:00-15:00
        manager.updateSubtask(updatedSubtask1);

        // when & then - пытаемся установить пересекающееся время для второй подзадачи
        Subtask subtask2 = subtasks.get(1);
        Subtask conflictingSubtask = new Subtask(subtask2.getId(), subtask2.getName(), subtask2.getDescription(),
                subtask2.getStatus(), Duration.ofHours(1),
                baseTime.plusHours(14).plusMinutes(30), subtask2.getEpicId()); // 14:30-15:30

        assertThrows(TaskValidationException.class, () -> manager.updateSubtask(conflictingSubtask),
                "Подзадача не должна пересекаться по времени с другими задачами");
    }

    @Test
    void getPrioritizedTasks_shouldReturnTasksSortedByTime() {
        // given
        int task1Id = manager.createTask("Задача на 15:00", "Описание");
        var task1Opt = manager.getTask(task1Id);
        assertTrue(task1Opt.isPresent());
        Task task1 = task1Opt.get();
        Task updatedTask1 = new Task(task1.getId(), task1.getName(), task1.getDescription(),
                task1.getStatus(), Duration.ofHours(1),
                baseTime.plusHours(15)); // 15:00
        manager.updateTask(updatedTask1);

        int task2Id = manager.createTask("Задача на 10:00", "Описание");
        var task2Opt = manager.getTask(task2Id);
        assertTrue(task2Opt.isPresent());
        Task task2 = task2Opt.get();
        Task updatedTask2 = new Task(task2.getId(), task2.getName(), task2.getDescription(),
                task2.getStatus(), Duration.ofHours(1),
                baseTime.plusHours(10)); // 10:00
        manager.updateTask(updatedTask2);

        manager.createTask("Задача без времени", "Описание"); // Без времени

        // when
        List<Task> prioritizedTasks = manager.getPrioritizedTasks();

        // then
        assertEquals(3, prioritizedTasks.size());
        assertEquals("Задача на 10:00", prioritizedTasks.get(0).getName());
        assertEquals("Задача на 15:00", prioritizedTasks.get(1).getName());
        assertEquals("Задача без времени", prioritizedTasks.get(2).getName());
    }

    @Test
    void hasTimeConflict_shouldReturnFalseForTasksWithoutTime() {
        // given
        Task taskWithoutTime = new Task("Задача без времени", "Описание");

        // when & then
        assertFalse(manager.hasTimeConflict(taskWithoutTime),
                "Задачи без времени не должны иметь временных конфликтов");
    }

    @Test
    void isTasksOverlapping_shouldWorkCorrectlyForCompatibility() {
        // given
        Task task1 = new Task("Задача 1", "Описание", Duration.ofHours(2),
                baseTime.plusHours(10)); // 10:00-12:00
        task1.setId(1);

        Task task2 = new Task("Задача 2", "Описание", Duration.ofHours(2),
                baseTime.plusHours(11)); // 11:00-13:00
        task2.setId(2);

        Task task3 = new Task("Задача 3", "Описание", Duration.ofHours(2),
                baseTime.plusHours(13)); // 13:00-15:00
        task3.setId(3);

        // when & then
        assertTrue(manager.isTasksOverlapping(task1, task2), "Задачи должны пересекаться");
        assertFalse(manager.isTasksOverlapping(task1, task3), "Задачи не должны пересекаться");
        assertFalse(manager.isTasksOverlapping(task1, task1), "Задача не должна пересекаться сама с собой");
    }

    @Test
    void deleteTask_shouldRemoveFromOptimizedStructures() {
        // given
        int taskId = manager.createTask("Задача для удаления", "Описание");
        var taskOpt = manager.getTask(taskId);
        assertTrue(taskOpt.isPresent());
        Task task = taskOpt.get();

        Task updatedTask = new Task(task.getId(), task.getName(), task.getDescription(),
                task.getStatus(), Duration.ofHours(1),
                baseTime.plusHours(10));
        manager.updateTask(updatedTask);

        // Проверяем, что задача есть в приоритизированном списке
        List<Task> prioritized = manager.getPrioritizedTasks();
        assertEquals(1, prioritized.size());

        // when
        manager.deleteTask(taskId);

        // then
        assertFalse(manager.getTask(taskId).isPresent());
        assertTrue(manager.getPrioritizedTasks().isEmpty());

        // Проверяем, что временной слот освободился
        Task newTask = new Task("Новая задача", "Описание", Duration.ofHours(1),
                baseTime.plusHours(10)); // То же время
        assertFalse(manager.hasTimeConflict(newTask), "Временной слот должен быть освобожден");
    }

    @Test
    void findNextFreeSlot_shouldFindAvailableTimeSlot() {
        // given
        int taskId = manager.createTask("Занятая задача", "Описание");
        var taskOpt = manager.getTask(taskId);
        assertTrue(taskOpt.isPresent());
        Task task = taskOpt.get();

        Task updatedTask = new Task(task.getId(), task.getName(), task.getDescription(),
                task.getStatus(), Duration.ofHours(2),
                baseTime.plusHours(10)); // 10:00-12:00
        manager.updateTask(updatedTask);

        // when
        LocalDateTime searchStart = baseTime.plusHours(9); // Начинаем поиск с 9:00
        LocalDateTime freeSlot = manager.findNextFreeSlot(60, searchStart); // Ищем 60 минут

        // then
        assertNotNull(freeSlot, "Должен найтись свободный слот");
        assertTrue(freeSlot.isBefore(baseTime.plusHours(10)) ||
                freeSlot.isAfter(baseTime.plusHours(12)) ||
                freeSlot.equals(baseTime.plusHours(12)),
                "Свободный слот не должен пересекаться с занятым временем");
    }

    @Test
    void getTimeSlotStatistics_shouldReturnStatistics() {
        // given
        int taskId = manager.createTask("Задача со временем", "Описание");
        var taskOpt = manager.getTask(taskId);
        assertTrue(taskOpt.isPresent());
        Task task = taskOpt.get();

        Task updatedTask = new Task(task.getId(), task.getName(), task.getDescription(),
                task.getStatus(), Duration.ofHours(1),
                baseTime.plusHours(10));
        manager.updateTask(updatedTask);

        // when
        String statistics = manager.getTimeSlotStatistics();

        // then
        assertNotNull(statistics);
        assertTrue(statistics.contains("Временная сетка"), "Статистика должна содержать информацию о сетке");
        assertTrue(statistics.contains("занято"), "Статистика должна показывать занятые слоты");
    }

    @Test
    void performanceTest_timeConflictCheckShouldBeFast() {
        // given - создаем много задач для нагрузочного теста
        for (int i = 0; i < 100; i++) {
            int taskId = manager.createTask("Задача " + i, "Описание");
            var taskOpt = manager.getTask(taskId);
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                Task updatedTask = new Task(task.getId(), task.getName(), task.getDescription(),
                        task.getStatus(), Duration.ofMinutes(30),
                        baseTime.plusHours(i)); // Каждая задача в свое время
                manager.updateTask(updatedTask);
            }
        }

        // when - измеряем время проверки конфликтов
        Task testTask = new Task("Тестовая задача", "Описание", Duration.ofMinutes(30),
                baseTime.plusHours(50));

        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            manager.hasTimeConflict(testTask); // Должно быть O(1)
        }
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        // then
        assertTrue(durationMs < 100,
                "1000 проверок конфликтов должны выполняться менее чем за 100мс (текущее время: " + durationMs + "мс)");
    }
}
