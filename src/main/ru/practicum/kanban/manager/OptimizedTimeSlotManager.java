package main.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.model.Task;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Оптимизированный менеджер проверки пересечений задач с временной сложностью
 * O(1).
 * Использует сетку времени с интервалами в 15 минут для быстрой проверки
 * занятости временных слотов.
 */
public class OptimizedTimeSlotManager {

    // Размер временного слота в минутах
    private static final int SLOT_SIZE_MINUTES = 15;

    // Количество слотов в году (365 дней * 24 часа * 4 слота по 15 минут)
    private static final int SLOTS_PER_YEAR = 365 * 24 * 4;

    // Базовое время для расчетов (начало планирования)
    private final LocalDateTime baseTime;

    // BitSet для отслеживания занятых временных слотов
    private final BitSet occupiedSlots;

    // Карта задач и их занятых слотов для быстрого освобождения
    private final Map<Integer, int[]> taskSlots;

    public OptimizedTimeSlotManager(LocalDateTime baseTime) {
        this.baseTime = baseTime;
        this.occupiedSlots = new BitSet(SLOTS_PER_YEAR);
        this.taskSlots = new HashMap<>();
    }

    /**
     * Проверяет, есть ли конфликт с существующими задачами.
     * Временная сложность: O(1) - проверка занятости всех слотов за константное время.
     */
    public boolean hasTimeConflict(Task task) {
        if (task.getStartTime() == null || task.getDuration() == null) {
            return false; // Задачи без времени не конфликтуют
        }

        int[] slots = calculateSlots(task);
        if (slots == null) {
            return false; // Задача вне планируемого периода
        }

        // Проверяем все слоты задачи за O(n), где n - количество слотов задачи
        // (константа)
        for (int slot : slots) {
            if (occupiedSlots.get(slot)) {
                return true; // Найден конфликт
            }
        }

        return false; // Конфликтов нет
    }

    /**
     * Рассчитывает массив индексов временных слотов для задачи.
     * Временная сложность: O(1) - количество слотов ограничено максимальной
     * длительностью задачи
     */
    private int[] calculateSlots(Task task) {
        LocalDateTime startTime = task.getStartTime();
        LocalDateTime endTime = task.getEndTime();

        // Проверяем, что задача в пределах планируемого периода
        LocalDateTime yearEnd = baseTime.plusYears(1);
        if (startTime.isBefore(baseTime) || endTime.isAfter(yearEnd)) {
            return null; // Задача вне планируемого периода
        }

        // Рассчитываем индексы начального и конечного слотов
        int startSlot = calculateSlotIndex(startTime);
        int endSlot = calculateSlotIndex(endTime);

        // Если задача заканчивается ровно на границе слота, не включаем этот слот
        if (endTime.getMinute() % SLOT_SIZE_MINUTES == 0 && endTime.getSecond() == 0) {
            endSlot--;
        }

        // Создаем массив индексов слотов
        int slotsCount = endSlot - startSlot + 1;
        int[] slots = new int[slotsCount];
        for (int i = 0; i < slotsCount; i++) {
            slots[i] = startSlot + i;
        }

        return slots;
    }

    /**
     * Рассчитывает индекс временного слота для заданного времени.
     * Временная сложность: O(1)
     */
    private int calculateSlotIndex(LocalDateTime dateTime) {
        long minutesFromBase = ChronoUnit.MINUTES.between(baseTime, dateTime);
        return (int) (minutesFromBase / SLOT_SIZE_MINUTES);
    }

    /**
     * Обновляет задачу в сетке времени.
     * Временная сложность: O(1)
     */
    public void updateTask(Task oldTask, Task newTask) {
        removeTask(oldTask);
        addTask(newTask);
    }

    /**
     * Удаляет задачу из сетки времени.
     * Временная сложность: O(1)
     */
    public void removeTask(Task task) {
        int[] slots = taskSlots.remove(task.getId());
        if (slots != null) {
            // Освобождаем слоты
            for (int slot : slots) {
                occupiedSlots.clear(slot);
            }
        }
    }

    /**
     * Добавляет задачу в сетку времени.
     * Временная сложность: O(1)
     */
    public void addTask(Task task) {
        if (task.getStartTime() == null || task.getDuration() == null) {
            return; // Игнорируем задачи без времени
        }

        int[] slots = calculateSlots(task);
        if (slots == null) {
            return; // Задача вне планируемого периода
        }

        // Отмечаем слоты как занятые
        for (int slot : slots) {
            occupiedSlots.set(slot);
        }

        // Сохраняем информацию о слотах задачи
        taskSlots.put(task.getId(), slots);
    }

    /**
     * Возвращает статистику использования слотов.
     */
    public String getStatistics() {
        int occupiedCount = occupiedSlots.cardinality();
        double occupancyPercentage = (double) occupiedCount / SLOTS_PER_YEAR * 100;

        return String.format(
                "Временная сетка: %d слотов по %d минут, занято: %d (%.2f%%)",
                SLOTS_PER_YEAR, SLOT_SIZE_MINUTES, occupiedCount, occupancyPercentage);
    }

    /**
     * Находит ближайший свободный слот заданной длительности.
     * Временная сложность: O(n), где n - количество слотов для поиска
     */
    public LocalDateTime findNextFreeSlot(int durationMinutes, LocalDateTime searchStart) {
        int requiredSlots = (int) Math.ceil((double) durationMinutes / SLOT_SIZE_MINUTES);
        int startSlot = calculateSlotIndex(searchStart);

        for (int slot = startSlot; slot <= SLOTS_PER_YEAR - requiredSlots; slot++) {
            boolean allFree = true;

            // Проверяем, свободны ли все необходимые слоты
            for (int i = 0; i < requiredSlots; i++) {
                if (occupiedSlots.get(slot + i)) {
                    allFree = false;
                    break;
                }
            }

            if (allFree) {
                // Найден свободный интервал
                return baseTime.plusMinutes(slot * SLOT_SIZE_MINUTES);
            }
        }

        return null; // Свободный слот не найден
    }
}
