package main.ru.practicum.kanban;

import main.ru.practicum.kanban.manager.Managers;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;

import java.util.List;

public class Main {

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

    public static void main(String[] args) {
        System.out.println("Поехали!");

        // Получаем менеджер задач через утилитарный класс
        TaskManager taskManager = Managers.getDefault();
        System.out.println("Создан менеджер задач: " + taskManager.getClass().getSimpleName());
        System.out.println();

        // Создаем задачи разного типа
        System.out.println("=== СОЗДАНИЕ ЗАДАЧ ===");
        int task1Id = taskManager.createTask("Задача 1", "Описание задачи 1");
        int task2Id = taskManager.createTask("Задача 2", "Описание задачи 2");

        int epic1Id = taskManager.createEpic("Эпик 1", "Описание эпика 1");
        taskManager.createSubtask("Подзадача 1-1", "Описание подзадачи 1-1", epic1Id);
        taskManager.createSubtask("Подзадача 1-2", "Описание подзадачи 1-2", epic1Id);

        int epic2Id = taskManager.createEpic("Эпик 2", "Описание эпика 2");
        taskManager.createSubtask("Подзадача 2-1", "Описание подзадачи 2-1", epic2Id);

        printAllTasks(taskManager);

        // Начинаем просматривать задачи и отслеживать историю
        System.out.println("=== ТЕСТИРОВАНИЕ ИСТОРИИ ПРОСМОТРОВ ===");

        System.out.println("1. Просматриваем задачу 1:");
        taskManager.getTask(task1Id);
        printAllTasks(taskManager);

        System.out.println("2. Просматриваем эпик 1:");
        taskManager.getEpic(epic1Id);
        printAllTasks(taskManager);

        System.out.println("3. Просматриваем задачу 2:");
        taskManager.getTask(task2Id);
        printAllTasks(taskManager);

        System.out.println("4. Просматриваем подзадачи эпика 1:");
        List<Subtask> subtasks = taskManager.getEpicSubtasks(epic1Id);
        for (Subtask subtask : subtasks) {
            taskManager.getSubtask(subtask.getId());
        }
        printAllTasks(taskManager);

        System.out.println("5. Просматриваем эпик 2:");
        taskManager.getEpic(epic2Id);
        printAllTasks(taskManager);

        System.out.println("6. Еще раз просматриваем задачу 1:");
        taskManager.getTask(task1Id);
        printAllTasks(taskManager);

        System.out.println("7. Просматриваем подзадачу эпика 2:");
        List<Subtask> epic2Subtasks = taskManager.getEpicSubtasks(epic2Id);
        if (!epic2Subtasks.isEmpty()) {
            taskManager.getSubtask(epic2Subtasks.getFirst().getId());
        }
        printAllTasks(taskManager);

        System.out.println("Тестирование завершено!");
    }
}