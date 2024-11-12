/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sitemap;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Command-line utility for generating HTML and Sitemaps.org protocol Sitemaps.
 *
 * @author Robert Tansley
 * @author Stuart Lewis
 */
public class GenerateSitemaps {
    /**
     * Logger
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(GenerateSitemaps.class);

    private static final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private static final CollectionService collectionService =
        ContentServiceFactory.getInstance().getCollectionService();
    private static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private static final SearchService searchService = SearchUtils.getSearchService();
    private static final int PAGE_SIZE = 100;

    /**
     * Default constructor
     */
    private GenerateSitemaps() { }

    public static void main(String[] args) throws Exception {
        final String usage = GenerateSitemaps.class.getCanonicalName();

        CommandLineParser parser = new DefaultParser();
        HelpFormatter hf = new HelpFormatter();

        Options options = new Options();

        options.addOption("h", "help", false, "help");
        options.addOption("s", "no_sitemaps", false,
                          "do not generate sitemaps.org protocol sitemap");
        options.addOption("b", "no_htmlmap", false,
                          "do not generate a basic HTML sitemap");
        options
            .addOption("d", "delete", false,
                "delete sitemaps dir and its contents");

        CommandLine line = null;

        try {
            line = parser.parse(options, args);
        } catch (ParseException pe) {
            hf.printHelp(usage, options);
            System.exit(1);
        }

        if (line.hasOption('h')) {
            hf.printHelp(usage, options);
            System.exit(0);
        }

        if (line.getArgs().length != 0) {
            hf.printHelp(usage, options);
            System.exit(1);
        }

        /*
         * Sanity check -- if no sitemap generation or deletion, print usage
         */
        if (line.getArgs().length != 0 || line.hasOption('d') || line.hasOption('b')
            && line.hasOption('s') && !line.hasOption('g')
            && !line.hasOption('m') && !line.hasOption('y')) {
            System.err
                .println("Nothing to do (no sitemap to generate)");
            hf.printHelp(usage, options);
            System.exit(1);
        }

        // Note the negation (CLI options indicate NOT to generate a sitemap)
        if (!line.hasOption('b') || !line.hasOption('s')) {
            generateSitemaps(!line.hasOption('b'), !line.hasOption('s'));
        }

        if (line.hasOption('d')) {
            deleteSitemaps();
        }

        System.exit(0);
    }

    /**
     * Runs generate-sitemaps without any params for the scheduler (task-scheduler.xml).
     *
     * @throws SQLException if a database error occurs.
     * @throws IOException  if IO error occurs.
     */
    public static void generateSitemapsScheduled() throws IOException, SQLException {
        generateSitemaps(true, true);
    }

    /**
     * Delete the sitemaps directory and its contents if it exists
     * @throws IOException  if IO error occurs
     */
    public static void deleteSitemaps() throws IOException {
        File outputDir = new File(configurationService.getProperty("sitemap.dir"));
        if (!outputDir.exists() && !outputDir.isDirectory()) {
            log.error("Unable to delete sitemaps directory, doesn't exist or isn't a directory");
        } else {
            FileUtils.deleteDirectory(outputDir);
        }
    }

