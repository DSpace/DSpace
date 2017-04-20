/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sitemap;

import org.apache.commons.cli.*;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
    private static final ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

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

        // Note the negation (CLI options indicate NOT to generate a sitemap)
        if (!line.hasOption('b') || !line.hasOption('s'))
        {
            generateSitemaps(!line.hasOption('b'), !line.hasOption('s'));
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
     * @throws SQLException if database error
     *             if a database error occurs.
     * @throws IOException if IO error
     *             if IO error occurs.
     */
    public static void generateSitemaps(boolean makeHTMLMap,
            boolean makeSitemapOrg) throws SQLException, IOException
    {
        String sitemapStem = configurationService.getProperty("dspace.url")
                + "/sitemap";
        String htmlMapStem = configurationService.getProperty("dspace.url")
                + "/htmlmap";
        String handleURLStem = configurationService.getProperty("dspace.url")
                + "/handle/";

        File outputDir = new File(configurationService.getProperty("sitemap.dir"));
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

        Context c = new Context(Context.Mode.READ_ONLY);

        List<Community> comms = communityService.findAll(c);

        for (Community comm : comms) {
            String url = handleURLStem + comm.getHandle();

            if (makeHTMLMap) {
                html.addURL(url, null);
            }
            if (makeSitemapOrg) {
                sitemapsOrg.addURL(url, null);
            }

            c.uncacheEntity(comm);
        }

        List<Collection> colls = collectionService.findAll(c);

        for (Collection coll : colls) {
            String url = handleURLStem + coll.getHandle();

            if (makeHTMLMap) {
                html.addURL(url, null);
            }
            if (makeSitemapOrg) {
                sitemapsOrg.addURL(url, null);
            }

            c.uncacheEntity(coll);
        }

        Iterator<Item> allItems = itemService.findAll(c);
        int itemCount = 0;

        while (allItems.hasNext())
        {
            Item i = allItems.next();
            String url = handleURLStem + i.getHandle();
            Date lastMod = i.getLastModified();

            if (makeHTMLMap)
            {
                html.addURL(url, lastMod);
            }
            if (makeSitemapOrg)
            {
                sitemapsOrg.addURL(url, lastMod);
            }

            c.uncacheEntity(i);

            itemCount++;
        }

        if (makeHTMLMap)
        {
            int files = html.finish();
            log.info(LogManager.getHeader(c, "write_sitemap",
                    "type=html,num_files=" + files + ",communities="
                            + comms.size() + ",collections=" + colls.size()
                            + ",items=" + itemCount));
        }

        if (makeSitemapOrg)
        {
            int files = sitemapsOrg.finish();
            log.info(LogManager.getHeader(c, "write_sitemap",
                    "type=html,num_files=" + files + ",communities="
                            + comms.size() + ",collections=" + colls.size()
                            + ",items=" + itemCount));
        }

        c.abort();
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
        String[] engineURLs = configurationService
                .getArrayProperty("sitemap.engineurls");
        
        if (ArrayUtils.isEmpty(engineURLs))
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
        if ((StringUtils.isNotBlank(configurationService.getProperty("http.proxy.host")))
                && (StringUtils.isNotBlank(configurationService.getProperty("http.proxy.port"))))
        {
            System.setProperty("proxySet", "true");
            System.setProperty("proxyHost", configurationService
                    .getProperty("http.proxy.host"));
            System.getProperty("proxyPort", configurationService
                    .getProperty("http.proxy.port"));
        }

        String sitemapURL = configurationService.getProperty("dspace.url")
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
