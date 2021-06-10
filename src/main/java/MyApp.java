import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.methods.response.views.ViewsPublishResponse;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;

import java.time.ZonedDateTime;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;
import static com.slack.api.model.view.Views.*;

public class MyApp {

    public static View buildModal() {
        return view(view -> view
                .callbackId("meeting-arrangement")
                .type("modal")
                .notifyOnClose(true)
                .title(viewTitle(title -> title.type("plain_text").text("Meeting Arrangement").emoji(true)))
                .submit(viewSubmit(submit -> submit.type("plain_text").text("Submit").emoji(true)))
                .close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
                .privateMetadata("{\"response_url\":\"https://hooks.slack.com/actions/T1ABCD2E12/330361579271/0dAEyLY19ofpLwxqozy3firz\"}")
                .blocks(asBlocks(
                        section(section -> section
                                .blockId("category-block")
                                .text(markdownText("Select a category of the meeting!"))
                                .accessory(staticSelect(staticSelect -> staticSelect
                                        .actionId("category-selection-action")
                                        .placeholder(plainText("Select a category"))
                                        .options(asOptions(
                                                option(plainText("Customer"), "customer"),
                                                option(plainText("Partner"), "partner"),
                                                option(plainText("Internal"), "internal")
                                        ))
                                ))
                        ),
                        input(input -> input
                                .blockId("agenda-block")
                                .element(plainTextInput(pti -> pti.actionId("agenda-action").multiline(true)))
                                .label(plainText(pt -> pt.text("Detailed Agenda").emoji(true)))
                        )
                ))
        );
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

        app.blockAction("button-action", (req, ctx) -> {
            ViewsOpenResponse viewsOpenRes = ctx.client().viewsOpen(r -> r
                    .triggerId(ctx.getTriggerId())
                    .view(buildModal()));
            return ctx.ack();
        });

        SlackAppServer slackAppServer = new SlackAppServer(app);
        slackAppServer.start();

    }
}
