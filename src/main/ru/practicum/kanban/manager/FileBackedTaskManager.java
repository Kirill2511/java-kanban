package main.ru.practicum.kanban.manager;

import main.ru.practicum.kanban.model.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final File file;

    public FileBackedTaskManager(File file) {
        this.file = file;
    }

    // Восстановление состояния менеджера из файла
    public static FileBackedTaskManager loadFromFile(File file) {
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        if (!file.exists()) {
            return manager;
        }

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String[] lines = content.split("\n");

            boolean isHistory = false;
            List<Integer> historyIds = new ArrayList<>();

            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();

                if (line.isEmpty()) {
                    isHistory = true;
                    continue;
                }

                if (isHistory) {
                    historyIds = historyFromString(line);
                    break;
                } else {
                    Task task = fromString(line);
                    if (task != null) {
                        manager.addTaskDirectly(task);
                    }
                }
            }

            // Восстанавливаем историю
            for (Integer id : historyIds) {
                Task task = manager.getTaskById(id);
                if (task != null) {
                    manager.addToHistoryDirectly(task);
                }
            }

            // Обновляем статусы и расчетные поля всех эпиков после загрузки
            for (Epic epic : manager.epics.values()) {
                manager.updateEpicStatus(epic);
            }

        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при загрузке из файла: " + file.getAbsolutePath(), e);
        }

        return manager;
    }

    // Преобразование строки в историю
    private static List<Integer> historyFromString(String value) {
        List<Integer> ids = new ArrayList<>();
        if (value != null && !value.trim().isEmpty()) {
            String[] stringIds = value.split(",");
            for (String stringId : stringIds) {
                try {
                    ids.add(Integer.parseInt(stringId.trim()));
                } catch (NumberFormatException e) {
                    // Log and skip malformed ID
                    System.err.println("Warning: Skipping malformed history ID: '" + stringId.trim() + "'");
                }
            }
        }
        return ids;
    }

    // Convert CSV string to task
    private static Task fromString(String value) {
        String[] fields = parseCsvLine(value);
        if (fields.length < 5) {
            return null;
        }

        try {
            int id = Integer.parseInt(fields[0]);
            TaskType type = TaskType.valueOf(fields[1]);
            String name = fields[2];
            TaskStatus status = TaskStatus.valueOf(fields[3]);
            String description = fields[4];

            Duration duration = Duration.ofMinutes(0);
            LocalDateTime startTime = null;
            String epicIdStr = "";

            // Process additional fields based on CSV format: epic,duration,startTime
            if (fields.length >= 6) {
                if (type == TaskType.SUBTASK && !fields[5].trim().isEmpty()) {
                    epicIdStr = fields[5];
                }
                // For Task and Epic field[5] is empty (epic column)
            }

            if (fields.length >= 7 && !fields[6].trim().isEmpty()) {
                // For all types: duration in field[6]
                try {
                    long minutes = Long.parseLong(fields[6]);
                    duration = Duration.ofMinutes(minutes);
                } catch (NumberFormatException e) {
                    // Ignore invalid value
                }
            }

            if (fields.length >= 8 && !fields[7].trim().isEmpty()) {
                // For all types: startTime in field[7]
                try {
                    startTime = LocalDateTime.parse(fields[7], DATE_TIME_FORMATTER);
                } catch (Exception e) {
                    // Ignore invalid value
                }
            }

            Task task = null;

            switch (type) {
                case TASK:
                    task = new Task(name, description);
                    break;
                case EPIC:
                    task = new Epic(name, description);
                    break;
                case SUBTASK:
                    if (!epicIdStr.trim().isEmpty()) {
                        int epicId = Integer.parseInt(epicIdStr);
                        task = new Subtask(name, description, epicId);
                    }
                    break;
            }

            if (task != null) {
                task.setId(id);
                task.setStatus(status);
                task.setDuration(duration);
                task.setStartTime(startTime);
            }

            return task;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Методы для работы с внутренним состоянием (для восстановления из файла)
    private void addTaskDirectly(Task task) {
        if (task instanceof Epic) {
            super.epics.put(task.getId(), (Epic) task);
        } else if (task instanceof Subtask subtask) {
            super.subtasks.put(task.getId(), subtask);
            addToPrioritizedTasks(subtask); // Добавляем в приоритизированный список
            // Добавляем подзадачу к эпику
            Epic epic = super.epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.addSubtaskId(subtask.getId());
            }
        } else {
            super.tasks.put(task.getId(), task);
            addToPrioritizedTasks(task); // Добавляем в приоритизированный список
        }

        // Обновляем nextId
        if (task.getId() >= super.nextId) {
            super.nextId = task.getId() + 1;
        }
    }

    private Task getTaskById(int id) {
        Task task = super.tasks.get(id);
        if (task != null)
            return task;

        task = super.epics.get(id);
        if (task != null)
            return task;

        return super.subtasks.get(id);
    }

    private void addToHistoryDirectly(Task task) {
        super.historyManager.add(task);
    }

    // Парсинг строки CSV с учетом кавычек
    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // Проверяем экранированные кавычки ""
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++; // Пропускаем следующую кавычку
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }

    // Переопределяем методы для автосохранения
    @Override
    public int createTask(String name, String description) {
        int taskId = super.createTask(name, description);
        save();
        return taskId;
    }

    // Сохранение состояния менеджера в файл
    private void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {

            // Заголовок CSV
            writer.write("id,type,name,status,description,epic,duration,startTime\n");

            // Сохраняем обычные задачи
            for (Task task : getAllTasks()) {
                writer.write(toString(task) + "\n");
            }

            // Сохраняем эпики
            for (Epic epic : getAllEpics()) {
                writer.write(toString(epic) + "\n");
            }

            // Сохраняем подзадачи
            for (Subtask subtask : getAllSubtasks()) {
                writer.write(toString(subtask) + "\n");
            }

            // Сохраняем историю
            writer.write("\n");
            writer.write(historyToString(getHistory()));

        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при сохранении в файл: " + file.getAbsolutePath(), e);
        }
    }

    // Преобразование задачи в строку CSV
    private String toString(Task task) {
        TaskType type = getTaskType(task);
        String epicId = "";
        String durationStr = task.getDuration() != null ? String.valueOf(task.getDuration().toMinutes()) : "";
        String startTimeStr = task.getStartTime() != null ? task.getStartTime().format(DATE_TIME_FORMATTER) : "";

        if (task instanceof Subtask) {
            epicId = String.valueOf(((Subtask) task).getEpicId());
            return String.format("%d,%s,%s,%s,%s,%s,%s,%s",
                    task.getId(),
                    type.name(),
                    escapeCsv(task.getName()),
                    task.getStatus().name(),
                    escapeCsv(task.getDescription()),
                    epicId,
                    durationStr,
                    startTimeStr);
        } else {
            return String.format("%d,%s,%s,%s,%s,%s,%s,%s",
                    task.getId(),
                    type.name(),
                    escapeCsv(task.getName()),
                    task.getStatus().name(),
                    escapeCsv(task.getDescription()),
                    "",
                    durationStr,
                    startTimeStr);
        }
    }

    // Преобразование истории в строку
    private String historyToString(List<Task> history) {
        List<String> ids = new ArrayList<>();
        for (Task task : history) {
            ids.add(String.valueOf(task.getId()));
        }
        return String.join(",", ids);
    }

    // Определение типа задачи
    private TaskType getTaskType(Task task) {
        if (task instanceof Epic) {
            return TaskType.EPIC;
        } else if (task instanceof Subtask) {
            return TaskType.SUBTASK;
        } else {
            return TaskType.TASK;
        }
    }

    // Экранирование строки для CSV
    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        // Если строка содержит запятые, кавычки, переводы строк или возвраты каретки,
        // оборачиваем в кавычки
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            // Экранируем кавычки удвоением
            String escaped = value.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }

        return value;
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void deleteTask(int id) {
        super.deleteTask(id);
        save();
    }

    @Override
    public void deleteAllTasks() {
        super.deleteAllTasks();
        save();
    }

    @Override
    public int createEpic(String name, String description) {
        int epicId = super.createEpic(name, description);
        save();
        return epicId;
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void deleteEpic(int id) {
        super.deleteEpic(id);
        save();
    }

    @Override
    public void deleteAllEpics() {
        super.deleteAllEpics();
        save();
    }

    @Override
    public void createSubtask(String name, String description, int epicId) {
        super.createSubtask(name, description, epicId);
        save();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void deleteSubtask(int id) {
        super.deleteSubtask(id);
        save();
    }

    @Override
    public void deleteAllSubtasks() {
        super.deleteAllSubtasks();
        save();
    }

    // Exception for save/load errors
    public static class ManagerSaveException extends RuntimeException {
        public ManagerSaveException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
