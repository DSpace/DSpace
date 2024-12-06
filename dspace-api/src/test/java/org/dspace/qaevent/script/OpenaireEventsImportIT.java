/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.script;

import static java.util.List.of;
import static org.dspace.content.QAEvent.COAR_NOTIFY_SOURCE;
import static org.dspace.content.QAEvent.OPENAIRE_SOURCE;
import static org.dspace.matcher.QAEventMatcher.pendingOpenaireEventWith;
import static org.dspace.qaevent.service.impl.QAEventServiceImpl.QAEVENTS_SOURCES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

import eu.dnetlib.broker.BrokerClient;
import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.matcher.QASourceMatcher;
import org.dspace.matcher.QATopicMatcher;
import org.dspace.qaevent.QANotifyPatterns;
import org.dspace.qaevent.QATopic;
import org.dspace.qaevent.service.OpenaireClientFactory;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.qaevent.service.impl.OpenaireClientFactoryImpl;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration tests for {@link OpenaireEventsImport}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OpenaireEventsImportIT extends AbstractIntegrationTestWithDatabase {

    private static final String ORDER_FIELD = "topic";
    private static final String BASE_JSON_DIR_PATH = "org/dspace/app/openaire-events/";

    private QAEventService qaEventService = new DSpace().getSingletonService(QAEventService.class);

    private Collection collection;

    private BrokerClient brokerClient = OpenaireClientFactory.getInstance().getBrokerClient();

    private BrokerClient mockBrokerClient = mock(BrokerClient.class);

    private ConfigurationService configurationService = new DSpace().getConfigurationService();

    @Before
    public void setup() {

        context.turnOffAuthorisationSystem();
        // we want all the test in this class to be run using the administrator user as OpenAIRE events are only visible
        // to administrators.
        // Please note that test related to forbidden and unauthorized access to qaevent are included in
        // the QAEventRestRepositoryIT here we are only testing that the import script is creating the expected events
        context.setCurrentUser(admin);
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .build();

        context.restoreAuthSystemState();
        configurationService.setProperty(QAEVENTS_SOURCES, new String[] { QAEvent.OPENAIRE_SOURCE });
        ((OpenaireClientFactoryImpl) OpenaireClientFactory.getInstance()).setBrokerClient(mockBrokerClient);
    }

    @After
    public void after() {
        ((OpenaireClientFactoryImpl) OpenaireClientFactory.getInstance()).setBrokerClient(brokerClient);
    }

    @Test
    public void testWithoutParameters() throws Exception {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "import-openaire-events" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());
        assertThat(handler.getInfoMessages(), empty());

        Exception exception = handler.getException();
        assertThat(exception, instanceOf(IllegalArgumentException.class));
        assertThat(exception.getMessage(), is("One parameter between the location of the file and the email "
            + "must be entered to proceed with the import."));

        verifyNoInteractions(mockBrokerClient);
    }

    @Test
    public void testWithBothFileAndEmailParameters() throws Exception {
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "import-openaire-events", "-f", getFileLocation("events.json"),
            "-e", "test@user.com" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());
        assertThat(handler.getInfoMessages(), empty());

        Exception exception = handler.getException();
        assertThat(exception, instanceOf(IllegalArgumentException.class));
        assertThat(exception.getMessage(), is("Only one parameter between the location of the file and the email "
            + "must be entered to proceed with the import."));

        verifyNoInteractions(mockBrokerClient);
    }

    @Test
    public void testManyEventsImportFromFile() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = createItem("Test item", "123456789/99998");
        Item secondItem = createItem("Test item 2", "123456789/99999");

        context.restoreAuthSystemState();

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "import-openaire-events", "-f", getFileLocation("events.json") };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());
        assertThat(handler.getInfoMessages(), contains(
            "Trying to read the QA events from the provided file",
            "Found 5 events in the given file"));

        assertThat(qaEventService.findAllSources(context, 0, 20),
            hasItem(QASourceMatcher.with(OPENAIRE_SOURCE, 5L))
        );

        List<QATopic> topicList = qaEventService.findAllTopicsBySource(context, OPENAIRE_SOURCE, 0, 20,
            ORDER_FIELD, false);
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MORE_PROJECT, 1L)));
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MORE_PID, 1L)));
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID, 1L)));
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MISSING_PROJECT, 1L)));
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 1L)));

        String projectMessage = "{\"projects[0].acronym\":\"PAThs\",\"projects[0].code\":\"687567\","
            + "\"projects[0].funder\":\"EC\",\"projects[0].fundingProgram\":\"H2020\","
            + "\"projects[0].jurisdiction\":\"EU\","
            + "\"projects[0].openaireId\":\"40|corda__h2020::6e32f5eb912688f2424c68b851483ea4\","
            + "\"projects[0].title\":\"Tracking Papyrus and Parchment Paths\"}";

        assertThat(qaEventService.findEventsByTopic(context, OPENAIRE_SOURCE,
            QANotifyPatterns.TOPIC_ENRICH_MORE_PROJECT, 0, 20, ORDER_FIELD, true),
                contains(
            pendingOpenaireEventWith("oai:www.openstarts.units.it:123456789/99998", firstItem,
                "Egypt, crossroad of translations and literary interweavings", projectMessage,
                QANotifyPatterns.TOPIC_ENRICH_MORE_PROJECT, 1.00d)));

        String abstractMessage = "{\"abstracts[0]\":\"Missing Abstract\"}";

        assertThat(qaEventService.findEventsByTopic(context, OPENAIRE_SOURCE,
            QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 0, 20, ORDER_FIELD, true),
                contains(
            pendingOpenaireEventWith("oai:www.openstarts.units.it:123456789/99999",
                secondItem, "Test Publication",
                abstractMessage, QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 1.00d)));

        verifyNoInteractions(mockBrokerClient);
    }

    @Test
    public void testManyEventsImportFromFileWithUnknownHandle() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = createItem("Test item", "123456789/99999");

        context.restoreAuthSystemState();

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "import-openaire-events", "-f", getFileLocation("events.json") };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(),
                contains("An error occurs storing the event with id 406fb9c5656c7f11cac8995abb746887: "
                        + "Skipped event 406fb9c5656c7f11cac8995abb746887 related to the oai record "
                        + "oai:www.openstarts.units.it:123456789/99998 as the record was not found",
                        "An error occurs storing the event with id eafd747feee49cca7603d30ba4e768dc: "
                        + "Skipped event eafd747feee49cca7603d30ba4e768dc related to the oai record "
                        + "oai:www.openstarts.units.it:123456789/99998 as the record was not found"));
        assertThat(handler.getInfoMessages(), contains(
            "Trying to read the QA events from the provided file",
            "Found 5 events in the given file"));

        assertThat(qaEventService.findAllSources(context, 0, 20), hasItem(QASourceMatcher.with(OPENAIRE_SOURCE, 3L)));

        List<QATopic> topicList = qaEventService.findAllTopicsBySource(context, OPENAIRE_SOURCE, 0, 20,
            ORDER_FIELD, false);
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 1L)));
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MISSING_PROJECT, 1L)));
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MORE_PID, 1L)));

        String abstractMessage = "{\"abstracts[0]\":\"Missing Abstract\"}";

        assertThat(qaEventService.findEventsByTopic(context, OPENAIRE_SOURCE,
                QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 0, 20,
                ORDER_FIELD, false), contains(
            pendingOpenaireEventWith("oai:www.openstarts.units.it:123456789/99999", item, "Test Publication",
                abstractMessage, QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 1.00d)));

        verifyNoInteractions(mockBrokerClient);
    }

    @Test
    public void testManyEventsImportFromFileWithUnknownTopic() throws Exception {

        context.turnOffAuthorisationSystem();

        createItem("Test item", "123456789/99999");
        Item secondItem = createItem("Test item 2", "123456789/999991");

        context.restoreAuthSystemState();

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "import-openaire-events", "-f", getFileLocation("unknown-topic-events.json") };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(),
            contains("An error occurs storing the event with id 8307aa56769deba961faed7162d91aab:"
                   + " Skipped event 8307aa56769deba961faed7162d91aab related to the oai record"
                   + " oai:www.openstarts.units.it:123456789/99998 as the record was not found"));
        assertThat(handler.getInfoMessages(), contains(
            "Trying to read the QA events from the provided file",
            "Found 2 events in the given file"));

        assertThat(qaEventService.findAllSources(context, 0, 20), hasItem(QASourceMatcher.with(OPENAIRE_SOURCE, 1L)));

        assertThat(qaEventService.findAllTopicsBySource(context, OPENAIRE_SOURCE, 0, 20, ORDER_FIELD, false),
                contains(QATopicMatcher.with(org.dspace.qaevent.QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 1L)));

        String abstractMessage = "{\"abstracts[0]\":\"Missing Abstract\"}";

        assertThat(qaEventService.findEventsByTopic(context, OPENAIRE_SOURCE,
            QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 0, 20, ORDER_FIELD, false), contains(
            pendingOpenaireEventWith("oai:www.openstarts.units.it:123456789/999991", secondItem, "Test Publication 2",
                abstractMessage, org.dspace.qaevent.QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 1.00d)));

        verifyNoInteractions(mockBrokerClient);
    }

    @Test
    public void testImportFromFileWithoutEvents() throws Exception {

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "import-openaire-events", "-f", getFileLocation("empty-file.json") };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);

        assertThat(handler.getErrorMessages(),
            contains(containsString("A not recoverable error occurs during OPENAIRE events import")));
        assertThat(handler.getWarningMessages(),empty());
        assertThat(handler.getInfoMessages(), contains("Trying to read the QA events from the provided file"));

        assertThat(qaEventService.findAllSources(context, 0, 20), hasItem(QASourceMatcher.with(OPENAIRE_SOURCE, 0L)));

        assertThat(qaEventService.findAllTopics(context, 0, 20, ORDER_FIELD, false), empty());

        verifyNoInteractions(mockBrokerClient);
    }

    @Test
    public void testImportFromOpenaireBroker() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = createItem("Test item", "123456789/99998");
        Item secondItem = createItem("Test item 2", "123456789/99999");
        Item thirdItem = createItem("Test item 3", "123456789/999991");

        context.restoreAuthSystemState();

        URL openaireURL = new URL("http://api.openaire.eu/broker");

        when(mockBrokerClient.listSubscriptions(openaireURL, "user@test.com")).thenReturn(of("sub1", "sub2", "sub3"));

        doAnswer(i -> writeToOutputStream(i.getArgument(2, OutputStream.class), "events.json"))
            .when(mockBrokerClient).downloadEvents(eq(openaireURL), eq("sub1"), any());

        doAnswer(i -> writeToOutputStream(i.getArgument(2, OutputStream.class), "empty-events-list.json"))
            .when(mockBrokerClient).downloadEvents(eq(openaireURL), eq("sub2"), any());

        doAnswer(i -> writeToOutputStream(i.getArgument(2, OutputStream.class), "unknown-topic-events.json"))
            .when(mockBrokerClient).downloadEvents(eq(openaireURL), eq("sub3"), any());

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "import-openaire-events", "-e", "user@test.com" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(),
            contains("Event for topic ENRICH/MORE/UNKNOWN is not allowed in the qaevents.cfg"));
        assertThat(handler.getInfoMessages(), contains(
            "Trying to read the QA events from the OPENAIRE broker",
            "Found 3 subscriptions related to the given email",
            "Found 5 events from the subscription sub1",
            "Found 0 events from the subscription sub2",
            "Found 2 events from the subscription sub3"));

        assertThat(qaEventService.findAllSources(context, 0, 20), hasItem(QASourceMatcher.with(OPENAIRE_SOURCE, 6L)));

        List<QATopic> topicList = qaEventService.findAllTopicsBySource(context, OPENAIRE_SOURCE,0,20,ORDER_FIELD,false);
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MORE_PROJECT, 1L)));
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MORE_PID, 1L)));
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID, 1L)));
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MISSING_PROJECT, 1L)));
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 2L)));

        String projectMessage = "{\"projects[0].acronym\":\"PAThs\",\"projects[0].code\":\"687567\","
            + "\"projects[0].funder\":\"EC\",\"projects[0].fundingProgram\":\"H2020\","
            + "\"projects[0].jurisdiction\":\"EU\","
            + "\"projects[0].openaireId\":\"40|corda__h2020::6e32f5eb912688f2424c68b851483ea4\","
            + "\"projects[0].title\":\"Tracking Papyrus and Parchment Paths\"}";

        assertThat(qaEventService.findEventsByTopic(context, OPENAIRE_SOURCE,
                QANotifyPatterns.TOPIC_ENRICH_MORE_PROJECT, 0 , 20,
                ORDER_FIELD, false), contains(
            pendingOpenaireEventWith("oai:www.openstarts.units.it:123456789/99998", firstItem,
                "Egypt, crossroad of translations and literary interweavings", projectMessage,
                QANotifyPatterns.TOPIC_ENRICH_MORE_PROJECT, 1.00d)));

        String abstractMessage = "{\"abstracts[0]\":\"Missing Abstract\"}";

        assertThat(qaEventService.findEventsByTopic(context, OPENAIRE_SOURCE,
                QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 0, 20,
                ORDER_FIELD, false), containsInAnyOrder(
            pendingOpenaireEventWith("oai:www.openstarts.units.it:123456789/99999", secondItem, "Test Publication",
                abstractMessage, QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 1.00d),
            pendingOpenaireEventWith("oai:www.openstarts.units.it:123456789/999991", thirdItem, "Test Publication 2",
                abstractMessage, QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 1.00d)));

        verify(mockBrokerClient).listSubscriptions(openaireURL, "user@test.com");
        verify(mockBrokerClient).downloadEvents(eq(openaireURL), eq("sub1"), any());
        verify(mockBrokerClient).downloadEvents(eq(openaireURL), eq("sub2"), any());
        verify(mockBrokerClient).downloadEvents(eq(openaireURL), eq("sub3"), any());

        verifyNoMoreInteractions(mockBrokerClient);
    }

    @Test
    public void testImportFromOpenaireBrokerWithErrorDuringListSubscription() throws Exception {

        URL openaireURL = new URL("http://api.openaire.eu/broker");

        when(mockBrokerClient.listSubscriptions(openaireURL, "user@test.com"))
            .thenThrow(new RuntimeException("Connection refused"));

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "import-openaire-events", "-e", "user@test.com" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);

        assertThat(handler.getErrorMessages(),
            contains("A not recoverable error occurs during OPENAIRE events import: Connection refused"));
        assertThat(handler.getWarningMessages(), empty());
        assertThat(handler.getInfoMessages(), contains("Trying to read the QA events from the OPENAIRE broker"));

        assertThat(qaEventService.findAllSources(context, 0, 20), hasItem(QASourceMatcher.with(OPENAIRE_SOURCE, 0L)));

        assertThat(qaEventService.findAllTopics(context, 0, 20, ORDER_FIELD, false), empty());

        verify(mockBrokerClient).listSubscriptions(openaireURL, "user@test.com");

        verifyNoMoreInteractions(mockBrokerClient);

    }

    @Test
    public void testImportFromOpenaireBrokerWithErrorDuringEventsDownload() throws Exception {

        context.turnOffAuthorisationSystem();

        createItem("Test item", "123456789/99998");
        createItem("Test item 2", "123456789/99999");
        createItem("Test item 3", "123456789/999991");

        context.restoreAuthSystemState();

        URL openaireURL = new URL("http://api.openaire.eu/broker");

        when(mockBrokerClient.listSubscriptions(openaireURL, "user@test.com")).thenReturn(of("sub1", "sub2", "sub3"));

        doAnswer(i -> writeToOutputStream(i.getArgument(2, OutputStream.class), "events.json"))
            .when(mockBrokerClient).downloadEvents(eq(openaireURL), eq("sub1"), any());

        doThrow(new RuntimeException("Invalid subscription id"))
            .when(mockBrokerClient).downloadEvents(eq(openaireURL), eq("sub2"), any());

        doAnswer(i -> writeToOutputStream(i.getArgument(2, OutputStream.class), "unknown-topic-events.json"))
            .when(mockBrokerClient).downloadEvents(eq(openaireURL), eq("sub3"), any());

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "import-openaire-events", "-e", "user@test.com" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);

        assertThat(handler.getErrorMessages(), contains("An error occurs downloading the events "
            + "related to the subscription sub2: Invalid subscription id"));
        assertThat(handler.getWarningMessages(),
            contains("Event for topic ENRICH/MORE/UNKNOWN is not allowed in the qaevents.cfg"));
        assertThat(handler.getInfoMessages(), contains(
            "Trying to read the QA events from the OPENAIRE broker",
            "Found 3 subscriptions related to the given email",
            "Found 5 events from the subscription sub1",
            "Found 0 events from the subscription sub2",
            "Found 2 events from the subscription sub3"));

        assertThat(qaEventService.findAllSources(context, 0, 20), hasItem(QASourceMatcher.with(OPENAIRE_SOURCE, 6L)));

        List<QATopic> topicList = qaEventService.findAllTopicsBySource(context, OPENAIRE_SOURCE, 0, 20,
            ORDER_FIELD, false);
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MORE_PROJECT, 1L)));
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID, 1L)));
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MORE_PID, 1L)));
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MISSING_PROJECT, 1L)));
        assertThat(topicList, hasItem(QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 2L)));

        assertThat(qaEventService.findEventsByTopic(context, OPENAIRE_SOURCE,
                   QANotifyPatterns.TOPIC_ENRICH_MORE_PROJECT, 0, 20, ORDER_FIELD, false), hasSize(1));
        assertThat(qaEventService.findEventsByTopic(context, OPENAIRE_SOURCE,
                   QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 0, 20, ORDER_FIELD, false), hasSize(2));

        verify(mockBrokerClient).listSubscriptions(openaireURL, "user@test.com");
        verify(mockBrokerClient).downloadEvents(eq(openaireURL), eq("sub1"), any());
        verify(mockBrokerClient).downloadEvents(eq(openaireURL), eq("sub2"), any());
        verify(mockBrokerClient).downloadEvents(eq(openaireURL), eq("sub3"), any());

        verifyNoMoreInteractions(mockBrokerClient);
    }

    /**
     * Improper test for ENRICH/MORE/REVIEW qa. It has the COAR_NOTIFY source
     * which must be tested via LDNMessage at DNInboxControllerIT
    */
    @Test
    @Ignore
    public void testImportFromFileEventMoreReview() throws Exception {

        context.turnOffAuthorisationSystem();
        Item firstItem = createItem("Test item", "123456789/99998");
        Item secondItem = createItem("Test item 2", "123456789/99999");

        context.restoreAuthSystemState();

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "import-openaire-events", "-f", getFileLocation("event-more-review.json") };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);

        assertThat(qaEventService.findAllTopicsBySource(context, COAR_NOTIFY_SOURCE, 0, 20,
            ORDER_FIELD, false), contains(
            QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MORE_REVIEW, 1L)));

        assertThat(qaEventService.findAllSources(context, 0, 20),
            hasItem(QASourceMatcher.with(COAR_NOTIFY_SOURCE, 1L)));

        verifyNoInteractions(mockBrokerClient);
    }

    private Item createItem(String title, String handle) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withHandle(handle)
            .build();
    }

    private Void writeToOutputStream(OutputStream outputStream, String fileName) {
        try {
            byte[] fileContent = getFileContent(fileName);
            IOUtils.write(fileContent, outputStream);
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getFileContent(String fileName) throws Exception {
        String fileLocation = getFileLocation(fileName);
        try (FileInputStream fis = new FileInputStream(new File(fileLocation))) {
            return IOUtils.toByteArray(fis);
        }
    }

    private String getFileLocation(String fileName) throws Exception {
        URL resource = getClass().getClassLoader().getResource(BASE_JSON_DIR_PATH + fileName);
        if (resource == null) {
            throw new IllegalStateException("No resource found named " + BASE_JSON_DIR_PATH + fileName);
        }
        return new File(resource.getFile()).getAbsolutePath();
    }
}

