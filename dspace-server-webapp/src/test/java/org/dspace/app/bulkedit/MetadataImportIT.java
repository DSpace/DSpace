/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static com.jayway.jsonpath.JsonPath.read;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ProcessStatus;
import org.dspace.content.Relationship;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.service.ProcessService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Integration tests for the MetadataImport script through ScriptRestRepository API.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MetadataImportIT extends AbstractEntityIntegrationTest {

    @Autowired
    private ItemService itemService;
    @Autowired
    private RelationshipService relationshipService;
    @Autowired
    private ProcessService processService;
    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

    private Collection collection;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        collection =
            CollectionBuilder.createCollection(context, community)
                             .withAdminGroup(eperson)
                             .build();
        context.restoreAuthSystemState();
    }

    @After
    public void after() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        List<Relationship> relationships = relationshipService.findAll(context);
        for (Relationship relationship : relationships) {
            relationshipService.delete(context, relationship);
        }
        context.restoreAuthSystemState();
    }

    @Test
    public void metadataImportTest() throws Exception {
        String[] csv = {"id,collection,dc.title,dc.contributor.author",
            "+," + collection.getHandle() + ",\"Test Import 1\"," + "\"Donald, SmithImported\""};

        performImportScript(csv, eperson);

        Item importedItem = findItemByName("Test Import 1");
        assertTrue(
            StringUtils.equals(
                itemService.getMetadata(
                    importedItem, "dc", "contributor", "author", Item.ANY).get(0).getValue(), "Donald, SmithImported"
            )
        );

        assertEquals(importedItem.getSubmitter(), eperson);

        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        context.restoreAuthSystemState();
    }

    private void performImportScript(String[] csv, EPerson eperson) throws Exception {
        File csvFile = File.createTempFile("dspace-test-import", "csv");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile), "UTF-8"));
        for (String csvLine : csv) {
            out.write(csvLine + "\n");
        }
        out.flush();
        out.close();
        MockMultipartFile mockCsvFile =
            new MockMultipartFile(
                "file", "metadata-import.csv",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, new FileInputStream(csvFile)
            );
        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-f", "metadata-import.csv"));
        parameters.add(new DSpaceCommandLineParameter("-s", ""));

        performImportScript(parameters, mockCsvFile, eperson);
    }

    private void performImportScript(
        LinkedList<DSpaceCommandLineParameter> parameters, MockMultipartFile csvFile, EPerson user)
        throws Exception {
        org.dspace.scripts.Process process = null;

        List<ParameterValueRest> list =
            parameters.stream()
                      .map(dSpaceCommandLineParameter ->
                               dSpaceRunnableParameterConverter.convert(dSpaceCommandLineParameter, Projection.DEFAULT)
                      )
                      .collect(Collectors.toList());

        try {
            AtomicReference<Integer> idRef = new AtomicReference<>();

            getClient(getAuthToken(user.getEmail(), password))
                .perform(multipart("/api/system/scripts/metadata-import/processes")
                             .file(csvFile)
                             .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", is(
                    ProcessMatcher.matchProcess("metadata-import",
                                                String.valueOf(user.getID()), parameters,
                                                ProcessStatus.COMPLETED))))
                .andDo(result -> idRef
                    .set(read(result.getResponse().getContentAsString(), "$.processId")));

            process = processService.find(context, idRef.get());
        } finally {
            ProcessBuilder.deleteProcess(process.getID());
        }
    }

    private Item findItemByName(String name) throws SQLException {
        Item importedItem = null;
        List<Item> allItems = IteratorUtils.toList(itemService.findAll(context));
        for (Item item : allItems) {
            if (item.getName().equals(name)) {
                importedItem = item;
            }
        }
        return importedItem;
    }


}
