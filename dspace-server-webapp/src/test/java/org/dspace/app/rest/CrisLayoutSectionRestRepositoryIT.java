/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withBrowseComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withFacetComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndBrowseComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndCountersComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndFacetComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndSearchComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndTopComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withSearchComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withTopComponent;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.layout.CrisLayoutSection;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.dspace.layout.service.impl.CrisLayoutSectionServiceImpl;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link CrisLayoutSectionRestRepository}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisLayoutSectionRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    CrisLayoutSectionServiceImpl crisLayoutSectionService;

    @Test
    public void testFindAll() throws Exception {

        String[] expectedBrowseNames = new String[] { "rodept", "author", "rsoTitle", "type", "dateissued", "subject" };

        getClient().perform(get("/api/layout/sections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.sections", hasSize(4)))

            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndBrowseComponent("researchoutputs", 0, 0, "col-md-4", expectedBrowseNames))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("researchoutputs", 0, 1, "col-md-8", "researchoutputs"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTopComponent("researchoutputs", 1, 0, "col-md-6",
                    "researchoutputs", "dc.date.accessioned", "desc", 5))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTopComponent("researchoutputs", 1, 1, "col-md-6",
                                    "researchoutputs", "metric.view", "desc", 5))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndFacetComponent("researchoutputs", 2, 0, "col-md-12", "researchoutputs"))))

            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndBrowseComponent("researcherprofiles", 0, 0, "col-md-4", "rpname", "rpdept"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("researcherprofiles", 0, 1, "col-md-8", "person"))))

            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndBrowseComponent("fundings_and_projects", 0, 0, "col-md-4", "pjtitle"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("fundings_and_projects", 0, 1, "col-md-8", "project_funding"))))


            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("site", 0, 0, "col-md-12", null))))

            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndCountersComponent("site", 1, 0, "col-md-12 py-4", Arrays.asList("researchoutputs",
                                                                                                 "project_funding",
                                                                                                 "person")))))

            .andExpect(jsonPath("$._embedded.sections",
              hasItem(withIdAndTopComponent("site", 2, 0, "col-md-6", "homePageTopItems", "dc.date.accessioned",
                                            "desc", 5))))
            .andExpect(jsonPath("$._embedded.sections",
              hasItem(withIdAndTopComponent("site", 2, 1, "col-md-6", "homePageTopItems", "metric.view",
                                            "desc", 5))))
            ;
    }

    @Test
    public void testSearchVisibleTopBarSections() throws Exception {

        List<CrisLayoutSection> originalSections = new LinkedList<>();
        originalSections.addAll(crisLayoutSectionService.getComponents());

        List<List<CrisLayoutSectionComponent>> components = new ArrayList<List<CrisLayoutSectionComponent>>();

        components.add(new ArrayList<CrisLayoutSectionComponent>());
        components.get(0).add(new org.dspace.layout.CrisLayoutSearchComponent());

        List<CrisLayoutSection> sectionsForMock = new LinkedList<>();
        sectionsForMock.add(new CrisLayoutSection("CasualIdForTestingPurposes1", true, components));
        sectionsForMock.add(new CrisLayoutSection("CasualIdForTestingPurposes2", false, components));
        sectionsForMock.add(new CrisLayoutSection("CasualIdForTestingPurposes3", true, components));


        //MOCKING the sections
        crisLayoutSectionService.getComponents().clear();
        crisLayoutSectionService.getComponents().addAll(sectionsForMock);
        //end setting up the mock

        try {
            getClient().perform(get("/api/layout/sections/search/visibleTopBarSections"))
                .andExpect(status().isOk())
                // Only 2 sections are set up to be visible in the top bar
                .andExpect(jsonPath("$._embedded.sections", hasSize(2)))
                // One has id -> CasualIdForTestingPurposes1
                .andExpect(jsonPath("$._embedded.sections[0].id", is("CasualIdForTestingPurposes1")))
                // The other has id -> CasualIdForTestingPurposes3
                .andExpect(jsonPath("$._embedded.sections[1].id", is("CasualIdForTestingPurposes3")));
        } catch (Exception e) {
            // Test Failed
        } finally {
            // Restoring situation previous to mock
            crisLayoutSectionService.getComponents().clear();
            crisLayoutSectionService.getComponents().addAll(originalSections);
            // end restoring
        }

    }

    @Test
    public void testFindOne() throws Exception {

        String[] expectedBrowseNames = new String[] { "rodept", "author", "rsoTitle", "type", "dateissued", "subject" };

        getClient().perform(get("/api/layout/sections/{id}", "researchoutputs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is("researchoutputs")))
            .andExpect(jsonPath("$", withBrowseComponent(0, 0, "col-md-4", expectedBrowseNames)))
            .andExpect(jsonPath("$", withSearchComponent(0, 1, "col-md-8", "researchoutputs")))
            .andExpect(jsonPath("$", withTopComponent(1, 0, "col-md-6", "researchoutputs",
                                                      "dc.date.accessioned", "desc", 5)))
            .andExpect(jsonPath("$", withTopComponent(1, 1, "col-md-6", "researchoutputs", "metric.view", "desc",
                5)))
            .andExpect(jsonPath("$", withFacetComponent(2, 0, "col-md-12", "researchoutputs")));
    }

    @Test
    public void testFindOneWithUnknownSectionId() throws Exception {

        getClient().perform(get("/api/layout/sections/{id}", "unknown-section-id"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testFindAllWithExistingOfNestedSections() throws Exception {

        List<CrisLayoutSection> originalSections = new LinkedList<>();
        originalSections.addAll(crisLayoutSectionService.getComponents());

        List<List<CrisLayoutSectionComponent>> components = new ArrayList<>();

        components.add(new ArrayList<>());
        components.get(0).add(new org.dspace.layout.CrisLayoutSearchComponent());

        List<CrisLayoutSection> sectionsForMock = new LinkedList<>();
        CrisLayoutSection sectionOne = new CrisLayoutSection("CasualIdForTestingPurposes1", true, components);
        CrisLayoutSection sectionTwo = new CrisLayoutSection("CasualIdForTestingPurposes2", true, null);
        sectionTwo.setNestedSections(List.of(sectionOne));
        CrisLayoutSection sectionThree = new CrisLayoutSection("CasualIdForTestingPurposes3", true, null);
        sectionThree.setNestedSections(List.of(sectionOne, sectionTwo));

        sectionsForMock.add(sectionOne);
        sectionsForMock.add(sectionTwo);
        sectionsForMock.add(sectionThree);

        crisLayoutSectionService.getComponents().clear();
        crisLayoutSectionService.getComponents().addAll(sectionsForMock);

        try {
            getClient().perform(get("/api/layout/sections"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.sections", hasSize(3)))
                       .andExpect(jsonPath("$._embedded.sections[0].id", is("CasualIdForTestingPurposes1")))
                       .andExpect(jsonPath("$._embedded.sections[0].nestedSections", empty()))
                       .andExpect(jsonPath("$._embedded.sections[1].id", is("CasualIdForTestingPurposes2")))
                       .andExpect(jsonPath("$._embedded.sections[1].componentRows", empty()))
                       .andExpect(jsonPath("$._embedded.sections[1].nestedSections",
                           Matchers.containsInAnyOrder(
                               hasJsonPath("$.id", is(sectionOne.getId()))
                           )))
                       .andExpect(jsonPath("$._embedded.sections[2].id", is("CasualIdForTestingPurposes3")))
                       .andExpect(jsonPath("$._embedded.sections[2].componentRows", empty()))
                       .andExpect(jsonPath("$._embedded.sections[2].nestedSections",
                           Matchers.containsInAnyOrder(
                               hasJsonPath("$.id", is(sectionOne.getId())),
                               hasJsonPath("$.id", is(sectionTwo.getId()))
                           )));
        } catch (Exception e) {
            // Test Failed
        } finally {
            // Restoring situation previous to mock
            crisLayoutSectionService.getComponents().clear();
            crisLayoutSectionService.getComponents().addAll(originalSections);
            // end restoring
        }
    }

    @Test
    public void testCreateCrisLayoutSectionWithNestedSectionsAndComponents() throws Exception {

        List<List<CrisLayoutSectionComponent>> components = new ArrayList<>();

        components.add(new ArrayList<>());
        components.get(0).add(new org.dspace.layout.CrisLayoutSearchComponent());

        CrisLayoutSection sectionOne =
            new CrisLayoutSection("CasualIdForTestingPurposes1", true, components);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            sectionOne.setNestedSections(
                List.of(new CrisLayoutSection("CasualIdForTestingPurposes2", true, components))
            );
        });

        assertTrue(exception.getMessage().contains(
            "cris layout section with id " + sectionOne.getId()
                + " accepts only sectionComponents or nestedSections"
            ));
    }

}
