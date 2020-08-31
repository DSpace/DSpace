/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test suite for the WorkspaceItem endpoint
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 *
 */
public class WorkspaceItemPatentStepIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    private static final String PATENT_COLLECTION_HANDLE_TEST = "123456789/patent-test";

    @Before
    @Override
    public void setUp() throws Exception {

        super.setUp();

        // disable file upload mandatory
        configurationService.setProperty("webui.submit.upload.required", false);
    }

    @Test
    /**
     * Test the metadata lookup
     *
     * @throws Exception
     */
    public void lookupWithpatentnoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // ** GIVEN **
        // 1. A community-collection structure with one parent community with
        // sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1, PATENT_COLLECTION_HANDLE_TEST)
                .withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1).build();

        List<Operation> addId = new ArrayList<Operation>();
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "DE102012108018");
        values.add(value);
        addId.add(new AddOperation("/sections/patent/dc.identifier.patentno", values));

        String patchBody = getPatchContent(addId);

        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                // testing lookup
                .andExpect(jsonPath("$.sections.patent['dc.title'][0].value",
                        is("Verfahren zur bedarfsgerechten Regelung einer Vorrichtung für eine Schichtlüftung "
                                + "und Vorrichtung für eine Schichtlüftung")))
                .andExpect(jsonPath("$.sections.patent['dc.date.issued'][0].value", is("2014-03-06")))
                .andExpect(jsonPath("$.sections.patent['dc.contributor.author'][0].value", is("HESSELBACH JENS [DE]")))
                .andExpect(jsonPath("$.sections.patent['dc.contributor.author'][1].value", is("SCHAEFER MIRKO [DE]")))
                .andExpect(jsonPath("$.sections.patent['dc.contributor.author'][2].value", is("DETZER RUEDIGER [DE]")))
                .andExpect(jsonPath("$.sections.patent_indexing['dc.description.abstract'][0].value",
                        is("Um ein Verfahren (100) zur Regelung einer Vorrichtung (10) für eine "
                                + "Schichtlüftung in einem zu belüftenden Raum (11), wobei sich eine "
                                + "Schichtgrenze (12) zwischen einer ersten geodätisch unteren Luftschicht "
                                + "(13) und einer zweiten geodätisch oberen Luftschicht (14) bildet "
                                + "(V2)/bereitzustellen, welche den Energiebedarf einer Vorrichtung zur "
                                + "Schichtlüftung durch eine flexible, vollautomatische, bedarfsgerechte "
                                + "Regelung der Höhe der Schichtgrenze senkt, wird vorgeschlagen, dass der "
                                + "Ist-Wert der Höhe der Schichtgrenze (12) an mindestens einem Ort (16) im "
                                + "zu belüftenden Raum (11) ermittelt wird (V4), dass der Ist-Wert mit "
                                + "einem Soll-Wert verglichen wird (V5), und dass einer Abweichung des "
                                + "Ist-Werts vom Soll-Wert durch Regelung der Vorrichtung (10) für eine "
                                + "Schichtlüftung entgegen gesteuert wird (V7).")));

        // verify that the patch changes have been persisted
        getClient(authToken)
                .perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                // testing lookup
                .andExpect(jsonPath("$.sections.patent['dc.title'][0].value",
                        is("Verfahren zur bedarfsgerechten Regelung einer Vorrichtung für eine Schichtlüftung "
                                + "und Vorrichtung für eine Schichtlüftung")))
                .andExpect(jsonPath("$.sections.patent['dc.date.issued'][0].value", is("2014-03-06")))
                .andExpect(jsonPath("$.sections.patent['dc.contributor.author'][0].value", is("HESSELBACH JENS [DE]")))
                .andExpect(jsonPath("$.sections.patent['dc.contributor.author'][1].value", is("SCHAEFER MIRKO [DE]")))
                .andExpect(jsonPath("$.sections.patent['dc.contributor.author'][2].value", is("DETZER RUEDIGER [DE]")))
                .andExpect(jsonPath("$.sections.patent_indexing['dc.description.abstract'][0].value",
                        is("Um ein Verfahren (100) zur Regelung einer Vorrichtung (10) für eine "
                                + "Schichtlüftung in einem zu belüftenden Raum (11), wobei sich eine "
                                + "Schichtgrenze (12) zwischen einer ersten geodätisch unteren Luftschicht "
                                + "(13) und einer zweiten geodätisch oberen Luftschicht (14) bildet "
                                + "(V2)/bereitzustellen, welche den Energiebedarf einer Vorrichtung zur "
                                + "Schichtlüftung durch eine flexible, vollautomatische, bedarfsgerechte "
                                + "Regelung der Höhe der Schichtgrenze senkt, wird vorgeschlagen, dass der "
                                + "Ist-Wert der Höhe der Schichtgrenze (12) an mindestens einem Ort (16) im "
                                + "zu belüftenden Raum (11) ermittelt wird (V4), dass der Ist-Wert mit "
                                + "einem Soll-Wert verglichen wird (V5), und dass einer Abweichung des "
                                + "Ist-Werts vom Soll-Wert durch Regelung der Vorrichtung (10) für eine "
                                + "Schichtlüftung entgegen gesteuert wird (V7).")));

    }

    @Test
    /**
     * Test the metadata lookup
     *
     * @throws Exception
     */
    public void lookupWithpatentnunmberTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // ** GIVEN **
        // 1. A community-collection structure with one parent community with
        // sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1, PATENT_COLLECTION_HANDLE_TEST)
                .withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1).build();

        List<Operation> addId = new ArrayList<Operation>();
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "DE102012108018");
        values.add(value);
        // dc.identifier.patentnumber is used instead of dc.identifier.patentno
        addId.add(new AddOperation("/sections/patent/dc.identifier.patentnumber", values));

        String patchBody = getPatchContent(addId);

        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                // testing lookup
                .andExpect(jsonPath("$.sections.patent['dc.title'][0].value",
                        is("Verfahren zur bedarfsgerechten Regelung einer Vorrichtung für eine Schichtlüftung "
                                + "und Vorrichtung für eine Schichtlüftung")))
                .andExpect(jsonPath("$.sections.patent['dc.date.issued'][0].value", is("2014-03-06")))
                .andExpect(jsonPath("$.sections.patent['dc.contributor.author'][0].value", is("HESSELBACH JENS [DE]")))
                .andExpect(jsonPath("$.sections.patent['dc.contributor.author'][1].value", is("SCHAEFER MIRKO [DE]")))
                .andExpect(jsonPath("$.sections.patent['dc.contributor.author'][2].value", is("DETZER RUEDIGER [DE]")))
                .andExpect(jsonPath("$.sections.patent_indexing['dc.description.abstract'][0].value",
                        is("Um ein Verfahren (100) zur Regelung einer Vorrichtung (10) für eine "
                                + "Schichtlüftung in einem zu belüftenden Raum (11), wobei sich eine "
                                + "Schichtgrenze (12) zwischen einer ersten geodätisch unteren Luftschicht "
                                + "(13) und einer zweiten geodätisch oberen Luftschicht (14) bildet "
                                + "(V2)/bereitzustellen, welche den Energiebedarf einer Vorrichtung zur "
                                + "Schichtlüftung durch eine flexible, vollautomatische, bedarfsgerechte "
                                + "Regelung der Höhe der Schichtgrenze senkt, wird vorgeschlagen, dass der "
                                + "Ist-Wert der Höhe der Schichtgrenze (12) an mindestens einem Ort (16) im "
                                + "zu belüftenden Raum (11) ermittelt wird (V4), dass der Ist-Wert mit "
                                + "einem Soll-Wert verglichen wird (V5), und dass einer Abweichung des "
                                + "Ist-Werts vom Soll-Wert durch Regelung der Vorrichtung (10) für eine "
                                + "Schichtlüftung entgegen gesteuert wird (V7).")));

        // verify that the patch changes have been persisted
        getClient(authToken)
                .perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                // testing lookup
                .andExpect(jsonPath("$.sections.patent['dc.title'][0].value",
                        is("Verfahren zur bedarfsgerechten Regelung einer Vorrichtung für eine Schichtlüftung "
                                + "und Vorrichtung für eine Schichtlüftung")))
                .andExpect(jsonPath("$.sections.patent['dc.date.issued'][0].value", is("2014-03-06")))
                .andExpect(jsonPath("$.sections.patent['dc.contributor.author'][0].value", is("HESSELBACH JENS [DE]")))
                .andExpect(jsonPath("$.sections.patent['dc.contributor.author'][1].value", is("SCHAEFER MIRKO [DE]")))
                .andExpect(jsonPath("$.sections.patent['dc.contributor.author'][2].value", is("DETZER RUEDIGER [DE]")))
                .andExpect(jsonPath("$.sections.patent_indexing['dc.description.abstract'][0].value",
                        is("Um ein Verfahren (100) zur Regelung einer Vorrichtung (10) für eine "
                                + "Schichtlüftung in einem zu belüftenden Raum (11), wobei sich eine "
                                + "Schichtgrenze (12) zwischen einer ersten geodätisch unteren Luftschicht "
                                + "(13) und einer zweiten geodätisch oberen Luftschicht (14) bildet "
                                + "(V2)/bereitzustellen, welche den Energiebedarf einer Vorrichtung zur "
                                + "Schichtlüftung durch eine flexible, vollautomatische, bedarfsgerechte "
                                + "Regelung der Höhe der Schichtgrenze senkt, wird vorgeschlagen, dass der "
                                + "Ist-Wert der Höhe der Schichtgrenze (12) an mindestens einem Ort (16) im "
                                + "zu belüftenden Raum (11) ermittelt wird (V4), dass der Ist-Wert mit "
                                + "einem Soll-Wert verglichen wird (V5), und dass einer Abweichung des "
                                + "Ist-Werts vom Soll-Wert durch Regelung der Vorrichtung (10) für eine "
                                + "Schichtlüftung entgegen gesteuert wird (V7).")));

    }

    @Test
    /**
     * Test the metadata lookup
     *
     * @throws Exception
     */
    public void lookupWithOnlyApplicationNumberTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // ** GIVEN **
        // 1. A community-collection structure with one parent community with
        // sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1, PATENT_COLLECTION_HANDLE_TEST)
                .withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1).build();

        List<Operation> addId = new ArrayList<Operation>();
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "202008015010");
        values.add(value);
        // dc.identifier.patentnumber is used instead of dc.identifier.patentno
        addId.add(new AddOperation("/sections/patent/dc.identifier.applicationnumber", values));

        String patchBody = getPatchContent(addId);

        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                // testing lookup
                .andExpect(jsonPath("$.sections.patent['dc.type']").doesNotExist());

        // verify that the patch changes have been persisted
        getClient(authToken)
                .perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                // testing lookup
                .andExpect(jsonPath("$.sections.patent['dc.type']").doesNotExist());

    }

    @Test
    /**
     * Test the metadata lookup
     *
     * @throws Exception
     */
    public void lookupWithApplicationNumberAndFilledDateTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // ** GIVEN **
        // 1. A community-collection structure with one parent community with
        // sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1, PATENT_COLLECTION_HANDLE_TEST)
                .withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1).build();

        List<Operation> addId = new ArrayList<Operation>();
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "202008015010");
        values.add(value);
        List<Map<String, String>> dateValues = new ArrayList<Map<String, String>>();
        value = new HashMap<String, String>();
        value.put("value", "2008-11-12");
        dateValues.add(value);
        addId.add(new AddOperation("/sections/patent/dc.identifier.applicationnumber", values));
        addId.add(new AddOperation("/sections/patent/dc.date.filled", dateValues));

        String patchBody = getPatchContent(addId);

        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                // testing lookup
                .andExpect(jsonPath("$.sections.patent['dc.title'][0].value",
                        is("Spielgerät für Schweine")))
                .andExpect(jsonPath("$.sections.patent['dc.date.issued'][0].value", is("2010-05-12")))
                .andExpect(jsonPath("$.sections.patent['dc.date.filled'][0].value", is("2008-11-12")))
                .andExpect(jsonPath("$.sections.patent['dc.identifier.applicationnumber'][0].value",
                        is("202008015010")))
                .andExpect(jsonPath("$.sections.patent['dc.identifier.applicationnumber'][1].value",
                        is("DE20082015010U")));

        // verify that the patch changes have been persisted
        getClient(authToken)
                .perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                // testing lookup
                .andExpect(jsonPath("$.sections.patent['dc.title'][0].value",
                        is("Spielgerät für Schweine")))
                .andExpect(jsonPath("$.sections.patent['dc.date.issued'][0].value", is("2010-05-12")))
                .andExpect(jsonPath("$.sections.patent['dc.date.filled'][0].value", is("2008-11-12")))
                .andExpect(jsonPath("$.sections.patent['dc.identifier.applicationnumber'][0].value",
                        is("202008015010")))
                .andExpect(jsonPath("$.sections.patent['dc.identifier.applicationnumber'][1].value",
                        is("DE20082015010U")));

    }
}
