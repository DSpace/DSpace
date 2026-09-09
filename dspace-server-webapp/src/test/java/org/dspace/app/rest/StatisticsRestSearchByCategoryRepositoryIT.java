/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.dspace.app.rest.matcher.UsageReportMatcher.matchUsageReport;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_CATEGORIES_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_CITIES_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_CONTINENTS_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_COUNTRIES_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_ITEMS_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_DOWNLOADS_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_VISITS_PER_MONTH_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_VISITS_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_VISITS_REPORT_ID_RELATION_PERSON_PROJECTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_VISITS_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.matcher.UsageReportMatcher;
import org.dspace.app.rest.model.UsageReportPointCityRest;
import org.dspace.app.rest.model.UsageReportPointCountryRest;
import org.dspace.app.rest.model.UsageReportPointDateRest;
import org.dspace.app.rest.model.UsageReportPointDsoTotalVisitsRest;
import org.dspace.app.rest.model.UsageReportPointRest;
import org.dspace.app.rest.repository.StatisticsRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.builder.SiteBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.EventService;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.usage.UsageEvent;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Addendum Integration test to test the /api/statistics/usagereports/search
 * endpoints when the category parameter is used, see {@link UsageReportUtils},
 * {@link StatisticsRestRepository} and {@link StatisticsRestRepositoryIT}
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class StatisticsRestSearchByCategoryRepositoryIT extends AbstractControllerIntegrationTest {
    @Autowired
    private EventService eventService;
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    protected AuthorizeService authorizeService;

    private Site site;
    private Community communityCRIS;
    private Collection collectionPeople;
    private Collection collectionProjects;
    private Collection collectionPublications;

    // person 1 has project 1, 2, publication 1
    // person 2 has project 3, publication 1, 2
    // person 3 has NO project, publication 3
    // project 1 is referenced by publication 1-2, project 2 by publication 2
    // project 2 is only visible by eperson
    // publicationItemWithoutBitstream is only visible by admin
    // bitstream2Visited2 is only visible by eperson
    private Item publicationItem;
    private Item publication2Item;
    private Item publication3Item;
    private Item publicationItemWithoutBitstreams;
    private Item projectItem;
    private Item project2Item;
    private Item project3Item;
    private Item personItem;
    private Item person2Item;
    private Item person3Item;
    private Bitstream bitstreamNotVisited;
    private Bitstream bitstreamVisited;
    private Bitstream bitstream2Visited;
    private Bitstream bitstream2Visited2;
    private Bitstream bitstream3NotVisited;
    private Bitstream bitstreamProjVisited;
    private String loggedInToken;
    private String adminToken;
    private List<UsageReportPointRest> sitePoints;

    @BeforeClass
    public static void clearStatistics() throws Exception {
        // To ensure these tests start "fresh", clear out any existing statistics data.
        // NOTE: this is committed immediately in removeIndex()
        StatisticsServiceFactory.getInstance().getSolrLoggerService().removeIndex("*:*");
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Explicitly use solr commit in SolrLoggerServiceImpl#postView
        configurationService.setProperty("solr-statistics.autoCommit", false);
        context.turnOffAuthorisationSystem();
        site = SiteBuilder.createSite(context).build();
        communityCRIS = CommunityBuilder.createCommunity(context).build();
        collectionPeople = CollectionBuilder.createCollection(context, communityCRIS).withName("People")
                .withEntityType("Person").build();
        collectionProjects = CollectionBuilder.createCollection(context, communityCRIS).withName("Projects")
                .withEntityType("Project").build();
        collectionPublications = CollectionBuilder.createCollection(context, communityCRIS).withName("Publications")
                .withEntityType("Publication").build();

        // person 1 has project 1, 2, publication 1
        // person 2 has project 3, publication 1, 2
        // person 3 has NO project, publication 3
        // project 1 is referenced by publication 1-2, project 2 by publication 2
        // project 2 is only visible by eperson
        // publicationItemWithoutBitstream is only visible by admin
        // bitstream2Visited2 is only visible by eperson
        personItem = ItemBuilder.createItem(context, collectionPeople).withTitle("Person 1").build();
        person2Item = ItemBuilder.createItem(context, collectionPeople).withTitle("Person 2").build();
        person3Item = ItemBuilder.createItem(context, collectionPeople).withTitle("Person 3").build();

        projectItem = ItemBuilder.createItem(context, collectionProjects).withTitle("Project 1")
                .withProjectInvestigator("Person#1", personItem.getID().toString()).build();
        project2Item = ItemBuilder.createItem(context, collectionProjects).withTitle("Project 2")
                .withProjectCoinvestigators("Person#1", personItem.getID().toString()).build();
        authorizeService.removeAllPolicies(context, project2Item);
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(project2Item)
                             .withAction(Constants.READ)
                             .build();

        project3Item = ItemBuilder.createItem(context, collectionProjects).withTitle("Project 3")
                .withProjectInvestigator("Person#2", person2Item.getID().toString()).build();

        publicationItem = ItemBuilder.createItem(context, collectionPublications).withTitle("Publication 1")
                .withAuthor("Pers.1", personItem.getID().toString())
                .withAuthor("Pers.2", person2Item.getID().toString())
                .withRelationProject("proj#1", projectItem.getID().toString())
                .build();
        publication2Item = ItemBuilder.createItem(context, collectionPublications).withTitle("Publication 2")
                .withAuthor("Pers.2", person2Item.getID().toString())
                .withRelationProject("proj#1", projectItem.getID().toString())
                .withRelationProject("proj#2", project2Item.getID().toString())
                .build();
        publication3Item = ItemBuilder.createItem(context, collectionPublications).withTitle("Publication 3")
                .withAuthor("Pers.3", person3Item.getID().toString())
                .build();
        publicationItemWithoutBitstreams = ItemBuilder.createItem(context, collectionPublications)
                .withTitle("Publication no bitstreams").build();
        authorizeService.removeAllPolicies(context, publicationItemWithoutBitstreams);

        bitstreamNotVisited = BitstreamBuilder.createBitstream(context,
                publicationItem, toInputStream("test", UTF_8)).withName("BitstreamNotVisitedName").build();
        bitstreamVisited = BitstreamBuilder.createBitstream(context,
                publicationItem, toInputStream("test", UTF_8)).withName("BitstreamVisitedName").build();
        bitstream2Visited = BitstreamBuilder.createBitstream(context,
                publication2Item, toInputStream("test", UTF_8)).withName("Bitstream2VisitedName").build();
        bitstream2Visited2  = BitstreamBuilder.createBitstream(context,
                publication2Item, toInputStream("test", UTF_8)).withName("Bitstream2Visited2Name").build();
        authorizeService.removeAllPolicies(context, bitstream2Visited2);
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(bitstream2Visited2)
                             .withAction(Constants.READ)
                             .build();
        bitstream3NotVisited = BitstreamBuilder.createBitstream(context,
                publication3Item, toInputStream("test", UTF_8)).withName("Bitstream3NotVisitedName").build();
        bitstreamProjVisited = BitstreamBuilder.createBitstream(context,
                projectItem, toInputStream("test", UTF_8)).withName("BitstreamProjVisitedName").build();
        // publication 1 is visited 13
        // publication 2 is visited 15
        // publication 3 is visited 17
        // publication without bitstream is visited 11
        // bitstreamVisited is visited 19
        // bitstream2Visited is visited 3
        // bitstream2Visited2 is visited 7
        // bitstreamProjVisited is visited 1
        // person 1 is visited 1
        // person 2 is visited 3
        // person 3 has NO visit
        // project 1 is visited 5
        // project 2 is visited 2
        // project 3 has NO visit
        postView(publicationItem, 13);
        postView(publication2Item, 15);
        postView(publication3Item, 17);
        postView(publicationItemWithoutBitstreams, 11);
        postView(bitstreamVisited, 3);
        postView(bitstream2Visited, 3);
        postView(bitstream2Visited2, 7);
        postView(bitstreamProjVisited, 1);
        postView(personItem, 1);
        postView(person2Item, 3);
        postView(projectItem, 5);
        postView(project2Item, 2);
        context.restoreAuthSystemState();

        sitePoints = new ArrayList<>();
        sitePoints.add(getPoint(publicationItem, 13));
        sitePoints.add(getPoint(publication2Item, 15));
        sitePoints.add(getPoint(publication3Item, 17));
        sitePoints.add(getPoint(publicationItemWithoutBitstreams, 11));
        sitePoints.add(getPoint(personItem, 1));
        sitePoints.add(getPoint(person2Item, 3));
        sitePoints.add(getPoint(projectItem, 5));
        sitePoints.add(getPoint(project2Item, 2));
        loggedInToken = getAuthToken(eperson.getEmail(), password);
        adminToken = getAuthToken(admin.getEmail(), password);
    }

    @Test
    public void usagereportsSearch_noProperCategory_Exception() throws Exception {
        getClient(adminToken).perform(get("/api/statistics/usagereports/search/object")
                .param("uri", getItemUri(publicationItem))
                .param("category", "not-existing-category"))
                   .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        getClient(adminToken).perform(get("/api/statistics/usagereports/search/object")
                .param("uri", getItemUri(publicationItem))
                .param("category", "site-mainReports"))
                   .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void usagereportsSearch_NonExistentUUID_Exception() throws Exception {
        getClient(adminToken).perform(get("/api/statistics/usagereports/search/object")
                .param("uri", "http://localhost:8080/server/api/core/items/" + UUID.randomUUID().toString())
                .param("category", "item-mainReports"))
                   .andExpect(status().isNotFound());
    }

    // project 2 is only visible by eperson
    // publicationItemWithoutBitstream is only visible by admin
    // bitstream2Visited2 is only visible by eperson
    @Test
    public void usagereportSearch_onlyAdminReadRights() throws Exception {
        // ** WHEN **
        // We request a dso's TotalVisits usage stat report as anon but dso has no read policy for anon
        getClient().perform(get("/api/statistics/usagereports/search/object")
                .param("uri", getItemUri(publicationItemWithoutBitstreams))
                .param("category", "publication-mainReports"))
                   // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasNoJsonPath("$._embedded.usagereports")));
        getClient(loggedInToken).perform(get("/api/statistics/usagereports/search/object")
                .param("uri", getItemUri(publicationItemWithoutBitstreams))
                .param("category", "publication-mainReports"))
                   // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasNoJsonPath("$._embedded.usagereports")));

        // We request as admin
        getClient(adminToken).perform(get("/api/statistics/usagereports/search/object")
                .param("uri", getItemUri(publicationItemWithoutBitstreams))
                .param("category", "publication-mainReports"))
            // ** THEN **
            .andExpect(status().isOk());
    }

    @Test
    public void usagereportSearch_loggedInUserReadRights() throws Exception {
        // ** WHEN **
        context.turnOffAuthorisationSystem();
        EPerson eperson1 = EPersonBuilder.createEPerson(context)
                                         .withEmail("eperson1@mail.com")
                                         .withPassword(password)
                                         .build();
        context.restoreAuthSystemState();
        String anotherLoggedInUserToken = getAuthToken(eperson1.getEmail(), password);
        // We request a dso's TotalVisits usage stat report as anon but dso has no read policy for anon
        getClient().perform(get("/api/statistics/usagereports/search/object")
                .param("uri", getItemUri(project2Item))
                .param("category", "project-mainReports"))
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasNoJsonPath("$._embedded.usagereports")));
        // We request a dso's TotalVisits usage stat report as logged in eperson and has read policy for this user
        getClient(loggedInToken).perform(get("/api/statistics/usagereports/search/object")
                .param("uri", getItemUri(project2Item))
                .param("category", "project-mainReports"))
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasNoJsonPath("$._embedded.usagereports")));
        // We request a dso's TotalVisits usage stat report as another logged in eperson and has no read policy for
        // this user
        getClient(anotherLoggedInUserToken).perform(get("/api/statistics/usagereports/search/object")
                .param("uri", getItemUri(project2Item))
                .param("category", "project-mainReports"))
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasNoJsonPath("$._embedded.usagereports")));
    }

    @Test
    public void usageReportsSearch_Site() throws Exception {
        // And request the sites global usage report (show top most popular items)
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/sites/" + site.getID())
                    .param("category", "site-mainReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                matchUsageReport(site.getID() + "_" + TOTAL_VISITS_REPORT_ID, TOP_ITEMS_REPORT_ID, sitePoints),
                matchUsageReport(site.getID() + "_" + TOP_CITIES_REPORT_ID, TOP_CITIES_REPORT_ID),
                matchUsageReport(site.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                    TOTAL_VISITS_PER_MONTH_REPORT_ID),
                matchUsageReport(site.getID() + "_" + TOP_CONTINENTS_REPORT_ID, TOP_CONTINENTS_REPORT_ID),
                matchUsageReport(site.getID() + "_" + TOP_CATEGORIES_REPORT_ID, TOP_CATEGORIES_REPORT_ID),
                matchUsageReport(site.getID() + "_" + TOP_COUNTRIES_REPORT_ID, TOP_COUNTRIES_REPORT_ID))));
    }

    @Test
    public void usageReportsSearch_Publication() throws Exception {
        // publication 1 is visited 13
        // publication 2 is visited 15
        // publication 3 is visited 17
        // publication without bitstream is visited 11
        // bitstreamVisited is visited 19
        // bitstream2Visited is visited 3
        // bitstream2Visited2 is visited 7
        // bitstreamProjVisited is visited 1
        // person 1 is visited 1
        // person 2 is visited 3
        // person 3 has NO visit
        // project 1 is visited 5
        // project 2 is visited 2
        // project 3 has NO visit

        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + publicationItem.getID())
                    .param("category", "publication-mainReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                UsageReportMatcher
                    .matchUsageReport(publicationItem.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID, "table", Arrays.asList(getPoint(publicationItem, 13))),
                UsageReportMatcher
                    .matchUsageReport(publicationItem.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID, "chart.line"),
                UsageReportMatcher
                    .matchUsageReport(publicationItem.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(publicationItem.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID, "table"),
                UsageReportMatcher
                    .matchUsageReport(publicationItem.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                            TOTAL_DOWNLOADS_REPORT_ID, Arrays.asList(getDownloadPoint(bitstreamVisited, 3)))
            )));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + publication2Item.getID())
                    .param("category", "publication-mainReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                UsageReportMatcher
                    .matchUsageReport(publication2Item.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(publication2Item, 15))),
                UsageReportMatcher
                    .matchUsageReport(publication2Item.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(publication2Item.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(publication2Item.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(publication2Item.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                            TOTAL_DOWNLOADS_REPORT_ID,
                            Arrays.asList(getDownloadPoint(bitstream2Visited2, 7),
                                    getDownloadPoint(bitstream2Visited, 3)))
            )));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + publication3Item.getID())
                    .param("category", "publication-mainReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                UsageReportMatcher
                    .matchUsageReport(publication3Item.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(publication3Item, 17))),
                UsageReportMatcher
                    .matchUsageReport(publication3Item.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(publication3Item.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(publication3Item.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(publication3Item.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                            TOTAL_DOWNLOADS_REPORT_ID, new ArrayList()))
            ));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + publicationItemWithoutBitstreams.getID())
                    .param("category", "publication-mainReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                UsageReportMatcher
                    .matchUsageReport(publicationItemWithoutBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(publicationItemWithoutBitstreams, 11))),
                UsageReportMatcher
                    .matchUsageReport(publicationItemWithoutBitstreams.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(publicationItemWithoutBitstreams.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(publicationItemWithoutBitstreams.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(publicationItemWithoutBitstreams.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                            TOTAL_DOWNLOADS_REPORT_ID, new ArrayList()))
            ));
    }

    @Test
    public void usageReportsSearch_Project() throws Exception {
        // project 1 is referenced by publication 1-2, project 2 by publication 2
        // publication 1 is visited 13
        // publication 2 is visited 15
        // publication 3 is visited 17
        // bitstreamVisited is visited 19
        // bitstream2Visited is visited 3
        // bitstream2Visited2 is visited 7
        // bitstreamProjVisited is visited 1
        // project 1 is visited 5
        // project 2 is visited 2
        // project 3 has NO visit

        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + projectItem.getID())
                    .param("category", "project-mainReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                UsageReportMatcher
                    .matchUsageReport(projectItem.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(projectItem, 5))),
                UsageReportMatcher
                    .matchUsageReport(projectItem.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(projectItem.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(projectItem.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(projectItem.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                            TOTAL_DOWNLOADS_REPORT_ID, Arrays.asList(getPoint(bitstreamProjVisited, 1)))
            )));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + projectItem.getID())
                    .param("category", "project-publicationsReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                UsageReportMatcher
                    .matchUsageReport(projectItem.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(projectItem, 5)))
            )));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + project2Item.getID())
                    .param("category", "project-mainReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                UsageReportMatcher
                    .matchUsageReport(project2Item.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(project2Item, 2))),
                UsageReportMatcher
                    .matchUsageReport(project2Item.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(project2Item.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(project2Item.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(project2Item.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                            TOTAL_DOWNLOADS_REPORT_ID, new ArrayList()))
            ));

        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + project2Item.getID())
                    .param("category", "project-publicationsReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                UsageReportMatcher
                    .matchUsageReport(project2Item.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(project2Item, 2)))
            )));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + project3Item.getID())
                    .param("category", "project-mainReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                UsageReportMatcher
                    .matchUsageReport(project3Item.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(project3Item, 0))),
                UsageReportMatcher
                    .matchUsageReport(project3Item.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(project3Item.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(project3Item.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(project3Item.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                            TOTAL_DOWNLOADS_REPORT_ID, new ArrayList()))

            ));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + project3Item.getID())
                    .param("category", "project-publicationsReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                UsageReportMatcher
                    .matchUsageReport(project3Item.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(project3Item, 0)))
            )));
    }

    @Test
    @Ignore
    public void usageReportsSearch_Person() throws Exception {
        // publication 1 is visited 13
        // publication 2 is visited 15
        // publication 3 is visited 17
        // publication without bitstream is visited 11
        // bitstreamVisited is visited 19
        // bitstream2Visited is visited 3
        // bitstream2Visited2 is visited 7
        // bitstreamProjVisited is visited 1
        // person 1 is visited 1
        // person 2 is visited 3
        // person 3 has NO visit
        // project 1 is visited 5
        // project 2 is visited 2
        // project 3 has NO visit

        // person 1 has project 1, 2, publication 1
        // person 2 has project 3, publication 1, 2
        // person 3 has NO project, publication 3
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + personItem.getID())
                    .param("category", "person-mainReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                UsageReportMatcher
                    .matchUsageReport(personItem.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(personItem, 1))),
                UsageReportMatcher
                    .matchUsageReport(personItem.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(personItem.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(personItem.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID)
            )));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + personItem.getID())
                    .param("category", "person-projectsReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.hasItem(
                UsageReportMatcher
                    .matchUsageReport(personItem.getID() + "_" + TOTAL_VISITS_REPORT_ID_RELATION_PERSON_PROJECTS,
                            TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(personItem, 7)))
            )));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + personItem.getID())
                    .param("category", "person-publicationsReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.hasItem(
                UsageReportMatcher
                                .matchUsageReport(
                                        personItem.getID() + "_"
                                                + TOTAL_VISITS_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS,
                                                TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(personItem, 13)))
            )));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + person2Item.getID())
                    .param("category", "person-mainReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.hasItems(
                UsageReportMatcher
                    .matchUsageReport(person2Item.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(person2Item, 3))),
                UsageReportMatcher
                    .matchUsageReport(person2Item.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(person2Item.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(person2Item.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID)
            )));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + person2Item.getID())
                    .param("category", "person-projectsReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.hasItem(
                UsageReportMatcher
                    .matchUsageReport(person2Item.getID() + "_" + TOTAL_VISITS_REPORT_ID_RELATION_PERSON_PROJECTS,
                            TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(person2Item, 0)))
            )));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + person2Item.getID())
                    .param("category", "person-publicationsReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.hasItems(
                        UsageReportMatcher.matchUsageReport(
                                person2Item.getID() + "_" + TOTAL_VISITS_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS,
                                TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(person2Item, 28)))
            )));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + person3Item.getID())
                    .param("category", "person-mainReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.hasItems(
                UsageReportMatcher
                    .matchUsageReport(person3Item.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(person3Item, 0))),
                UsageReportMatcher
                    .matchUsageReport(person3Item.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(person3Item.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID),
                UsageReportMatcher
                    .matchUsageReport(person3Item.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID)
            )));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + person3Item.getID())
                    .param("category", "person-projectsReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.hasItem(
                        UsageReportMatcher.matchUsageReport(
                                person3Item.getID() + "_" + TOTAL_VISITS_REPORT_ID_RELATION_PERSON_PROJECTS,
                                TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(person3Item, 0)))
            )));
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                    .param("uri", "http://localhost:8080/server/api/core/items/" + person3Item.getID())
                    .param("category", "person-publicationsReports")
                    )
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.hasItem(
                        UsageReportMatcher.matchUsageReport(
                                person3Item.getID() + "_" + TOTAL_VISITS_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS,
                                TOTAL_VISITS_REPORT_ID, Arrays.asList(getPoint(person3Item, 17)))
            )));
    }

    // Create expected points from -6 months to now, with given number of views in current month
    private List<UsageReportPointRest> getListOfVisitsPerMonthsPoints(int viewsLastMonth) {
        List<UsageReportPointRest> expectedPoints = new ArrayList<>();
        int nrOfMonthsBack = 6;
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i <= nrOfMonthsBack; i++) {
            UsageReportPointDateRest expectedPoint = new UsageReportPointDateRest();
            if (i > 0) {
                expectedPoint.addValue("views", 0);
            } else {
                expectedPoint.addValue("views", viewsLastMonth);
            }
            String month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
            expectedPoint.setId(month + " " + cal.get(Calendar.YEAR));

            expectedPoints.add(expectedPoint);
            cal.add(Calendar.MONTH, -1);
        }
        return expectedPoints;
    }

    private UsageReportPointCountryRest getCountyPoint(String countryCode, String label, int count) {
        UsageReportPointCountryRest expectedPoint = new UsageReportPointCountryRest();
        expectedPoint.addValue("views", count);
        expectedPoint.setId(countryCode);
        expectedPoint.setLabel(label);
        return expectedPoint;
    }

    private UsageReportPointCityRest getCountyPoint(String city, int count) {
        UsageReportPointCityRest expectedPoint = new UsageReportPointCityRest();
        expectedPoint.addValue("views", count);
        expectedPoint.setId(city);
        return expectedPoint;
    }

    private UsageReportPointDsoTotalVisitsRest getPoint(DSpaceObject dso, int count) {
        UsageReportPointDsoTotalVisitsRest expectedPoint1 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint1.addValue("views", count);
        if (dso instanceof Item) {
            expectedPoint1.setType("item");
            expectedPoint1.setLabel(dso.getName());
            expectedPoint1.setId(dso.getID().toString());
        } else if (dso instanceof Bitstream) {
            expectedPoint1.setType("bitstream");
            expectedPoint1.setLabel(dso.getName());
            expectedPoint1.setId(dso.getID().toString());
        }
        return expectedPoint1;
    }

    private UsageReportPointDsoTotalVisitsRest getDownloadPoint(Bitstream bit, int count) {
        UsageReportPointDsoTotalVisitsRest expectedPoint1 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint1.addValue("views", count);
        expectedPoint1.setType("bitstream");
        expectedPoint1.setId(bit.getID().toString());
        expectedPoint1.setLabel(bit.getName());
        return expectedPoint1;
    }

    private void postView(DSpaceObject dso, int numView) throws Exception, SQLException, JsonProcessingException {
        HttpServletRequest req =  MockMvcRequestBuilders.post("/api/statistics/viewevents").buildRequest(null);
        UsageEvent usageEvent = new UsageEvent(UsageEvent.Action.VIEW, req, context, dso);
        for (int i = 0; i < numView; i++) {
            eventService.fireEvent(usageEvent);
        }
    }

    private String getItemUri(Item item) {
        return "http://localhost:8080/server/api/core/items/" + item.getID().toString();
    }
}
