package main.ru.practicum.kanban;

import main.ru.practicum.kanban.manager.Managers;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("Поехали!");

        // Получаем менеджер задач через утилитарный класс
        TaskManager taskManager = Managers.getDefault();
        System.out.println("Создан менеджер задач: " + taskManager.getClass().getSimpleName());
        System.out.println();

        // Создаем две задачи
        System.out.println("=== СОЗДАНИЕ ДВУХ ЗАДАЧ ===");
        int task1Id = taskManager.createTask("Задача 1", "Описание задачи 1");
        int task2Id = taskManager.createTask("Задача 2", "Описание задачи 2");
        System.out.println("Созданы две задачи с ID: " + task1Id + " и " + task2Id);
        System.out.println();

        // Создаем эпик с тремя подзадачами
        System.out.println("=== СОЗДАНИЕ ЭПИКА С ТРЕМЯ ПОДЗАДАЧАМИ ===");
        int epic1Id = taskManager.createEpic("Эпик с подзадачами", "Описание эпика с тремя подзадачами");
        taskManager.createSubtask("Подзадача 1", "Описание подзадачи 1", epic1Id);
        taskManager.createSubtask("Подзадача 2", "Описание подзадачи 2", epic1Id);
        taskManager.createSubtask("Подзадача 3", "Описание подзадачи 3", epic1Id);
        System.out.println("Создан эпик с ID: " + epic1Id + " и тремя подзадачами");

        // Получаем ID подзадач для дальнейшего использования
        List<Subtask> epic1Subtasks = taskManager.getEpicSubtasks(epic1Id);
        int subtask1Id = epic1Subtasks.get(0).getId();
        int subtask2Id = epic1Subtasks.get(1).getId();
        int subtask3Id = epic1Subtasks.get(2).getId();
        System.out.println("ID подзадач: " + subtask1Id + ", " + subtask2Id + ", " + subtask3Id);
        System.out.println();

        // Создаем эпик без подзадач
        System.out.println("=== СОЗДАНИЕ ЭПИКА БЕЗ ПОДЗАДАЧ ===");
        int epic2Id = taskManager.createEpic("Эпик без подзадач", "Описание эпика без подзадач");
        System.out.println("Создан эпик без подзадач с ID: " + epic2Id);
        System.out.println();

        // Запрашиваем созданные задачи в разном порядке и выводим историю
        System.out.println("=== ЗАПРОС ЗАДАЧ В РАЗНОМ ПОРЯДКЕ ===");

        System.out.println("1. Запрашиваем задачу 1:");
        taskManager.getTask(task1Id);
        System.out.println("История:");
        for (Task task : taskManager.getHistory()) {
            System.out.println("  " + task);
        }
        System.out.println();

        System.out.println("2. Запрашиваем эпик с подзадачами:");
        taskManager.getEpic(epic1Id);
        System.out.println("История:");
        for (Task task : taskManager.getHistory()) {
            System.out.println("  " + task);
        }
        System.out.println();

        System.out.println("3. Запрашиваем подзадачу 2:");
        taskManager.getSubtask(subtask2Id);
        System.out.println("История:");
        for (Task task : taskManager.getHistory()) {
            System.out.println("  " + task);
        }
        System.out.println();

        System.out.println("4. Запрашиваем задачу 2:");
        taskManager.getTask(task2Id);
        System.out.println("История:");
        for (Task task : taskManager.getHistory()) {
            System.out.println("  " + task);
        }
        System.out.println();

        System.out.println("5. Запрашиваем эпик без подзадач:");
        taskManager.getEpic(epic2Id);
        System.out.println("История:");
        for (Task task : taskManager.getHistory()) {
            System.out.println("  " + task);
        }
        System.out.println();

        System.out.println("6. Запрашиваем подзадачу 1:");
        taskManager.getSubtask(subtask1Id);
        System.out.println("История:");
        for (Task task : taskManager.getHistory()) {
            System.out.println("  " + task);
        }
        System.out.println();

        System.out.println("7. Повторно запрашиваем задачу 1 (проверка на дубликаты):");
        taskManager.getTask(task1Id);
        System.out.println("История (не должно быть дубликатов):");
        for (Task task : taskManager.getHistory()) {
            System.out.println("  " + task);
        }
        System.out.println();

        System.out.println("8. Запрашиваем подзадачу 3:");
        taskManager.getSubtask(subtask3Id);
        System.out.println("История:");
        for (Task task : taskManager.getHistory()) {
            System.out.println("  " + task);
        }
        System.out.println();

        // Удаляем задачу, которая есть в истории
        System.out.println("=== УДАЛЕНИЕ ЗАДАЧИ ИЗ ИСТОРИИ ===");
        System.out.println("Удаляем задачу 2 (ID: " + task2Id + "), которая есть в истории");
        taskManager.deleteTask(task2Id);
        System.out.println("История после удаления задачи 2:");
        for (Task task : taskManager.getHistory()) {
            System.out.println("  " + task);
        }
        System.out.println("Убеждаемся, что удаленная задача не выводится в истории");
        System.out.println();

        // Удаляем эпик с тремя подзадачами
        System.out.println("=== УДАЛЕНИЕ ЭПИКА С ПОДЗАДАЧАМИ ===");
        System.out.println("Удаляем эпик с подзадачами (ID: " + epic1Id + ")");
        System.out.println("Это должно удалить из истории сам эпик и все его подзадачи");
        taskManager.deleteEpic(epic1Id);
        System.out.println("История после удаления эпика с подзадачами:");
        for (Task task : taskManager.getHistory()) {
            System.out.println("  " + task);
        }
        System.out.println("Убеждаемся, что эпик и все его подзадачи удалены из истории");
        System.out.println();

        System.out.println("=== ИТОГОВОЕ СОСТОЯНИЕ ===");
        printAllTasks(taskManager);

        System.out.println("Тестирование завершено!");
    }

    private static void printAllTasks(TaskManager manager) {
        System.out.println("Задачи:");
        for (Task task : manager.getAllTasks()) {
            System.out.println(task);
        }
        System.out.println("Эпики:");
        for (Epic epic : manager.getAllEpics()) {
            System.out.println(epic);

            for (Subtask task : manager.getEpicSubtasks(epic.getId())) {
                System.out.println("--> " + task);
            }
        }
        System.out.println("Подзадачи:");
        for (Subtask subtask : manager.getAllSubtasks()) {
            System.out.println(subtask);
        }

        System.out.println("История:");
        for (Task task : manager.getHistory()) {
            System.out.println(task);
        }
        System.out.println();
    }
}