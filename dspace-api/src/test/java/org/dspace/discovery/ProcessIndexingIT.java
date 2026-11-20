/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.dspace.discovery.indexobject.ProcessIndexFactoryImpl.PROCESS_ARGUMENTS;
import static org.dspace.discovery.indexobject.ProcessIndexFactoryImpl.PROCESS_FINISH_TIME;
import static org.dspace.discovery.indexobject.ProcessIndexFactoryImpl.PROCESS_ID;
import static org.dspace.discovery.indexobject.ProcessIndexFactoryImpl.PROCESS_LOG;
import static org.dspace.discovery.indexobject.ProcessIndexFactoryImpl.PROCESS_NAME;
import static org.dspace.discovery.indexobject.ProcessIndexFactoryImpl.PROCESS_OUTPUT_FILE;
import static org.dspace.discovery.indexobject.ProcessIndexFactoryImpl.PROCESS_SIZE_BYTES;
import static org.dspace.discovery.indexobject.ProcessIndexFactoryImpl.PROCESS_START_TIME;
import static org.dspace.discovery.indexobject.ProcessIndexFactoryImpl.PROCESS_STATUS;
import static org.dspace.discovery.indexobject.ProcessIndexFactoryImpl.PROCESS_USER_EMAIL;
import static org.dspace.discovery.indexobject.ProcessIndexFactoryImpl.PROCESS_USER_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.ProcessStatus;
import org.dspace.discovery.DiscoverResult.SearchDocument;
import org.dspace.discovery.indexobject.IndexableProcess;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.Process;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ProcessService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for indexing {@link Process} entities in Discovery.
 */
public class ProcessIndexingIT extends AbstractIntegrationTestWithDatabase {

    private final ProcessService processService = ScriptServiceFactory.getInstance().getProcessService();
    private final IndexingService indexingService = DSpaceServicesFactory.getInstance().getServiceManager()
        .getServiceByName(IndexingService.class.getName(), IndexingService.class);
    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    private SearchService searchService;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        searchService = SearchUtils.getSearchService();
        configurationService.setProperty("process.index.log", false);
    }

    @Test
    public void processIsIndexedAndDiscoverableTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson owner = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Process", "Owner")
                                      .withEmail("process-owner@example.com")
                                      .withPassword(password)
                                      .build();
        context.setCurrentUser(owner);

        Process process = ProcessBuilder.createProcess(
            context,
            owner,
            "discovery-index-process",
            List.of(new DSpaceCommandLineParameter("-i", "input.csv"),
                    new DSpaceCommandLineParameter("--force", null)))
            .withProcessStatus(ProcessStatus.COMPLETED)
            .withStartAndEndTime("2026-01-02", "2026-01-03")
            .build();
        assertNotNull(process);

        byte[] input = "input".getBytes(StandardCharsets.UTF_8);
        byte[] output = "output".getBytes(StandardCharsets.UTF_8);
        processService.appendFile(context, process, new ByteArrayInputStream(input), "input_file", "input.csv");
        processService.appendFile(
            context,
            process,
            new ByteArrayInputStream(output),
            Process.OUTPUT_TYPE,
            "process.log"
        );
        context.dispatchEvents();
        indexingService.commit();

        DiscoverQuery query = new DiscoverQuery();
        query.setQuery("*:*");
        query.addFilterQueries(
            "search.resourcetype:Process",
            PROCESS_NAME + "_keyword:\"discovery-index-process\"",
            PROCESS_USER_EMAIL + "_keyword:\"process-owner@example.com\""
        );
        query.addSearchField(PROCESS_ID + "_i");
        query.addSearchField(PROCESS_NAME);
        query.addSearchField(PROCESS_USER_NAME);
        query.addSearchField(PROCESS_USER_EMAIL);
        query.addSearchField(PROCESS_START_TIME);
        query.addSearchField(PROCESS_FINISH_TIME);
        query.addSearchField(PROCESS_STATUS);
        query.addSearchField(PROCESS_ARGUMENTS);
        query.addSearchField(PROCESS_OUTPUT_FILE);
        query.addSearchField(PROCESS_SIZE_BYTES + "_l");
        query.addSearchField(PROCESS_LOG);

        DiscoverResult result = searchService.search(context, query);

        assertEquals(1, result.getTotalSearchResults());
        assertEquals(1, result.getIndexableObjects().size());
        IndexableObject<?, ?> indexedObject = result.getIndexableObjects().get(0);
        assertThat(indexedObject, instanceOf(IndexableProcess.class));
        assertEquals(process.getID(), indexedObject.getID());

        SearchDocument document = result.getSearchDocument(indexedObject).get(0);
        assertThat(document.getSearchFieldValues(PROCESS_ID + "_i"), contains(process.getID().toString()));
        assertThat(document.getSearchFieldValues(PROCESS_NAME), contains("discovery-index-process"));
        assertThat(document.getSearchFieldValues(PROCESS_USER_NAME), contains("Process Owner"));
        assertThat(document.getSearchFieldValues(PROCESS_USER_EMAIL), contains("process-owner@example.com"));
        assertThat(document.getSearchFieldValues(PROCESS_START_TIME), contains("2026-01-02"));
        assertThat(document.getSearchFieldValues(PROCESS_FINISH_TIME), contains("2026-01-03"));
        assertThat(document.getSearchFieldValues(PROCESS_STATUS), contains(ProcessStatus.COMPLETED.name()));
        assertThat(document.getSearchFieldValues(PROCESS_ARGUMENTS), contains("-i input.csv --force"));
        assertThat(document.getSearchFieldValues(PROCESS_OUTPUT_FILE), contains("process.log"));
        assertThat(document.getSearchFieldValues(PROCESS_SIZE_BYTES + "_l"),
                   contains(String.valueOf(input.length + output.length)));
        assertThat(document.getSearchFieldValues(PROCESS_LOG), empty());
    }
}
