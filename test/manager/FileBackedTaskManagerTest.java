package manager;

import main.ru.practicum.kanban.manager.FileBackedTaskManager;
import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.model.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileBackedTaskManagerTest {

    private File tempFile;
    private FileBackedTaskManager manager;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("kanban_test", ".csv").toFile();
        manager = new FileBackedTaskManager(tempFile);
    }

    @AfterEach
    void tearDown() {
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    void shouldSaveAndLoadEmptyFile() {
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        assertTrue(loadedManager.getAllTasks().isEmpty());
        assertTrue(loadedManager.getAllEpics().isEmpty());
        assertTrue(loadedManager.getAllSubtasks().isEmpty());
        assertTrue(loadedManager.getHistory().isEmpty());
    }

    @Test
    void shouldLoadFromNonExistentFile() {
        File nonExistentFile = new File("non_existent_file.csv");
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(nonExistentFile);

        assertTrue(loadedManager.getAllTasks().isEmpty());
        assertTrue(loadedManager.getAllEpics().isEmpty());
        assertTrue(loadedManager.getAllSubtasks().isEmpty());
        assertTrue(loadedManager.getHistory().isEmpty());
    }

    @Test
    void shouldSaveAndLoadTasksEpicsSubtasks() {
        // Создаем тестовые данные
        int taskId = manager.createTask("Задача 1", "Описание задачи 1");
        int epicId = manager.createEpic("Эпик 1", "Описание эпика 1");
        manager.createSubtask("Подзадача 1", "Описание подзадачи 1", epicId);

        // Получаем задачи для добавления в историю
        manager.getTask(taskId);
        manager.getEpic(epicId);

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        // Проверяем задачи
        List<Task> loadedTasks = loadedManager.getAllTasks();
        assertEquals(1, loadedTasks.size());
        assertEquals("Задача 1", loadedTasks.getFirst().getName());
        assertEquals("Описание задачи 1", loadedTasks.getFirst().getDescription());

        // Проверяем эпики
        List<Epic> loadedEpics = loadedManager.getAllEpics();
        assertEquals(1, loadedEpics.size());
        assertEquals("Эпик 1", loadedEpics.getFirst().getName());
        assertEquals("Описание эпика 1", loadedEpics.getFirst().getDescription());

        // Проверяем подзадачи
        List<Subtask> loadedSubtasks = loadedManager.getAllSubtasks();
        assertEquals(1, loadedSubtasks.size());
        assertEquals("Подзадача 1", loadedSubtasks.getFirst().getName());
        assertEquals("Описание подзадачи 1", loadedSubtasks.getFirst().getDescription());
        assertEquals(epicId, loadedSubtasks.getFirst().getEpicId());
    }

    @Test
    void shouldHandleSpecialCharactersInCsv() {
        // Создаем задачу с специальными символами
        manager.createTask("Задача с \"кавычками\"", "Описание с запятой, и кавычками \"test\"");

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        List<Task> loadedTasks = loadedManager.getAllTasks();
        assertEquals(1, loadedTasks.size());
        assertEquals("Задача с \"кавычками\"", loadedTasks.getFirst().getName());
        assertEquals("Описание с запятой, и кавычками \"test\"", loadedTasks.getFirst().getDescription());
    }

    @Test
    void shouldUpdateTasksAutomatically() {
        // Создаем задачу
        int taskId = manager.createTask("Задача", "Описание");

        // Обновляем задачу
        Task task = manager.getTask(taskId);
        task.setStatus(TaskStatus.IN_PROGRESS);
        manager.updateTask(task);

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        Task loadedTask = loadedManager.getTask(taskId);
        assertEquals(TaskStatus.IN_PROGRESS, loadedTask.getStatus());
    }

    @Test
    void shouldDeleteTasksAndSave() {
        // Создаем несколько задач
        int taskId1 = manager.createTask("Задача 1", "Описание 1");
        int taskId2 = manager.createTask("Задача 2", "Описание 2");

        // Удаляем одну задачу
        manager.deleteTask(taskId1);

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        List<Task> loadedTasks = loadedManager.getAllTasks();
        assertEquals(1, loadedTasks.size());
        assertEquals("Задача 2", loadedTasks.getFirst().getName());
        assertEquals(taskId2, loadedTasks.getFirst().getId());
    }

    @Test
    void shouldSaveMultipleTasksWithDifferentStatuses() {
        // Создаем различные типы задач
        int taskId = manager.createTask("Тестовая задача", "Описание задачи");
        int epicId = manager.createEpic("Тестовый эпик", "Описание эпика");
        manager.createSubtask("Тестовая подзадача", "Описание подзадачи", epicId);

        // Обновляем статусы
        Task task = manager.getTask(taskId);
        task.setStatus(TaskStatus.IN_PROGRESS);
        manager.updateTask(task);

        // Добавляем в историю
        manager.getEpic(epicId);

        // Проверяем, что файл существует и не пустой
        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);

        // Загружаем и проверяем данные
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        assertEquals(1, loadedManager.getAllTasks().size());
        assertEquals(1, loadedManager.getAllEpics().size());
        assertEquals(1, loadedManager.getAllSubtasks().size());

        // Проверяем статус
        Task loadedTask = loadedManager.getTask(taskId);
        assertEquals(TaskStatus.IN_PROGRESS, loadedTask.getStatus());
    }

    @Test
    void shouldLoadMultipleTasksWithComplexData() throws IOException {
        // Создаем сложную структуру данных
        int taskId = manager.createTask("Задача для загрузки", "Описание");
        int epicId = manager.createEpic("Эпик для загрузки", "Описание эпика");
        manager.createSubtask("Подзадача 1", "Первая подзадача", epicId);
        manager.createSubtask("Подзадача 2", "Вторая подзадача", epicId);

        // Обновляем статусы
        Task task = manager.getTask(taskId);
        task.setStatus(TaskStatus.DONE);
        manager.updateTask(task);

        List<Subtask> subtasks = manager.getAllSubtasks();
        if (!subtasks.isEmpty()) {
            Subtask subtask = subtasks.getFirst();
            subtask.setStatus(TaskStatus.IN_PROGRESS);
            manager.updateSubtask(subtask);
        }

        // Добавляем в историю
        manager.getTask(taskId);
        manager.getEpic(epicId);
        for (Subtask subtask : subtasks) {
            manager.getSubtask(subtask.getId());
        }

        // Создаем новый временный файл и загружаем данные
        File newTempFile = Files.createTempFile("test_load_complex", ".csv").toFile();
        try {
            // Копируем данные
            FileBackedTaskManager newManager = FileBackedTaskManager.loadFromFile(tempFile);

            // Проверяем количество задач
            assertEquals(1, newManager.getAllTasks().size());
            assertEquals(1, newManager.getAllEpics().size());
            assertEquals(2, newManager.getAllSubtasks().size());
            assertFalse(newManager.getHistory().isEmpty());

            // Проверяем содержимое
            Task loadedTask = newManager.getTask(taskId);
            Epic loadedEpic = newManager.getEpic(epicId);

            assertNotNull(loadedTask);
            assertEquals("Задача для загрузки", loadedTask.getName());
            assertEquals(TaskStatus.DONE, loadedTask.getStatus());

            assertNotNull(loadedEpic);
            assertEquals("Эпик для загрузки", loadedEpic.getName());

            // Проверяем связи эпик-подзадача
            List<Subtask> epicSubtasks = newManager.getEpicSubtasks(epicId);
            assertEquals(2, epicSubtasks.size());

        } finally {
            if (newTempFile.exists()) {
                newTempFile.delete();
            }
        }
    }

    @Test
    void shouldHandleFileOperationsCorrectly() {
        // Создаем задачу
        int taskId = manager.createTask("Задача для удаления", "Описание");

        // Проверяем, что файл обновляется при создании
        long sizeAfterCreate = tempFile.length();
        assertTrue(sizeAfterCreate > 0, "Файл должен быть не пустым после создания задачи");

        // Удаляем задачу
        manager.deleteTask(taskId);

        // Проверяем, что файл обновляется при удалении
        long sizeAfterDelete = tempFile.length();

        // Загружаем менеджер заново
        FileBackedTaskManager newManager = FileBackedTaskManager.loadFromFile(tempFile);

        assertTrue(newManager.getAllTasks().isEmpty(), "Задача должна быть удалена");
        assertTrue(tempFile.exists(), "Файл должен существовать");
        assertNotEquals(sizeAfterCreate, sizeAfterDelete, "Размер файла должен измениться после удаления");
    }

    @Test
    void shouldPreserveTaskIdsAfterReload() {
        // Создаем задачи с определенными ID
        int taskId1 = manager.createTask("Задача 1", "Описание 1");
        int taskId2 = manager.createTask("Задача 2", "Описание 2");
        int epicId = manager.createEpic("Эпик", "Описание эпика");

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        // Проверяем, что ID сохранились
        assertNotNull(loadedManager.getTask(taskId1));
        assertNotNull(loadedManager.getTask(taskId2));
        assertNotNull(loadedManager.getEpic(epicId));

        assertEquals("Задача 1", loadedManager.getTask(taskId1).getName());
        assertEquals("Задача 2", loadedManager.getTask(taskId2).getName());
        assertEquals("Эпик", loadedManager.getEpic(epicId).getName());
    }

    @Test
    void shouldHandleEmptyStringsInTaskData() {
        // Создаем задачу с пустым описанием
        int taskId = manager.createTask("Задача с пустым описанием", "");

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        Task loadedTask = loadedManager.getTask(taskId);
        assertNotNull(loadedTask);
        assertEquals("Задача с пустым описанием", loadedTask.getName());
        assertEquals("", loadedTask.getDescription());
    }
}
