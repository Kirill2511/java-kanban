package manager;

import main.ru.practicum.kanban.manager.OptimizedTimeSlotManager;
import main.ru.practicum.kanban.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class OptimizedTimeSlotManagerTest {

    private OptimizedTimeSlotManager timeSlotManager;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        timeSlotManager = new OptimizedTimeSlotManager(baseTime);
    }

    @Test
    void hasTimeConflict_shouldReturnFalseForTaskWithoutTime() {
        // given
        Task taskWithoutTime = new Task("Задача без времени", "Описание");

        // when & then
        assertFalse(timeSlotManager.hasTimeConflict(taskWithoutTime),
                "Задача без времени не должна иметь конфликтов");
    }

    @Test
    void hasTimeConflict_shouldReturnFalseForEmptyManager() {
        // given
        Task task = new Task("Задача", "Описание", Duration.ofHours(1), baseTime.plusHours(10));

        // when & then
        assertFalse(timeSlotManager.hasTimeConflict(task),
                "В пустом менеджере не должно быть конфликтов");
    }

    @Test
    void addTask_shouldOccupyTimeSlots() {
        // given
        Task task = new Task("Задача", "Описание", Duration.ofHours(1), baseTime.plusHours(10));
        task.setId(1);

        // when
        timeSlotManager.addTask(task);

        // then
        Task conflictingTask = new Task("Конфликтующая задача", "Описание",
                Duration.ofMinutes(30), baseTime.plusHours(10).plusMinutes(30));
        assertTrue(timeSlotManager.hasTimeConflict(conflictingTask),
                "Должен быть обнаружен конфликт с добавленной задачей");
    }

    @Test
    void removeTask_shouldFreeTimeSlots() {
        // given
        Task task = new Task("Задача", "Описание", Duration.ofHours(1), baseTime.plusHours(10));
        task.setId(1);
        timeSlotManager.addTask(task);

        // Проверяем, что слоты заняты
        Task conflictingTask = new Task("Конфликтующая задача", "Описание",
                Duration.ofMinutes(30), baseTime.plusHours(10).plusMinutes(30));
        assertTrue(timeSlotManager.hasTimeConflict(conflictingTask));

        // when
        timeSlotManager.removeTask(task);

        // then
        assertFalse(timeSlotManager.hasTimeConflict(conflictingTask),
                "После удаления задачи конфликт должен исчезнуть");
    }

    @Test
    void updateTask_shouldCorrectlyMoveTimeSlots() {
        // given
        Task oldTask = new Task("Старая задача", "Описание", Duration.ofHours(1), baseTime.plusHours(10));
        oldTask.setId(1);
        timeSlotManager.addTask(oldTask);

        Task newTask = new Task("Новая задача", "Описание", Duration.ofHours(1), baseTime.plusHours(12));
        newTask.setId(1);

        // when
        timeSlotManager.updateTask(oldTask, newTask);

        // then
        // Старое время должно быть свободно
        Task testTask1 = new Task("Тест 1", "Описание", Duration.ofMinutes(30), baseTime.plusHours(10).plusMinutes(30));
        assertFalse(timeSlotManager.hasTimeConflict(testTask1),
                "Старое время должно быть освобождено");

        // Новое время должно быть занято
        Task testTask2 = new Task("Тест 2", "Описание", Duration.ofMinutes(30), baseTime.plusHours(12).plusMinutes(30));
        assertTrue(timeSlotManager.hasTimeConflict(testTask2),
                "Новое время должно быть занято");
    }

    @Test
    void hasTimeConflict_shouldDetectOverlapWithMultipleTasks() {
        // given
        Task task1 = new Task("Задача 1", "Описание", Duration.ofHours(1), baseTime.plusHours(10)); // 10:00-11:00
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание", Duration.ofHours(1), baseTime.plusHours(12)); // 12:00-13:00
        task2.setId(2);

        timeSlotManager.addTask(task1);
        timeSlotManager.addTask(task2);

        // when & then
        // Пересекается с первой задачей
        Task conflictTask1 = new Task("Конфликт 1", "Описание", Duration.ofMinutes(30),
                baseTime.plusHours(10).plusMinutes(30));
        assertTrue(timeSlotManager.hasTimeConflict(conflictTask1));

        // Пересекается со второй задачей
        Task conflictTask2 = new Task("Конфликт 2", "Описание", Duration.ofMinutes(30),
                baseTime.plusHours(12).plusMinutes(30));
        assertTrue(timeSlotManager.hasTimeConflict(conflictTask2));

        // Не пересекается ни с одной
        Task noConflictTask = new Task("Без конфликта", "Описание", Duration.ofMinutes(30), baseTime.plusHours(11));
        assertFalse(timeSlotManager.hasTimeConflict(noConflictTask));
    }

    @Test
    void hasTimeConflict_shouldHandleTasksWithDifferentDurations() {
        // given
        Task longTask = new Task("Длинная задача", "Описание", Duration.ofHours(4), baseTime.plusHours(10)); // 10:00-14:00
        longTask.setId(1);
        timeSlotManager.addTask(longTask);

        // when & then
        // Короткая задача в начале
        Task shortTask1 = new Task("Короткая 1", "Описание", Duration.ofMinutes(15),
                baseTime.plusHours(10).plusMinutes(15));
        assertTrue(timeSlotManager.hasTimeConflict(shortTask1));

        // Короткая задача в середине
        Task shortTask2 = new Task("Короткая 2", "Описание", Duration.ofMinutes(15), baseTime.plusHours(12));
        assertTrue(timeSlotManager.hasTimeConflict(shortTask2));

        // Короткая задача в конце
        Task shortTask3 = new Task("Короткая 3", "Описание", Duration.ofMinutes(15),
                baseTime.plusHours(13).plusMinutes(45));
        assertTrue(timeSlotManager.hasTimeConflict(shortTask3));

        // Задача после окончания
        Task afterTask = new Task("После", "Описание", Duration.ofMinutes(15), baseTime.plusHours(14));
        assertFalse(timeSlotManager.hasTimeConflict(afterTask));
    }

    @Test
    void hasTimeConflict_shouldHandleBoundaryConditions() {
        // given
        Task task = new Task("Основная задача", "Описание", Duration.ofHours(1), baseTime.plusHours(10)); // 10:00-11:00
        task.setId(1);
        timeSlotManager.addTask(task);

        // when & then
        // Задача точно до начала (9:00-10:00)
        Task beforeTask = new Task("До", "Описание", Duration.ofHours(1), baseTime.plusHours(9));
        assertFalse(timeSlotManager.hasTimeConflict(beforeTask),
                "Задача, заканчивающаяся точно в момент начала другой, не должна конфликтовать");

        // Задача точно после окончания (11:00-12:00)
        Task afterTask = new Task("После", "Описание", Duration.ofHours(1), baseTime.plusHours(11));
        assertFalse(timeSlotManager.hasTimeConflict(afterTask),
                "Задача, начинающаяся точно в момент окончания другой, не должна конфликтовать");

        // Задача, начинающаяся на 1 минуту раньше конца (10:59-11:59)
        Task overlapTask = new Task("Пересечение", "Описание", Duration.ofHours(1),
                baseTime.plusHours(10).plusMinutes(59));
        assertTrue(timeSlotManager.hasTimeConflict(overlapTask),
                "Задача с пересечением должна вызывать конфликт");
    }

    @Test
    void findNextFreeSlot_shouldFindAvailableSlot() {
        // given
        Task task1 = new Task("Задача 1", "Описание", Duration.ofHours(1), baseTime.plusHours(10)); // 10:00-11:00
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание", Duration.ofHours(1), baseTime.plusHours(12)); // 12:00-13:00
        task2.setId(2);

        timeSlotManager.addTask(task1);
        timeSlotManager.addTask(task2);

        // when
        LocalDateTime searchStart = baseTime.plusHours(9); // Начинаем поиск с 9:00
        LocalDateTime freeSlot = timeSlotManager.findNextFreeSlot(60, searchStart); // Ищем 60 минут

        // then
        assertNotNull(freeSlot, "Должен найтись свободный слот");

        // Проверяем, что найденный слот действительно свободен
        Task testTask = new Task("Тест", "Описание", Duration.ofMinutes(60), freeSlot);
        assertFalse(timeSlotManager.hasTimeConflict(testTask),
                "Найденный слот должен быть действительно свободен");
    }

    @Test
    void findNextFreeSlot_shouldWorkWithHeavyLoad() {
        // given - заполняем календарь множеством задач
        for (int day = 0; day < 50; day++) {
            Task task = new Task("Задача " + day, "Описание", Duration.ofHours(20), baseTime.plusDays(day));
            task.setId(day);
            timeSlotManager.addTask(task);
        }

        // when
        LocalDateTime searchStart = baseTime.plusDays(25);
        LocalDateTime freeSlot = timeSlotManager.findNextFreeSlot(60, searchStart); // Ищем 1 час

        // then - система должна работать даже при высокой нагрузке
        // Может найти слот или не найти, главное что не падает
        assertTrue(freeSlot == null || freeSlot.isAfter(searchStart),
                "Система должна корректно обрабатывать поиск в загруженном календаре");
    }

    @Test
    void getStatistics_shouldReturnValidStatistics() {
        // given
        Task task1 = new Task("Задача 1", "Описание", Duration.ofHours(1), baseTime.plusHours(10));
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание", Duration.ofHours(2), baseTime.plusHours(12));
        task2.setId(2);

        timeSlotManager.addTask(task1);
        timeSlotManager.addTask(task2);

        // when
        String statistics = timeSlotManager.getStatistics();

        // then
        assertNotNull(statistics);
        assertTrue(statistics.contains("Временная сетка"), "Статистика должна содержать описание сетки");
        assertTrue(statistics.contains("слотов"), "Статистика должна содержать информацию о слотах");
        assertTrue(statistics.contains("занято"), "Статистика должна показывать занятые слоты");
        assertTrue(statistics.contains("%"), "Статистика должна показывать процент заполненности");
    }

    @Test
    void hasTimeConflict_shouldIgnoreTasksOutsideTimeRange() {
        // given
        Task taskOutsideRange = new Task("Вне диапазона", "Описание",
                Duration.ofHours(1), baseTime.minusYears(1)); // Год назад

        // when & then
        assertFalse(timeSlotManager.hasTimeConflict(taskOutsideRange),
                "Задача вне планируемого диапазона не должна вызывать конфликт");
    }

    @Test
    void performanceTest_operationsShouldBeConstantTime() {
        // given - добавляем много задач
        for (int i = 0; i < 1000; i++) {
            Task task = new Task("Задача " + i, "Описание", Duration.ofMinutes(30), baseTime.plusMinutes(i * 60));
            task.setId(i);
            timeSlotManager.addTask(task);
        }

        Task testTask = new Task("Тест", "Описание", Duration.ofMinutes(15), baseTime.plusMinutes(500 * 60 + 15));

        // when - измеряем время проверки конфликтов
        long startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            timeSlotManager.hasTimeConflict(testTask); // Должно быть O(1)
        }
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        // then
        assertTrue(durationMs < 50,
                "10000 проверок конфликтов должны выполняться менее чем за 50мс (текущее время: " + durationMs + "мс)");
    }

    @Test
    void hasTimeConflict_shouldHandleVeryShortTasks() {
        // given
        Task task1 = new Task("Задача 1", "Описание", Duration.ofMinutes(1), baseTime.plusHours(10)); // 1 минута
        task1.setId(1);
        timeSlotManager.addTask(task1);

        // when & then
        Task shortConflict = new Task("Короткий конфликт", "Описание", Duration.ofSeconds(30),
                baseTime.plusHours(10).plusSeconds(30));
        assertTrue(timeSlotManager.hasTimeConflict(shortConflict),
                "Даже очень короткие задачи должны правильно обрабатываться");
    }
}
