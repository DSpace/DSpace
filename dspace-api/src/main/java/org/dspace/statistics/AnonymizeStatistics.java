/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Integer.parseInt;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Collections.singletonList;
import static org.apache.commons.cli.Option.builder;
import static org.apache.commons.lang.time.DateFormatUtils.format;
import static org.apache.log4j.Logger.getLogger;
import static org.dspace.core.LogManager.getHeader;
import static org.dspace.statistics.SolrLoggerServiceImpl.DATE_FORMAT_8601;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class AnonymizeStatistics {

    private static Logger log = getLogger(AnonymizeStatistics.class);
    private static Context context = new Context();
    private static String action = "anonymize_statistics";

    private static final String HELP_OPTION = "h";
    private static final String SLEEP_OPTION = "s";
    private static final String BATCH_SIZE_OPTION = "b";
    private static final String THREADS_OPTION = "t";

    private static int sleep;

    private static SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();
    private static ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    private static int batchSize = 100;
    private static int threads = 2;

    private static final Object DNS_MASK = configurationService.getProperty("anonymize_statistics.dns_mask", "anonymized");

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


    private static void anonymizeStatistics() {
        try {
            long updated = 0;
            long total = getDocuments().getResults().getNumFound();
            printInfo(total + " documents to update");

            ExecutorService executorService = Executors.newFixedThreadPool(threads);

            QueryResponse documents;
            do {
                documents = getDocuments();

                Collection<Callable<Boolean>> callables = new ArrayList<>();
                Set<String> shards = new HashSet<>();

                for (SolrDocument document : documents.getResults()) {
                    updated++;


                    callables.add(new DoProcessing(document, updated));
                    String shard = (String) document.getFieldValue("[shard]");

                    if(StringUtils.isNotBlank(shard)){
                        shards.add(shard);
                    }
                }

                executorService.invokeAll(callables);

                solrLoggerService.commit();

                for (String shard : shards) {
                    solrLoggerService.commitShard(shard);
                }

                System.out.println("processed " + updated + " records");
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

    private static QueryResponse getDocuments() throws SolrServerException {

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
                null, batchSize, -1, null, null, null, null, null, false, false, true
        );
    }

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
