/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import static java.lang.Integer.parseInt;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Collections.singletonList;
import static org.apache.commons.cli.Option.builder;
import static org.apache.commons.lang.time.DateFormatUtils.format;
import static org.apache.logging.log4j.LogManager.getLogger;
import static org.dspace.core.LogHelper.getHeader;
import static org.dspace.statistics.SolrLoggerServiceImpl.DATE_FORMAT_8601;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;

/**
 * Script to anonymize solr statistics according to GDPR specifications.
 * This script will anonymize records older than a certain threshold, configurable with the
 * 'anonymize_statistics.time_threshold' config, with a default value of 90 days.
 * The records will be anonymized by replacing the last part of the ip address with a mask, this mask is configurable:
 * For IPv4 addresses, the config is 'anonymize_statistics.ip_v4_mask', with a default value of '255'
 * For IPv6 addresses, the config is 'anonymize_statistics.ip_v6_mask', with a default value of 'FFFF:FFFF'
 * The DNS value of the records will also be replaced by a mask, configurable with 'anonymize_statistics.dns_mask',
 * and with a default value of 'anonymized'.
 */
public class AnonymizeStatistics {

    private static final Logger log = getLogger();
    private static final Context context = new Context();
    private static final String action = "anonymize_statistics";

    private static final String HELP_OPTION = "h";
    private static final String SLEEP_OPTION = "s";
    private static final String BATCH_SIZE_OPTION = "b";
    private static final String THREADS_OPTION = "t";

    private static int sleep;

    private static final SolrLoggerService solrLoggerService =
            StatisticsServiceFactory.getInstance().getSolrLoggerService();
    private static final ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    private static int batchSize = 100;
    private static int threads = 2;

    private static final Object DNS_MASK =
            configurationService.getProperty("anonymize_statistics.dns_mask", "anonymized");

    private static final String TIME_LIMIT;

    static {
        Calendar calendar = Calendar.getInstance();
        calendar.add(DAY_OF_YEAR, -configurationService.getIntProperty("anonymize_statistics.time_threshold", 90));
        TIME_LIMIT = format(calendar, DATE_FORMAT_8601);
    }

    private AnonymizeStatistics() {

    }


    public static void main(String... args) throws ParseException {

        parseCommandLineOptions(createCommandLineOptions(), args);
        anonymizeStatistics();
    }

    private static Options createCommandLineOptions() {

        Options options = new Options();
        options.addOption(
                builder(HELP_OPTION)
                        .longOpt("help")
                        .desc("Print the usage of the script")
                        .hasArg(false)
                        .build()
        );

        options.addOption(
                builder(SLEEP_OPTION)
                        .longOpt("sleep")
                        .desc("Sleep a certain time given in milliseconds between each solr request")
                        .hasArg(true)
                        .build()
        );

        options.addOption(
            builder(BATCH_SIZE_OPTION)
                .longOpt("batch")
                .desc("The amount of Solr records to be processed per batch (defaults to 100)")
                .hasArg(true)
                .build()
        );

        options.addOption(
            builder(THREADS_OPTION)
                .longOpt("threads")
                .desc("The amount of threads used by the script (defaults to 2")
                .hasArg(true)
                .build()
        );

        return options;
    }

    private static void parseCommandLineOptions(Options options, String... args) throws ParseException {

        CommandLine commandLine = new DefaultParser().parse(options, args);

        if (commandLine.hasOption(HELP_OPTION)) {
            printHelp(options);
            System.exit(-1);
        }

        if (commandLine.hasOption(SLEEP_OPTION)) {
            sleep = parseInt(commandLine.getOptionValue(SLEEP_OPTION));
        }

        if (commandLine.hasOption(BATCH_SIZE_OPTION)) {
            batchSize = parseInt(commandLine.getOptionValue(BATCH_SIZE_OPTION));
        }

        if (commandLine.hasOption(THREADS_OPTION)) {
            threads = parseInt(commandLine.getOptionValue(THREADS_OPTION));
        }
    }

    private static void printHelp(Options options) {
        new HelpFormatter().printHelp("dsrun " + AnonymizeStatistics.class.getCanonicalName(), options);
    }

    private static void printInfo(String info) {
        System.out.println(info);
        log.info(getHeader(context, action, info));
    }

    private static void printWarning(String warning) {
        System.out.println(warning);
        log.warn(getHeader(context, action, warning));
    }

    private static void printError(Exception error) {
        error.printStackTrace();
        log.error(getHeader(context, action, error.getMessage()), error);
    }


    /**
     * Anonymize the relevant solr documents, returned by the getDocuments method.
     */
    private static void anonymizeStatistics() {
        try {
            long updated = 0;
            long total = getDocuments().getResults().getNumFound();
            printInfo(total + " documents to update");

            // The documents will be processed in separate threads.
            ExecutorService executorService = Executors.newFixedThreadPool(threads);

            QueryResponse documents;
            do {
                documents = getDocuments();

                // list of the processing callables to execute
                Collection<DoProcessing> callables = new ArrayList<>();

                for (SolrDocument document : documents.getResults()) {
                    updated++;
                    callables.add(new DoProcessing(document, updated));
                }

                // execute the processing callables
                executorService.invokeAll(callables);

                // Commit the solr core
                solrLoggerService.commit();

                printInfo("processed " + updated + " records");
            } while (documents.getResults().getNumFound() > 0);

            printInfo(updated + " documents updated");
            if (updated == total) {
                printInfo("all relevant documents were updated");
            } else {
                printWarning("not all relevant documents were updated, check the DSpace logs for more details");
            }
        } catch (Exception e) {
            printError(e);
        }
    }

    /**
     * Get the documents to anonymize.
     * @return
     *      Non-anonymized documents, which are older than the time period configured by the
     *      'anonymize_statistics.time_threshold' config (or 90 days, if not configured)
     */
    private static QueryResponse getDocuments() throws SolrServerException, IOException {

        if (sleep > 0) {
            try {
                printInfo("sleep " + sleep + "ms");
                sleep(sleep);
            } catch (InterruptedException e) {
                printError(e);
                currentThread().interrupt();
            }
        }

        return solrLoggerService.query(
            "ip:*",
            "time:[* TO " + TIME_LIMIT + "] AND -dns:" + DNS_MASK,
            null, batchSize, -1, null, null, null, null,
            null, false, -1, false
        );
    }

    /**
     * {@link Callable} implementation to process a solr document to be anonymized.
     * It will return true if the anonymization succeeded.
     */
    public static class DoProcessing implements Callable<Boolean> {

        private final SolrDocument document;
        private final long updated;

        public DoProcessing(SolrDocument document, long updated) {
            this.document = document;
            this.updated = updated;
        }

        @Override
        public Boolean call() {
            try {
                solrLoggerService.update(
                    "uid:" + document.getFieldValue("uid"),
                    "replace",
                    asList(
                        "ip",
                        "dns"
                    ),
                    asList(
                        singletonList(solrLoggerService.anonymizeIp(document.getFieldValue("ip").toString())),
                        singletonList(DNS_MASK)
                    ),
                    false
                );
                printInfo(updated + ": updated document with uid " + document.getFieldValue("uid") + " " + new Date());
                return true;
            } catch (Exception e) {
                printError(e);
                return false;
            }
        }
    }
}
