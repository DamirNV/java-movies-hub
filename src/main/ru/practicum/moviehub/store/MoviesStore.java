package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MoviesStore {
    private final Map<Long, Movie> movies = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public List<Movie> findAll() {
        return new ArrayList<>(movies.values());
    }

    public Movie findById(Long id) {
        return movies.get(id);
    }

    public Movie save(Movie movie) {
        if (movie.getId() == null) {
            movie.setId(idCounter.getAndIncrement());
        }
        movies.put(movie.getId(), movie);
        return movie;
    }

    public boolean delete(Long id) {
        return movies.remove(id) != null;
    }

    public List<Movie> findByYear(int year) {
        return movies.values().stream()
                .filter(movie -> movie.getYear() == year)
                .collect(Collectors.toList());
    }

    public void clear() {
        movies.clear();
        idCounter.set(1);
    }
}