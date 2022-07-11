/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.script;

import static org.dspace.app.matcher.LambdaMatcher.matches;
import static org.dspace.content.QAEvent.OPENAIRE_SOURCE;
import static org.dspace.matcher.QAEventMatcher.pendingOpenaireEventWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.qaevent.QASource;
import org.dspace.qaevent.QATopic;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link OpenaireEventsRunnable}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OpenaireEventsRunnableIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_JSON_DIR_PATH = "org/dspace/app/openaire-events/";

    private QAEventService qaEventService = new DSpace().getSingletonService(QAEventService.class);

    private Collection collection;

    @Before
    public void setup() {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .build();

        context.restoreAuthSystemState();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testManyEventsImport() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withTitle("Egypt, crossroad of translations and literary interweavings")
            .withHandle("123456789/99998")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withHandle("123456789/99999")
            .build();

        context.restoreAuthSystemState();

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "import-openaire-events", "-f", getFileLocation("events.json") };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());
        assertThat(handler.getInfoMessages(), contains("Found 2 events to store"));

        List<QASource> sources = qaEventService.findAllSources(0, 20);
        assertThat(sources, hasSize(1));

        assertThat(sources.get(0).getName(), is(OPENAIRE_SOURCE));
        assertThat(sources.get(0).getTotalEvents(), is(2L));

        List<QATopic> topics = qaEventService.findAllTopics(0, 20);
        assertThat(topics, hasSize(2));
        assertThat(topics, containsInAnyOrder(
            matches(topic -> topic.getKey().equals("ENRICH/MORE/PROJECT") && topic.getTotalEvents() == 1L),
            matches(topic -> topic.getKey().equals("ENRICH/MISSING/ABSTRACT") && topic.getTotalEvents() == 1L)));

        String projectMessage = "{\"projects[0].acronym\":\"PAThs\",\"projects[0].code\":\"687567\","
            + "\"projects[0].funder\":\"EC\",\"projects[0].fundingProgram\":\"H2020\","
            + "\"projects[0].jurisdiction\":\"EU\","
            + "\"projects[0].openaireId\":\"40|corda__h2020::6e32f5eb912688f2424c68b851483ea4\","
            + "\"projects[0].title\":\"Tracking Papyrus and Parchment Paths\"}";

        assertThat(qaEventService.findEventsByTopic("ENRICH/MORE/PROJECT"), contains(
            pendingOpenaireEventWith("oai:www.openstarts.units.it:123456789/99998", firstItem,
                "Egypt, crossroad of translations and literary interweavings", projectMessage,
                "ENRICH/MORE/PROJECT", 1.00d)));

        String abstractMessage = "{\"abstracts[0]\":\"Missing Abstract\"}";

        assertThat(qaEventService.findEventsByTopic("ENRICH/MISSING/ABSTRACT"), contains(
            pendingOpenaireEventWith("oai:www.openstarts.units.it:123456789/99999", secondItem, "Test Publication",
                abstractMessage, "ENRICH/MISSING/ABSTRACT", 1.00d)));

    }

    @Test
    public void testManyEventsImportWithUnknownHandle() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withHandle("123456789/99999")
            .build();

        context.restoreAuthSystemState();

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "import-openaire-events", "-f", getFileLocation("events.json") };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(),
            contains("IllegalArgumentException: Skipped event b4e09c71312cd7c397969f56c900823f" +
                " related to the oai record oai:www.openstarts.units.it:123456789/99998 as the record was not found"));
        assertThat(handler.getInfoMessages(), contains("Found 2 events to store"));

        List<QASource> sources = qaEventService.findAllSources(0, 20);
        assertThat(sources, hasSize(1));

        assertThat(sources.get(0).getName(), is(OPENAIRE_SOURCE));
        assertThat(sources.get(0).getTotalEvents(), is(1L));

        List<QATopic> topics = qaEventService.findAllTopics(0, 20);
        assertThat(topics, hasSize(1));
        QATopic topic = topics.get(0);
        assertThat(topic.getKey(), is("ENRICH/MISSING/ABSTRACT"));
        assertThat(topic.getTotalEvents(), is(1L));

        String abstractMessage = "{\"abstracts[0]\":\"Missing Abstract\"}";

        assertThat(qaEventService.findEventsByTopic("ENRICH/MISSING/ABSTRACT"), contains(
            pendingOpenaireEventWith("oai:www.openstarts.units.it:123456789/99999", item, "Test Publication",
                abstractMessage, "ENRICH/MISSING/ABSTRACT", 1.00d)));

    }

    @Test
    public void testManyEventsImportWithUnknownTopic() throws Exception {

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, collection)
            .withTitle("Egypt, crossroad of translations and literary interweavings")
            .withHandle("123456789/99998")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withHandle("123456789/99999")
            .build();

        context.restoreAuthSystemState();

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "import-openaire-events", "-f", getFileLocation("unknown-topic-events.json") };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(),
            contains("Event for topic ENRICH/MORE/UNKNOWN is not allowed in the qaevents.cfg"));
        assertThat(handler.getInfoMessages(), contains("Found 2 events to store"));

        List<QASource> sources = qaEventService.findAllSources(0, 20);
        assertThat(sources, hasSize(1));

        assertThat(sources.get(0).getName(), is(OPENAIRE_SOURCE));
        assertThat(sources.get(0).getTotalEvents(), is(1L));

        List<QATopic> topics = qaEventService.findAllTopics(0, 20);
        assertThat(topics, hasSize(1));
        QATopic topic = topics.get(0);
        assertThat(topic.getKey(), is("ENRICH/MISSING/ABSTRACT"));
        assertThat(topic.getTotalEvents(), is(1L));

        String abstractMessage = "{\"abstracts[0]\":\"Missing Abstract\"}";

        assertThat(qaEventService.findEventsByTopic("ENRICH/MISSING/ABSTRACT"), contains(
            pendingOpenaireEventWith("oai:www.openstarts.units.it:123456789/99999", secondItem, "Test Publication",
                abstractMessage, "ENRICH/MISSING/ABSTRACT", 1.00d)));

    }

    @Test
    public void testWithoutEvents() throws Exception {

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "import-openaire-events", "-f", getFileLocation("empty-events.json") };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);

        assertThat(handler.getErrorMessages(),
            contains(containsString("A not recoverable error occurs during OPENAIRE events import.")));
        assertThat(handler.getWarningMessages(),empty());
        assertThat(handler.getInfoMessages(), empty());

        List<QASource> sources = qaEventService.findAllSources(0, 20);
        assertThat(sources, hasSize(1));

        assertThat(sources.get(0).getName(), is(OPENAIRE_SOURCE));
        assertThat(sources.get(0).getTotalEvents(), is(0L));

        assertThat(qaEventService.findAllTopics(0, 20), empty());
    }

    private String getFileLocation(String fileName) throws Exception {
        URL resource = getClass().getClassLoader().getResource(BASE_JSON_DIR_PATH + fileName);
        if (resource == null) {
            throw new IllegalStateException("No resource found named " + BASE_JSON_DIR_PATH + fileName);
        }
        return new File(resource.getFile()).getAbsolutePath();
    }
}
