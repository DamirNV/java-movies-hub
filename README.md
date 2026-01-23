# MovieHub API

REST API для управления коллекцией фильмов с полным TDD циклом разработки.

## 📦 Быстрый старт

### Требования
- Java 11 или выше
- Библиотека Gson

### Запуск
1. **Скачайте зависимости:**
    - [gson-2.10.1.jar](https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar) → в папку `lib/`

2. **Скомпилируйте:**
   ```bash
   mkdir -p out
   javac -cp "lib/*" -d out src/main/java/ru/practicum/moviehub/**/*.java
   ```

3. **Запустите сервер:**
   ```bash
   java -cp "out:lib/*" ru.practicum.moviehub.MovieHubApp
   ```

4. **Проверьте работу:**
   ```bash
   curl http://localhost:8080/movies
   # Должен вернуть: []
   ```

## 🔧 API Endpoints

### GET /movies
Получить все фильмы.  
**Пример:** `curl http://localhost:8080/movies`

### GET /movies?year=2020
Фильмы по году выпуска.  
**Пример:** `curl "http://localhost:8080/movies?year=2020"`

### POST /movies
Добавить новый фильм.  
**Пример:**
```bash
curl -X POST http://localhost:8080/movies \
  -H "Content-Type: application/json" \
  -d '{"title":"The Matrix","year":1999}'
```

### GET /movies/{id}
Получить фильм по ID.  
**Пример:** `curl http://localhost:8080/movies/1`

### DELETE /movies/{id}
Удалить фильм.  
**Пример:** `curl -X DELETE http://localhost:8080/movies/1`

## ✅ Валидация данных

### Название фильма:
- Обязательное поле
- Не пустое (не только пробелы)
- ≤ 100 символов

### Год выпуска:
- Обязательное поле
- Число (не строка)
- 1888 ≤ год ≤ (текущий год + 1)

## ⚠️ Коды ошибок

- **200** - Успешный GET
- **201** - Фильм создан
- **204** - Фильм удален
- **400** - Некорректный запрос (ID, JSON, параметры)
- **404** - Фильм не найден
- **415** - Неверный Content-Type
- **422** - Ошибка валидации данных

## 🧪 Тестирование

Проект разработан по методологии **TDD** (Test-Driven Development):

### Особенности тестов:
- **19 интеграционных тестов** - полное покрытие API
- **Независимые тесты** - каждый тест самодостаточен
- **Проверка всех сценариев**: успешные и ошибочные

### Запуск тестов:
В IntelliJ IDEA: Правой кнопкой на `MoviesApiTest` → "Run"

## 🏗️ Архитектура

```
ru.practicum.moviehub/
├── MovieHubApp.java          # Главный класс
├── http/
│   ├── MoviesServer.java     # HTTP сервер
│   ├── MoviesHandler.java    # Обработчик запросов
│   └── BaseHttpHandler.java  # Базовый класс
├── model/
│   ├── Movie.java            # Модель фильма
│   └── ErrorResponse.java    # Модель ошибки
└── store/
    └── MoviesStore.java      # Хранилище (ConcurrentHashMap)
```

## 📊 Примеры использования

### Рабочий сценарий:
```bash
# 1. Добавляем фильм
curl -X POST http://localhost:8080/movies \
  -H "Content-Type: application/json" \
  -d '{"title":"Inception","year":2010}'

# 2. Получаем все фильмы
curl http://localhost:8080/movies
# Ответ: [{"id":1,"title":"Inception","year":2010}]

# 3. Получаем по ID
curl http://localhost:8080/movies/1

# 4. Фильтруем по году
curl "http://localhost:8080/movies?year=2010"

# 5. Удаляем
curl -X DELETE http://localhost:8080/movies/1
```

### Примеры ошибок:
```bash
# Пустое название
curl -X POST ... -d '{"title":"","year":2020}'
# Ответ: 422 с деталями ошибки

# Год как строка
curl -X POST ... -d '{"title":"Test","year":"2020"}'
# Ответ: 400 (некорректный тип)

# Несуществующий ID
curl http://localhost:8080/movies/999
# Ответ: 404
```



