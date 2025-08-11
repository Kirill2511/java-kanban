package main.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.model.Task;

import java.util.List;

public interface HistoryManager {
    void add(Task task);

    void remove(int id);

    List<Task> getHistory();
}