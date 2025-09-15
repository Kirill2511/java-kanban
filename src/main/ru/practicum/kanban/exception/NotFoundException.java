package main.ru.practicum.kanban.exception;

/**
 * Исключение, которое выбрасывается когда запрашиваемый ресурс не найден
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}