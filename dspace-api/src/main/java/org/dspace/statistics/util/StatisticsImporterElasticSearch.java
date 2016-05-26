/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import org.apache.commons.cli.*;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.statistics.SolrLoggerServiceImpl;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.ElasticSearchLoggerService;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;

import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;


import java.io.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Class to load intermediate statistics files (produced from log files by <code>ClassicDSpaceLogConverter</code>) into Elastic Search
 *
 * @see ClassicDSpaceLogConverter
 *
 * @author Peter Dietz (pdietz84@gmail.com)
 */
public class StatisticsImporterElasticSearch {
    private static final Logger log = Logger.getLogger(StatisticsImporterElasticSearch.class);

    /** Date format */
    private static ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        }
    };

    //TODO ES Client

    /** GEOIP lookup service */
    private static LookupService geoipLookup;

    /** Metadata storage information */
    private static Map<String, String> metadataStorageInfo;

    /** Whether to skip the DNS reverse lookup or not */
    private static boolean skipReverseDNS = false;

    private static ElasticSearchLoggerService elasticSearchLoggerInstance = StatisticsServiceFactory.getInstance().getElasticSearchLoggerService();
    ;
    private static Client client;
    private static BulkRequestBuilder bulkRequest;

    /**
     * Read lines from the statistics file and load their data into Elastic Search.
     *
     * @param filename The filename of the file to load
     * @param context The DSpace Context
     * @param verbose Whether to display verbose output
     */
    private void load(String filename, Context context, boolean verbose)
    {
        // Item counter
        int counter = 0;
        int errors = 0;
        int searchengines = 0;

        try
        {
            BufferedReader input;
            if (null == filename || "-".equals(filename))
            {
                input = new BufferedReader(new InputStreamReader(System.in));
                filename = "standard input";
            }
            else
                input = new BufferedReader(new FileReader(new File(filename)));

            // Print out the filename for confirmation
            System.out.println("Processing file: " + filename);

            String line;
//            String uuid;
            String action;
            String id;
            Date date;
            String user;
            String ip;

            String continent = "";
            String country = "";
            String countryCode = "";
            float longitude = 0f;
            float latitude = 0f;
            String city = "";
            String dns;

            DNSCache dnsCache = new DNSCache(2500, 0.75f, 2500);
            Object fromCache;

            ContentServiceFactory contentServiceFactory = ContentServiceFactory.getInstance();
            while ((line = input.readLine()) != null)
            {
                // Tokenise the line
                counter++;
                errors++;
                if (verbose)
                {
                    System.out.println("Line:" + line);
                }
                String[] parts = line.split(",");
//                uuid = parts[0];
                action = parts[1];
                id = parts[2];
                date = dateFormat.get().parse(parts[3]);
                user = parts[4];
                ip = parts[5];

                // Resolve the dns (if applicable) to get rid of search engine bots early on in the processing chain
                dns = "";
                if (!skipReverseDNS)
                {
                    // Is the IP address in the cache?
                    fromCache = dnsCache.get(ip);
                    if (fromCache != null)
                    {
                        dns = (String)fromCache;
                    }
                    else
                    {
                        try
                        {
                            dns = DnsLookup.reverseDns(ip);
                            dnsCache.put(ip, dns);
                        } catch (Exception e)
                        {
                            dns = "";
                        }
                    }
                }


                

                String data = "";
                data += ("ip addr = " + ip);
                data += (", dns name = " + dns);
                if ((dns.endsWith(".googlebot.com.")) ||
                        (dns.endsWith(".crawl.yahoo.net.")) ||
                        (dns.endsWith(".search.msn.com.")))
                {
                    if (verbose)
                    {
                        System.out.println(data + ", IGNORE (search engine)");
                    }
                    errors--;
                    searchengines++;
                    continue;
                }

                // Get the geo information for the user
                Location location;
                try {
                    location = geoipLookup.getLocation(ip);
                    city = location.city;
                    country = location.countryName;
                    countryCode = location.countryCode;
                    longitude = location.longitude;
                    latitude = location.latitude;
                    if(verbose) {
                        data += (", country = " + country);
                        data += (", city = " + city);
                        System.out.println(data);
                    }
                    try {
                        continent = LocationUtils.getContinentCode(countryCode);
                    } catch (Exception e) {
                        if (verbose)
                        {
                            System.out.println("Unknown country code: " + countryCode);
                        }
                        continue;
                    }
                } catch (Exception e) {
                    // No problem - just can't look them up
                }

                // Now find our dso
                DSpaceObjectLegacySupportService legacySupportService = null;
                int type = 0;
                if ("view_bitstream".equals(action))
                {
                    legacySupportService = contentServiceFactory.getBitstreamService();
                    type = Constants.BITSTREAM;
                }
                else if ("view_item".equals(action))
                {
                    legacySupportService = contentServiceFactory.getItemService();
                    type = Constants.ITEM;
                }
                else if ("view_collection".equals(action))
                {
                    legacySupportService = contentServiceFactory.getCollectionService();
                    type = Constants.COLLECTION;
                }
                else if ("view_community".equals(action))
                {
                    legacySupportService = contentServiceFactory.getCommunityService();
                    type = Constants.COMMUNITY;
                }
                if(legacySupportService == null)
                {
                    continue;
                }


                DSpaceObject dso = legacySupportService.findByIdOrLegacyId(context, id);
                if (dso == null)
                {
                    if (verbose)
                    {
                        System.err.println(" - DSO with ID '" + id + "' is no longer in the system");
                    }
                    continue;
                }

                // Get the eperson details
                EPerson eperson = EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, user);
                int epersonId = 0;
                if (eperson != null)
                {
                    eperson.getID();
                }

                //TODO Is there any way to reuse ElasticSearchLogger.post() ?

                // Save it in our server
                XContentBuilder postBuilder = XContentFactory.jsonBuilder().startObject()
                        .field("id", dso.getID())
                        .field("typeIndex", dso.getType())
                        .field("type", contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso))

                        .field("geo", new GeoPoint(latitude, longitude))
                        .field("continent", continent)
                        .field("countryCode", countryCode)
                        .field("country", country)
                        .field("city", city)

                        .field("ip", ip)

                        .field("time", DateFormatUtils.format(date, SolrLoggerServiceImpl.DATE_FORMAT_8601));

                // Unable to get UserAgent from logs. .field("userAgent")

                if (dso instanceof Bitstream) {
                    Bitstream bit = (Bitstream) dso;
                    List<Bundle> bundles = bit.getBundles();
                    postBuilder = postBuilder.field("bundleName").startArray();
                    for (Bundle bundle : bundles) {
                        postBuilder = postBuilder.value(bundle.getName());
                    }
                    postBuilder = postBuilder.endArray();
                }

                if (epersonId > 0)
                {
                    postBuilder = postBuilder.field("epersonid", epersonId);
                }
                if (dns != null)
                {
                    postBuilder = postBuilder.field("dns", dns.toLowerCase());
                }


                //Save for later: .field("isBot")

                elasticSearchLoggerInstance.storeParents(postBuilder, elasticSearchLoggerInstance.getParents(dso));

                bulkRequest.add(client.prepareIndex(elasticSearchLoggerInstance.getIndexName(), elasticSearchLoggerInstance.getIndexType())
                        .setSource(postBuilder.endObject()));


                errors--;
            }

            if(bulkRequest.numberOfActions() > 0) {
                BulkResponse bulkResponse = bulkRequest.execute().actionGet();
                if(bulkResponse.hasFailures()) {
                    log.error("Bulk Request Failed due to: " + bulkResponse.buildFailureMessage());
                }
            }
        }
        catch (RuntimeException re)
        {
            throw re;
        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
            log.error(e.getMessage(), e);
        }

        DecimalFormat percentage = new DecimalFormat("##.###");
        int committed = counter - errors - searchengines;
        System.out.println("Processed " + counter + " log lines");
        if (counter > 0)
        {
            Double committedpercentage = 100d * committed / counter;
            System.out.println(" - " + committed + " entries added to ElasticSearch: " + percentage.format(committedpercentage) + "%");
            Double errorpercentage = 100d * errors / counter;
            System.out.println(" - " + errors + " errors: " + percentage.format(errorpercentage) + "%");
            Double sepercentage = 100d * searchengines / counter;
            System.out.println(" - " + searchengines + " search engine activity skipped: " + percentage.format(sepercentage) + "%");
        }
        System.out.println(" done!");
    }


    /**
     * Print the help message
     *
     * @param options The command line options the user gave
     * @param exitCode the system exit code to use
     */
    private static void printHelp(Options options, int exitCode)
    {
        // print the help message
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("StatisticsImporterElasticSearch\n", options);
        System.exit(exitCode);
    }


    /**
     * Main method to run the statistics importer.
     *
     * @param args The command line arguments
     * @throws Exception If something goes wrong
     */
    public static void main(String[] args) throws Exception
    {
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("i", "in", true, "the input file ('-' or omit for standard input)");
        options.addOption("m", "multiple", false, "treat the input file as having a wildcard ending");
        options.addOption("s", "skipdns", false, "skip performing reverse DNS lookups on IP addresses");
        options.addOption("v", "verbose", false, "display verbose output (useful for debugging)");
        options.addOption("h", "help", false, "help");

        CommandLine line = parser.parse(options, args);

        // Did the user ask to see the help?
        if (line.hasOption('h'))
        {
            printHelp(options, 0);
        }

        if (line.hasOption('s'))
        {
            skipReverseDNS = true;
        }

        log.info("Getting ElasticSearch Transport Client for StatisticsImporterElasticSearch...");

        // This is only invoked via terminal, do not use _this_ node as that data storing node.
        // Need to get a NodeClient or TransportClient, but definitely do not want to get a local data storing client.
        client = elasticSearchLoggerInstance.getClient(ElasticSearchLoggerService.ClientType.TRANSPORT);

        client.admin().indices().prepareRefresh(StatisticsServiceFactory.getInstance().getElasticSearchLoggerService().getIndexName()).execute().actionGet();
        bulkRequest = client.prepareBulk();

        // We got all our parameters now get the rest
        Context context = new Context();

        // Verbose option
        boolean verbose = line.hasOption('v');

        String dbfile = ConfigurationManager.getProperty("usage-statistics", "dbfile");
        try
        {
            geoipLookup = new LookupService(dbfile, LookupService.GEOIP_STANDARD);
        }
        catch (FileNotFoundException fe)
        {
            log.error("The GeoLite Database file is missing (" + dbfile + ")! Elastic Search  Statistics cannot generate location based reports! Please see the DSpace installation instructions for instructions to install this file.", fe);
        }
        catch (IOException e)
        {
            log.error("Unable to load GeoLite Database file (" + dbfile + ")! You may need to reinstall it. See the DSpace installation instructions for more details.", e);
        }


        StatisticsImporterElasticSearch elasticSearchImporter = new StatisticsImporterElasticSearch();
        if (line.hasOption('m'))
        {
            // Convert all the files
            final File sample = new File(line.getOptionValue('i'));
            File dir = sample.getParentFile();
            FilenameFilter filter = new FilenameFilter()
            {
                @Override
                public boolean accept(File dir, String name)
                {
                    return name.startsWith(sample.getName());
                }
            };
            String[] children = dir.list(filter);
            for (String in : children)
            {
                System.out.println(in);
                elasticSearchImporter.load(dir.getAbsolutePath() + System.getProperty("file.separator") + in, context, verbose);
            }
        }
        else
        {
            // Just convert the one file
            elasticSearchImporter.load(line.getOptionValue('i'), context, verbose);
        }
    }


    /**
     * Inner class to hold a cache of reverse lookups of IP addresses
     * @param <K> IP address
     * @param <V> hostname looked up via DNS
     */
    static class DNSCache<K,V> extends LinkedHashMap<K,V>
    {
        private int maxCapacity;

        public DNSCache(int initialCapacity, float loadFactor, int maxCapacity)
        {
            super(initialCapacity, loadFactor, true);
            this.maxCapacity = maxCapacity;
        }

        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<K,V> eldest)
        {
            return size() >= this.maxCapacity;
        }
    }
}
