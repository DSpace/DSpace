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

import gr.ekt.bte.core.DataLoader;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.WorkspaceItemBuilder;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.dspace.submit.listener.MetadataListener;
import org.dspace.submit.lookup.ADSOnlineDataLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Test suite for the ADS endpoint
 *
 * @author David Vivarelli
 */
public class ADSOnlineDataLoaderTest extends AbstractControllerIntegrationTest {

    @Autowired
    MetadataListener metadataListener;

    @Autowired
    private ADSOnlineDataLoader onlineDataLoader;

    private Map<String, DataLoader> dataLoaderMap;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // remove all providers in order to test just ADSOnlineDataLoader
        dataLoaderMap = metadataListener.getDataloadersMap();
        metadataListener.getDataloadersMap().clear();
        metadataListener.getDataloadersMap().put("ads", onlineDataLoader);
    }

    @After
    public void destroy() throws Exception {
        // restore standard listener
        metadataListener.getDataloadersMap().clear();
        metadataListener.getDataloadersMap().putAll(dataLoaderMap);
        super.destroy();
    }

    @Test
    public void lookupAdsMetadataTest() throws Exception {

        String adsCode = "2015zndo...1079909R";
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1, "123456789/publication-1")
                .withName("Collection 1").build();
        // make our eperson the submitter
        context.setCurrentUser(eperson);
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
            .build();
        context.restoreAuthSystemState();

        List<Operation> addId = new ArrayList<Operation>();
        // create a list of values to use in add operation
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", adsCode);
        values.add(value);
        addId.add(new AddOperation("/sections/publication/dc.identifier.adsbibcode", values));

        String pathBody = getPatchContent(addId);
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(patch("/api/submission/workspaceitems/" + witem.getID())
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON).content(pathBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.publication.['dc.title'][0].value", is("Cmaqv5.1")))
            .andExpect(jsonPath("$.sections.publication.['dc.date.issued'][0].value",
                is("2015")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][0].value",
                is("Research Development, US EPA Office of")))
            .andExpect(jsonPath("$.sections.publication_indexing.['dc.description.abstract'][0].value",
                is("The Community Multiscale Air Quality (CMAQ) model is an active open-source " +
                    "development project of the U.S. EPA that consists of a suite " +
                    "of programs for " +
                    "conducting air quality model simulations. CMAQ combines current knowledge in " +
                    "atmospheric science and air quality modeling, multi-processor computing techniques, " +
                    "and an open-source framework to deliver fast, technically sound estimates of ozone, " +
                    "particulates, toxics and acid deposition. For further information please visit the " +
                    "EPA website for the CMAQ system: www.epa.gov/cmaq CMAQv5.1 System Updates  Improved " +
                    "consistency in representation of radiation attenuation by clouds between WRF and " +
                    "photolysis module in CMAQ. Included the Rodas3 Rosenbrock solver to solve cloud " +
                    "chemistry, kinetic mass transfer, ionic dissociation, and wet deposition. " +
                    "Improvements to the land-surface model and ACM mixing scheme to enable finer-scale " +
                    "applications. Improvements in representation of aerosol mixing state and optical " +
                    "properties for 2-way coupled WRF-CMAQ configurations.  New Features and Processes " +
                    "in v5.1  Incorporated the RACM2 chemical mechanism. Included detailed " +
                    "representation of impacts of halogen chemistry on O3 in marine environments. " +
                    "Improved representation of O3 in coastal regions as well as representation " +
                    "of O3 loss in air masses transported intercontinentally across vast oceans. " +
                    "New secondary organic aerosol (SOA) sources from isoprene, alkanes, and " +
                    "polyaromatic hydrocarbons (PAHs). Incorporation of new binary nucleation and " +
                    "updates to PM2.5 emission size distribution to improve aerosol size distribution " +
                    "simulation. Included gravitational settling for coarse aerosols.")));

        getClient(token).perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.publication.['dc.title'][0].value", is("Cmaqv5.1")))
            .andExpect(jsonPath("$.sections.publication.['dc.date.issued'][0].value",
                is("2015")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][0].value",
                is("Research Development, US EPA Office of")))
            .andExpect(jsonPath("$.sections.publication_indexing.['dc.description.abstract'][0].value",
                is("The Community Multiscale Air Quality (CMAQ) model is an active open-source " +
                    "development project of the U.S. EPA that consists of a suite of programs for " +
                    "conducting air quality model simulations. CMAQ combines current knowledge in " +
                    "atmospheric science and air quality modeling, multi-processor computing techniques, " +
                    "and an open-source framework to deliver fast, technically sound estimates of ozone, " +
                    "particulates, toxics and acid deposition. For further information please visit the " +
                    "EPA website for the CMAQ system: www.epa.gov/cmaq CMAQv5.1 System Updates  Improved " +
                    "consistency in representation of radiation attenuation by clouds between WRF and " +
                    "photolysis module in CMAQ. Included the Rodas3 Rosenbrock solver to solve cloud " +
                    "chemistry, kinetic mass transfer, ionic dissociation, and wet deposition. " +
                    "Improvements to the land-surface model and ACM mixing scheme to enable finer-scale " +
                    "applications. Improvements in representation of aerosol mixing state and optical " +
                    "properties for 2-way coupled WRF-CMAQ configurations.  New Features and Processes " +
                    "in v5.1  Incorporated the RACM2 chemical mechanism. Included detailed " +
                    "representation of impacts of halogen chemistry on O3 in marine environments. " +
                    "Improved representation of O3 in coastal regions as well as representation " +
                    "of O3 loss in air masses transported intercontinentally across vast oceans. " +
                    "New secondary organic aerosol (SOA) sources from isoprene, alkanes, and " +
                    "polyaromatic hydrocarbons (PAHs). Incorporation of new binary nucleation and " +
                    "updates to PM2.5 emission size distribution to improve aerosol size distribution " +
                    "simulation. Included gravitational settling for coarse aerosols.")));

    }

    @Test
    public void lookupAdsMetadataByDOITest() throws Exception {

        String doi = "10.5281/zenodo.1079909";
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1, "123456789/publication-1")
                .withName("Collection 1").build();
        // make our eperson the submitter
        context.setCurrentUser(eperson);
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
            .build();
        context.restoreAuthSystemState();

        List<Operation> addId = new ArrayList<Operation>();
        // create a list of values to use in add operation
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", doi);
        values.add(value);
        addId.add(new AddOperation("/sections/publication/dc.identifier.doi", values));

        String pathBody = getPatchContent(addId);
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(patch("/api/submission/workspaceitems/" + witem.getID())
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON).content(pathBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.publication.['dc.title'][0].value", is("Cmaqv5.1")))
            .andExpect(jsonPath("$.sections.publication.['dc.date.issued'][0].value",
                is("2015")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][0].value",
                is("Research Development, US EPA Office of")))
            .andExpect(jsonPath("$.sections.publication_indexing.['dc.description.abstract'][0].value",
                is("The Community Multiscale Air Quality (CMAQ) model is an active open-source " +
                    "development project of the U.S. EPA that consists of a suite of programs for " +
                    "conducting air quality model simulations. CMAQ combines current knowledge in " +
                    "atmospheric science and air quality modeling, multi-processor computing techniques, " +
                    "and an open-source framework to deliver fast, technically sound estimates of ozone, " +
                    "particulates, toxics and acid deposition. For further information please visit the " +
                    "EPA website for the CMAQ system: www.epa.gov/cmaq CMAQv5.1 System Updates  Improved " +
                    "consistency in representation of radiation attenuation by clouds between WRF and " +
                    "photolysis module in CMAQ. Included the Rodas3 Rosenbrock solver to solve cloud " +
                    "chemistry, kinetic mass transfer, ionic dissociation, and wet deposition. " +
                    "Improvements to the land-surface model and ACM mixing scheme to enable finer-scale " +
                    "applications. Improvements in representation of aerosol mixing state and optical " +
                    "properties for 2-way coupled WRF-CMAQ configurations.  New Features and Processes " +
                    "in v5.1  Incorporated the RACM2 chemical mechanism. Included detailed " +
                    "representation of impacts of halogen chemistry on O3 in marine environments. " +
                    "Improved representation of O3 in coastal regions as well as representation " +
                    "of O3 loss in air masses transported intercontinentally across vast oceans. " +
                    "New secondary organic aerosol (SOA) sources from isoprene, alkanes, and " +
                    "polyaromatic hydrocarbons (PAHs). Incorporation of new binary nucleation and " +
                    "updates to PM2.5 emission size distribution to improve aerosol size distribution " +
                    "simulation. Included gravitational settling for coarse aerosols.")));


        getClient(token).perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.publication.['dc.title'][0].value", is("Cmaqv5.1")))
            .andExpect(jsonPath("$.sections.publication.['dc.date.issued'][0].value",
                is("2015")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][0].value",
                is("Research Development, US EPA Office of")))
            .andExpect(jsonPath("$.sections.publication_indexing.['dc.description.abstract'][0].value",
                is("The Community Multiscale Air Quality (CMAQ) model is an active open-source " +
                    "development project of the U.S. EPA that consists of a suite of programs for " +
                    "conducting air quality model simulations. CMAQ combines current knowledge in " +
                    "atmospheric science and air quality modeling, multi-processor computing techniques, " +
                    "and an open-source framework to deliver fast, technically sound estimates of ozone, " +
                    "particulates, toxics and acid deposition. For further information please visit the " +
                    "EPA website for the CMAQ system: www.epa.gov/cmaq CMAQv5.1 System Updates  Improved " +
                    "consistency in representation of radiation attenuation by clouds between WRF and " +
                    "photolysis module in CMAQ. Included the Rodas3 Rosenbrock solver to solve cloud " +
                    "chemistry, kinetic mass transfer, ionic dissociation, and wet deposition. " +
                    "Improvements to the land-surface model and ACM mixing scheme to enable finer-scale " +
                    "applications. Improvements in representation of aerosol mixing state and optical " +
                    "properties for 2-way coupled WRF-CMAQ configurations.  New Features and Processes " +
                    "in v5.1  Incorporated the RACM2 chemical mechanism. Included detailed " +
                    "representation of impacts of halogen chemistry on O3 in marine environments. " +
                    "Improved representation of O3 in coastal regions as well as representation " +
                    "of O3 loss in air masses transported intercontinentally across vast oceans. " +
                    "New secondary organic aerosol (SOA) sources from isoprene, alkanes, and " +
                    "polyaromatic hydrocarbons (PAHs). Incorporation of new binary nucleation and " +
                    "updates to PM2.5 emission size distribution to improve aerosol size distribution " +
                    "simulation. Included gravitational settling for coarse aerosols.")));
    }

    @Test
    public void lookupAdsMetadataBArXivTest() throws Exception {

        String arXiv = "2017arXiv170910372I";
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1, "123456789/publication-1")
                .withName("Collection 1").build();
        context.setCurrentUser(eperson);
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
            .build();
        context.restoreAuthSystemState();

        List<Operation> addId = new ArrayList<Operation>();
        // create a list of values to use in add operation
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", arXiv);
        values.add(value);
        addId.add(new AddOperation("/sections/publication/dc.identifier.arxiv", values));

        String pathBody = getPatchContent(addId);
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(patch("/api/submission/workspaceitems/" + witem.getID())
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON).content(pathBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.publication.['dc.title'][0].value",
                is("A New Approach for a Better Load Balancing and a Better Distribution of Resources" +
                    " in Cloud Computing")))
            .andExpect(jsonPath("$.sections.publication.['dc.date.issued'][0].value",
                is("2015")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][0].value",
                is("Idrissi, Abdellah")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][1].value",
                is("Zegrari, Faouzia")))
            .andExpect(jsonPath("$.sections.publication_indexing.['dc.description.abstract'][0].value",
                is("Cloud computing is a new paradigm where data and services of Information " +
                    "Technology are provided via the Internet by using remote servers. It represents " +
                    "a new way of delivering computing resources allowing access to the network on " +
                    "demand. Cloud computing consists of several services, each of which can hold " +
                    "several tasks. As the problem of scheduling tasks is an NP-complete problem, the " +
                    "task management can be an important element in the technology of cloud computing. " +
                    "To optimize the performance of virtual machines hosted in cloud computing, several " +
                    "algorithms of scheduling tasks have been proposed. In this paper, we present an " +
                    "approach allowing to solve the problem optimally and to take into account the QoS " +
                    "constraints based on the different user requests. This technique, based on the " +
                    "Branch and Bound algorithm, allows to assign tasks to different virtual machines " +
                    "while ensuring load balance and a better distribution of resources. The " +
                    "experimental results show that our approach gives very promising results for an " +
                    "effective tasks planning. - See more at: http://thesai.org/Publications/ViewPaper" +
                    "?Volume=6&amp;Issue=10&amp;Code=IJACSA&amp;SerialNo=36#sthash.aV1fxMaQ.dpuf <P />")));


        getClient(token).perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.publication.['dc.title'][0].value",
                is("A New Approach for a Better Load Balancing and a Better Distribution of Resources" +
                    " in Cloud Computing")))
            .andExpect(jsonPath("$.sections.publication.['dc.date.issued'][0].value",
                is("2015")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][0].value",
                is("Idrissi, Abdellah")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][1].value",
                is("Zegrari, Faouzia")))
            .andExpect(jsonPath("$.sections.publication_indexing.['dc.description.abstract'][0].value",
                is("Cloud computing is a new paradigm where data and services of Information " +
                    "Technology are provided via the Internet by using remote servers. It represents " +
                    "a new way of delivering computing resources allowing access to the network on " +
                    "demand. Cloud computing consists of several services, each of which can hold " +
                    "several tasks. As the problem of scheduling tasks is an NP-complete problem, the " +
                    "task management can be an important element in the technology of cloud computing. " +
                    "To optimize the performance of virtual machines hosted in cloud computing, several " +
                    "algorithms of scheduling tasks have been proposed. In this paper, we present an " +
                    "approach allowing to solve the problem optimally and to take into account the QoS " +
                    "constraints based on the different user requests. This technique, based on the " +
                    "Branch and Bound algorithm, allows to assign tasks to different virtual machines " +
                    "while ensuring load balance and a better distribution of resources. The " +
                    "experimental results show that our approach gives very promising results for an " +
                    "effective tasks planning. - See more at: http://thesai.org/Publications/ViewPaper" +
                    "?Volume=6&amp;Issue=10&amp;Code=IJACSA&amp;SerialNo=36#sthash.aV1fxMaQ.dpuf <P />")));


    }

    @Test
    public void lookupAdsMetadataByNotValidDOITest() throws Exception {

        String doi = "not_valid_doi";
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1, "123456789/publication-1")
                .withName("Collection 1").build();
        context.setCurrentUser(eperson);
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
            .build();
        context.restoreAuthSystemState();

        List<Operation> addId = new ArrayList<Operation>();
        // create a list of values to use in add operation
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", doi);
        values.add(value);
        addId.add(new AddOperation("/sections/publication/dc.identifier.doi", values));

        String pathBody = getPatchContent(addId);
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(patch("/api/submission/workspaceitems/" + witem.getID())
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON).content(pathBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.publication.['dc.title'][0].value").doesNotExist())
            .andExpect(jsonPath("$.sections.publication.['dc.date.issued'][0].value").doesNotExist());


        getClient(token).perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.publication.['dc.title'][0].value").doesNotExist())
            .andExpect(jsonPath("$.sections.publication.['dc.date.issued'][0].value").doesNotExist());
    }
}
