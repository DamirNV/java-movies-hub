package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static final Gson gson = new Gson();
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;

    @BeforeAll
    static void beforeAll() throws Exception {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @BeforeEach
    void clearStore() {
        store.clear();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""));
        assertEquals("[]", resp.body().trim());
    }

    @Test
    void postMovie_whenValidData_returnsCreatedMovie() throws Exception {
        String json = "{\"title\":\"Inception\",\"year\":2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""));

        JsonObject jsonResponse = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertTrue(jsonResponse.has("id"));
        assertEquals("Inception", jsonResponse.get("title").getAsString());
        assertEquals(2010, jsonResponse.get("year").getAsInt());
    }

    @Test
    void getMovies_whenNotEmpty_returnsMoviesArray() throws Exception {
        String json = "{\"title\":\"The Matrix\",\"year\":1999}";
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(getReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body(),
                new com.google.gson.reflect.TypeToken<List<Movie>>(){}.getType());
        assertFalse(movies.isEmpty());
        assertEquals("The Matrix", movies.get(0).getTitle());
    }

    @Test
    void postMovie_whenEmptyTitle_returnsValidationError() throws Exception {
        String json = "{\"title\":\"\",\"year\":2020}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertTrue(error.getDetails().contains("название не должно быть пустым"));
    }

    @Test
    void postMovie_whenInvalidYear_returnsValidationError() throws Exception {
        String json = "{\"title\":\"Test\",\"year\":1800}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertTrue(error.getDetails().get(0).contains("год должен быть между"));
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        String json = "{\"title\":\"Interstellar\",\"year\":2014}";
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> postResp = client.send(postReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonObject movie = JsonParser.parseString(postResp.body()).getAsJsonObject();
        Long id = movie.get("id").getAsLong();

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(getReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        Movie retrieved = gson.fromJson(resp.body(), Movie.class);
        assertEquals("Interstellar", retrieved.getTitle());
        assertEquals(2014, retrieved.getYear());
    }

    @Test
    void getMovieById_whenNotExists_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/99999"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Фильм не найден", error.getError());
    }

    @Test
    void getMovieById_whenInvalidId_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Некорректный ID", error.getError());
    }

    @Test
    void deleteMovie_whenExists_returns204() throws Exception {
        String json = "{\"title\":\"To Delete\",\"year\":2020}";
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> postResp = client.send(postReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonObject movie = JsonParser.parseString(postResp.body()).getAsJsonObject();
        Long id = movie.get("id").getAsLong();

        HttpRequest deleteReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(deleteReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode());
        assertEquals("", resp.body());
    }

    @Test
    void deleteMovie_whenNotExists_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/99999"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Фильм не найден", error.getError());
    }

    @Test
    void getMoviesByYear_whenValidYear_returnsFiltered() throws Exception {
        addTestMovie("Movie 2020", 2020);
        addTestMovie("Movie 2021", 2021);
        addTestMovie("Another 2020", 2020);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2020"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body(),
                new com.google.gson.reflect.TypeToken<List<Movie>>(){}.getType());
        assertEquals(2, movies.size());
        assertTrue(movies.stream().allMatch(m -> m.getYear() == 2020));
    }

    @Test
    void getMoviesByYear_whenInvalidYear_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Некорректный параметр запроса — 'year'", error.getError());
    }

    @Test
    void postMovie_whenWrongContentType_returns415() throws Exception {
        String json = "{\"title\":\"Test\",\"year\":2020}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Unsupported Media Type", error.getError());
    }

    @Test
    void postMovie_whenInvalidJson_returns400() throws Exception {
        String invalidJson = "{\"title\":\"Test\",\"year\":2020";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Некорректный JSON", error.getError());
    }

    @Test
    void postMovie_whenTitleTooLong_returnsValidationError() throws Exception {
        String longTitle = "a".repeat(101);
        String json = String.format("{\"title\":\"%s\",\"year\":2020}", longTitle);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertTrue(error.getDetails().contains("название не должно превышать 100 символов"));
    }

    @Test
    void postMovie_whenYearIsString_returns400() throws Exception {
        String json = "{\"title\":\"Test Movie\",\"year\":\"2020\"}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        String errorMessage = error.getError().toLowerCase();
        assertTrue(errorMessage.contains("некорректный") ||
                errorMessage.contains("тип") ||
                errorMessage.contains("год") ||
                errorMessage.contains("число"));
    }

    @Test
    void postMovie_whenYearMissing_returnsValidationError() throws Exception {
        String json = "{\"title\":\"Test Movie\"}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertTrue(error.getDetails().contains("год не должен быть пустым"));
    }

    @Test
    void deleteMovie_whenInvalidId_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Некорректный ID", error.getError());
    }

    @Test
    void getMoviesByYear_whenEmptyYear_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year="))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Некорректный параметр запроса — 'year'", error.getError());
    }

    @Test
    void getMoviesByYear_whenNoMoviesForYear_returnsEmptyArray() throws Exception {
        addTestMovie("Movie 2021", 2021);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2020"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body(),
                new com.google.gson.reflect.TypeToken<List<Movie>>(){}.getType());
        assertTrue(movies.isEmpty());
    }

    @Test
    void postAndGetMultipleMovies_worksCorrectly() throws Exception {
        addTestMovie("Movie 1", 2020);
        addTestMovie("Movie 2", 2021);
        addTestMovie("Movie 3", 2020);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body(),
                new com.google.gson.reflect.TypeToken<List<Movie>>(){}.getType());
        assertEquals(3, movies.size());
    }

    @Test
    void postMovie_withMaxLengthTitle_works() throws Exception {
        String maxTitle = "a".repeat(100); // Максимальная длина
        String json = String.format("{\"title\":\"%s\",\"year\":2020}", maxTitle);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode());
    }

    @Test
    void unsupportedMethod_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Method Not Allowed", error.getError());
    }

    private void addTestMovie(String title, int year) throws Exception {
        String json = String.format("{\"title\":\"%s\",\"year\":%d}", title, year);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}