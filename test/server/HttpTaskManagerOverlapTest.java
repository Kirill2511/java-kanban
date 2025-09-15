package server;

import com.google.gson.Gson;
import main.ru.practicum.kanban.manager.InMemoryTaskManager;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.model.TaskStatus;
import main.ru.practicum.kanban.server.HttpTaskServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpTaskManagerOverlapTest {

    TaskManager manager = new InMemoryTaskManager();
    HttpTaskServer taskServer = new HttpTaskServer(manager);
    Gson gson = HttpTaskServer.getGson();

    public HttpTaskManagerOverlapTest() throws IOException {
    }

    @BeforeEach
    public void setUp() throws IOException {
        manager.deleteAllTasks();
        manager.deleteAllSubtasks();
        manager.deleteAllEpics();
        taskServer.start();
    }

    @AfterEach
    public void shutDown() {
        taskServer.stop();
    }

    @Test
    public void testTaskOverlapDetection() throws IOException, InterruptedException {
        // создаём первую задачу с временными рамками
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setStartTime(startTime);
        task1.setDuration(Duration.ofHours(2));
        task1.setStatus(TaskStatus.NEW);

        // добавляем первую задачу
        String task1Json = gson.toJson(task1);

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/tasks");
        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(task1Json))
                .build();

        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response1.statusCode());

        // пытаемся создать вторую задачу, которая пересекается по времени
        Task task2 = new Task("Задача 2", "Описание 2");
        task2.setStartTime(startTime.plusMinutes(30)); // пересекается с первой задачей
        task2.setDuration(Duration.ofHours(1));
        task2.setStatus(TaskStatus.NEW);

        String task2Json = gson.toJson(task2);

        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(task2Json))
                .build();

        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());

        // ожидаем код 406 (Not Acceptable) из-за пересечения
        assertEquals(406, response2.statusCode());

        // проверяем, что в системе только одна задача
        assertEquals(1, manager.getAllTasks().size());
    }

    @Test
    public void testTaskNoOverlapWithDifferentTimes() throws IOException, InterruptedException {
        // создаём первую задачу
        LocalDateTime startTime1 = LocalDateTime.now().plusHours(1);
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setStartTime(startTime1);
        task1.setDuration(Duration.ofHours(1));
        task1.setStatus(TaskStatus.NEW);

        String task1Json = gson.toJson(task1);

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/tasks");
        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(task1Json))
                .build();

        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response1.statusCode());

        // создаём вторую задачу, которая НЕ пересекается по времени
        LocalDateTime startTime2 = startTime1.plusHours(2); // начинается после окончания первой
        Task task2 = new Task("Задача 2", "Описание 2");
        task2.setStartTime(startTime2);
        task2.setDuration(Duration.ofHours(1));
        task2.setStatus(TaskStatus.NEW);

        String task2Json = gson.toJson(task2);

        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(task2Json))
                .build();

        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());

        // ожидаем успешное создание
        assertEquals(201, response2.statusCode());

        // проверяем, что в системе две задачи
        assertEquals(2, manager.getAllTasks().size());
    }

    @Test
    public void testTaskWithoutTimeDoesNotCauseOverlap() throws IOException, InterruptedException {
        // создаём задачу с временными рамками
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setStartTime(startTime);
        task1.setDuration(Duration.ofHours(2));
        task1.setStatus(TaskStatus.NEW);

        String task1Json = gson.toJson(task1);

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/tasks");
        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(task1Json))
                .build();

        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response1.statusCode());

        // создаём задачу без времени
        Task task2 = new Task("Задача 2", "Описание 2");
        task2.setStatus(TaskStatus.NEW);
        // не устанавливаем startTime и duration

        String task2Json = gson.toJson(task2);

        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(task2Json))
                .build();

        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());

        // ожидаем успешное создание (задача без времени не вызывает пересечения)
        assertEquals(201, response2.statusCode());

        // проверяем, что в системе две задачи
        assertEquals(2, manager.getAllTasks().size());
    }

    @Test
    public void testSubtaskOverlapDetection() throws IOException, InterruptedException {
        // создаём эпик
        int epicId = manager.createEpic("Тестовый эпик", "Описание эпика");

        // создаём подзадачу с временными рамками
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);

        // Сначала создаем подзадачу через менеджер, потом попробуем создать пересекающуюся через API
        manager.createSubtask("Подзадача 1", "Описание 1", epicId);
        var subtasks = manager.getAllSubtasks();
        var subtask1 = subtasks.get(0);
        subtask1.setStartTime(startTime);
        subtask1.setDuration(Duration.ofHours(2));
        manager.updateSubtask(subtask1);

        // пытаемся создать вторую подзадачу через API, которая пересекается по времени
        Task overlappingTask = new Task("Пересекающаяся задача", "Описание");
        overlappingTask.setStartTime(startTime.plusMinutes(30));
        overlappingTask.setDuration(Duration.ofHours(1));
        overlappingTask.setStatus(TaskStatus.NEW);

        String taskJson = gson.toJson(overlappingTask);

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/tasks");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // ожидаем код 406 из-за пересечения с подзадачей
        assertEquals(406, response.statusCode());
    }
}