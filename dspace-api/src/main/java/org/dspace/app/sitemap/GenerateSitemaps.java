/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sitemap;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
<<<<<<< HEAD

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
=======
import org.apache.commons.cli.*;
>>>>>>> 1e8c07f... add -x and -S options
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

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
    private static Logger log = Logger.getLogger(GenerateSitemaps.class);

    public static void main(String[] args) throws Exception {
        final String usage = GenerateSitemaps.class.getCanonicalName();
        CommandLineParser parser = new PosixParser();
        HelpFormatter hf = new HelpFormatter();

        Options options = new Options();
        options.addOption("h", "help", false, "help");
        options.addOption("s", "no_sitemaps", false, "do not generate sitemaps.org protocol sitemap");
        options.addOption("b", "no_htmlmap", false, "do not generate a basic HTML sitemap");
        options.addOption("a", "ping_all", false, "ping configured search engines");
        options.addOption("x", "exclude", true, "exclude items from list of communities, collections, and items, given as comma separated list of  TYPE.id, or handle");
        options.addOption("S", "scholar", false, "exclude items without bitstreams readable by anonymous users");
        options.addOption("p", "ping", true, "ping specified search engine URL");

        CommandLine line = null;
        Boolean error = false;
        Context c = new Context();
        try {
            ArrayList<DSpaceObject> excludeComm = new ArrayList<DSpaceObject>();
            ArrayList<DSpaceObject> excludeColl = new ArrayList<DSpaceObject>();
            ArrayList<DSpaceObject> excludeItem = new ArrayList<DSpaceObject>();
            line = parser.parse(options, args);

            if (line.hasOption('h')) {
                hf.printHelp(usage, options);
                return;
            }

            if (line.hasOption('x')) {
                String excludes = line.getOptionValue('x');
                String[] excludeIds = excludes.split(",");
                if (excludeIds.length == 0) {
                    System.err.println("must provide id list to exclude");
                    error = true;
                }
                for (String id : excludeIds) {
                    DSpaceObject obj = DSpaceObject.fromString(c, id);
                    if (obj == null) {
                        error = true;
                    } else {
                        System.out.println("Will exclude " + obj.toString() + " " + obj.getHandle());
                        switch (obj.getType()) {
                            case Constants.COMMUNITY:
                                excludeComm.add(obj);
                                break;
                            case Constants.COLLECTION:
                                excludeColl.add(obj);
                                break;
                            case Constants.ITEM:
                                excludeItem.add(obj);
                                break;
                            default:
                                System.err.println("'" + id + " ' not a community, collection or item");
                                error = true;
                        }
                    }
                }
            }

            /*
             * Sanity check -- error if no sitemap generation or pinging to do
             */
            if (line.hasOption('b') &&
                    line.hasOption('s') && !line.hasOption('g') &&
                    !line.hasOption('m') && !line.hasOption('y') &&
                    !line.hasOption('p')) {
                System.err.println("Nothing to do (no sitemap to generate, no search engines to ping)");
                error = true;
            }

            if (error)
                return;

            // Note the negation (CLI options indicate NOT to generate a sitemap)
            if (!line.hasOption('b') || !line.hasOption('s')) {
                if (!line.hasOption('b')) {
                    System.out.println("Generate HTLM map");
                }
                if (!line.hasOption('s')) {
                    System.out.println("Generate Sitemap.org map");
                }
                if (line.hasOption('S')) {
                    System.out.println("Exclude Items without READable Bitstreams");
                }
                generateSitemaps(!line.hasOption('b'), !line.hasOption('s'), !line.hasOption('S'),
                        c, excludeComm, excludeColl, excludeItem);
            }

            if (line.hasOption('a')) {
                pingConfiguredSearchEngines();
            }

            if (line.hasOption('p')) {
                try {
                    pingSearchEngine(line.getOptionValue('p'));
                } catch (MalformedURLException me) {
                    System.err.println("Bad search engine URL (include all except sitemap URL)");
                    System.exit(1);
                }
            }

        } catch (ParseException pe) {
            System.err.println("Could not parse arguments");
            error = true;
        } finally {
            c.complete();
            if (error) {
                hf.printHelp(usage, options);
                System.exit(1);
            } else {
                System.exit(0);
            }
        }
    }

    /**
     * Generate sitemap.org protocol and/or basic HTML sitemaps.
     *
     * @param makeHTMLMap    if {@code true}, generate an HTML sitemap.
     * @param makeSitemapOrg if {@code true}, generate an sitemap.org sitemap.
     * @throws SQLException if a database error occurs.
     * @throws IOException  if IO error occurs.
     */
    public static void generateSitemaps(boolean makeHTMLMap, boolean makeSitemapOrg, boolean doItemsWithoutBiistreams,
                                        Context c, ArrayList<DSpaceObject> excludeComm, ArrayList<DSpaceObject> excludeColl, ArrayList<DSpaceObject> excludeItem) throws SQLException, IOException {
        String sitemapStem = ConfigurationManager.getProperty("dspace.url") + "/sitemap";
        String htmlMapStem = ConfigurationManager.getProperty("dspace.url") + "/htmlmap";
        String handleURLStem = ConfigurationManager.getProperty("dspace.url") + "/handle/";

        File outputDir = new File(ConfigurationManager.getProperty("sitemap.dir"));
        if (!outputDir.exists() && !outputDir.mkdir()) {
            log.error("Unable to create output directory");
        }

        AbstractGenerator html = null;
        AbstractGenerator sitemapsOrg = null;

        if (makeHTMLMap) {
            html = new HTMLSitemapGenerator(outputDir, htmlMapStem + "?map=", null);
        }

        if (makeSitemapOrg) {
            sitemapsOrg = new SitemapsOrgGenerator(outputDir, sitemapStem + "?map=", null);
        }


        Community[] comms = Community.findAll(c);
        int commCount = 0;
        for (int i = 0; i < comms.length; i++) {
            if (excludeComm.contains(comms[i])) {
                System.out.println("Skipping Community " + comms[i].getHandle() + "\t" + ((Community) comms[i]).getName());
            } else {
                if (AuthorizeManager.authorizeActionBoolean(c, comms[i], org.dspace.core.Constants.READ)) {
                    System.out.println("Doing    Community " + comms[i].getHandle() + "\t" + ((Community) comms[i]).getName());
                    String url = handleURLStem + comms[i].getHandle();

                    if (makeHTMLMap) {
                        html.addURL(url, null);
                    }
                    if (makeSitemapOrg) {
                        sitemapsOrg.addURL(url, null);
                    }
                }
                commCount++;
            }
        }

        Collection[] colls = Collection.findAll(c);
        int itemCount = 0;
        int collCount = 0;
        for (int i = 0; i < colls.length; i++) {
            if (excludeColl.contains(colls[i]) || excludeComm.contains(colls[i].getParentObject())) {
                System.out.println("Skipping Collection " + colls[i].getHandle() + " in " + colls[i].getParentObject().getHandle() + "\t" + ((Collection) colls[i]).getName());
            } else {
                if (AuthorizeManager.authorizeActionBoolean(c, colls[i], org.dspace.core.Constants.READ)) {
                    System.out.println("Doing    Collection " + colls[i].getHandle() + " in " + colls[i].getParentObject().getHandle() + "\t" + ((Collection) colls[i]).getName());
                    String url = handleURLStem + colls[i].getHandle();

                    if (makeHTMLMap) {
                        html.addURL(url, null);
                    }
                    if (makeSitemapOrg) {
                        sitemapsOrg.addURL(url, null);
                    }
                    collCount++;
                    ItemIterator collItens = colls[i].getItems();
                    while (collItens.hasNext()) {
                        Item itm = collItens.next();
                        if (itm.getOwningCollection() == colls[i] &&
                                !excludeItem.contains(itm) &&
                                AuthorizeManager.authorizeActionBoolean(c, itm, org.dspace.core.Constants.READ) &&
                                (doItemsWithoutBiistreams || hasReadableBitstream(c, itm))) {
                            url = handleURLStem + itm.getHandle();
                            Date lastMod = itm.getLastModified();

                            if (makeHTMLMap) {
                                html.addURL(url, lastMod);
                            }
                            if (makeSitemapOrg) {
                                sitemapsOrg.addURL(url, lastMod);
                            }
                            itm.decache();

                            itemCount++;

                        } else {
                            System.out.println("Skipping Item      " + itm.getHandle() + " in " + colls[i].getHandle() + "\t" + itm.getName());
                        }
                    }
                }
            }
        }

        if (makeHTMLMap) {
            int files = html.finish();
            log.info(LogManager.getHeader(c, "write_sitemap", "type=html,num_files=" + files + ",communities=" + commCount + ",collections=" + collCount + ",items=" + itemCount));
        }

        if (makeSitemapOrg) {
            int files = sitemapsOrg.finish();
            log.info(LogManager.getHeader(c, "write_sitemap", "type=html,num_files=" + files + ",communities=" + commCount + ",collections=" + collCount + ",items=" + itemCount));
        }
    }


    private static boolean hasReadableBitstream(Context c, Item item) throws SQLException {
        Bundle bundles[] = item.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            Bitstream bitstreams[] = bundles[i].getBitstreams();
            for (int b = 0; b < bitstreams.length; b++) {
                if (AuthorizeManager.authorizeActionBoolean(c, bitstreams[b], Constants.READ))
                    return true;
            }
        }
        return false;
    }

    /**
     * Ping all search engines configured in {@code dspace.cfg}.
     *
     * @throws UnsupportedEncodingException theoretically should never happen
     */
    public static void pingConfiguredSearchEngines() throws UnsupportedEncodingException {
        String engineURLProp = ConfigurationManager.getProperty("sitemap.engineurls");
        String engineURLs[] = null;

        if (engineURLProp != null) {
            engineURLs = engineURLProp.trim().split("\\s*,\\s*");
        }

        if (engineURLProp == null || engineURLs == null || engineURLs.length == 0 || engineURLs[0].trim().equals("")) {
            log.warn("No search engine URLs configured to ping");
            return;
        }

        for (int i = 0; i < engineURLs.length; i++) {
            try {
                pingSearchEngine(engineURLs[i]);
            } catch (MalformedURLException me) {
                log.warn("Bad search engine URL in configuration: " + engineURLs[i]);
            }
        }
    }

    /**
     * Ping the given search engine.
     *
     * @param engineURL Search engine URL minus protocol etc, e.g.
     *                  {@code www.google.com}
     * @throws MalformedURLException        if the passed in URL is malformed
     * @throws UnsupportedEncodingException theoretically should never happen
     */
    public static void pingSearchEngine(String engineURL) throws MalformedURLException, UnsupportedEncodingException {
        // Set up HTTP proxy
        if ((StringUtils.isNotBlank(ConfigurationManager.getProperty("http.proxy.host")))
                && (StringUtils.isNotBlank(ConfigurationManager.getProperty("http.proxy.port"))))
        {
            System.setProperty("proxySet", "true");
            System.setProperty("proxyHost", ConfigurationManager.getProperty("http.proxy.host"));
            System.getProperty("proxyPort", ConfigurationManager.getProperty("http.proxy.port"));
        }

        String sitemapURL = ConfigurationManager.getProperty("dspace.url") + "/sitemap";

        URL url = new URL(engineURL + URLEncoder.encode(sitemapURL, "UTF-8"));

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String inputLine;
            StringBuffer resp = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                resp.append(inputLine).append("\n");
            }
            in.close();

            if (connection.getResponseCode() == 200) {
                log.info("Pinged " + url.toString() + " successfully");
            } else {
                log.warn("Error response pinging " + url.toString() + ":\n" + resp);
            }
        } catch (IOException e) {
            log.warn("Error pinging " + url.toString(), e);
        }
    }
}
