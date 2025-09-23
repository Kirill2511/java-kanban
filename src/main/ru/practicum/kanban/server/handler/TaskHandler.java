package main.ru.practicum.kanban.server.handler;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import main.ru.practicum.kanban.exception.NotFoundException;
import main.ru.practicum.kanban.exception.TaskValidationException;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.server.util.JsonUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * HTTP обработчик для работы с задачами
 * Поддерживает операции: GET /tasks, GET /tasks/{id}, POST /tasks, DELETE
 * /tasks/{id}
 */
public class TaskHandler extends BaseHttpHandler implements HttpHandler {

    private final TaskManager taskManager;

    public TaskHandler(TaskManager taskManager) {
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
     * GET /tasks - получить все задачи
     * GET /tasks/{id} - получить задачу по ID
     */
    private void handleGet(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/tasks") || path.equals("/tasks/")) {
            // Получить все задачи
            List<Task> tasks = taskManager.getAllTasks();
            String json = JsonUtils.toJson(tasks);
            sendText(exchange, json);
        } else {
            // Получить задачу по ID
            int id = extractIdFromPath(path);
            if (id == -1) {
                sendBadRequest(exchange, "Некорректный ID задачи");
                return;
            }

            Optional<Task> taskOpt = taskManager.getTask(id);
            if (taskOpt.isPresent()) {
                String json = JsonUtils.toJson(taskOpt.get());
                sendText(exchange, json);
            } else {
                throw new NotFoundException("Задача с ID " + id + " не найдена");
            }
        }
    }

    /**
     * Обрабатывает POST запросы для создания/обновления задач
     * POST /tasks - создать или обновить задачу
     */
    private void handlePost(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);

        if (body.isEmpty()) {
            sendBadRequest(exchange, "Тело запроса не может быть пустым");
            return;
        }

        try {
            Task task = JsonUtils.fromJson(body, Task.class);

            if (task.getId() == 0) {
                // Создание новой задачи

                // Сначала проверяем на пересечения, если у задачи есть время
                if (task.getStartTime() != null && task.getDuration() != null) {
                    if (taskManager.hasTimeConflict(task)) {
                        throw new TaskValidationException("Задача пересекается по времени с существующими задачами");
                    }
                }

                int taskId = taskManager.createTask(task.getName(), task.getDescription());

                // Если есть дополнительные поля (время, длительность), обновляем задачу
                if (task.getStartTime() != null || task.getDuration() != null || task.getStatus() != null) {
                    Optional<Task> createdTaskOpt = taskManager.getTask(taskId);
                    if (createdTaskOpt.isPresent()) {
                        Task createdTask = createdTaskOpt.get();
                        if (task.getStartTime() != null) {
                            createdTask.setStartTime(task.getStartTime());
                        }
                        if (task.getDuration() != null) {
                            createdTask.setDuration(task.getDuration());
                        }
                        if (task.getStatus() != null) {
                            createdTask.setStatus(task.getStatus());
                        }
                        // Поскольку мы уже проверили пересечения, просто обновляем
                        taskManager.updateTask(createdTask);
                    }
                }

                sendCreated(exchange);
            } else {
                // Обновление существующей задачи
                Optional<Task> existingTaskOpt = taskManager.getTask(task.getId());
                if (existingTaskOpt.isEmpty()) {
                    throw new NotFoundException("Задача с ID " + task.getId() + " не найдена");
                }

                taskManager.updateTask(task);
                sendCreated(exchange);
            }
        } catch (JsonSyntaxException e) {
            sendBadRequest(exchange, "Некорректный JSON: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает DELETE запросы для удаления задач
     * DELETE /tasks/{id} - удалить задачу по ID
     */
    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        int id = extractIdFromPath(path);
        if (id == -1) {
            sendBadRequest(exchange, "Некорректный ID задачи");
            return;
        }

        Optional<Task> taskOpt = taskManager.getTask(id);
        if (taskOpt.isEmpty()) {
            throw new NotFoundException("Задача с ID " + id + " не найдена");
        }

        taskManager.deleteTask(id);
        sendText(exchange); // 200 для успешного удаления
    }
}