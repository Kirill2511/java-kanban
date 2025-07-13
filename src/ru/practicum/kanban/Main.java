package ru.practicum.kanban;

import java.util.List;

import ru.practicum.kanban.manager.TaskManager;
import ru.practicum.kanban.model.Epic;
import ru.practicum.kanban.model.Subtask;
import ru.practicum.kanban.model.Task;
import ru.practicum.kanban.model.TaskStatus;

public class Main {

	public static void main(String[] args) {
		System.out.println("Поехали!");

		TaskManager taskManager = new TaskManager();

		// Создание двух обычных задач
		int task1Id = taskManager.createTask("Собрать коробки", "Упаковать вещи для переезда");
		int task2Id = taskManager.createTask("Упаковать кошку", "Подготовить переноску для кошки");

		// Создание эпика с двумя подзадачами
		int epic1Id = taskManager.createEpic("Переезд", "Организовать переезд в новую квартиру");
		taskManager.createSubtask("Заказать машину", "Арендовать грузовик", epic1Id);
		taskManager.createSubtask("Упаковать посуду", "Аккуратно упаковать хрупкие предметы", epic1Id);

		// Создание эпика с одной подзадачей
		int epic2Id = taskManager.createEpic("Важный эпик 2", "Описание второго эпика");
		taskManager.createSubtask("Сказать слова прощания", "Попрощаться с соседями", epic2Id);

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
		Task task1 = taskManager.getTask(task1Id);
		task1.setStatus(TaskStatus.IN_PROGRESS);
		taskManager.updateTask(task1);
		System.out.println("Изменили статус задачи 1 на IN_PROGRESS: " + taskManager.getTask(task1Id));

		// Получаем подзадачи первого эпика
		List<Subtask> epic1Subtasks = taskManager.getEpicSubtasks(epic1Id);

		// Изменяем статус первой подзадачи в первом эпике
		if (!epic1Subtasks.isEmpty()) {
			Subtask subtask1 = epic1Subtasks.getFirst();
			subtask1.setStatus(TaskStatus.DONE);
			taskManager.updateSubtask(subtask1);
			System.out.println("Изменили статус подзадачи 1 на DONE: " + subtask1);
			System.out.println("Эпик 1 теперь: " + taskManager.getEpic(epic1Id));
		}

		// Изменяем статус второй подзадачи в первом эпике
		if (epic1Subtasks.size() > 1) {
			Subtask subtask2 = epic1Subtasks.get(1);
			subtask2.setStatus(TaskStatus.IN_PROGRESS);
			taskManager.updateSubtask(subtask2);
			System.out.println("Изменили статус подзадачи 2 на IN_PROGRESS: " + subtask2);
			System.out.println("Эпик 1 теперь: " + taskManager.getEpic(epic1Id));

			// Завершаем вторую подзадачу первого эпика
			subtask2.setStatus(TaskStatus.DONE);
			taskManager.updateSubtask(subtask2);
			System.out.println("Завершили все подзадачи эпика 1");
			System.out.println("Эпик 1 теперь: " + taskManager.getEpic(epic1Id));
		}

		// Изменяем статус подзадачи во втором эпике
		List<Subtask> epic2Subtasks = taskManager.getEpicSubtasks(epic2Id);
		if (!epic2Subtasks.isEmpty()) {
			Subtask subtask3 = epic2Subtasks.getFirst();
			subtask3.setStatus(TaskStatus.DONE);
			taskManager.updateSubtask(subtask3);
			System.out.println("Завершили подзадачу эпика 2");
			System.out.println("Эпик 2 теперь: " + taskManager.getEpic(epic2Id));
		}

		// Удаление одной задачи и одного эпика
		System.out.println("\n=== УДАЛЕНИЕ ===");
		System.out.println("Удаляем задачу 2 (id=" + task2Id + ")");
		taskManager.deleteTask(task2Id);

		System.out.println("Удаляем эпик 1 (id=" + epic1Id + ") вместе с подзадачами");
		taskManager.deleteEpic(epic1Id);

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