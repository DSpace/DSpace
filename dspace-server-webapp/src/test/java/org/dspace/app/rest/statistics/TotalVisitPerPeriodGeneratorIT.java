/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.statistics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.sql.SQLException;
import java.time.ZonedDateTime;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.matcher.LambdaMatcher;
import org.dspace.app.rest.model.UsageReportPointRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.EmbeddedSolrClientFactory;
import org.dspace.statistics.SolrLoggerServiceImpl;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;
import org.dspace.util.MultiFormatDateParser;
import org.dspace.utils.DSpace;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link TotalVisitPerPeriodGenerator}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 *
 */
public class TotalVisitPerPeriodGeneratorIT extends AbstractIntegrationTestWithDatabase {

    private final SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();

    private final SiteService siteService = ContentServiceFactory.getInstance().getSiteService();

    private final ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
        .getConfigurationService();

    private final DiscoveryConfigurationService discoveryConfigurationService = new DSpace().getServiceManager()
        .getServicesByType(DiscoveryConfigurationService.class)
        .get(0);

    private final EmbeddedSolrClientFactory solrClientFactory = new EmbeddedSolrClientFactory();

    private Collection collection;

    private Site site;

    @Before
    public void setup() throws SQLException {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("My collection")
            .build();

        site = siteService.createSite(context);

        context.restoreAuthSystemState();

        configurationService.setProperty("solr-statistics.autoCommit", false);

    }

    @Test
    public void testSiteViewsWithMonthPeriodType() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item1 = createItem("First item");
        Item item2 = createItem("Second item");
        Item item3 = createItem("Third item");

        context.restoreAuthSystemState();

        view(item3, "2022-01-25");

        view(item2, "2022-02-13");
        view(item1, "2022-02-23");

        view(item2, "2022-04-13");

        view(item2, "2022-07-21");
        view(item3, "2022-07-12");

        view(item3, "2022-08-01");

        view(item3, "2022-09-12");
        view(item2, "2022-09-21");

        view(item1, "2022-11-15");

        view(item3, "2022-12-25");
        view(item3, "2022-12-26");

        view(item3, "2023-02-01");
        view(item1, "2023-02-04");
        view(item2, "2023-02-04");
        view(item1, "2023-02-05");
        view(item2, "2023-02-15");

        view(item1, "2023-03-03");
        view(item3, "2023-03-10");
        view(item3, "2023-03-21");

        TotalVisitPerPeriodGenerator generator = createGenerator("month", 1, Constants.ITEM);

        UsageReportRest usageReportRest = generator.createUsageReport(context, site, null, null);
        assertThat(usageReportRest.getPoints(), hasItems(
            dateWithViews("January 2022", 1),
            dateWithViews("February 2022", 2),
            dateWithViews("March 2022", 0),
            dateWithViews("April 2022", 1),
            dateWithViews("May 2022", 0),
            dateWithViews("June 2022", 0),
            dateWithViews("July 2022", 2),
            dateWithViews("August 2022", 1),
            dateWithViews("September 2022", 2),
            dateWithViews("October 2022", 0),
            dateWithViews("November 2022", 1),
            dateWithViews("December 2022", 2),
            dateWithViews("January 2023", 0),
            dateWithViews("February 2023", 5),
            dateWithViews("March 2023", 3)));

        usageReportRest = generator.createUsageReport(context, site, "2023-02-01", null);
        assertThat(usageReportRest.getPoints(), hasItems(
            dateWithViews("February 2023", 5),
            dateWithViews("March 2023", 3)));

        usageReportRest = generator.createUsageReport(context, site, "2022-10-15", null);
        assertThat(usageReportRest.getPoints(), hasItems(
            dateWithViews("October 2022", 0),
            dateWithViews("November 2022", 1),
            dateWithViews("December 2022", 2),
            dateWithViews("January 2023", 0),
            dateWithViews("February 2023", 5),
            dateWithViews("March 2023", 3)));

        usageReportRest = generator.createUsageReport(context, site, "2023-02-05", null);
        assertThat(usageReportRest.getPoints(), hasItems(
            dateWithViews("February 2023", 2),
            dateWithViews("March 2023", 3)));

        usageReportRest = generator.createUsageReport(context, site, null, "2023-02-28");
        assertThat(usageReportRest.getPoints(), contains(
            dateWithViews("January 2022", 1),
            dateWithViews("February 2022", 2),
            dateWithViews("March 2022", 0),
            dateWithViews("April 2022", 1),
            dateWithViews("May 2022", 0),
            dateWithViews("June 2022", 0),
            dateWithViews("July 2022", 2),
            dateWithViews("August 2022", 1),
            dateWithViews("September 2022", 2),
            dateWithViews("October 2022", 0),
            dateWithViews("November 2022", 1),
            dateWithViews("December 2022", 2),
            dateWithViews("January 2023", 0),
            dateWithViews("February 2023", 5)));

