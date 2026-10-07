/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static com.jayway.jsonpath.JsonPath.read;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.ProcessStatus;
import org.dspace.content.Relationship;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.WorkspaceItemService;
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
 * Integration tests for BulkImportScript run through the ScriptRestRepository
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class BulkImportIT extends AbstractEntityIntegrationTest {

    private static final String BASE_XLS_DIR_PATH = "./target/testing/dspace/assetstore/bulk-import/";

    @Autowired
    private RelationshipService relationshipService;
    @Autowired
    private ProcessService processService;
    @Autowired
    private WorkspaceItemService workspaceItemService;
    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

    private Community community;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context).build();
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
    public void testCreatePatent() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        File xlsFile = getXlsFile("create-patent.xls");

        MockMultipartFile mockXlsFile =
            new MockMultipartFile(
                "file", "create-patent.xls",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, new FileInputStream(xlsFile)
            );
        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-c", patents.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-f", "create-patent.xls"));

        performImportScript(parameters, mockXlsFile, eperson);

        List<WorkspaceItem> patentsItems = this.workspaceItemService.findByCollection(context, patents);
        assertThat(patentsItems, hasSize(1));

        Item createdItem = patentsItems.get(0).getItem();
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Luca Stone", 1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 2)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, hasItems(with("dc.type", "Patent")));

    }

    private void performImportScript(
        LinkedList<DSpaceCommandLineParameter> parameters, MockMultipartFile xlsFile, EPerson user)
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
                .perform(multipart("/api/system/scripts/bulk-import/processes")
                             .file(xlsFile)
                             .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", is(
                    ProcessMatcher.matchProcess("bulk-import",
                                                String.valueOf(user.getID()), parameters,
                                                ProcessStatus.COMPLETED))))
                .andDo(result -> idRef
                    .set(read(result.getResponse().getContentAsString(), "$.processId")));

            process = processService.find(context, idRef.get());
        } finally {
            ProcessBuilder.deleteProcess(process.getID());
        }
    }


    private WorkspaceItem findWorkspaceItem(Item item) throws SQLException {
        return workspaceItemService.findByItem(context, item);
    }


    private File getXlsFile(String name) {
        return new File(BASE_XLS_DIR_PATH, name);
    }

}
