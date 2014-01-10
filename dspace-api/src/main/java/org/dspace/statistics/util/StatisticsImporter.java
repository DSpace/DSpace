/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import org.apache.commons.cli.*;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;
import org.dspace.statistics.SolrLogger;

import java.text.*;
import java.io.*;
import java.util.*;

import com.maxmind.geoip.LookupService;
import com.maxmind.geoip.Location;

/**
 * Class to load intermediate statistics files into solr
 *
 * @author Stuart Lewis
 */
public class StatisticsImporter
{
    private static final Logger log = Logger.getLogger(StatisticsImporter.class);

    /** Date format (for solr) */
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    /** Solr server connection */
    private static HttpSolrServer solr;

    /** GEOIP lookup service */
    private static LookupService geoipLookup;

    /** Metadata storage information */
    private static Map<String, String> metadataStorageInfo;

    /** Whether to skip the DNS reverse lookup or not */
    private static boolean skipReverseDNS = false;

    /** Local items */
    private List<Integer> localItems;

    /** Local collections */
    private List<Integer> localCollections;

    /** Local communities */
    private List<Integer> localCommunities;

    /** Local bitstreams */
    private List<Integer> localBitstreams;

    /** Whether or not to replace item IDs with local values (for testing) */
    private boolean useLocal;

