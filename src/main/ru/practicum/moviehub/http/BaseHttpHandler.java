package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected static final Gson gson = new Gson();
    protected static final int CURRENT_YEAR = Year.now().getValue();
    protected static final int MIN_YEAR = 1888;
    protected static final int MAX_YEAR = CURRENT_YEAR + 1;

    protected void sendJson(HttpExchange exchange, int statusCode, Object response) throws IOException {
        String json = gson.toJson(response);
        exchange.getResponseHeaders().set("Content-Type", CT_JSON);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    protected void sendError(HttpExchange exchange, int statusCode, String error) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(error);
        sendJson(exchange, statusCode, errorResponse);
    }

    protected void sendValidationError(HttpExchange exchange, List<String> details) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", details);
        sendJson(exchange, 422, errorResponse);
    }

    protected <T> T parseJson(HttpExchange exchange, Class<T> clazz) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            if (body.trim().isEmpty()) {
                sendError(exchange, 400, "Пустое тело запроса");
                return null;
            }

            try {
                com.google.gson.JsonObject jsonObject = gson.fromJson(body, com.google.gson.JsonObject.class);

                if (Movie.class.equals(clazz) && jsonObject.has("year")) {
                    com.google.gson.JsonElement yearElement = jsonObject.get("year");
                    if (yearElement.isJsonPrimitive()) {
                        com.google.gson.JsonPrimitive primitive = yearElement.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            sendError(exchange, 400, "Некорректный тип данных: год должен быть числом, а не строкой");
                            return null;
                        }
                    }
                }

                return gson.fromJson(body, clazz);

            } catch (JsonSyntaxException e) {
                sendError(exchange, 400, "Некорректный JSON");
                return null;
            }
        }
    }

    protected List<String> validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();

        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > 100) {
            errors.add("название не должно превышать 100 символов");
        }

        if (movie.getYear() == null) {
            errors.add("год не должен быть пустым");
        } else {
            if (movie.getYear() < MIN_YEAR || movie.getYear() > MAX_YEAR) {
                errors.add(String.format("год должен быть между %d и %d", MIN_YEAR, MAX_YEAR));
            }
        }

        return errors;
    }

    protected boolean isJsonContentType(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        return contentType != null && contentType.contains("application/json");
    }
}