import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbMovies;
import info.movito.themoviedbapi.model.Data;
import info.movito.themoviedbapi.model.MovieDb;

import java.util.HashMap;

public class DataSource {
    static HashMap<Integer, MovieDb> mapOfMovies = new HashMap<>();

    private static void loadMovies(){
        /**
         * Uses library from https://github.com/holgerbrandl/themoviedbapi
         * This example hard codes Star wars movies
         * curl --location --request GET 'https://api.themoviedb.org/3/search/movie?api_key=179cfa0a673b02ddfa1a6aa6629fa730&query=star+wars'
         */
        TmdbMovies movies = new TmdbApi("179cfa0a673b02ddfa1a6aa6629fa730").getMovies();
        int[] movieIds =   {11,
                181812,
                181808,
                348350,
                140607,
                732670,
                330459,
                12180,
                392216,
                1893,
                667574,
                1895,
                42979,
                1894,
                74849,
                435365,
                825647,
                287663,
                42982,
                70608};

        int movieIndex = 0;
        for(int movieId : movieIds){
            MovieDb movieDb = movies.getMovie(movieId, "en", TmdbMovies.MovieMethod.images, TmdbMovies.MovieMethod.release_dates);
            mapOfMovies.put(movieId, movieDb);
        }
    }

    private DataSource(){
    }

    // factory method to load movies and return map
    public static HashMap<Integer, MovieDb> getMapOfMovies(){
        loadMovies();
        return mapOfMovies;
    }
}
