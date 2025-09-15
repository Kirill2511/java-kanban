package main.ru.practicum.kanban.server.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Утилитный класс для работы с JSON сериализацией/десериализацией
 */
public class JsonUtils {

    private static final Gson gson = createGson();

    /**
     * Создает настроенный экземпляр Gson с поддержкой LocalDateTime и Duration
     */
    private static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(Duration.class, new DurationAdapter())
                .setPrettyPrinting()
                .create();
    }

    /**
     * Создает публичный экземпляр настроенного Gson для внешнего использования
     */
    public static Gson createConfiguredGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(Duration.class, new DurationAdapter())
                .setPrettyPrinting()
                .create();
    }

    /**
     * Преобразует объект в JSON строку
     *
     * @param object объект для сериализации
     * @return JSON строка
     */
    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    /**
     * Преобразует JSON строку в объект указанного типа
     *
     * @param json  JSON строка
     * @param clazz класс результирующего объекта
     * @return объект указанного типа
     * @throws JsonSyntaxException если JSON некорректен
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws JsonSyntaxException {
        return gson.fromJson(json, clazz);
    }

    /**
     * Проверяет, является ли строка валидным JSON
     *
     * @param json строка для проверки
     * @return true если JSON валиден, false иначе
     */
    public static boolean isValidJson(String json) {
        try {
            gson.fromJson(json, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    /**
     * Адаптер для сериализации/десериализации LocalDateTime
     */
    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(formatter));
            }
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String dateTime = in.nextString();
            return LocalDateTime.parse(dateTime, formatter);
        }
    }

    /**
     * Адаптер для сериализации/десериализации Duration
     */
    private static class DurationAdapter extends TypeAdapter<Duration> {

        @Override
        public void write(JsonWriter out, Duration value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toMinutes()); // Сохраняем в минутах
            }
        }

        @Override
        public Duration read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            long minutes = in.nextLong();
            return Duration.ofMinutes(minutes);
        }
    }
}