/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.ProcessStatus;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.Process;
import org.dspace.scripts.service.ProcessService;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic integration testing for the SAF Export feature via UI {@link ItemExport}.
 * https://wiki.lyrasis.org/display/DSDOC7x/Importing+and+Exporting+Items+via+Simple+Archive+Format
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemExportIT extends AbstractControllerIntegrationTest {

    private static final String title = "A Tale of Two Cities";
    private static final String dateIssued = "1990";
    private static final String titleAlternative = "J'aime les Printemps";

    @Autowired
    private ItemService itemService;
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private ProcessService processService;
    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;
    private Collection collection;
    private Path workDir;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .withEntityType("Publication")
                .build();
        context.restoreAuthSystemState();

        File file = new File(configurationService.getProperty("org.dspace.app.itemexport.work.dir"));
        if (!file.exists()) {
            Files.createDirectory(Path.of(file.getAbsolutePath()));
        }
        workDir = Path.of(file.getAbsolutePath());
    }

    @After
    @Override
    public void destroy() throws Exception {
        for (Path path : Files.list(workDir).collect(Collectors.toList())) {
            PathUtils.delete(path);
        }
        super.destroy();
    }

    @Test
    public void exportCollection() throws Exception {
        // create items
        context.turnOffAuthorisationSystem();
        Item item1 = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        Item item2 = ItemBuilder.createItem(context, collection)
                .withTitle(title + " 2")
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        context.restoreAuthSystemState();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-t", "COLLECTION"));
        parameters.add(new DSpaceCommandLineParameter("-i", collection.getHandle()));
        perfomExportScript(parameters);

        checkCollection();
    }

    @Test
    public void exportItemWithMetadataOnly() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        context.restoreAuthSystemState();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-t", "ITEM"));
        parameters.add(new DSpaceCommandLineParameter("-i", item.getHandle()));
        perfomExportScript(parameters);

        checkItem(item);
    }

    @Test
    public void exportItemWithBitstreams() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        // create bitstream
        String bitstreamContent = "TEST TEST TEST";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withMimeType("text/plain")
                    .build();
        }
        context.restoreAuthSystemState();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-t", "ITEM"));
        parameters.add(new DSpaceCommandLineParameter("-i", item.getHandle()));
        perfomExportScript(parameters);

        checkItem(item);
    }

    @Test
    public void exportItemWithAnotherMetadataSchema() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .withMetadata("dcterms", "title", "", title)
                .build();
        context.restoreAuthSystemState();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-t", "ITEM"));
        parameters.add(new DSpaceCommandLineParameter("-i", item.getHandle()));
        perfomExportScript(parameters);

        checkItem(item);
    }

    @Test
    public void migrateCollection() throws Exception {
        // create items
        context.turnOffAuthorisationSystem();
        Item item1 = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        Item item2 = ItemBuilder.createItem(context, collection)
                .withTitle(title + " 2")
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        context.restoreAuthSystemState();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-t", "COLLECTION"));
        parameters.add(new DSpaceCommandLineParameter("-i", collection.getHandle()));
        parameters.add(new DSpaceCommandLineParameter("-m", ""));
        perfomExportScript(parameters);

        checkCollection();
        checkItem(item1);
        checkItem(item2);
    }

    @Test
    public void migrateItemWithMetadataOnly() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        context.restoreAuthSystemState();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-t", "ITEM"));
        parameters.add(new DSpaceCommandLineParameter("-i", item.getHandle()));
        parameters.add(new DSpaceCommandLineParameter("-m", ""));
        perfomExportScript(parameters);

        checkItem(item);
    }

    @Test
    public void migrateItemWithBitstreams() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        // create bitstream
        String bitstreamContent = "TEST TEST TEST";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withMimeType("text/plain")
                    .build();
        }
        context.restoreAuthSystemState();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-t", "ITEM"));
        parameters.add(new DSpaceCommandLineParameter("-i", item.getHandle()));
        parameters.add(new DSpaceCommandLineParameter("-m", ""));
        perfomExportScript(parameters);

        checkItem(item);
    }

    @Test
    public void migrateItemWithAnotherMetadataSchema() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .withMetadata("dcterms", "title", "", title)
                .build();
        context.restoreAuthSystemState();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-t", "ITEM"));
        parameters.add(new DSpaceCommandLineParameter("-i", item.getHandle()));
        parameters.add(new DSpaceCommandLineParameter("-m", ""));
        perfomExportScript(parameters);

        checkItem(item);
    }

    /**
     * Check collection exists
     * @throws Exception
     */
    private void checkCollection() throws Exception {
        assertNotNull(collectionService.find(context, collection.getID()));
    }

    /**
     * Check item exists
     * @param item
     * @throws Exception
     */
    private void checkItem(Item item) throws Exception {
        assertNotNull(itemService.find(context, item.getID()));
    }

    private void perfomExportScript(LinkedList<DSpaceCommandLineParameter> parameters)
            throws Exception {
        Process process = null;

        List<ParameterValueRest> list = parameters.stream()
                .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                        .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                .collect(Collectors.toList());

        try {
            AtomicReference<Integer> idRef = new AtomicReference<>();
            String token = getAuthToken(admin.getEmail(), password);

            getClient(token)
                .perform(multipart("/api/system/scripts/export/processes")
                        .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", is(
                        ProcessMatcher.matchProcess("export",
                                String.valueOf(admin.getID()), parameters,
                                ProcessStatus.COMPLETED))))
                .andDo(result -> idRef
                    .set(read(result.getResponse().getContentAsString(), "$.processId")));

            process = processService.find(context, idRef.get());
            checkProcess(process);
        } finally {
            ProcessBuilder.deleteProcess(process.getID());
        }
    }

    private void checkProcess(Process process) {
        assertNotNull(process.getBitstreams());
        assertEquals(2, process.getBitstreams().size());
        assertEquals(1,
                process.getBitstreams().stream()
                .filter(b -> StringUtils.contains(b.getName(), ".log"))
                .count());
        assertEquals(1,
                process.getBitstreams().stream()
                .filter(b -> StringUtils.contains(b.getName(), ".zip"))
                .count());
    }
}
