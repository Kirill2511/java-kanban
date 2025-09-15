package server;

import com.google.gson.Gson;
import main.ru.practicum.kanban.manager.InMemoryTaskManager;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.model.Task;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpTaskManagerHistoryAndPriorityTest {

    TaskManager manager = new InMemoryTaskManager();
    HttpTaskServer taskServer = new HttpTaskServer(manager);
    Gson gson = HttpTaskServer.getGson();

    public HttpTaskManagerHistoryAndPriorityTest() throws IOException {
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
    public void testGetHistory() throws IOException, InterruptedException {
        // создаём задачи
        int task1Id = manager.createTask("Задача 1", "Описание 1");
        int task2Id = manager.createTask("Задача 2", "Описание 2");
        int epicId = manager.createEpic("Эпик", "Описание эпика");

        // просматриваем задачи, чтобы они попали в историю
        manager.getTask(task1Id);
        manager.getTask(task2Id);
        manager.getEpic(epicId);

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/history");
        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String responseBody = response.body();
        assertTrue(responseBody.contains("Задача 1"));
        assertTrue(responseBody.contains("Задача 2"));
        assertTrue(responseBody.contains("Эпик"));
    }

    @Test
    public void testGetEmptyHistory() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/history");
        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String responseBody = response.body();
        assertEquals("[]", responseBody.trim());
    }

    @Test
    public void testGetPrioritizedTasks() throws IOException, InterruptedException {
        // создаём задачи с разным временем начала
        int task1Id = manager.createTask("Задача 1", "Описание 1");
        int task2Id = manager.createTask("Задача 2", "Описание 2");

        // обновляем задачи, добавляя время
        var task1Opt = manager.getTask(task1Id);
        var task2Opt = manager.getTask(task2Id);

        assertTrue(task1Opt.isPresent());
        assertTrue(task2Opt.isPresent());

        Task task1 = task1Opt.get();
        Task task2 = task2Opt.get();

        // используем фиксированное базовое время для избежания пересечений
        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 15, 10, 0);
        
        // задача 2 начинается раньше задачи 1, без пересечений
        task1.setStartTime(baseTime.plusHours(2)); // 12:00-13:00
        task1.setDuration(Duration.ofHours(1));
        task2.setStartTime(baseTime); // 10:00-11:00
        task2.setDuration(Duration.ofHours(1));

        manager.updateTask(task1);
        manager.updateTask(task2);

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/prioritized");
        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String responseBody = response.body();
        // проверяем, что задачи есть в ответе
        assertTrue(responseBody.contains("Задача 1"));
        assertTrue(responseBody.contains("Задача 2"));

        // проверяем порядок: задача 2 должна быть раньше задачи 1
        int task1Index = responseBody.indexOf("Задача 1");
        int task2Index = responseBody.indexOf("Задача 2");
        assertTrue(task2Index < task1Index, "Задача 2 должна быть раньше в списке приоритетов");
    }

    @Test
    public void testGetEmptyPrioritizedTasks() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/prioritized");
        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String responseBody = response.body();
        assertEquals("[]", responseBody.trim());
    }

    @Test
    public void testHistoryInvalidPath() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/history/invalid");
        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    @Test
    public void testPrioritizedInvalidPath() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/prioritized/invalid");
        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    @Test
    public void testHistoryWithUnsupportedMethod() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/history");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    @Test
    public void testPrioritizedWithUnsupportedMethod() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/prioritized");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }
}