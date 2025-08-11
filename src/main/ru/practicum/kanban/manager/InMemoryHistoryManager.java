package main.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.model.Epic;
import main.ru.practicum.kanban.model.Subtask;
import main.ru.practicum.kanban.model.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryHistoryManager implements HistoryManager {

    // HashMap для быстрого доступа к узлам по id задачи
    private final Map<Integer, Node> nodeMap = new HashMap<>();
    // Головной и хвостовой узлы двусвязного списка
    private Node head;
    private Node tail;

    @Override
    public void add(Task task) {
        if (task == null) {
            return;
        }

        // Создаем копию задачи для сохранения в истории
        Task taskCopy;
        if (task instanceof Subtask) {
            taskCopy = new Subtask((Subtask) task);
        } else if (task instanceof Epic) {
            taskCopy = new Epic((Epic) task);
        } else {
            taskCopy = new Task(task);
        }

        // Если задача уже есть в истории, удаляем старый узел
        if (nodeMap.containsKey(task.getId())) {
            removeNode(nodeMap.get(task.getId()));
        }

        // Добавляем новую задачу в конец списка
        linkLast(taskCopy);
    }

    @Override
    public void remove(int id) {
        Node nodeToRemove = nodeMap.get(id);
        if (nodeToRemove != null) {
            removeNode(nodeToRemove);
        }
    }

    @Override
    public List<Task> getHistory() {
        return getTasks();
    }

    /**
     * Собирает все задачи из двусвязного списка в ArrayList
     */
    private List<Task> getTasks() {
        List<Task> tasks = new ArrayList<>();
        Node current = head;

        while (current != null) {
            tasks.add(current.task);
            current = current.next;
        }

        return tasks;
    }

    /**
     * Удаляет узел из двусвязного списка за O(1)
     */
    private void removeNode(Node node) {
        if (node == null) {
            return;
        }

        // Удаляем из HashMap
        nodeMap.remove(node.task.getId());

        // Корректируем связи в двусвязном списке
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            // Удаляемый узел был головой списка
            head = node.next;
        }

        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            // Удаляемый узел был хвостом списка
            tail = node.prev;
        }
    }

    /**
     * Добавляет задачу в конец двусвязного списка
     */
    private void linkLast(Task task) {
        Node newNode = new Node(task);

        if (tail == null) {
            // Список пустой - новый узел становится и головой, и хвостом
            head = newNode;
            tail = newNode;
        } else {
            // Добавляем узел в конец списка
            tail.next = newNode;
            newNode.prev = tail;
            tail = newNode;
        }

        // Обновляем HashMap
        nodeMap.put(task.getId(), newNode);
    }

    // Класс для узла двусвязного списка
    private static class Node {
        Task task;
        Node prev;
        Node next;

        Node(Task task) {
            this.task = task;
        }
    }
}
