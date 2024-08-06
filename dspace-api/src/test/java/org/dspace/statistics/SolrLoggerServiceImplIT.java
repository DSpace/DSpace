/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import static org.dspace.statistics.SolrLoggerServiceImpl.DATE_FORMAT_8601;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test some methods of SolrLoggerServiceImpl.
 *
 * @author mwood
 */
public class SolrLoggerServiceImplIT
        extends AbstractIntegrationTestWithDatabase {
    private static final ConfigurationService cfg
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    // Bot IP list should contain no RFC 1918 private addresses.
    private static final String NOT_BOT_IP = "192.168.1.1";
    private static final String BOT_IP = "192.168.2.1";

    private static final String NOT_BOT_DNS = "angel.com";
    private static final String BOT_DNS = "demon.com";

    private static final String NOT_BOT_AGENT = "Firefox";
    private static final String BOT_AGENT = "Punchbot";

    private static final String F_AGENT = "userAgent";
    private static final String F_DNS = "dns";
    private static final String F_EPERSON = "epersonid";
    private static final String F_ID = "id";
    private static final String F_IP = "ip";
    private static final String F_IS_BOT = "isBot";
    private static final String F_STATISTICS_TYPE = "statistics_type";
    private static final String F_TIME = "time";
    private static final String F_TYPE = "type";

    private static final String Q_ALL = "*:*";

    private static final String COMMUNITY_NAME = "Top";

    private static Path testAddressesPath;
    private static Path testAgentsPath;

    @BeforeClass
    public static void setUpClass()
            throws IOException {
        Path spidersPath = Paths.get(cfg.getProperty("dspace.dir"), "config", "spiders");
        Writer writer;

        // Ensure the presence of a known "bot" address.
        testAddressesPath = Files.createTempFile(spidersPath, "test-ips-", ".txt");
        writer = Files.newBufferedWriter(testAddressesPath, StandardCharsets.UTF_8,
                StandardOpenOption.WRITE);
        writer.append(BOT_IP)
                .append('\n')
                .close();

        // Ensure the presence of a known "bot" agent.
        testAgentsPath = Files.createTempFile(spidersPath.resolve("agents"), "test-agents-", ".txt");
        writer = Files.newBufferedWriter(testAgentsPath, StandardCharsets.UTF_8,
                StandardOpenOption.WRITE);
        writer.append('^')
                .append(BOT_AGENT)
                .append('\n')
                .close();
    }

    @AfterClass
    public static void tearDownClass()
            throws IOException {
        Files.deleteIfExists(testAddressesPath);
        Files.deleteIfExists(testAgentsPath);
    }

    @Before
    public void setUpTest() {
    }

    @After
    public void tearDownTest() {
    }

    /**
     * Test of markRobots method, of class SolrLoggerServiceImpl.
     *
     * @throws SolrServerException passed through.
     * @throws IOException passed through.
     */
    @Test
    public void testMarkRobots()
            throws SolrServerException, IOException, Exception {
        System.out.println("markRobots");

        EmbeddedSolrClientFactory clientFactory = new EmbeddedSolrClientFactory();
        ContentServiceFactory csf = ContentServiceFactory.getInstance();
        DSpace dspace = new DSpace();

        SolrLoggerServiceImpl instance = new SolrLoggerServiceImpl();
        instance.bitstreamService = csf.getBitstreamService();
        instance.contentServiceFactory = csf;
        instance.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        instance.clientInfoService = CoreServiceFactory.getInstance().getClientInfoService();
        instance.geoIpService = dspace.getSingletonService(GeoIpService.class);
        instance.solrStatisticsCore = dspace.getSingletonService(SolrStatisticsCore.class);
        instance.afterPropertiesSet();

        // Create objects to view.
        context.turnOffAuthorisationSystem();
        Community topCommunity = CommunityBuilder.createCommunity(context)
                .withName(COMMUNITY_NAME)
                .build();
        context.restoreAuthSystemState();

        // Set up some documents.
        SolrClient client = clientFactory.getClient(cfg.getProperty("solr-statistics.server"));
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField(F_STATISTICS_TYPE, SolrLoggerServiceImpl.StatisticsType.VIEW);
        doc.setField(F_TYPE, String.valueOf(Constants.COMMUNITY));
        doc.setField(F_ID, topCommunity.getID().toString());
        doc.setField(F_EPERSON, eperson.getID().toString());

        doc.setField(F_IP, NOT_BOT_IP);
        doc.setField(F_DNS, NOT_BOT_DNS);
        doc.setField(F_AGENT, NOT_BOT_AGENT);
        doc.setField(F_TIME, DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
        client.add(doc);

        doc.setField(F_IP, BOT_IP);
        doc.setField(F_DNS, BOT_DNS);
        doc.setField(F_AGENT, NOT_BOT_AGENT);
        doc.setField(F_TIME, DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
        client.add(doc);

        doc.setField(F_IP, NOT_BOT_IP);
        doc.setField(F_DNS, NOT_BOT_DNS);
        doc.setField(F_AGENT, BOT_AGENT);
        doc.setField(F_TIME, DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
        client.add(doc);

        doc.setField(F_IP, BOT_IP);
        doc.setField(F_DNS, BOT_DNS);
        doc.setField(F_AGENT, BOT_AGENT);
        doc.setField(F_TIME, DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
        client.add(doc);

        client.commit(true, true);

        // Scan the core for robot entries and mark them.
        cfg.setProperty("solr-statistics.query.filter.isBot", "false");
        instance.markRobots();

        // Check that documents are marked correctly.
        SolrQuery readbackQuery = new SolrQuery()
                .setRows(10)
                .setQuery(Q_ALL);
        QueryResponse response = client.query(readbackQuery);
        long nDocs = 0;
        long nGood = 0;
        for (SolrDocument document : response.getResults()) {
            String ip = (String) document.getFieldValue(F_IP);
            String agent = (String) document.getFieldValue(F_AGENT);
            Object isBotRaw = document.getFieldValue(F_IS_BOT);
            boolean isBot = (null == isBotRaw) ? false : (Boolean) isBotRaw;

            if (NOT_BOT_IP.equals(ip) && NOT_BOT_AGENT.equals(agent)) {
                assertFalse(String.format("IP %s plus Agent %s is marked as bot --", ip, agent),
                        isBot);
            } else {
                assertTrue(String.format("IP %s or Agent %s is not marked as bot --", ip, agent),
                        isBot);
            }

            nDocs++;
            if (!isBot) {
                nGood++;
            }
        }
        assertEquals("Wrong number of documents", 4, nDocs);
        assertEquals("Wrong number of non-bot views", 1, nGood);
    }

    /**
     * Test of deleteRobots method, of class SolrLoggerServiceImpl.
     * @throws SolrServerException passed through.
     * @throws IOException passed through.
     */
    @Test
    public void testDeleteRobots()
            throws SolrServerException, IOException, Exception {
        System.out.println("deleteRobots");

        EmbeddedSolrClientFactory clientFactory = new EmbeddedSolrClientFactory();
        ContentServiceFactory csf = ContentServiceFactory.getInstance();
        DSpace dspace = new DSpace();

        SolrLoggerServiceImpl instance = new SolrLoggerServiceImpl();
        instance.bitstreamService = csf.getBitstreamService();
        instance.contentServiceFactory = csf;
        instance.configurationService = cfg;
        instance.clientInfoService = CoreServiceFactory.getInstance().getClientInfoService();
        instance.geoIpService = dspace.getSingletonService(GeoIpService.class);
        instance.solrStatisticsCore = dspace.getSingletonService(SolrStatisticsCore.class);
        instance.afterPropertiesSet();

        // Create objects to view.
        context.turnOffAuthorisationSystem();
        Community topCommunity = CommunityBuilder.createCommunity(context)
                .withName(COMMUNITY_NAME)
                .build();
        context.restoreAuthSystemState();

        // Set up some documents.
        SolrClient client = clientFactory.getClient(cfg.getProperty("solr-statistics.server"));
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField(F_STATISTICS_TYPE, SolrLoggerServiceImpl.StatisticsType.VIEW);
        doc.setField(F_TYPE, String.valueOf(Constants.COMMUNITY));
        doc.setField(F_ID, topCommunity.getID().toString());
        doc.setField(F_EPERSON, eperson.getID().toString());

        doc.setField(F_IP, NOT_BOT_IP);
        doc.setField(F_DNS, NOT_BOT_DNS);
        doc.setField(F_AGENT, NOT_BOT_AGENT);
        doc.setField(F_TIME, DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
        doc.setField(F_IS_BOT, Boolean.FALSE.toString());
        client.add(doc);

        doc.setField(F_IP, BOT_IP);
        doc.setField(F_DNS, BOT_DNS);
        doc.setField(F_AGENT, NOT_BOT_AGENT);
        doc.setField(F_TIME, DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
        doc.setField(F_IS_BOT, Boolean.TRUE.toString());
        client.add(doc);

        doc.setField(F_IP, NOT_BOT_IP);
        doc.setField(F_DNS, NOT_BOT_DNS);
        doc.setField(F_AGENT, BOT_AGENT);
        doc.setField(F_TIME, DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
        doc.setField(F_IS_BOT, Boolean.TRUE.toString());
        client.add(doc);

        doc.setField(F_IP, BOT_IP);
        doc.setField(F_DNS, BOT_DNS);
        doc.setField(F_AGENT, BOT_AGENT);
        doc.setField(F_TIME, DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
        doc.setField(F_IS_BOT, Boolean.TRUE.toString());
        client.add(doc);

        client.commit(true, true);

        // Scan the core for marked robot entries and delete them.
        instance.deleteRobots();

        // Check that the correct documents (and only those) are gone.
        QueryResponse response = instance.query(Q_ALL, null, null,
                Integer.MAX_VALUE, -1,
                null, null, null, null, null, true, 0);
        long nDocs = 0;
        for (SolrDocument document : response.getResults()) {
            nDocs++;

            Object isBotRaw = document.getFieldValue(F_IS_BOT);
            boolean isBot = (null == isBotRaw) ? false : (Boolean) isBotRaw;

            assertEquals("Marked document was not removed --",
                    false, isBot);
        }
        assertEquals("Wrong number of documents remaining --", 1, nDocs);
    }
}
