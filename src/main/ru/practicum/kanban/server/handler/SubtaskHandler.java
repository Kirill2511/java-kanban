package main.ru.practicum.kanban.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import main.ru.practicum.kanban.exception.NotFoundException;
import main.ru.practicum.kanban.exception.TaskValidationException;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.server.util.JsonUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * HTTP обработчик для работы с подзадачами
 * Поддерживает операции: GET /subtasks, GET /subtasks/{id}, POST /subtasks,
 * DELETE /subtasks/{id}
 */
public class SubtaskHandler extends BaseHttpHandler implements HttpHandler {

    private final TaskManager taskManager;

    public SubtaskHandler(TaskManager taskManager) {
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
     * GET /subtasks - получить все подзадачи
     * GET /subtasks/{id} - получить подзадачу по ID
     */
    private void handleGet(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/subtasks") || path.equals("/subtasks/")) {
            // Получить все подзадачи
            List<Subtask> subtasks = taskManager.getAllSubtasks();
            String json = JsonUtils.toJson(subtasks);
            sendText(exchange, json);
        } else {
            // Получить подзадачу по ID
            int id = extractIdFromPath(path);
            if (id == -1) {
                sendBadRequest(exchange, "Некорректный ID подзадачи");
                return;
            }

            Optional<Subtask> subtaskOpt = taskManager.getSubtask(id);
            if (subtaskOpt.isPresent()) {
                String json = JsonUtils.toJson(subtaskOpt.get());
                sendText(exchange, json);
            } else {
                throw new NotFoundException("Подзадача с ID " + id + " не найдена");
            }
        }
    }

    /**
     * Обрабатывает POST запросы для создания/обновления подзадач
     * POST /subtasks - создать или обновить подзадачу
     */
    private void handlePost(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);

        if (body.isEmpty()) {
            sendBadRequest(exchange, "Тело запроса не может быть пустым");
            return;
        }

        try {
            Subtask subtask = JsonUtils.fromJson(body, Subtask.class);

            if (subtask.getId() == 0) {
                // Создание новой подзадачи
                if (subtask.getEpicId() <= 0) {
                    sendBadRequest(exchange, "Для создания подзадачи требуется валидный ID эпика");
                    return;
                }

                taskManager.createSubtask(subtask.getName(), subtask.getDescription(), subtask.getEpicId());
                sendCreated(exchange);
            } else {
                // Обновление существующей подзадачи
                Optional<Subtask> existingSubtaskOpt = taskManager.getSubtask(subtask.getId());
                if (existingSubtaskOpt.isEmpty()) {
                    throw new NotFoundException("Подзадача с ID " + subtask.getId() + " не найдена");
                }

                taskManager.updateSubtask(subtask);
                sendCreated(exchange);
            }
        } catch (com.google.gson.JsonSyntaxException e) {
            sendBadRequest(exchange, "Некорректный JSON: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает DELETE запросы для удаления подзадач
     * DELETE /subtasks/{id} - удалить подзадачу по ID
     */
    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        int id = extractIdFromPath(path);
        if (id == -1) {
            sendBadRequest(exchange, "Некорректный ID подзадачи");
            return;
        }

        Optional<Subtask> subtaskOpt = taskManager.getSubtask(id);
        if (subtaskOpt.isEmpty()) {
            throw new NotFoundException("Подзадача с ID " + id + " не найдена");
        }

        taskManager.deleteSubtask(id);
        sendText(exchange); // 200 для успешного удаления
    }
}