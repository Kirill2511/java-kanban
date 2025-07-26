package main.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;

import java.util.ArrayList;
import java.util.List;

public class InMemoryHistoryManager implements HistoryManager {
    private final List<Task> history = new ArrayList<>();

    @Override
    public void add(Task task) {
        if (task != null) {
            // Создаем копию задачи для сохранения в истории
            Task taskCopy;
            if (task instanceof Subtask) {
                taskCopy = new Subtask((Subtask) task);
            } else if (task instanceof Epic) {
                taskCopy = new Epic((Epic) task);
            } else {
                taskCopy = new Task(task);
            }

            history.add(taskCopy);
            // Ограничиваем историю последними 10 просмотрами
            if (history.size() > 10) {
                history.removeFirst();
            }
        }
    }

    @Override
    public List<Task> getHistory() {
        return new ArrayList<>(history);
    }
}
