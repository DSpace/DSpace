/*
 * GenerateSitemaps.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/03/17 00:04:38 $
 *
 * Copyright (c) 2002-2006, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

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
     * @throws SQLException
     *             if a database error occurs.
     * @throws IOException
     *             if IO error occurs.
     */
    public static void generateSitemaps(boolean makeHTMLMap,
            boolean makeSitemapOrg) throws SQLException, IOException
    {
        String sitemapStem = ConfigurationManager.getProperty("dspace.url")
                + "/sitemap";
        String htmlMapStem = ConfigurationManager.getProperty("dspace.url")
                + "/htmlmap";
        String handleURLStem = ConfigurationManager.getProperty("dspace.url")
                + "/handle/";

        File outputDir = new File(ConfigurationManager
                .getProperty("dspace.dir"), "sitemaps");
        if (!outputDir.exists()) {
        	outputDir.mkdir();
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

        Context c = new Context();

        Community[] comms = Community.findAll(c);

        for (int i = 0; i < comms.length; i++)
        {
            String url = handleURLStem + comms[i].getHandle();

            if (makeHTMLMap)
                html.addURL(url, null);
            if (makeSitemapOrg)
                sitemapsOrg.addURL(url, null);
        }

        Collection[] colls = Collection.findAll(c);

        for (int i = 0; i < colls.length; i++)
        {
            String url = handleURLStem + colls[i].getHandle();

            if (makeHTMLMap)
                html.addURL(url, null);
            if (makeSitemapOrg)
                sitemapsOrg.addURL(url, null);
        }

        ItemIterator allItems = Item.findAll(c);
        try
        {
            int itemCount = 0;

            while (allItems.hasNext())
            {
                Item i = allItems.next();
                String url = handleURLStem + i.getHandle();
                Date lastMod = i.getLastModified();

                if (makeHTMLMap)
                    html.addURL(url, lastMod);
                if (makeSitemapOrg)
                    sitemapsOrg.addURL(url, lastMod);
                i.decache();

                itemCount++;
            }

            if (makeHTMLMap)
            {
                int files = html.finish();
                log.info(LogManager.getHeader(c, "write_sitemap",
                        "type=html,num_files=" + files + ",communities="
                                + comms.length + ",collections=" + colls.length
                                + ",items=" + itemCount));
            }

            if (makeSitemapOrg)
            {
                int files = sitemapsOrg.finish();
                log.info(LogManager.getHeader(c, "write_sitemap",
                        "type=html,num_files=" + files + ",communities="
                                + comms.length + ",collections=" + colls.length
                                + ",items=" + itemCount));
            }
        }
        finally
        {
            if (allItems != null)
                allItems.close();
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
     * @return {@code true} if the operation was successful
     * @throws MalformedURLException
     *             if the passed in URL is malformed
     * @throws UnsupportedEncodingException
     *             theoretically should never happen
     */
    public static void pingSearchEngine(String engineURL)
            throws MalformedURLException, UnsupportedEncodingException
    {
        // Set up HTTP proxy
        if ((ConfigurationManager.getProperty("http.proxy.host") != null)
                && (ConfigurationManager.getProperty("http.proxy.port") != null))
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
