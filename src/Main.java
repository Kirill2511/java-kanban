public class Main {

    public static void main(String[] args) {
        System.out.println("Поехали!");

        TaskManager taskManager = new TaskManager();

        // Создание двух обычных задач
        Task task1 = taskManager.createTask("Собрать коробки", "Упаковать вещи для переезда");
        Task task2 = taskManager.createTask("Упаковать кошку", "Подготовить переноску для кошки");

        // Создание эпика с двумя подзадачами
        Epic epic1 = taskManager.createEpic("Переезд", "Организовать переезд в новую квартиру");
        Subtask subtask1 = taskManager.createSubtask("Заказать машину", "Арендовать грузовик", epic1.getId());
        Subtask subtask2 = taskManager.createSubtask("Упаковать посуду", "Аккуратно упаковать хрупкие предметы",
                epic1.getId());

        // Создание эпика с одной подзадачей
        Epic epic2 = taskManager.createEpic("Важный эпик 2", "Описание второго эпика");
        Subtask subtask3 = taskManager.createSubtask("Сказать слова прощания", "Попрощаться с соседями", epic2.getId());

        // Распечатка списков задач, эпиков и подзадач
        System.out.println("=== НАЧАЛЬНОЕ СОСТОЯНИЕ ===");
        System.out.println("Задачи:");
        for (Task task : taskManager.getAllTasks()) {
            System.out.println("  " + task);
        }

        System.out.println("\nЭпики:");
        for (Epic epic : taskManager.getAllEpics()) {
            System.out.println("  " + epic);
        }

        System.out.println("\nПодзадачи:");
        for (Subtask subtask : taskManager.getAllSubtasks()) {
            System.out.println("  " + subtask);
        }

        // Изменение статусов и проверка
        System.out.println("\n=== ИЗМЕНЕНИЕ СТАТУСОВ ===");

        // Изменяем статус обычной задачи
        task1.setStatus(TaskStatus.IN_PROGRESS);
        taskManager.updateTask(task1);
        System.out.println("Изменили статус задачи 1 на IN_PROGRESS: " + taskManager.getTask(task1.getId()));

        // Изменяем статус одной подзадачи в первом эпике
        subtask1.setStatus(TaskStatus.DONE);
        taskManager.updateSubtask(subtask1);
        System.out.println("Изменили статус подзадачи 1 на DONE: " + taskManager.getSubtask(subtask1.getId()));
        System.out.println("Эпик 1 теперь: " + taskManager.getEpic(epic1.getId()));

        // Изменяем статус второй подзадачи в первом эпике
        subtask2.setStatus(TaskStatus.IN_PROGRESS);
        taskManager.updateSubtask(subtask2);
        System.out.println("Изменили статус подзадачи 2 на IN_PROGRESS: " + taskManager.getSubtask(subtask2.getId()));
        System.out.println("Эпик 1 теперь: " + taskManager.getEpic(epic1.getId()));

        // Завершаем все подзадачи первого эпика
        subtask2.setStatus(TaskStatus.DONE);
        taskManager.updateSubtask(subtask2);
        System.out.println("Завершили все подзадачи эпика 1");
        System.out.println("Эпик 1 теперь: " + taskManager.getEpic(epic1.getId()));

        // Изменяем статус подзадачи во втором эпике
        subtask3.setStatus(TaskStatus.DONE);
        taskManager.updateSubtask(subtask3);
        System.out.println("Завершили подзадачу эпика 2");
        System.out.println("Эпик 2 теперь: " + taskManager.getEpic(epic2.getId()));

        // Удаление одной задачи и одного эпика
        System.out.println("\n=== УДАЛЕНИЕ ===");
        System.out.println("Удаляем задачу 2 (id=" + task2.getId() + ")");
        taskManager.deleteTask(task2.getId());

        System.out.println("Удаляем эпик 1 (id=" + epic1.getId() + ") вместе с подзадачами");
        taskManager.deleteEpic(epic1.getId());

        // Финальное состояние
        System.out.println("\n=== ФИНАЛЬНОЕ СОСТОЯНИЕ ===");
        System.out.println("Задачи:");
        for (Task task : taskManager.getAllTasks()) {
            System.out.println("  " + task);
        }

        System.out.println("\nЭпики:");
        for (Epic epic : taskManager.getAllEpics()) {
            System.out.println("  " + epic);
        }

        System.out.println("\nПодзадачи:");
        for (Subtask subtask : taskManager.getAllSubtasks()) {
            System.out.println("  " + subtask);
        }

        System.out.println("\nТестирование завершено!");
    }
}
