import com.slack.api.app_backend.interactive_components.response.Option;
import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.bolt.util.JsonOps;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.methods.response.views.ViewsPublishResponse;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewState;
import info.movito.themoviedbapi.TmdbAccount;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbMovies;
import info.movito.themoviedbapi.model.MovieDb;

import java.util.*;
import java.util.stream.Collectors;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.*;
import static com.slack.api.model.view.Views.*;
import static java.util.stream.Collectors.toList;

public class MyApp {

    static HashMap<Integer, MovieDb> mapOfMovies = new HashMap<>();

    public static View buildMovieSelectionModal() {
        return view(view -> view
                .callbackId("movie-title-selection")
                .type("modal")
                .notifyOnClose(true)
                .title(viewTitle(title -> title.type("plain_text").text("Movie Info").emoji(true)))
                .submit(viewSubmit(submit -> submit.type("plain_text").text("Submit").emoji(true)))
                .close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
                .privateMetadata("{\"response_url\":\"https://hooks.slack.com/actions/T1ABCD2E12/330361579271/0dAEyLY19ofpLwxqozy3firz\"}")
                .blocks(asBlocks(
                        section(section -> section.text(markdownText("Select a Movie"))),
                        divider(),
                        section(section -> section
                                .blockId("category-block")
                                .text(plainText(" "))
                                .accessory(
                                        multiExternalSelect(conf ->
                                                conf.actionId("movie-action")
                                                .minQueryLength(3)
                                                .placeholder(
                                                plainText("Select an Item ( Try Star )", true)
                                                )))
                        ))));
    }

    //  Load movies from tmdb
    public static void loadMovies(){
        //https://github.com/holgerbrandl/themoviedbapi
        // Get movies related to star wars
        // curl --location --request GET 'https://api.themoviedb.org/3/search/movie?api_key=179cfa0a673b02ddfa1a6aa6629fa730&query=star+wars'

        // Hard coded for star wars as in example
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
        System.out.println(mapOfMovies.size());
    }

    public static void main(String[] args) throws Exception {
        App app = new App();
        loadMovies();

        // Display initial home page - and button to act
        app.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
            View appHomeView = view(view -> view
                    .type("home")
                    .blocks(asBlocks(
                            header(header ->
                            header.text(plainText(pt -> pt.emoji(true).text("*Welcome to Movie Info! :tada:")))),
                            section(section ->
                                    section.text(markdownText("Click the button below to pick a movie !"))),
                            divider(),
                            actions(actions ->
                                    actions.elements(
                                    asElements(
                                    button(b ->
                                    b.actionId("home-button-action")
                                    .text(plainText(pt -> pt.text("Select a Movie !")))
                                    .value("selected"))))))));

            ViewsPublishResponse res = ctx.client().viewsPublish(r -> r
                    .userId(payload.getEvent().getUser())
                    .view(appHomeView)
            );
            return ctx.ack();
        });

        //  Handle button action - show Modal for screen 2
        app.blockAction("home-button-action", (req, ctx) -> {
            ViewsOpenResponse viewsOpenRes = ctx.client().viewsOpen(r -> r
                    .triggerId(ctx.getTriggerId())
                    .view(buildMovieSelectionModal()));
            return ctx.ack();
        });

        app.blockSuggestion("movie-action", (req, ctx) -> {
            final List<Option> allOptions = new ArrayList<>();

            mapOfMovies.keySet().forEach(movieId ->
                    allOptions.
                    add(new Option(plainText(mapOfMovies.get(movieId).getTitle(), true), String.valueOf(movieId)))
            );

            String keyword = req.getPayload().getValue();

            List<Option> options = allOptions.stream()
                    .filter(o -> ((PlainTextObject) o.getText()).getText().toLowerCase().contains(keyword))
                    .collect(toList());

            return ctx.ack(r -> r.options(options.isEmpty() ? allOptions : options));
        });

        app.viewSubmission("movie-title-selection", (req, ctx) -> {
            List<Integer> movieIds = fetchMovieId(req.getPayload().getView().getState());
            for(Integer movieId : movieIds) {
                MovieDb movieDb = mapOfMovies.get(movieId);
                app.client().chatPostMessage(r -> r.token(ctx.getBotToken()).channel(ctx.getRequestUserId())
                        .blocks(asBlocks(
                                header(t -> t.text(plainText(movieDb.getTitle()))),
                                divider(),
                                section(section -> section.text(markdownText("**Release Date:**" + movieDb.getReleaseDate()))),
                                section(section -> section.text(markdownText(movieDb.getOverview())))
                        )));
            }
            return ctx.ack();
        });

        SlackAppServer slackAppServer = new SlackAppServer(app);
        slackAppServer.start();

    }

    public static List<Integer> fetchMovieId(ViewState viewState){
        List<ViewState.SelectedOption> options = viewState.getValues().get("category-block").get("movie-action").getSelectedOptions();
        return options.stream().map(option -> Integer.parseInt(option.getValue())).collect(Collectors.toList());
    }

    public static void craftTextMessage(int movieId){

    }
}
