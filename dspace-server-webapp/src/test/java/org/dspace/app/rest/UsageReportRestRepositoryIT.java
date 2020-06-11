/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.dspace.app.rest.builder.BitstreamBuilder.createBitstream;
import static org.dspace.app.rest.builder.CollectionBuilder.createCollection;
import static org.dspace.app.rest.builder.CommunityBuilder.createCommunity;
import static org.dspace.app.rest.builder.CommunityBuilder.createSubCommunity;
import static org.dspace.app.rest.builder.ItemBuilder.createItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.ResourcePolicyBuilder;
import org.dspace.app.rest.matcher.UsageReportMatcher;
import org.dspace.app.rest.model.UsageReportPointCityRest;
import org.dspace.app.rest.model.UsageReportPointCountryRest;
import org.dspace.app.rest.model.UsageReportPointDateRest;
import org.dspace.app.rest.model.UsageReportPointDsoTotalVisitsRest;
import org.dspace.app.rest.model.UsageReportPointRest;
import org.dspace.app.rest.model.ViewEventRest;
import org.dspace.app.rest.repository.UsageReportRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Integration test to test the /api/statistics/usagereports/ endpoints, see {@link UsageReportRestRepository}
 *
 * @author Maria Verdonck (Atmire) on 10/06/2020
 */
public class UsageReportRestRepositoryIT extends AbstractControllerIntegrationTest {
    @Autowired
    protected AuthorizeService authorizeService;

    private Community communityNotVisited;
    private Community communityVisited;
    private Collection collectionNotVisited;
    private Collection collectionVisited;
    private Item itemNotVisitedWithBitstreams;
    private Item itemVisited;
    private Bitstream bitstreamNotVisited;
    private Bitstream bitstreamVisited;

    private String loggedInToken;
    private String adminToken;

    private static final String TOTAL_VISITS_REPORT_ID = "TotalVisits";
    private static final String TOTAL_VISITS_PER_MONTH_REPORT_ID = "TotalVisitsPerMonth";
    private static final String TOTAL_DOWNLOADS_REPORT_ID = "TotalDownloads";
    private static final String TOP_COUNTRIES_REPORT_ID = "TopCountries";
    private static final String TOP_CITIES_REPORT_ID = "TopCities";

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

        context.turnOffAuthorisationSystem();

        Community community = createCommunity(context).build();
        communityNotVisited = createSubCommunity(context, community).build();
        communityVisited = createSubCommunity(context, community).build();
        collectionNotVisited = createCollection(context, community).build();
        collectionVisited = createCollection(context, community).build();
        itemVisited = createItem(context, collectionNotVisited).build();
        itemNotVisitedWithBitstreams = createItem(context, collectionNotVisited).build();
        bitstreamNotVisited = createBitstream(context,
            itemNotVisitedWithBitstreams, toInputStream("test", UTF_8)).build();
        bitstreamVisited = createBitstream(context, itemNotVisitedWithBitstreams, toInputStream("test", UTF_8))
            .withName("Bitstream").build();

        loggedInToken = getAuthToken(eperson.getEmail(), password);
        adminToken = getAuthToken(admin.getEmail(), password);

