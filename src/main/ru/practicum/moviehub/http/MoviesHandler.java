package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private static final Pattern MOVIES_PATH = Pattern.compile("^/movies/(.+)$");

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        try {
            switch (method) {
                case "GET":
                    handleGet(exchange, path, query);
                    break;
                case "POST":
                    handlePost(exchange);
                    break;
                case "DELETE":
                    handleDelete(exchange, path);
                    break;
                default:
                    sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            sendError(exchange, 500, "Internal Server Error");
        }
    }

    private void handleGet(HttpExchange exchange, String path, String query) throws IOException {
        if ("/movies".equals(path)) {
            if (query != null && query.startsWith("year=")) {
                handleGetByYear(exchange, query);
            } else {
                List<Movie> movies = store.findAll();
                sendJson(exchange, 200, movies);
            }
            return;
        }

        var matcher = MOVIES_PATH.matcher(path);
        if (matcher.matches()) {
            String idStr = matcher.group(1);
            handleGetById(exchange, idStr);
            return;
        }

        sendError(exchange, 404, "Not Found");
    }

    private void handleGetByYear(HttpExchange exchange, String query) throws IOException {
        try {
            int year = Integer.parseInt(query.substring(5));
            List<Movie> movies = store.findByYear(year);
            sendJson(exchange, 200, movies);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный параметр запроса — 'year'");
        }
    }

    private void handleGetById(HttpExchange exchange, String idStr) throws IOException {
        if (!isNumeric(idStr)) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }

        try {
            Long id = Long.parseLong(idStr);
            Movie movie = store.findById(id);
            if (movie == null) {
                sendError(exchange, 404, "Фильм не найден");
            } else {
                sendJson(exchange, 200, movie);
            }
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный ID");
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        if (!isJsonContentType(exchange)) {
            sendError(exchange, 415, "Unsupported Media Type");
            return;
        }

        Movie movie = parseJson(exchange, Movie.class);
        if (movie == null) return;

        List<String> validationErrors = validateMovie(movie);
        if (!validationErrors.isEmpty()) {
            sendValidationError(exchange, validationErrors);
            return;
        }

        Movie savedMovie = store.save(movie);
        sendJson(exchange, 201, savedMovie);
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        var matcher = MOVIES_PATH.matcher(path);
        if (!matcher.matches()) {
            sendError(exchange, 404, "Not Found");
            return;
        }

        String idStr = matcher.group(1);
        if (!isNumeric(idStr)) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }

        try {
            Long id = Long.parseLong(idStr);
            boolean deleted = store.delete(id);
            if (deleted) {
                sendNoContent(exchange);
            } else {
                sendError(exchange, 404, "Фильм не найден");
            }
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный ID");
        }
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
}