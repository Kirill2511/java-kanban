package server;

import com.google.gson.Gson;
import main.ru.practicum.kanban.manager.InMemoryTaskManager;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.server.HttpTaskServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HttpTaskManagerEpicsTest {

    private final TaskManager manager = new InMemoryTaskManager();
    private final HttpTaskServer taskServer = new HttpTaskServer(manager);
    private final Gson gson = HttpTaskServer.getGson();

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
    public void testAddEpic() throws IOException, InterruptedException {
        Epic epic = new Epic("Тестовый эпик", "Описание эпика");
        String epicJson = gson.toJson(epic);

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/epics");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(epicJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());

        List<Epic> epicsFromManager = manager.getAllEpics();
        assertNotNull(epicsFromManager);
        assertEquals(1, epicsFromManager.size());
        assertEquals("Тестовый эпик", epicsFromManager.get(0).getName());
    }

    @Test
    public void testGetEpics() throws IOException, InterruptedException {
        // создаём эпики напрямую через менеджер
        manager.createEpic("Эпик 1", "Описание 1");
        manager.createEpic("Эпик 2", "Описание 2");

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/epics");
        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String responseBody = response.body();
        assertTrue(responseBody.contains("Эпик 1"));
        assertTrue(responseBody.contains("Эпик 2"));
    }

    @Test
    public void testGetEpicById() throws IOException, InterruptedException {
        int epicId = manager.createEpic("Тестовый эпик", "Описание тестового эпика");

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/epics/" + epicId);
        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String responseBody = response.body();
        assertTrue(responseBody.contains("Тестовый эпик"));
        assertTrue(responseBody.contains("Описание тестового эпика"));
    }

    @Test
    public void testGetEpicByIdNotFound() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/epics/999");
        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    public void testGetEpicSubtasks() throws IOException, InterruptedException {
        // создаём эпик и подзадачи
        int epicId = manager.createEpic("Тестовый эпик", "Описание эпика");
        manager.createSubtask("Подзадача 1", "Описание 1", epicId);
        manager.createSubtask("Подзадача 2", "Описание 2", epicId);

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/epics/" + epicId + "/subtasks");
        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String responseBody = response.body();
        assertTrue(responseBody.contains("Подзадача 1"));
        assertTrue(responseBody.contains("Подзадача 2"));
    }

    @Test
    public void testGetEpicSubtasksForNonExistentEpic() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/epics/999/subtasks");
        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    public void testDeleteEpic() throws IOException, InterruptedException {
        // создаём эпик с подзадачами
        int epicId = manager.createEpic("Эпик для удаления", "Описание");
        manager.createSubtask("Подзадача 1", "Описание 1", epicId);
        manager.createSubtask("Подзадача 2", "Описание 2", epicId);

        // проверяем, что эпик и подзадачи существуют
        assertTrue(manager.getEpic(epicId).isPresent());
        assertEquals(2, manager.getAllSubtasks().size());

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/epics/" + epicId);
        HttpRequest request = HttpRequest.newBuilder().uri(url).DELETE().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        // проверяем, что эпик и его подзадачи удалены
        assertTrue(manager.getEpic(epicId).isEmpty());
        assertEquals(0, manager.getAllSubtasks().size());
    }

    @Test
    public void testDeleteEpicNotFound() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/epics/999");
        HttpRequest request = HttpRequest.newBuilder().uri(url).DELETE().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    public void testUpdateEpicShouldFail() throws IOException, InterruptedException {
        // создаём эпик
        int epicId = manager.createEpic("Исходный эпик", "Исходное описание");

        var epicOpt = manager.getEpic(epicId);
        assertTrue(epicOpt.isPresent());
        Epic epic = epicOpt.get();
        epic.setName("Обновленный эпик");

        String epicJson = gson.toJson(epic);

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/epics");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(epicJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // обновление эпиков не поддерживается
        assertEquals(400, response.statusCode());
    }

    @Test
    private void testAddEpicWithInvalidJson() throws IOException, InterruptedException {
        String invalidJson = "{\"name\": }";

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/epics");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    @Test
    public void testAddEpicWithEmptyBody() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/epics");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }
}