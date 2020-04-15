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
import org.dspace.submit.lookup.PubmedEuropeOnlineDataLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test suite for the Pubmed Europe endpoint
 *
 * @author David Vivarelli
 */
public class PubmedEuropeOnlineDataLoaderTest extends AbstractControllerIntegrationTest {

    @Autowired
    private PubmedEuropeOnlineDataLoader onlineDataLoader;

    @Autowired
    MetadataListener metadataListener;

    private Map<String, DataLoader> dataLoaderMap;


    @Before
    public void setUp() throws Exception {
        super.setUp();
        // remove all dataloaders except PubmedEurope
        dataLoaderMap = metadataListener.getDataloadersMap();
        metadataListener.getDataloadersMap().clear();
        metadataListener.getDataloadersMap().put("pubmedEurope", onlineDataLoader);
    }

    @After
    public void destroy() throws Exception {
        // restore standard listener
        metadataListener.getDataloadersMap().clear();
        metadataListener.getDataloadersMap().putAll(dataLoaderMap);
        super.destroy();
    }


    @Test
    public void lookupPubmedEuropeMetadataTest() throws Exception {
        String pubmedId = "PPR139359";
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
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
        value.put("value", pubmedId);
        values.add(value);
        addId.add(new AddOperation("/sections/publication/dc.identifier.pmid", values));

        String pathBody = getPatchContent(addId);
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(patch("/api/submission/workspaceitems/" + witem.getID())
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON).content(pathBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.publication.['dc.identifier.pmid'][0].value",
                is(pubmedId)))
            .andExpect(jsonPath("$.sections.publication.['dc.title'][0].value",
                is("Evaluation of two commercial multiplex PCR tests for the diagnosis of acute " +
                    "respiratory infections in hospitalized children")))
            .andExpect(jsonPath("$.sections.publication.['dc.date.issued'][0].value",
                is("2020-04-04")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][0].value",
                is("Li, Guixia")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][1].value",
                is("Wang, Le")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][2].value",
                is("Yang, Shuo")))
            .andExpect(jsonPath("$.sections.publication_indexing.['dc.description.abstract'][0].value",
                is("<title>Abstract</title>  <p>  Background Acute respiratory tract infections " +
                    "(ARTI), including the common cold, pharyngitis, sinusitis, otitis media, " +
                    "tonsillitis, bronchiolitis and pneumonia are the most common diagnoses in " +
                    "pediatric patients, and account for most antibiotic prescriptions. A confirmed and " +
                    "rapid ARTI diagnosis is key to preventing antibiotic abuse. Recently, based on " +
                    "different detection principles, many multi-target molecular analyses that can " +
                    "detect dozens of pathogens at the same time have been developed, greatly improving " +
                    "sensitivity and shortening turnaround time. In this work, we performed a " +
                    "head-to-head comparative study between melting curve analysis (MCA) and capillary " +
                    "electrophoresis assay (CE) in the detection of nine respiratory pathogens in sputum " +
                    "samples collected from hospitalized children with ARTI. Methods By MCA and CE " +
                    "analysis, nine common respiratory pathogens were tested in hospitalized children< " +
                    "13 years of age who met the ARTI criteria respectively. Results A total of 237 " +
                    "children with sputum specimens were tested. For all the targets combined, the " +
                    "positive detection rate of XYRes-MCA was significantly higher than that of ResP-CE " +
                    "(72.2% vs. 63.7%, p=.002). Some pathogens were detected more often with MCA, such " +
                    "as parainfluenza virus, influenza B and coronavirus, and some pathogens do the " +
                    "opposite, such as adenovirus and influenza A (all p<.01). Very good kappa values " +
                    "for most of pathogens were observed, except for Influenza B and coronavirus " +
                    "(both κ=.39). Conclusions Multiplex melting curve and capillary electrophoresis " +
                    "assays performed similarly for the detection of common respiratory pathogens in " +
                    "hospitalized children, except for Influenza B and coronavirus. A higher sensitivity " +
                    "was observed in the melting curve assay. By using this sensitive and rapid test, it " +
                    "may be possible to achieve improved patient prognosis and antimicrobial " +
                    "management.  </p>")));

        getClient(token).perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.publication.['dc.identifier.pmid'][0].value",
                is(pubmedId)))
            .andExpect(jsonPath("$.sections.publication.['dc.title'][0].value",
                is("Evaluation of two commercial multiplex PCR tests for the diagnosis of acute " +
                    "respiratory infections in hospitalized children")))
            .andExpect(jsonPath("$.sections.publication.['dc.date.issued'][0].value",
                is("2020-04-04")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][0].value",
                is("Li, Guixia")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][1].value",
                is("Wang, Le")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][2].value",
                is("Yang, Shuo")))
            .andExpect(jsonPath("$.sections.publication_indexing.['dc.description.abstract'][0].value",
                is("<title>Abstract</title>  <p>  Background Acute respiratory tract infections " +
                    "(ARTI), including the common cold, pharyngitis, sinusitis, otitis media, " +
                    "tonsillitis, bronchiolitis and pneumonia are the most common diagnoses in " +
                    "pediatric patients, and account for most antibiotic prescriptions. A confirmed and " +
                    "rapid ARTI diagnosis is key to preventing antibiotic abuse. Recently, based on " +
                    "different detection principles, many multi-target molecular analyses that can " +
                    "detect dozens of pathogens at the same time have been developed, greatly improving " +
                    "sensitivity and shortening turnaround time. In this work, we performed a " +
                    "head-to-head comparative study between melting curve analysis (MCA) and capillary " +
                    "electrophoresis assay (CE) in the detection of nine respiratory pathogens in sputum " +
                    "samples collected from hospitalized children with ARTI. Methods By MCA and CE " +
                    "analysis, nine common respiratory pathogens were tested in hospitalized children< " +
                    "13 years of age who met the ARTI criteria respectively. Results A total of 237 " +
                    "children with sputum specimens were tested. For all the targets combined, the " +
                    "positive detection rate of XYRes-MCA was significantly higher than that of ResP-CE " +
                    "(72.2% vs. 63.7%, p=.002). Some pathogens were detected more often with MCA, such " +
                    "as parainfluenza virus, influenza B and coronavirus, and some pathogens do the " +
                    "opposite, such as adenovirus and influenza A (all p<.01). Very good kappa values " +
                    "for most of pathogens were observed, except for Influenza B and coronavirus " +
                    "(both κ=.39). Conclusions Multiplex melting curve and capillary electrophoresis " +
                    "assays performed similarly for the detection of common respiratory pathogens in " +
                    "hospitalized children, except for Influenza B and coronavirus. A higher sensitivity " +
                    "was observed in the melting curve assay. By using this sensitive and rapid test, it " +
                    "may be possible to achieve improved patient prognosis and antimicrobial " +
                    "management.  </p>")));
    }


    @Test
    public void lookupPubmedEuropeMetadataByDOITest() throws Exception {
        String doi = "10.21203/rs.3.rs-20478/v1";
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
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
            .andExpect(jsonPath("$.sections.publication.['dc.identifier.doi'][0].value",
                is(doi)))
            .andExpect(jsonPath("$.sections.publication.['dc.title'][0].value",
                is("Evaluation of two commercial multiplex PCR tests for the diagnosis of acute" +
                    " respiratory infections in hospitalized children")))
            .andExpect(jsonPath("$.sections.publication.['dc.date.issued'][0].value",
                is("2020-04-04")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][0].value",
                is("Li, Guixia")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][1].value",
                is("Wang, Le")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][2].value",
                is("Yang, Shuo")))
            .andExpect(jsonPath("$.sections.publication_indexing.['dc.description.abstract'][0].value",
                is("<title>Abstract</title>  <p>  Background Acute respiratory tract infections " +
                    "(ARTI), including the common cold, pharyngitis, sinusitis, otitis media, " +
                    "tonsillitis, bronchiolitis and pneumonia are the most common diagnoses in " +
                    "pediatric patients, and account for most antibiotic prescriptions. A confirmed and " +
                    "rapid ARTI diagnosis is key to preventing antibiotic abuse. Recently, based on " +
                    "different detection principles, many multi-target molecular analyses that can " +
                    "detect dozens of pathogens at the same time have been developed, greatly improving " +
                    "sensitivity and shortening turnaround time. In this work, we performed a " +
                    "head-to-head comparative study between melting curve analysis (MCA) and capillary " +
                    "electrophoresis assay (CE) in the detection of nine respiratory pathogens in sputum " +
                    "samples collected from hospitalized children with ARTI. Methods By MCA and CE " +
                    "analysis, nine common respiratory pathogens were tested in hospitalized children< " +
                    "13 years of age who met the ARTI criteria respectively. Results A total of 237 " +
                    "children with sputum specimens were tested. For all the targets combined, the " +
                    "positive detection rate of XYRes-MCA was significantly higher than that of ResP-CE " +
                    "(72.2% vs. 63.7%, p=.002). Some pathogens were detected more often with MCA, such " +
                    "as parainfluenza virus, influenza B and coronavirus, and some pathogens do the " +
                    "opposite, such as adenovirus and influenza A (all p<.01). Very good kappa values " +
                    "for most of pathogens were observed, except for Influenza B and coronavirus " +
                    "(both κ=.39). Conclusions Multiplex melting curve and capillary electrophoresis " +
                    "assays performed similarly for the detection of common respiratory pathogens in " +
                    "hospitalized children, except for Influenza B and coronavirus. A higher sensitivity " +
                    "was observed in the melting curve assay. By using this sensitive and rapid test, it " +
                    "may be possible to achieve improved patient prognosis and antimicrobial " +
                    "management.  </p>")));

        getClient(token).perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.publication.['dc.identifier.doi'][0].value",
                is(doi)))
            .andExpect(jsonPath("$.sections.publication.['dc.title'][0].value",
                is("Evaluation of two commercial multiplex PCR tests for the diagnosis of acute " +
                    "respiratory infections in hospitalized children")))
            .andExpect(jsonPath("$.sections.publication.['dc.date.issued'][0].value",
                is("2020-04-04")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][0].value",
                is("Li, Guixia")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][1].value",
                is("Wang, Le")))
            .andExpect(jsonPath("$.sections.publication.['dc.contributor.author'][2].value",
                is("Yang, Shuo")))
            .andExpect(jsonPath("$.sections.publication_indexing.['dc.description.abstract'][0].value",
                is("<title>Abstract</title>  <p>  Background Acute respiratory tract infections " +
                    "(ARTI), including the common cold, pharyngitis, sinusitis, otitis media, " +
                    "tonsillitis, bronchiolitis and pneumonia are the most common diagnoses in " +
                    "pediatric patients, and account for most antibiotic prescriptions. A confirmed and " +
                    "rapid ARTI diagnosis is key to preventing antibiotic abuse. Recently, based on " +
                    "different detection principles, many multi-target molecular analyses that can " +
                    "detect dozens of pathogens at the same time have been developed, greatly improving " +
                    "sensitivity and shortening turnaround time. In this work, we performed a " +
                    "head-to-head comparative study between melting curve analysis (MCA) and capillary " +
                    "electrophoresis assay (CE) in the detection of nine respiratory pathogens in sputum " +
                    "samples collected from hospitalized children with ARTI. Methods By MCA and CE " +
                    "analysis, nine common respiratory pathogens were tested in hospitalized children< " +
                    "13 years of age who met the ARTI criteria respectively. Results A total of 237 " +
                    "children with sputum specimens were tested. For all the targets combined, the " +
                    "positive detection rate of XYRes-MCA was significantly higher than that of ResP-CE " +
                    "(72.2% vs. 63.7%, p=.002). Some pathogens were detected more often with MCA, such " +
                    "as parainfluenza virus, influenza B and coronavirus, and some pathogens do the " +
                    "opposite, such as adenovirus and influenza A (all p<.01). Very good kappa values " +
                    "for most of pathogens were observed, except for Influenza B and coronavirus " +
                    "(both κ=.39). Conclusions Multiplex melting curve and capillary electrophoresis " +
                    "assays performed similarly for the detection of common respiratory pathogens in " +
                    "hospitalized children, except for Influenza B and coronavirus. A higher sensitivity " +
                    "was observed in the melting curve assay. By using this sensitive and rapid test, it " +
                    "may be possible to achieve improved patient prognosis and antimicrobial " +
                    "management.  </p>")));

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
