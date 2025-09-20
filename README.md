# Kanban HTTP API

REST API для управления задачами, подзадачами и эпиками в системе Kanban.

## Запуск сервера

```bash
# Компилируем проект (убедитесь, что gson-2.10.1.jar в classpath)
javac -cp "lib/gson-2.10.1.jar" src/main/ru/practicum/kanban/**/*.java

# Запускаем HTTP сервер
java -cp "src:lib/gson-2.10.1.jar" main.ru.practicum.kanban.server.HttpTaskServer

# Или через Main класс с аргументом
java -cp "src:lib/gson-2.10.1.jar" main.ru.practicum.kanban.Main server
```

Сервер запускается на порту **8080**.

## API Эндпоинты

### Задачи (Tasks)

#### Получить все задачи

```
GET /tasks
```

**Ответ**: JSON массив задач

```json
[
  {
    "id": 1,
    "name": "Задача 1",
    "description": "Описание задачи",
    "status": "NEW",
    "duration": 60,
    "startTime": "2023-12-01T10:00:00"
  }
]
```

#### Получить задачу по ID

```
GET /tasks/{id}
```

**Ответ**: JSON объект задачи или 404 если не найдена

#### Создать задачу

```
POST /tasks
Content-Type: application/json

{
  "name": "Новая задача",
  "description": "Описание новой задачи",
  "duration": 120,
  "startTime": "2023-12-01T14:00:00"
}
```

**Ответ**: 201 Created

#### Обновить задачу

```
POST /tasks
Content-Type: application/json

{
  "id": 1,
  "name": "Обновленная задача",
  "description": "Новое описание",
  "status": "IN_PROGRESS",
  "duration": 90,
  "startTime": "2023-12-01T15:00:00"
}
```

**Ответ**: 201 Created

#### Удалить задачу

```
DELETE /tasks/{id}
```

**Ответ**: 200 OK или 404 если не найдена

### Подзадачи (Subtasks)

#### Получить все подзадачи

```
GET /subtasks
```

#### Получить подзадачу по ID

```
GET /subtasks/{id}
```

#### Создать подзадачу

```
POST /subtasks
Content-Type: application/json

{
  "name": "Новая подзадача",
  "description": "Описание подзадачи",
  "epicId": 2,
  "duration": 30,
  "startTime": "2023-12-01T16:00:00"
}
```

**Важно**: `epicId` обязательно для создания подзадачи

#### Обновить подзадачу

```
POST /subtasks
Content-Type: application/json

{
  "id": 3,
  "name": "Обновленная подзадача",
  "description": "Новое описание",
  "epicId": 2,
  "status": "DONE"
}
```

#### Удалить подзадачу

```
DELETE /subtasks/{id}
```

### Эпики (Epics)

#### Получить все эпики

```
GET /epics
```

#### Получить эпик по ID

```
GET /epics/{id}
```

#### Получить подзадачи эпика

```
GET /epics/{id}/subtasks
```

Возвращает все подзадачи указанного эпика

#### Создать эпик

```
POST /epics
Content-Type: application/json

{
  "name": "Новый эпик",
  "description": "Описание эпика"
}
```

**Примечание**: Статус эпика рассчитывается автоматически на основе подзадач

#### Удалить эпик

```
DELETE /epics/{id}
```

Удаляет эпик и все его подзадачи

### История и приоритет

#### Получить историю просмотров

```
GET /history
```

Возвращает список последних просмотренных задач

#### Получить задачи по приоритету

```
GET /prioritized
```

Возвращает задачи, отсортированные по времени начала (startTime)

## HTTP статусы ответов

- **200 OK** - Успешный запрос с данными
- **201 Created** - Успешное создание/обновление/удаление
- **400 Bad Request** - Некорректный запрос (плохой JSON, отсутствующие поля)
- **404 Not Found** - Ресурс не найден
- **406 Not Acceptable** - Конфликт времени выполнения задач
- **500 Internal Server Error** - Внутренняя ошибка сервера

## Формат данных

### Task (Задача)

```json
{
  "id": 1,
  "name": "Название задачи",
  "description": "Описание задачи",
  "status": "NEW|IN_PROGRESS|DONE",
  "duration": 60,
  "startTime": "2023-12-01T10:00:00"
}
```

### Subtask (Подзадача)

```json
{
  "id": 2,
  "name": "Название подзадачи",
  "description": "Описание подзадачи",
  "status": "NEW|IN_PROGRESS|DONE",
  "epicId": 3,
  "duration": 30,
  "startTime": "2023-12-01T11:00:00"
}
```

### Epic (Эпик)

```json
{
  "id": 3,
  "name": "Название эпика",
  "description": "Описание эпика",
  "status": "NEW|IN_PROGRESS|DONE",
  "subtaskIds": [
    2,
    4,
    5
  ],
  "duration": 120,
  "startTime": "2023-12-01T10:00:00",
  "endTime": "2023-12-01T12:00:00"
}
```

## Примеры использования

### Создание эпика с подзадачами

1. **Создать эпик**:

```bash
curl -X POST http://localhost:8080/epics \
  -H "Content-Type: application/json" \
  -d '{"name": "Разработка фичи", "description": "Новая функциональность"}'
```

2. **Создать подзадачи**:

```bash
curl -X POST http://localhost:8080/subtasks \
  -H "Content-Type: application/json" \
  -d '{"name": "Анализ", "description": "Анализ требований", "epicId": 1}'

curl -X POST http://localhost:8080/subtasks \
  -H "Content-Type: application/json" \
  -d '{"name": "Разработка", "description": "Написание кода", "epicId": 1}'
```

3. **Проверить эпик с подзадачами**:

```bash
curl http://localhost:8080/epics/1/subtasks
```

### Управление задачами

```bash
# Получить все задачи
curl http://localhost:8080/tasks

# Создать задачу
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"name": "Срочная задача", "description": "Важная работа"}'

# Получить историю
curl http://localhost:8080/history

# Получить приоритизированные задачи
curl http://localhost:8080/prioritized
```

## Обработка ошибок

Все ошибки возвращаются в формате JSON:

```json
{
  "error": "Описание ошибки"
}
```

### Типичные ошибки

- **400 Bad Request**: Некорректный JSON или отсутствующие обязательные поля
- **404 Not Found**: Попытка получить/обновить/удалить несуществующую задачу
- **406 Not Acceptable**: Конфликт времени выполнения (пересечение с другими задачами)

## Валидация данных

- Название задачи не может быть пустым
- Описание задачи обязательно
- Для подзадач обязательно указание `epicId`
- Время начала и продолжительность должны быть корректными
- Проверяется пересечение времени выполнения задач