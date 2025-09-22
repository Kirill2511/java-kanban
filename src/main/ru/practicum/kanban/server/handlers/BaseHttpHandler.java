package main.ru.practicum.kanban.server.handlers;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Базовый класс для HTTP обработчиков с общими методами для отправки ответов
 */
public class BaseHttpHandler {

    /**
     * Отправляет успешный ответ с данными в формате JSON
     *
     * @param h    HTTP обмен
     * @param text JSON строка для отправки
     * @throws IOException при ошибке отправки
     */
    protected void sendText(HttpExchange h, String text) throws IOException {
        byte[] resp = text.getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(200, resp.length);
        h.getResponseBody().write(resp);
        h.close();
    }

    /**
     * Отправляет успешный ответ без данных (HTTP 200)
     *
     * @param h HTTP обмен
     * @throws IOException при ошибке отправки
     */
    protected void sendText(HttpExchange h) throws IOException {
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(200, 0);
        h.close();
    }

    /**
     * Отправляет ответ 201 (Created) при успешном создании/обновлении
     *
     * @param h HTTP обмен
     * @throws IOException при ошибке отправки
     */
    protected void sendCreated(HttpExchange h) throws IOException {
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(201, 0);
        h.close();
    }

    /**
     * Отправляет ответ 404 (Not Found) когда объект не найден
     *
     * @param h HTTP обмен
     * @throws IOException при ошибке отправки
     */
    protected void sendNotFound(HttpExchange h) throws IOException {
        String response = "{\"error\":\"Ресурс не найден\"}";
        byte[] resp = response.getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(404, resp.length);
        h.getResponseBody().write(resp);
        h.close();
    }

    /**
     * Отправляет ответ 406 (Not Acceptable) при пересечении задач
     *
     * @param h HTTP обмен
     * @throws IOException при ошибке отправки
     */
    protected void sendHasOverlaps(HttpExchange h) throws IOException {
        String response = "{\"error\":\"Задача пересекается с существующими задачами\"}";
        byte[] resp = response.getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(406, resp.length);
        h.getResponseBody().write(resp);
        h.close();
    }

    /**
     * Отправляет ответ 500 (Internal Server Error) при внутренней ошибке
     *
     * @param h       HTTP обмен
     * @param message сообщение об ошибке
     * @throws IOException при ошибке отправки
     */
    protected void sendInternalError(HttpExchange h, String message) throws IOException {
        String response = "{\"error\":\"Внутренняя ошибка сервера: " + message + "\"}";
        byte[] resp = response.getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(500, resp.length);
        h.getResponseBody().write(resp);
        h.close();
    }

    /**
     * Отправляет ответ 400 (Bad Request) при некорректном запросе
     *
     * @param h       HTTP обмен
     * @param message сообщение об ошибке
     * @throws IOException при ошибке отправки
     */
    protected void sendBadRequest(HttpExchange h, String message) throws IOException {
        String response = "{\"error\":\"Некорректный запрос: " + message + "\"}";
        byte[] resp = response.getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(400, resp.length);
        h.getResponseBody().write(resp);
        h.close();
    }

    /**
     * Читает тело HTTP запроса
     *
     * @param h HTTP обмен
     * @return строка с телом запроса
     * @throws IOException при ошибке чтения
     */
    protected String readRequestBody(HttpExchange h) throws IOException {
        return new String(h.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Извлекает ID из пути запроса (например, из /tasks/123 извлекает 123)
     *
     * @param path путь запроса
     * @return ID или -1 если не найден
     */
    protected int extractIdFromPath(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 3) {
            try {
                return Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}