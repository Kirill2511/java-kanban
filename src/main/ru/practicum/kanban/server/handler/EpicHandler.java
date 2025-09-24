package main.ru.practicum.kanban.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import main.ru.practicum.kanban.exception.NotFoundException;
import main.ru.practicum.kanban.exception.TaskValidationException;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.server.util.JsonUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * HTTP обработчик для работы с эпиками
 * Поддерживает операции: GET /epics, GET /epics/{id}, GET /epics/{id}/subtasks,
 * POST /epics, DELETE /epics/{id}
 */
public class EpicHandler extends BaseHttpHandler implements HttpHandler {

    private final TaskManager taskManager;

    public EpicHandler(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            switch (method) {
                case "GET":
                    handleGet(exchange, path);
                    break;
                case "POST":
                    handlePost(exchange);
                    break;
                case "DELETE":
                    handleDelete(exchange, path);
                    break;
                default:
                    sendBadRequest(exchange, "Метод " + method + " не поддерживается");
            }
        } catch (NotFoundException e) {
            sendNotFound(exchange);
        } catch (TaskValidationException e) {
            sendHasOverlaps(exchange);
        } catch (IllegalArgumentException e) {
            sendBadRequest(exchange, e.getMessage());
        } catch (Exception e) {
            sendInternalError(exchange, e.getMessage());
        }
    }

    /**
     * Обрабатывает GET запросы
     * GET /epics - получить все эпики
     * GET /epics/{id} - получить эпик по ID
     * GET /epics/{id}/subtasks - получить подзадачи эпика
     */
    private void handleGet(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/epics") || path.equals("/epics/")) {
            // Получить все эпики
            List<Epic> epics = taskManager.getAllEpics();
            String json = JsonUtils.toJson(epics);
            sendText(exchange, json);
        } else if (path.matches("/epics/\\d+/subtasks/?")) {
            // Получить подзадачи эпика: /epics/{id}/subtasks
            String[] parts = path.split("/");
            if (parts.length >= 3) {
                try {
                    int epicId = Integer.parseInt(parts[2]);

                    // Проверяем, что эпик существует
                    Optional<Epic> epicOpt = taskManager.getEpic(epicId);
                    if (epicOpt.isEmpty()) {
                        throw new NotFoundException("Эпик с ID " + epicId + " не найден");
                    }

                    List<Subtask> subtasks = taskManager.getEpicSubtasks(epicId);
                    String json = JsonUtils.toJson(subtasks);
                    sendText(exchange, json);
                } catch (NumberFormatException e) {
                    sendBadRequest(exchange, "Некорректный ID эпика");
                }
            } else {
                sendBadRequest(exchange, "Некорректный путь запроса");
            }
        } else {
            // Получить эпик по ID: /epics/{id}
            int id = extractIdFromPath(path);
            if (id == -1) {
                sendBadRequest(exchange, "Некорректный ID эпика");
                return;
            }

            Optional<Epic> epicOpt = taskManager.getEpic(id);
            if (epicOpt.isPresent()) {
                String json = JsonUtils.toJson(epicOpt.get());
                sendText(exchange, json);
            } else {
                throw new NotFoundException("Эпик с ID " + id + " не найден");
            }
        }
    }

    /**
     * Обрабатывает POST запросы для создания эпиков
     * POST /epics - создать новый эпик
     * Обновление эпиков не поддерживается, так как их статус рассчитывается
     * автоматически
     */
    private void handlePost(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);

        if (body.isEmpty()) {
            sendBadRequest(exchange, "Тело запроса не может быть пустым");
            return;
        }

        try {
            Epic epic = JsonUtils.fromJson(body, Epic.class);

            if (epic.getId() != 0) {
                // Эпики нельзя обновлять через API, только создавать
                sendBadRequest(exchange,
                        "Обновление эпиков не поддерживается. Используйте POST для создания нового эпика.");
                return;
            }

            // Создание нового эпика
            taskManager.createEpic(epic.getName(), epic.getDescription());
            sendCreated(exchange);

        } catch (com.google.gson.JsonSyntaxException e) {
            sendBadRequest(exchange, "Некорректный JSON: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает DELETE запросы для удаления эпиков
     * DELETE /epics/{id} - удалить эпик по ID
     */
    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        int id = extractIdFromPath(path);
        if (id == -1) {
            sendBadRequest(exchange, "Некорректный ID эпика");
            return;
        }

        Optional<Epic> epicOpt = taskManager.getEpic(id);
        if (epicOpt.isEmpty()) {
            throw new NotFoundException("Эпик с ID " + id + " не найден");
        }

        taskManager.deleteEpic(id);
        sendText(exchange); // 200 для успешного удаления
    }
}