    /**
     * Constructor. Optionally loads local data to replace foreign data
     * if using someone else's log files
     *
     * @param local Whether to use local data
     */
    public StatisticsImporter(boolean local)
    {
        // Setup the lists of communities, collections, items & bitstreams if required
        useLocal = local;
        if (local)
        {
            try
            {
                System.out.print("Loading local communities... ");
                Context c = new Context();
                Community[] communities = Community.findAll(c);
                localCommunities = new ArrayList<Integer>();
                for (Community community : communities)
                {
                    localCommunities.add(community.getID());
                }
                System.out.println("Found " + localCommunities.size());

                System.out.print("Loading local collections... ");
                Collection[] collections = Collection.findAll(c);
                localCollections = new ArrayList<Integer>();
                for (Collection collection : collections)
                {
                    localCollections.add(collection.getID());
                }
                System.out.println("Found " + localCollections.size());

                System.out.print("Loading local items... ");
                ItemIterator items = Item.findAll(c);
                localItems = new ArrayList<Integer>();
                Item i;
                while (items.hasNext())
                {
                    i = items.next();
                    localItems.add(i.getID());
                }
                System.out.println("Found " + localItems.size());

                System.out.print("Loading local bitstreams... ");
                Bitstream[] bitstreams = Bitstream.findAll(c);
                localBitstreams = new ArrayList<Integer>();
                for (Bitstream bitstream : bitstreams)
                {
                    if (bitstream.getName() != null)
                    {
                        localBitstreams.add(bitstream.getID());
                    }
                }
                System.out.println("Found " + localBitstreams.size());

            } catch (Exception e)
            {
                System.err.println("Error retrieving items from DSpace database:");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Read lines from the statistics file and load their data into solr.
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
            Random rand = new Random();

            while ((line = input.readLine()) != null)
            {
                // Tokenise the line
                String data = "";
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
                date = dateFormat.parse(parts[3]);
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
                int type = 0;
                if ("view_bitstream".equals(action))
                {
                    type = Constants.BITSTREAM;
                    if (useLocal)
                    {
                        id = "" + localBitstreams.get(rand.nextInt(localBitstreams.size()));
                    }
                }
                else if ("view_item".equals(action))
                {
                    type = Constants.ITEM;
                    if (useLocal)
                    {
                        id = "" + localItems.get(rand.nextInt(localItems.size()));
                    }
                }
                else if ("view_collection".equals(action))
                {
                    type = Constants.COLLECTION;
                    if (useLocal)
                    {
                        id = "" + localCollections.get(rand.nextInt(localCollections.size()));
                    }
                }
                else if ("view_community".equals(action))
                {
                    type = Constants.COMMUNITY;
                    if (useLocal)
                    {
                        id = "" + localCommunities.get(rand.nextInt(localCommunities.size()));
                    }
                }

                DSpaceObject dso = DSpaceObject.find(context, type, Integer.parseInt(id));
                if (dso == null)
                {
                    if (verbose)
                    {
                        System.err.println(" - DSO with ID '" + id + "' is no longer in the system");
                    }
                    continue;
                }

                // Get the eperson details
                EPerson eperson = EPerson.findByEmail(context, user);
                int epersonId = 0;
                if (eperson != null)
                {
                    eperson.getID();
                }

                // Save it in our server
                SolrInputDocument sid = new SolrInputDocument();
                sid.addField("ip", ip);
                sid.addField("type", dso.getType());
                sid.addField("id", dso.getID());
                sid.addField("time", DateFormatUtils.format(date, SolrLogger.DATE_FORMAT_8601));
                sid.addField("continent", continent);
                sid.addField("country", country);
                sid.addField("countryCode", countryCode);
                sid.addField("city", city);
                sid.addField("latitude", latitude);
                sid.addField("longitude", longitude);
                if (epersonId > 0)
                {
                    sid.addField("epersonid", epersonId);
                }
                if (dns != null)
                {
                    sid.addField("dns", dns.toLowerCase());
                }

                if (dso instanceof Item) {
                    Item item = (Item) dso;
                    // Store the metadata
                    for (String storedField : metadataStorageInfo.keySet()) {
                        String dcField = metadataStorageInfo.get(storedField);

                        DCValue[] vals = item.getMetadata(dcField.split("\\.")[0],
                                dcField.split("\\.")[1], dcField.split("\\.")[2],
                                Item.ANY);
                        for (DCValue val1 : vals) {
                            String val = val1.value;
                            sid.addField(String.valueOf(storedField), val);
                            sid.addField(String.valueOf(storedField + "_search"),
                                    val.toLowerCase());
                        }
                    }
                }

                SolrLogger.storeParents(sid, dso);
                solr.add(sid);
                errors--;
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
            System.out.println(" - " + committed + " entries added to solr: " + percentage.format(committedpercentage) + "%");
            Double errorpercentage = 100d * errors / counter;
            System.out.println(" - " + errors + " errors: " + percentage.format(errorpercentage) + "%");
            Double sepercentage = 100d * searchengines / counter;
            System.out.println(" - " + searchengines + " search engine activity skipped: " + percentage.format(sepercentage) + "%");
            System.out.print("About to commit data to solr...");

            // Commit at the end because it takes a while
            try
            {
                solr.commit();
            }
            catch (SolrServerException sse)
            {
                System.err.println("Error committing statistics to solr server!");
                sse.printStackTrace();
                System.exit(1);
            }
            catch (IOException ioe)
            {
                System.err.println("Error writing to solr server!");
                ioe.printStackTrace();
                System.exit(1);
            }
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
        myhelp.printHelp("StatisticsImporter\n", options);
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
        options.addOption("l", "local", false, "developers tool - map external log file to local handles");
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

        // Whether or not to convert handles to handles used in a local system
        // (useful if using someone else's log file for testing)
        boolean local = line.hasOption('l');

		// We got all our parameters now get the rest
		Context context = new Context();

        // Verbose option
        boolean verbose = line.hasOption('v');

        // Find our solr server
        String sserver = ConfigurationManager.getProperty("solr-statistics", "server");
        if (verbose)
        {
            System.out.println("Writing to solr server at: " + sserver);
        }
		solr = new HttpSolrServer(sserver);

		metadataStorageInfo = SolrLogger.getMetadataStorageInfo();
        String dbfile = ConfigurationManager.getProperty("usage-statistics", "dbfile");
        try
        {
            geoipLookup = new LookupService(dbfile, LookupService.GEOIP_STANDARD);
        }
        catch (FileNotFoundException fe)
        {
            log.error("The GeoLite Database file is missing (" + dbfile + ")! Solr Statistics cannot generate location based reports! Please see the DSpace installation instructions for instructions to install this file.", fe);
        }
        catch (IOException e)
        {
            log.error("Unable to load GeoLite Database file (" + dbfile + ")! You may need to reinstall it. See the DSpace installation instructions for more details.", e);
        }


        StatisticsImporter si = new StatisticsImporter(local);
        if (line.hasOption('m'))
        {
            // Convert all the files
            final File sample = new File(line.getOptionValue('i'));
            File dir = sample.getParentFile();
            FilenameFilter filter = new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    return name.startsWith(sample.getName());
                }
            };
            String[] children = dir.list(filter);
            for (String in : children)
            {
                System.out.println(in);
                si.load(dir.getAbsolutePath() + System.getProperty("file.separator") + in, context, verbose);
            }
        }
        else
        {
            // Just convert the one file
            si.load(line.getOptionValue('i'), context, verbose);
        }
    }


    /**
     * Inner class to hold a cache of reverse lookups of IP addresses
     * @param <K>
     * @param <V>
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