    /**
     * Generate sitemap.org protocol and/or basic HTML sitemaps.
     *
     * @param makeHTMLMap    if {@code true}, generate an HTML sitemap.
     * @param makeSitemapOrg if {@code true}, generate an sitemap.org sitemap.
     * @throws SQLException if database error
     *                      if a database error occurs.
     * @throws IOException  if IO error
     *                      if IO error occurs.
     */
    public static void generateSitemaps(boolean makeHTMLMap, boolean makeSitemapOrg) throws SQLException, IOException {
        String uiURLStem = configurationService.getProperty("dspace.ui.url");
        if (!uiURLStem.endsWith("/")) {
            uiURLStem = uiURLStem + '/';
        }
        String sitemapStem = uiURLStem + "sitemap";

        File outputDir = new File(configurationService.getProperty("sitemap.dir"));
        if (!outputDir.exists() && !outputDir.mkdir()) {
            log.error("Unable to create output directory");
        }

        AbstractGenerator html = null;
        AbstractGenerator sitemapsOrg = null;

        if (makeHTMLMap) {
            html = new HTMLSitemapGenerator(outputDir, sitemapStem, ".html");
        }

        if (makeSitemapOrg) {
            sitemapsOrg = new SitemapsOrgGenerator(outputDir, sitemapStem, ".xml");
        }

        Context c = new Context(Context.Mode.READ_ONLY);
        int offset = 0;
        long commsCount = 0;
        long collsCount = 0;
        long itemsCount = 0;

        try {
            DiscoverQuery discoveryQuery = new DiscoverQuery();
            discoveryQuery.setMaxResults(PAGE_SIZE);
            discoveryQuery.setQuery("search.resourcetype:Community");
            do {
                discoveryQuery.setStart(offset);
                DiscoverResult discoverResult = searchService.search(c, discoveryQuery);
                List<IndexableObject> docs = discoverResult.getIndexableObjects();
                commsCount = discoverResult.getTotalSearchResults();

                for (IndexableObject doc : docs) {
                    String url = uiURLStem + "communities/" + doc.getID();
                    c.uncacheEntity(doc.getIndexedObject());

                    if (makeHTMLMap) {
                        html.addURL(url, null);
                    }
                    if (makeSitemapOrg) {
                        sitemapsOrg.addURL(url, null);
                    }
                }
                offset += PAGE_SIZE;
            } while (offset < commsCount);

            offset = 0;
            discoveryQuery = new DiscoverQuery();
            discoveryQuery.setMaxResults(PAGE_SIZE);
            discoveryQuery.setQuery("search.resourcetype:Collection");
            do {
                discoveryQuery.setStart(offset);
                DiscoverResult discoverResult = searchService.search(c, discoveryQuery);
                List<IndexableObject> docs = discoverResult.getIndexableObjects();
                collsCount = discoverResult.getTotalSearchResults();

                for (IndexableObject doc : docs) {
                    String url = uiURLStem + "collections/" + doc.getID();
                    c.uncacheEntity(doc.getIndexedObject());

                    if (makeHTMLMap) {
                        html.addURL(url, null);
                    }
                    if (makeSitemapOrg) {
                        sitemapsOrg.addURL(url, null);
                    }
                }
                offset += PAGE_SIZE;
            } while (offset < collsCount);

            offset = 0;
            discoveryQuery = new DiscoverQuery();
            discoveryQuery.setMaxResults(PAGE_SIZE);
            discoveryQuery.setQuery("search.resourcetype:Item");
            discoveryQuery.addSearchField("search.entitytype");
            do {

                discoveryQuery.setStart(offset);
                DiscoverResult discoverResult = searchService.search(c, discoveryQuery);
                List<IndexableObject> docs = discoverResult.getIndexableObjects();
                itemsCount = discoverResult.getTotalSearchResults();

                for (IndexableObject doc : docs) {
                    String url;
                    List<String> entityTypeFieldValues = discoverResult.getSearchDocument(doc).get(0)
                                            .getSearchFieldValues("search.entitytype");
                    if (CollectionUtils.isNotEmpty(entityTypeFieldValues)) {
                        url = uiURLStem + "entities/" + StringUtils.lowerCase(entityTypeFieldValues.get(0)) + "/"
                                + doc.getID();
                    } else {
                        url = uiURLStem + "items/" + doc.getID();
                    }
                    Date lastMod = doc.getLastModified();
                    c.uncacheEntity(doc.getIndexedObject());

                    if (makeHTMLMap) {
                        html.addURL(url, null);
                    }
                    if (makeSitemapOrg) {
                        sitemapsOrg.addURL(url, null);
                    }
                }
                offset += PAGE_SIZE;
            } while (offset < itemsCount);

            if (makeHTMLMap) {
                int files = html.finish();
                log.info(LogHelper.getHeader(c, "write_sitemap",
                                              "type=html,num_files=" + files + ",communities="
                                                  + commsCount + ",collections=" + collsCount
                                                  + ",items=" + itemsCount));
            }

            if (makeSitemapOrg) {
                int files = sitemapsOrg.finish();
                log.info(LogHelper.getHeader(c, "write_sitemap",
                                              "type=html,num_files=" + files + ",communities="
                                                  + commsCount + ",collections=" + collsCount
                                                  + ",items=" + itemsCount));
            }
        } catch (SearchServiceException e) {
            throw new RuntimeException(e);
        } finally {
            c.abort();
        }
    }
}
