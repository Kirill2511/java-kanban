package server;

import com.google.gson.Gson;
import main.ru.practicum.kanban.manager.InMemoryTaskManager;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.model.Subtask;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HttpTaskManagerSubtasksTest {

    TaskManager manager = new InMemoryTaskManager();
    HttpTaskServer taskServer = new HttpTaskServer(manager);
    Gson gson = HttpTaskServer.getGson();

    public HttpTaskManagerSubtasksTest() throws IOException {
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
    public void testAddSubtask() throws IOException, InterruptedException {
        // сначала создаём эпик
        int epicId = manager.createEpic("Тестовый эпик", "Описание эпика");

        // создаём подзадачу
        Subtask subtask = new Subtask("Тестовая подзадача", "Описание подзадачи", epicId);
        String subtaskJson = gson.toJson(subtask);

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/subtasks");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(subtaskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());

        List<Subtask> subtasksFromManager = manager.getAllSubtasks();
        assertNotNull(subtasksFromManager);
        assertEquals(1, subtasksFromManager.size());
        assertEquals("Тестовая подзадача", subtasksFromManager.get(0).getName());
        assertEquals(epicId, subtasksFromManager.get(0).getEpicId());
    }

    @Test
    public void testGetSubtasks() throws IOException, InterruptedException {
        // создаём эпик и подзадачи
        int epicId = manager.createEpic("Тестовый эпик", "Описание эпика");
        manager.createSubtask("Подзадача 1", "Описание 1", epicId);
        manager.createSubtask("Подзадача 2", "Описание 2", epicId);

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/subtasks");
        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String responseBody = response.body();
        assertTrue(responseBody.contains("Подзадача 1"));
        assertTrue(responseBody.contains("Подзадача 2"));
    }

    @Test
    public void testGetSubtaskById() throws IOException, InterruptedException {
        // создаём эпик и подзадачу
        int epicId = manager.createEpic("Тестовый эпик", "Описание эпика");
        manager.createSubtask("Тестовая подзадача", "Описание подзадачи", epicId);

        // получаем ID подзадачи
        List<Subtask> subtasks = manager.getAllSubtasks();
        int subtaskId = subtasks.get(0).getId();

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/subtasks/" + subtaskId);
        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String responseBody = response.body();
        assertTrue(responseBody.contains("Тестовая подзадача"));
        assertTrue(responseBody.contains("Описание подзадачи"));
    }

    @Test
    public void testGetSubtaskByIdNotFound() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/subtasks/999");
        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    public void testUpdateSubtask() throws IOException, InterruptedException {
        // создаём эпик и подзадачу
        int epicId = manager.createEpic("Тестовый эпик", "Описание эпика");
        manager.createSubtask("Исходная подзадача", "Исходное описание", epicId);

        // получаем подзадачу
        List<Subtask> subtasks = manager.getAllSubtasks();
        Subtask subtask = subtasks.get(0);

        // обновляем подзадачу
        subtask.setName("Обновленная подзадача");
        subtask.setDescription("Обновленное описание");
        subtask.setStatus(TaskStatus.DONE);

        String subtaskJson = gson.toJson(subtask);

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/subtasks");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(subtaskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());

        var updatedSubtaskOpt = manager.getSubtask(subtask.getId());
        assertTrue(updatedSubtaskOpt.isPresent());
        Subtask updatedSubtask = updatedSubtaskOpt.get();
        assertEquals("Обновленная подзадача", updatedSubtask.getName());
        assertEquals("Обновленное описание", updatedSubtask.getDescription());
        assertEquals(TaskStatus.DONE, updatedSubtask.getStatus());
    }

    @Test
    public void testDeleteSubtask() throws IOException, InterruptedException {
        // создаём эпик и подзадачу
        int epicId = manager.createEpic("Тестовый эпик", "Описание эпика");
        manager.createSubtask("Подзадача для удаления", "Описание", epicId);

        // получаем ID подзадачи
        List<Subtask> subtasks = manager.getAllSubtasks();
        int subtaskId = subtasks.get(0).getId();

        // проверяем, что подзадача существует
        assertTrue(manager.getSubtask(subtaskId).isPresent());

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/subtasks/" + subtaskId);
        HttpRequest request = HttpRequest.newBuilder().uri(url).DELETE().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        // проверяем, что подзадача удалена
        assertTrue(manager.getSubtask(subtaskId).isEmpty());
    }

    @Test
    public void testDeleteSubtaskNotFound() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/subtasks/999");
        HttpRequest request = HttpRequest.newBuilder().uri(url).DELETE().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    public void testAddSubtaskWithoutEpicId() throws IOException, InterruptedException {
        // пытаемся создать подзадачу без epicId
        Subtask subtask = new Subtask("Подзадача", "Описание", 0);
        String subtaskJson = gson.toJson(subtask);

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/subtasks");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(subtaskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    @Test
    public void testAddSubtaskWithNonExistentEpic() throws IOException, InterruptedException {
        // пытаемся создать подзадачу для несуществующего эпика
        Subtask subtask = new Subtask("Подзадача", "Описание", 999);
        String subtaskJson = gson.toJson(subtask);

        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/subtasks");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(subtaskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }
}