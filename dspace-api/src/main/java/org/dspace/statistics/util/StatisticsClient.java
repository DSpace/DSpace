/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.logging.log4j.Logger;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;

/**
 * Class to load intermediate statistics files into Solr.
 *
 * @author Stuart Lewis
 */
public class StatisticsClient {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(StatisticsClient.class);

    /**
     * Default constructor
     */
    private StatisticsClient() { }

    /**
     * Print the help message
     *
     * @param options  The command line options the user gave
     * @param exitCode the system exit code to use
     */
    private static void printHelp(Options options, int exitCode) {
        // print the help message
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("StatisticsClient\n", options);
        System.exit(exitCode);
    }

    /**
     * Main method to run the statistics importer.
     *
     * @param args The command line arguments
     * @throws Exception If something goes wrong
     */
    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();

        options.addOption("u", "update-spider-files", false,
                          "Update Spider IP Files from internet into " +
                              DSpaceServicesFactory.getInstance().getConfigurationService()
                                                   .getProperty("dspace.dir") + "/config/spiders");

        options.addOption("m", "mark-spiders", false, "Update isBot Flag in Solr");
        options.addOption("f", "delete-spiders-by-flag", false, "Delete Spiders in Solr By isBot Flag");
        options.addOption("b", "reindex-bitstreams", false, "Reindex the bitstreams to ensure we have the bundle name");
        options.addOption("e", "export", false,
                          "Export SOLR view statistics data to usage-statistics-intermediate-format");
        options.addOption("r", "remove-deleted-bitstreams", false,
                          "While indexing the bundle names remove the statistics about deleted bitstreams");
        options.addOption("s", "shard-solr-index", false,
                          "Split the data from the main Solr core into separate Solr cores per year");
        options.addOption("h", "help", false, "help");

        CommandLine line = parser.parse(options, args);

        // Did the user ask to see the help?
        if (line.hasOption('h')) {
            printHelp(options, 0);
        }

        SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();
        if (line.hasOption("u")) {
            StatisticsClient.updateSpiderFiles();
        } else if (line.hasOption('m')) {
            solrLoggerService.markRobots();
        } else if (line.hasOption('f')) {
            solrLoggerService.deleteRobots();
        } else if (line.hasOption('b')) {
            solrLoggerService.reindexBitstreamHits(line.hasOption('r'));
        } else if (line.hasOption('e')) {
            solrLoggerService.exportHits();
        } else if (line.hasOption('s')) {
            solrLoggerService.shardSolrIndex();
        } else {
            printHelp(options, 0);
        }
    }

    /**
     * Method to update Spiders in configuration directory.
     */
    private static void updateSpiderFiles() {
        try {
            System.out.println("Downloading latest spider IP addresses:");

            // Get the list URLs to download from
            String[] urls = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                 .getArrayProperty("solr-statistics.spiderips.urls");
            if ((urls == null) || (urls.length == 0)) {
                System.err.println(" - Missing setting from dspace.cfg: solr.spiderips.urls");
                System.exit(0);
            }

            // Get the location of spiders directory
            File spiders = new File(
                DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir"),
                "config/spiders");

            if (!spiders.exists() && !spiders.mkdirs()) {
                log.error("Unable to create spiders directory");
            }

            try (CloseableHttpClient httpClient = DSpaceHttpClientFactory.getInstance().build()) {
                for (String value : urls) {
                    value = value.trim();
                    System.out.println(" Downloading: " + value);

                    URI uri = URI.create(value);
                    File destination = new File(spiders, uri.getHost() + uri.getRawPath().replace("/", "-"));
                    download(httpClient, uri, destination);
                }
            }

        } catch (Exception e) {
            System.err.println(" - Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Download a URL to a file, unless the file is at least as recent as the remote copy.
     * The file is only replaced once the download is complete, and gets the remote
     * Last-Modified date so the next run can skip it when nothing changed.
     *
     * @param httpClient  the HTTP client to use
     * @param uri         the URL to download
     * @param destination the file to write to
     * @throws IOException if the download fails or the server does not return the file
     */
    private static void download(CloseableHttpClient httpClient, URI uri, File destination) throws IOException {
        HttpGet get = new HttpGet(uri);
        if (destination.exists()) {
            get.setHeader(HttpHeaders.IF_MODIFIED_SINCE,
                          DateUtils.formatStandardDate(Instant.ofEpochMilli(destination.lastModified())));
        }
        httpClient.execute(get, response -> {
            int status = response.getCode();
            if (status == HttpStatus.SC_NOT_MODIFIED) {
                System.out.println(" Not modified: " + destination.getName());
                return null;
            }
            if (status != HttpStatus.SC_OK || response.getEntity() == null) {
                throw new IOException("Unexpected HTTP status " + status + " for " + uri);
            }
            Path temporary = Files.createTempFile(destination.getParentFile().toPath(), destination.getName(), ".tmp");
            try {
                try (InputStream content = response.getEntity().getContent()) {
                    Files.copy(content, temporary, REPLACE_EXISTING);
                }
                Files.move(temporary, destination.toPath(), REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
            Header lastModified = response.getFirstHeader(HttpHeaders.LAST_MODIFIED);
            Instant remoteDate = lastModified == null ? null : DateUtils.parseStandardDate(lastModified.getValue());
            if (remoteDate != null && !destination.setLastModified(remoteDate.toEpochMilli())) {
                log.warn("Unable to set the modification date of {}", destination);
            }
            return null;
        });
    }


}
