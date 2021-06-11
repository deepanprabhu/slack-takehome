import com.slack.api.app_backend.interactive_components.response.Option;
import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.methods.response.views.ViewsPublishResponse;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewState;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbMovies;
import info.movito.themoviedbapi.model.MovieDb;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.*;
import static com.slack.api.model.view.Views.*;
import static java.util.stream.Collectors.toList;

public class MyApp {
    static HashMap<Integer, MovieDb> mapOfMovies;

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



    public static void main(String[] args) throws Exception {
        App app = new App();
        mapOfMovies = DataSource.getMapOfMovies();

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

                StringBuilder sb = new StringBuilder();

                try {
                    SimpleDateFormat iFormat = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat oFormat = new SimpleDateFormat("MMMM dd, yyyy");
                    Date inputDate = iFormat.parse(movieDb.getReleaseDate());
                    sb.append("*Release Date*:").append(oFormat.format(inputDate));
                }
                catch (ParseException parseException){
                    System.out.println("Date parsing error");
                }

                String imageUrl = "https://image.tmdb.org/t/p/w600_and_h900_bestv2" + movieDb.getPosterPath();


                app.client().chatPostMessage(r -> r.token(ctx.getBotToken()).channel(ctx.getRequestUserId())
                        .blocks(asBlocks(
                                header(t -> t.text(plainText(movieDb.getTitle()))),
                                divider(),
                                section(section -> section.text(markdownText(sb.toString()))),
                                section(section -> section.text(markdownText(movieDb.getOverview()))
                                        .accessory(imageElement(im -> im.imageUrl(imageUrl).altText("Movie poster Thumb"))))
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
}
