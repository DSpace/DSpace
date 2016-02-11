/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sitemap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

/**
 * Command-line utility for generating HTML and Sitemaps.org protocol Sitemaps.
 * 
 * @author Robert Tansley
 * @author Stuart Lewis
 */
public class GenerateSitemaps
{
    /** Logger */
    private static Logger log = Logger.getLogger(GenerateSitemaps.class);

    private static final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private static final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static final HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    public static void main(String[] args) throws Exception
    {
        final String usage = GenerateSitemaps.class.getCanonicalName();

        CommandLineParser parser = new PosixParser();
        HelpFormatter hf = new HelpFormatter();

        Options options = new Options();

        options.addOption("h", "help", false, "help");
        options.addOption("s", "no_sitemaps", false,
                "do not generate sitemaps.org protocol sitemap");
        options.addOption("b", "no_htmlmap", false,
                "do not generate a basic HTML sitemap");
        options.addOption("a", "ping_all", false,
                "ping configured search engines");
        options
                .addOption("p", "ping", true,
                        "ping specified search engine URL");
        options.addOption("x", "exclude", true,
                "exclude items from list of communities, collections, and items, given as comma separated list of handles");

        CommandLine line = null;

        try
        {
            line = parser.parse(options, args);
        }
        catch (ParseException pe)
        {
            hf.printHelp(usage, options);
            System.exit(1);
        }

        if (line.hasOption('h'))
        {
            hf.printHelp(usage, options);
            System.exit(0);
        }

        if (line.getArgs().length != 0)
        {
            hf.printHelp(usage, options);
            System.exit(1);
        }

        /*
         * Sanity check -- if no sitemap generation or pinging to do, print
         * usage
         */
        if (line.getArgs().length != 0 || line.hasOption('b')
                && line.hasOption('s') && !line.hasOption('g')
                && !line.hasOption('m') && !line.hasOption('y')
                && !line.hasOption('p'))
        {
            System.err
                    .println("Nothing to do (no sitemap to generate, no search engines to ping)");
            hf.printHelp(usage, options);
            System.exit(1);
        }

        Context c = new Context();

        // Note the negation (CLI options indicate NOT to generate a sitemap)
        if (!line.hasOption('b') || !line.hasOption('s'))
        {
            ArrayList<DSpaceObject> excludeObjs = new ArrayList<DSpaceObject>();

            String excludes = line.getOptionValue('x');
            if (null != excludes) {
                String[] excludeIds = excludes.split(",");
                if (excludeIds.length == 0) {
                    System.err.println("must provide id list to exclude");
                    hf.printHelp(usage, options);
                    System.exit(1);
                }

                for (String id : excludeIds) {
                    DSpaceObject obj = handleService.resolveToObject(c, id);
                    if (obj == null) {
                        hf.printHelp(usage, options);
                        System.exit(1);
                    } else {
                        System.out.println("Will exclude " + obj.toString() + " " + obj.getHandle());
                        excludeObjs.add(obj);
                    }
                }
            }

            generateSitemaps(c, !line.hasOption('b'), !line.hasOption('s'), excludeObjs);
        }

        if (line.hasOption('a'))
        {
            pingConfiguredSearchEngines();
        }

        if (line.hasOption('p'))
        {
            try
            {
                pingSearchEngine(line.getOptionValue('p'));
            }
            catch (MalformedURLException me)
            {
                System.err
                        .println("Bad search engine URL (include all except sitemap URL)");
                System.exit(1);
            }
        }

        System.exit(0);
    }

    /**
     * Generate sitemap.org protocol and/or basic HTML sitemaps.
     * 
     * @param makeHTMLMap
     *            if {@code true}, generate an HTML sitemap.
     * @param makeSitemapOrg
     *            if {@code true}, generate an sitemap.org sitemap.
     * @throws SQLException
     *             if a database error occurs.
     * @throws IOException
     *             if IO error occurs.
     */
    public static void generateSitemaps(Context c, boolean makeHTMLMap,
            boolean makeSitemapOrg, ArrayList<DSpaceObject> excludeObjs) throws SQLException, IOException
    {
        String sitemapStem = ConfigurationManager.getProperty("dspace.url")
                + "/sitemap";
        String htmlMapStem = ConfigurationManager.getProperty("dspace.url")
                + "/htmlmap";
        String handleURLStem = ConfigurationManager.getProperty("dspace.url")
                + "/handle/";

        File outputDir = new File(ConfigurationManager.getProperty("sitemap.dir"));
        if (!outputDir.exists() && !outputDir.mkdir())
        {
            log.error("Unable to create output directory");
        }
        
        AbstractGenerator html = null;
        AbstractGenerator sitemapsOrg = null;

        if (makeHTMLMap)
        {
            html = new HTMLSitemapGenerator(outputDir, htmlMapStem + "?map=",
                    null);
        }

        if (makeSitemapOrg)
        {
            sitemapsOrg = new SitemapsOrgGenerator(outputDir, sitemapStem
                    + "?map=", null);
        }

        ArrayList<AbstractMap.SimpleEntry<String, Date>> handleDateList = new java.util.ArrayList<>();

        ArrayList<String> handelList = new ArrayList<String>();
        List<Community> comms = communityService.findAllTop(c);
        for (Community com: comms) {
            addCommunityHandles(c, com, excludeObjs, handleDateList);
        }

        int nurl = 0;
        for (AbstractMap.SimpleEntry<String, Date> handleDate: handleDateList) {
            String hdl = handleDate.getKey();
            Date date = handleDate.getValue();
            String url = handleURLStem + hdl;
            if (makeHTMLMap) {
                html.addURL(url, date);
            }
            if (makeSitemapOrg) {
                sitemapsOrg.addURL(url, date);
            }
            nurl++;
        }

        if (makeHTMLMap)
        {
            int files = html.finish();
            log.info(LogManager.getHeader(c, "write_sitemap",
                    "type=html,num_files=" + files + ", number of urls=" + nurl));
        }

        if (makeSitemapOrg)
        {
            int files = sitemapsOrg.finish();
            log.info(LogManager.getHeader(c, "write_sitemap",
                    "type=xml,num_files=" + files + ", number of urls=" + nurl));
        }

        c.abort();
    }