        context.restoreAuthSystemState();
    }

    @Test
    public void usagereports_withoutId_NotImplementedException() throws Exception {
        getClient().perform(get("/api/statistics/usagereports"))
                   .andExpect(status().is(HttpStatus.METHOD_NOT_ALLOWED.value()));
    }

    @Test
    public void usagereports_notProperUUIDAndReportId_Exception() throws Exception {
        getClient().perform(get("/api/statistics/usagereports/notProperUUIDAndReportId"))
                   .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void usagereports_nonValidUUIDpart_Exception() throws Exception {
        getClient().perform(get("/api/statistics/usagereports/notAnUUID_TotalVisits"))
                   .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void usagereports_nonValidReportIDpart_Exception() throws Exception {
        getClient().perform(get("/api/statistics/usagereports/" + UUID.randomUUID() + "_NotValidReport"))
                   .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void usagereports_NonExistentUUID_Exception() throws Exception {
        getClient().perform(get("/api/statistics/usagereports/" + UUID.randomUUID() + "_" + TOTAL_VISITS_REPORT_ID))
                   .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void usagereport_onlyAdminReadRights() throws Exception {
        // ** WHEN **
        authorizeService.removeAllPolicies(context, itemNotVisitedWithBitstreams);
        // We request a dso's TotalVisits usage stat report as anon but dso has no read policy for anon
        getClient().perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isUnauthorized());
        // We request a dso's TotalVisits usage stat report as admin
        getClient(adminToken).perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                             // ** THEN **
                             .andExpect(status().isOk());
    }

    @Test
    public void usagereport_onlyAdminReadRights_unvalidToken() throws Exception {
        // ** WHEN **
        authorizeService.removeAllPolicies(context, itemNotVisitedWithBitstreams);
        // We request a dso's TotalVisits usage stat report with unvalid token
        getClient("unvalidToken").perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                                 // ** THEN **
                                 .andExpect(status().isForbidden());
    }

    @Test
    public void usagereport_loggedInUserReadRights() throws Exception {
        // ** WHEN **
        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, itemNotVisitedWithBitstreams);
        ResourcePolicyBuilder.createResourcePolicy(context)
                             .withDspaceObject(itemNotVisitedWithBitstreams)
                             .withAction(Constants.READ)
                             .withUser(eperson).build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
                                         .withEmail("eperson1@mail.com")
                                         .withPassword(password)
                                         .build();
        context.restoreAuthSystemState();
        String anotherLoggedInUserToken = getAuthToken(eperson1.getEmail(), password);
        // We request a dso's TotalVisits usage stat report as anon but dso has no read policy for anon
        getClient().perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isUnauthorized());
        // We request a dso's TotalVisits usage stat report as logged in eperson and has read policy for this user
        getClient(loggedInToken).perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                                // ** THEN **
                                .andExpect(status().isOk());
        // We request a dso's TotalVisits usage stat report as another logged in eperson and has no read policy for
        // this user
        getClient(anotherLoggedInUserToken).perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                                           // ** THEN **
                                           .andExpect(status().isForbidden());
    }

    @Test
    public void totalVisitsReport_Community_Visited() throws Exception {
        // ** WHEN **
        // We visit the community
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("community");
        viewEventRest.setTargetId(communityVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        UsageReportPointDsoTotalVisitsRest expectedPoint = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint.addValue("views", 1);
        expectedPoint.setType("community");
        expectedPoint.setId(communityVisited.getID().toString());

        // And request that community's TotalVisits stat report
        getClient().perform(
            get("/api/statistics/usagereports/" + communityVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(communityVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                               TOTAL_VISITS_REPORT_ID, Arrays.asList(expectedPoint))))
                             );
    }

    @Test
    public void totalVisitsReport_Community_NotVisited() throws Exception {
        // ** WHEN **
        // Community is never visited
        UsageReportPointDsoTotalVisitsRest expectedPoint = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint.addValue("views", 0);
        expectedPoint.setType("community");
        expectedPoint.setId(communityNotVisited.getID().toString());

        // And request that community's TotalVisits stat report
        getClient().perform(
            get("/api/statistics/usagereports/" + communityNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(communityNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                               TOTAL_VISITS_REPORT_ID, Arrays.asList(expectedPoint))))
                             );
    }

    @Test
    public void totalVisitsReport_Collection_Visited() throws Exception {
        // ** WHEN **
        // We visit the collection twice
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("collection");
        viewEventRest.setTargetId(collectionVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        UsageReportPointDsoTotalVisitsRest expectedPoint = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint.addValue("views", 2);
        expectedPoint.setType("collection");
        expectedPoint.setId(collectionVisited.getID().toString());

        // And request that collection's TotalVisits stat report
        getClient().perform(
            get("/api/statistics/usagereports/" + collectionVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(collectionVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                               TOTAL_VISITS_REPORT_ID, Arrays.asList(expectedPoint))))
                             );
    }

    @Test
    public void totalVisitsReport_Collection_NotVisited() throws Exception {
        // ** WHEN **
        // Collection is never visited
        UsageReportPointDsoTotalVisitsRest expectedPoint = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint.addValue("views", 0);
        expectedPoint.setType("collection");
        expectedPoint.setId(collectionNotVisited.getID().toString());

        // And request that collection's TotalVisits stat report
        getClient().perform(
            get("/api/statistics/usagereports/" + collectionNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(collectionNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                               TOTAL_VISITS_REPORT_ID, Arrays.asList(expectedPoint))))
                             );
    }

    @Test
    public void totalVisitsReport_Item_Visited() throws Exception {
        // ** WHEN **
        // We visit an Item
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(itemVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        UsageReportPointDsoTotalVisitsRest expectedPoint = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint.addValue("views", 1);
        expectedPoint.setType("item");
        expectedPoint.setId(itemVisited.getID().toString());

        // And request that collection's TotalVisits stat report
        getClient().perform(
            get("/api/statistics/usagereports/" + itemVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(itemVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                               TOTAL_VISITS_REPORT_ID, Arrays.asList(expectedPoint))))
                             );
    }

    @Test
    public void totalVisitsReport_Item_NotVisited() throws Exception {
        // ** WHEN **
        //Item is never visited
        UsageReportPointDsoTotalVisitsRest expectedPoint = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint.addValue("views", 0);
        expectedPoint.setType("item");
        expectedPoint.setId(itemNotVisitedWithBitstreams.getID().toString());

        // And request that item's TotalVisits stat report
        getClient().perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                               TOTAL_VISITS_REPORT_ID, Arrays.asList(expectedPoint))))
                             );
    }

    @Test
    public void totalVisitsReport_Bitstream_Visited() throws Exception {
        // ** WHEN **
        // We visit a Bitstream
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("bitstream");
        viewEventRest.setTargetId(bitstreamVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        UsageReportPointDsoTotalVisitsRest expectedPoint = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint.addValue("views", 1);
        expectedPoint.setType("bitstream");
        expectedPoint.setId(bitstreamVisited.getID().toString());

        // And request that bitstream's TotalVisits stat report
        getClient().perform(
            get("/api/statistics/usagereports/" + bitstreamVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(bitstreamVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                               TOTAL_VISITS_REPORT_ID, Arrays.asList(expectedPoint))))
                             );
    }

    @Test
    public void totalVisitsReport_Bitstream_NotVisited() throws Exception {
        // ** WHEN **
        // Bitstream is never visited
        UsageReportPointDsoTotalVisitsRest expectedPoint = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint.addValue("views", 0);
        expectedPoint.setType("bitstream");
        expectedPoint.setId(bitstreamNotVisited.getID().toString());

        // And request that bitstream's TotalVisits stat report
        getClient().perform(
            get("/api/statistics/usagereports/" + bitstreamNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(bitstreamNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                               TOTAL_VISITS_REPORT_ID, Arrays.asList(expectedPoint))))
                             );
    }

    @Test
    public void totalVisitsPerMonthReport_Item_Visited() throws Exception {
        // ** WHEN **
        // We visit an Item
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(itemVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        List<UsageReportPointRest> expectedPoints = this.getListOfVisitsPerMonthsPoints(1);

        // And request that item's TotalVisitsPerMonth stat report
        getClient().perform(
            get("/api/statistics/usagereports/" + itemVisited.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(itemVisited.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                               TOTAL_VISITS_PER_MONTH_REPORT_ID, expectedPoints))));
    }

    @Test
    public void totalVisitsPerMonthReport_Item_NotVisited() throws Exception {
        // ** WHEN **
        // Item is not visited
        List<UsageReportPointRest> expectedPoints = this.getListOfVisitsPerMonthsPoints(0);

        // And request that item's TotalVisitsPerMonth stat report
        getClient().perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" +
                TOTAL_VISITS_PER_MONTH_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(
                               itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                               TOTAL_VISITS_PER_MONTH_REPORT_ID, expectedPoints))));
    }

    @Test
    public void totalVisitsPerMonthReport_Collection_Visited() throws Exception {
        // ** WHEN **
        // We visit a Collection twice
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("collection");
        viewEventRest.setTargetId(collectionVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        List<UsageReportPointRest> expectedPoints = this.getListOfVisitsPerMonthsPoints(2);

        // And request that collection's TotalVisitsPerMonth stat report
        getClient().perform(
            get("/api/statistics/usagereports/" + collectionVisited.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(collectionVisited.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                               TOTAL_VISITS_PER_MONTH_REPORT_ID, expectedPoints))));
    }

    @Test
    public void TotalDownloadsReport_Bitstream() throws Exception {
        // ** WHEN **
        // We visit a Bitstream
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("bitstream");
        viewEventRest.setTargetId(bitstreamVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        UsageReportPointDsoTotalVisitsRest expectedPoint = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint.addValue("views", 1);
        expectedPoint.setType("bitstream");
        expectedPoint.setId(bitstreamVisited.getID().toString());

        // And request that bitstreams's TotalDownloads stat report
        getClient().perform(
            get("/api/statistics/usagereports/" + bitstreamVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(bitstreamVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                               TOTAL_DOWNLOADS_REPORT_ID, Arrays.asList(expectedPoint)))));
    }

    @Test
    public void TotalDownloadsReport_Item() throws Exception {
        // ** WHEN **
        // We visit an Item's bitstream
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("bitstream");
        viewEventRest.setTargetId(bitstreamVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        UsageReportPointDsoTotalVisitsRest expectedPoint = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint.addValue("views", 1);
        expectedPoint.setType("bitstream");
        expectedPoint.setId("Bitstream");

        // And request that item's TotalDownloads stat report
        getClient().perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" +
                TOTAL_DOWNLOADS_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                               TOTAL_DOWNLOADS_REPORT_ID, Arrays.asList(expectedPoint)))));
    }

    @Test
    public void TotalDownloadsReport_Item_NotVisited() throws Exception {
        // ** WHEN **
        // You don't visit an item's bitstreams
        // And request that item's TotalDownloads stat report
        getClient().perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" +
                TOTAL_DOWNLOADS_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                               TOTAL_DOWNLOADS_REPORT_ID, new ArrayList<>()))));
    }

    @Test
    public void TotalDownloadsReport_NotSupportedDSO_Collection() throws Exception {
        getClient()
            .perform(get("/api/statistics/usagereports/" + collectionVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID))
            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    /**
     * Note: Geolite response mocked in {@link org.dspace.statistics.MockSolrLoggerServiceImpl}
     */
    @Test
    public void topCountriesReport_Collection_Visited() throws Exception {
        // ** WHEN **
        // We visit a Collection
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("collection");
        viewEventRest.setTargetId(collectionVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        UsageReportPointCountryRest expectedPoint = new UsageReportPointCountryRest();
        expectedPoint.addValue("views", 1);
        expectedPoint.setId("US");
        expectedPoint.setLabel("United States");

        // And request that collection's TopCountries report
        getClient().perform(
            get("/api/statistics/usagereports/" + collectionVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(collectionVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                               TOP_COUNTRIES_REPORT_ID, Arrays.asList(expectedPoint)))));
    }

    /**
     * Note: Geolite response mocked in {@link org.dspace.statistics.MockSolrLoggerServiceImpl}
     */
    @Test
    public void topCountriesReport_Community_Visited() throws Exception {
        // ** WHEN **
        // We visit a Community twice
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("community");
        viewEventRest.setTargetId(communityVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        UsageReportPointCountryRest expectedPoint = new UsageReportPointCountryRest();
        expectedPoint.addValue("views", 2);
        expectedPoint.setId("US");
        expectedPoint.setLabel("United States");

        // And request that collection's TopCountries report
        getClient().perform(
            get("/api/statistics/usagereports/" + communityVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(communityVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                               TOP_COUNTRIES_REPORT_ID, Arrays.asList(expectedPoint)))));
    }

    /**
     * Note: Geolite response mocked in {@link org.dspace.statistics.MockSolrLoggerServiceImpl}
     */
    @Test
    public void topCountriesReport_Item_NotVisited() throws Exception {
        // ** WHEN **
        // Item is not visited
        // And request that item's TopCountries report
        getClient().perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" + TOP_COUNTRIES_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(itemNotVisitedWithBitstreams.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                               TOP_COUNTRIES_REPORT_ID, new ArrayList<>()))));
    }

    /**
     * Note: Geolite response mocked in {@link org.dspace.statistics.MockSolrLoggerServiceImpl}
     */
    @Test
    public void topCitiesReport_Item_Visited() throws Exception {
        // ** WHEN **
        // We visit an Item
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(itemVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        UsageReportPointCityRest expectedPoint = new UsageReportPointCityRest();
        expectedPoint.addValue("views", 1);
        expectedPoint.setId("New York");

        // And request that item's TopCities report
        getClient().perform(
            get("/api/statistics/usagereports/" + itemVisited.getID() + "_" + TOP_CITIES_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(itemVisited.getID() + "_" + TOP_CITIES_REPORT_ID,
                               TOP_CITIES_REPORT_ID, Arrays.asList(expectedPoint)))));
    }

    /**
     * Note: Geolite response mocked in {@link org.dspace.statistics.MockSolrLoggerServiceImpl}
     */
    @Test
    public void topCitiesReport_Community_Visited() throws Exception {
        // ** WHEN **
        // We visit a Community thrice
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("community");
        viewEventRest.setTargetId(communityVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
            .content(mapper.writeValueAsBytes(viewEventRest))
            .contentType(contentType))
                                .andExpect(status().isCreated());

        UsageReportPointCityRest expectedPoint = new UsageReportPointCityRest();
        expectedPoint.addValue("views", 3);
        expectedPoint.setId("New York");

        // And request that community's TopCities report
        getClient().perform(
            get("/api/statistics/usagereports/" + communityVisited.getID() + "_" + TOP_CITIES_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(communityVisited.getID() + "_" + TOP_CITIES_REPORT_ID,
                               TOP_CITIES_REPORT_ID, Arrays.asList(expectedPoint)))));
    }

    /**
     * Note: Geolite response mocked in {@link org.dspace.statistics.MockSolrLoggerServiceImpl}
     */
    @Test
    public void topCitiesReport_Collection_NotVisited() throws Exception {
        // ** WHEN **
        // Collection is not visited
        // And request that collection's TopCountries report
        getClient().perform(
            get("/api/statistics/usagereports/" + collectionNotVisited.getID() + "_" + TOP_CITIES_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher
                           .matchUsageReport(collectionNotVisited.getID() + "_" + TOP_CITIES_REPORT_ID,
                               TOP_CITIES_REPORT_ID, new ArrayList<>()))));
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
}
