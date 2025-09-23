package main.ru.practicum.kanban.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import main.ru.practicum.kanban.manager.Managers;
import main.ru.practicum.kanban.manager.TaskManager;
import main.ru.practicum.kanban.server.handler.*;
import main.ru.practicum.kanban.server.util.JsonUtils;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * HTTP сервер для работы с задачами через REST API
 */
public class HttpTaskServer {
    private static final int PORT = 8080;
    private final TaskManager taskManager;
    private HttpServer httpServer;

    public HttpTaskServer() {
        this.taskManager = Managers.getDefault();
    }

    public HttpTaskServer(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    /**
     * Возвращает экземпляр Gson для тестирования
     */
    public static Gson getGson() {
        return JsonUtils.createConfiguredGson(); // Используем настроенный Gson с TypeAdapter'ами
    }

    /**
     * Главный метод для запуска сервера
     */
    public static void main(String[] args) {
        HttpTaskServer server = new HttpTaskServer();
        try {
            server.start();
            // Сервер будет работать до завершения программы
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (IOException e) {
            System.err.println("Ошибка запуска сервера: " + e.getMessage());
        }
    }

    /**
     * Запускает HTTP сервер
     *
     * @throws IOException при ошибке запуска сервера
     */
    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Настраиваем обработчики для базовых путей
        httpServer.createContext("/tasks", new TaskHandler(taskManager));
        httpServer.createContext("/subtasks", new SubtaskHandler(taskManager));
        httpServer.createContext("/epics", new EpicHandler(taskManager));
        httpServer.createContext("/history", new HistoryHandler(taskManager));
        httpServer.createContext("/prioritized", new PrioritizedHandler(taskManager));

        httpServer.start();
        System.out.println("HTTP сервер запущен на порту " + PORT);
    }

    /**
     * Останавливает HTTP сервер
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            System.out.println("HTTP сервер остановлен");
        }
    }

    /**
     * Возвращает экземпляр TaskManager, используемый сервером
     */
    public TaskManager getTaskManager() {
        return taskManager;
    }
}