    public static void addCommunityHandles(Context c, Community obj, ArrayList<DSpaceObject> excludes,
                                           ArrayList<AbstractMap.SimpleEntry<String, Date>> handleList) throws SQLException {
        if (excludes.contains(obj)) {
            log.info("Skipping " + obj.getHandle()  + " '" + obj.getName() + "'");
            return;
        }
        handleList.add(new java.util.AbstractMap.SimpleEntry<String, Date>(obj.getHandle(),null));
        List<Community> comms = obj.getSubcommunities();
        for (Community com : comms) {
            addCommunityHandles(c, com, excludes, handleList);
        }

        List<Collection> colls = obj.getCollections();
        for (Collection col : colls) {
            addCollectionHandles(c, col, excludes, handleList);
        }
    }

    public static void addCollectionHandles(Context c, Collection obj,
                                            ArrayList<DSpaceObject> excludes,
                                            ArrayList<AbstractMap.SimpleEntry<String, Date>> handleList) throws SQLException {
        if (excludes.contains(obj)) {
            log.info("Skipping " + obj.getHandle()  + "  '" + obj.getName() + "'");

            return;
        }
        handleList.add(new java.util.AbstractMap.SimpleEntry<String, Date>(obj.getHandle(),null));

        Iterator<Item> iter = itemService.findAllByCollection(c, obj);
        while (iter.hasNext()) {
            Item i = iter.next();
            if (excludes.contains(i)) {
                log.info("Skipping " + i.getHandle() + "  '" + i.getName() + "'");
            } else {
                Date lastMod = i.getLastModified();
                handleList.add(new java.util.AbstractMap.SimpleEntry<String, Date>(i.getHandle(),lastMod));
            }
        }
    }

        /**
         * Ping all search engines configured in {@code dspace.cfg}.
         *
         * @throws UnsupportedEncodingException
         *             theoretically should never happen
         */
    public static void pingConfiguredSearchEngines()
            throws UnsupportedEncodingException
    {
        String engineURLProp = ConfigurationManager
                .getProperty("sitemap.engineurls");
        String engineURLs[] = null;

        if (engineURLProp != null)
        {
            engineURLs = engineURLProp.trim().split("\\s*,\\s*");
        }

        if (engineURLProp == null || engineURLs == null
                || engineURLs.length == 0 || engineURLs[0].trim().equals(""))
        {
            log.warn("No search engine URLs configured to ping");
            return;
        }

        for (int i = 0; i < engineURLs.length; i++)
        {
            try
            {
                pingSearchEngine(engineURLs[i]);
            }
            catch (MalformedURLException me)
            {
                log.warn("Bad search engine URL in configuration: "
                        + engineURLs[i]);
            }
        }
    }

    /**
     * Ping the given search engine.
     * 
     * @param engineURL
     *            Search engine URL minus protocol etc, e.g.
     *            {@code www.google.com}
     * @throws MalformedURLException
     *             if the passed in URL is malformed
     * @throws UnsupportedEncodingException
     *             theoretically should never happen
     */
    public static void pingSearchEngine(String engineURL)
            throws MalformedURLException, UnsupportedEncodingException
    {
        // Set up HTTP proxy
        if ((StringUtils.isNotBlank(ConfigurationManager.getProperty("http.proxy.host")))
                && (StringUtils.isNotBlank(ConfigurationManager.getProperty("http.proxy.port"))))
        {
            System.setProperty("proxySet", "true");
            System.setProperty("proxyHost", ConfigurationManager
                    .getProperty("http.proxy.host"));
            System.getProperty("proxyPort", ConfigurationManager
                    .getProperty("http.proxy.port"));
        }

        String sitemapURL = ConfigurationManager.getProperty("dspace.url")
                + "/sitemap";

        URL url = new URL(engineURL + URLEncoder.encode(sitemapURL, "UTF-8"));

        try
        {
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();

            BufferedReader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));

            String inputLine;
            StringBuffer resp = new StringBuffer();
            while ((inputLine = in.readLine()) != null)
            {
                resp.append(inputLine).append("\n");
            }
            in.close();

            if (connection.getResponseCode() == 200)
            {
                log.info("Pinged " + url.toString() + " successfully");
            }
            else
            {
                log.warn("Error response pinging " + url.toString() + ":\n"
                        + resp);
            }
        }
        catch (IOException e)
        {
            log.warn("Error pinging " + url.toString(), e);
        }
    }
}
