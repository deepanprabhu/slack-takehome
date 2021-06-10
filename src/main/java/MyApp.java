import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.bolt.util.JsonOps;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.methods.response.views.ViewsPublishResponse;
import com.slack.api.app_backend.interactive_components.response.Option;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;
import static com.slack.api.model.view.Views.*;
import static java.util.stream.Collectors.toList;

public class MyApp {

    class PrivateMetadata {
        String channelId;
    }

    public static View buildModal() {
        return view(view -> view
                .callbackId("meeting-arrangement")
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
                                                        .minQueryLength(1)
                                                        .placeholder(plainText("Enter to search", true))))
                                ))));
    }
    public static void main(String[] args) throws Exception {
        App app = new App();

        app.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
            // Build a Home tab view
            ZonedDateTime now = ZonedDateTime.now();
            View appHomeView = view(view -> view
                    .type("home")
                    .blocks(asBlocks(
                            header(header -> header.text(plainText(pt -> pt.emoji(true).text("*Welcome to Movie Info! :tada:")))),
                            section(section -> section.text(markdownText("Click the button below to pick a movie !"))),
                            divider(),
                            actions(actions -> actions.elements(asElements(button(b -> b.actionId("button-action").text(plainText(pt -> pt.text("Select a Movie !"))).value("ping"))))))));
            // Update the App Home for the given user
            ViewsPublishResponse res = ctx.client().viewsPublish(r -> r
                    .userId(payload.getEvent().getUser())
                    .view(appHomeView)
            );
            return ctx.ack();
        });

        app.blockSuggestion("movie-action", (req, ctx) -> {
            final List<Option> allOptions = Arrays.asList(
                    new Option(plainText("Schedule", true), "schedule"),
                    new Option(plainText("Budget", true), "budget"),
                    new Option(plainText("Assignment", true), "assignment")
            );

            String keyword = req.getPayload().getValue();
            List<Option> options = allOptions.stream()
                    .filter(o -> ((PlainTextObject) o.getText()).getText().toLowerCase().contains(keyword))
                    .collect(toList());
            return ctx.ack(r -> r.options(options.isEmpty() ? allOptions : options));
        });

        app.blockAction("movie-action", (req, ctx) -> {
            String metaData = req.getPayload().getView().getPrivateMetadata();
            PrivateMetadata privateMetadata = JsonOps.fromJson(metaData, PrivateMetadata.class);
            app.client().chatPostMessage(r -> r.channel(privateMetadata.channelId).token(ctx.getBotToken()).text("Thanks !"));
            return ctx.ack();
        });

        app.viewSubmission("meeting-arrangement", (req, ctx) -> {
            app.client().chatPostMessage(r -> r.token(ctx.getBotToken()).channel(ctx.getRequestUserId()).text("hi"));
            return ctx.ack();
        });

        app.blockAction("button-action", (req, ctx) -> {
            ViewsOpenResponse viewsOpenRes = ctx.client().viewsOpen(r -> r
                    .triggerId(ctx.getTriggerId())
                    .view(buildModal()));
            return ctx.ack();
        });

        app.endpoint("/queryTmdb", (req, ctx) -> {
            return ctx.ack();
        });

        SlackAppServer slackAppServer = new SlackAppServer(app);
        slackAppServer.start();

    }
}