        usageReportRest = generator.createUsageReport(context, site, null, "2023-03-15");
        assertThat(usageReportRest.getPoints(), contains(
            dateWithViews("January 2022", 1),
            dateWithViews("February 2022", 2),
            dateWithViews("March 2022", 0),
            dateWithViews("April 2022", 1),
            dateWithViews("May 2022", 0),
            dateWithViews("June 2022", 0),
            dateWithViews("July 2022", 2),
            dateWithViews("August 2022", 1),
            dateWithViews("September 2022", 2),
            dateWithViews("October 2022", 0),
            dateWithViews("November 2022", 1),
            dateWithViews("December 2022", 2),
            dateWithViews("January 2023", 0),
            dateWithViews("February 2023", 5),
            dateWithViews("March 2023", 2)));

        usageReportRest = generator.createUsageReport(context, site, "2023-02-05", "2023-03-15");
        assertThat(usageReportRest.getPoints(), contains(
            dateWithViews("February 2023", 2),
            dateWithViews("March 2023", 2)));

        usageReportRest = generator.createUsageReport(context, site, "2023-03-14", "2023-03-15");
        assertThat(usageReportRest.getPoints(), contains(dateWithViews("March 2023", 0)));


    }

    @Test
    public void testSiteViewsWithYearPeriodType() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item1 = createItem("First item");
        Item item2 = createItem("Second item");
        Item item3 = createItem("Third item");

        context.restoreAuthSystemState();

        view(item2, "2020-04-13");

        view(item1, "2021-02-23");
        view(item2, "2021-04-13");
        view(item2, "2021-07-21");
        view(item3, "2021-07-12");

        view(item3, "2022-01-25");
        view(item2, "2022-02-13");
        view(item1, "2022-02-23");
        view(item2, "2022-04-13");
        view(item2, "2022-07-21");
        view(item3, "2022-07-12");
        view(item3, "2022-08-01");
        view(item3, "2022-09-12");
        view(item3, "2022-12-26");

        view(item3, "2023-01-01");
        view(item3, "2023-02-01");
        view(item1, "2023-02-04");
        view(item1, "2023-03-03");
        view(item3, "2023-03-21");

        TotalVisitPerPeriodGenerator generator = createGenerator("year", 1, Constants.ITEM);

        UsageReportRest usageReportRest = generator.createUsageReport(context, site, null, null);
        assertThat(usageReportRest.getPoints(), hasItems(
            dateWithViews("2020", 1),
            dateWithViews("2021", 4),
            dateWithViews("2022", 9),
            dateWithViews("2023", 5)));

        usageReportRest = generator.createUsageReport(context, site, "2021-02-01", null);
        assertThat(usageReportRest.getPoints(), hasItems(
            dateWithViews("2021", 4),
            dateWithViews("2022", 9),
            dateWithViews("2023", 5)));

        usageReportRest = generator.createUsageReport(context, site, "2021-05-15", null);
        assertThat(usageReportRest.getPoints(), hasItems(
            dateWithViews("2021", 2),
            dateWithViews("2022", 9),
            dateWithViews("2023", 5)));

        usageReportRest = generator.createUsageReport(context, site, "2022-07-05", null);
        assertThat(usageReportRest.getPoints(), hasItems(
            dateWithViews("2022", 5),
            dateWithViews("2023", 5)));

        usageReportRest = generator.createUsageReport(context, site, null, "2022-12-31");
        assertThat(usageReportRest.getPoints(), contains(
            dateWithViews("2020", 1),
            dateWithViews("2021", 4),
            dateWithViews("2022", 9)));

        usageReportRest = generator.createUsageReport(context, site, null, "2021-05-15");
        assertThat(usageReportRest.getPoints(), contains(
            dateWithViews("2020", 1),
            dateWithViews("2021", 2)));

        usageReportRest = generator.createUsageReport(context, site, "2021-02-05", "2022-12-31");
        assertThat(usageReportRest.getPoints(), contains(
            dateWithViews("2021", 4),
            dateWithViews("2022", 9)));

        usageReportRest = generator.createUsageReport(context, site, "2023-03-14", "2023-03-15");
        assertThat(usageReportRest.getPoints(), contains(dateWithViews("2023", 0)));

    }

    @Test
    public void testSiteViewsWithDayPeriodType() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item1 = createItem("First item");
        Item item2 = createItem("Second item");
        Item item3 = createItem("Third item");

        context.restoreAuthSystemState();

        view(item3, "2022-01-24");

        view(item3, "2022-01-25");
        view(item3, "2022-01-25");

        view(item2, "2022-02-13");
        view(item1, "2022-02-13");
        view(item1, "2022-02-13");

        view(item2, "2022-07-21");

        view(item1, "2023-02-04");
        view(item2, "2023-02-04");

        view(item1, "2023-03-03");

        TotalVisitPerPeriodGenerator generator = createGenerator("day", 1, Constants.ITEM);

        UsageReportRest usageReportRest = generator.createUsageReport(context, site, null, null);

        assertThat(usageReportRest.getPoints(), not(empty()));
        assertThat(usageReportRest.getPoints().get(0), is(dateWithViews("24-01-2022", 1)));

        assertThat(usageReportRest.getPoints(), hasItems(
            dateWithViews("24-01-2022", 1),
            dateWithViews("25-01-2022", 2),
            dateWithViews("13-02-2022", 3),
            dateWithViews("14-02-2022", 0),
            dateWithViews("21-07-2022", 1),
            dateWithViews("03-02-2023", 0),
            dateWithViews("04-02-2023", 2),
            dateWithViews("03-03-2023", 1)));

        usageReportRest = generator.createUsageReport(context, site, "2023-02-01", null);

        assertThat(usageReportRest.getPoints(), hasItems(
            dateWithViews("04-02-2023", 2),
            dateWithViews("05-02-2023", 0),
            dateWithViews("21-02-2023", 0),
            dateWithViews("03-03-2023", 1)));

        assertThat(usageReportRest.getPoints().get(0), is(dateWithViews("01-02-2023", 0)));

        usageReportRest = generator.createUsageReport(context, site, null, "2022-02-28");

        assertThat(usageReportRest.getPoints(), hasItems(
            dateWithViews("24-01-2022", 1),
            dateWithViews("25-01-2022", 2),
            dateWithViews("13-02-2022", 3)));

        UsageReportPointRest lastPoint = usageReportRest.getPoints().get(usageReportRest.getPoints().size() - 1);
        assertThat(lastPoint, is(dateWithViews("28-02-2022", 0)));

        usageReportRest = generator.createUsageReport(context, site, "2023-02-03", "2023-02-07");
        assertThat(usageReportRest.getPoints(), contains(
            dateWithViews("03-02-2023", 0),
            dateWithViews("04-02-2023", 2),
            dateWithViews("05-02-2023", 0),
            dateWithViews("06-02-2023", 0),
            dateWithViews("07-02-2023", 0)));

        usageReportRest = generator.createUsageReport(context, site, "2023-03-14", "2023-03-15");
        assertThat(usageReportRest.getPoints(), contains(
            dateWithViews("14-03-2023", 0),
            dateWithViews("15-03-2023", 0)));


    }

    private TotalVisitPerPeriodGenerator createGenerator(String periodType, int increment, int dsoType) {
        TotalVisitPerPeriodGenerator generator = new TotalVisitPerPeriodGenerator();
        generator.setDiscoveryConfigurationService(discoveryConfigurationService);
        generator.setSolrLoggerService(solrLoggerService);
        generator.setPeriodType(periodType);
        generator.setIncrement(increment);
        generator.setDsoType(dsoType);
        return generator;
    }

    private Matcher<UsageReportPointRest> dateWithViews(String date, int views) {
        return LambdaMatcher.matches(point -> point.getId().equals(date)
            && point.getValues().containsKey("views") && point.getValues().get("views").equals(views));
    }

    private Item createItem(String title) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .build();
    }

    /**
     * Store a VIEW usage event into the statistics Solr core with a specific
     * time. The vanilla {@link SolrLoggerService} does not expose a
     * {@code postView} overload accepting a custom date, so we index the
     * statistics document directly using an embedded Solr client, mirroring the
     * fields set by {@link SolrLoggerServiceImpl}.
     */
    private void view(DSpaceObject dso, String time) throws Exception {
        ZonedDateTime zonedDateTime = MultiFormatDateParser.parse(time);

        SolrClient solrClient = solrClientFactory
            .getClient(configurationService.getProperty("solr-statistics.server"));

        SolrInputDocument document = new SolrInputDocument();
        document.addField("statistics_type", SolrLoggerServiceImpl.StatisticsType.VIEW.text());
        document.addField("type", dso.getType());
        document.addField("id", dso.getID().toString());
        document.addField("epersonid", eperson.getID().toString());
        document.addField("isBot", false);
        document.addField("time", zonedDateTime.toInstant().toString());

        solrClient.add(document);
        solrClient.commit();
    }


}
