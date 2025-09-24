package main.ru.practicum.kanban.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.model.Task;
import main.ru.practicum.kanban.server.util.JsonUtils;

import java.io.IOException;
import java.util.List;

/**
 * HTTP обработчик для работы с историей просмотров
 * Поддерживает операции: GET /history
 */
public class HistoryHandler extends BaseHttpHandler implements HttpHandler {

    private final TaskManager taskManager;

    public HistoryHandler(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method)) {
                handleGet(exchange, path);
            } else {
                sendBadRequest(exchange, "Метод " + method + " не поддерживается для /history");
            }
        } catch (Exception e) {
            sendInternalError(exchange, e.getMessage());
        }
    }

    /**
     * Обрабатывает GET запросы
     * GET /history - получить историю просмотров
     */
    private void handleGet(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/history") || path.equals("/history/")) {
            List<Task> history = taskManager.getHistory();
            String json = JsonUtils.toJson(history);
            sendText(exchange, json);
        } else {
            sendBadRequest(exchange, "Некорректный путь для /history");
        }
    }
}