/*
 * SitemapsOrgGenerator.java
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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class for generating <a href="http://sitemaps.org/">Sitemaps</a> to improve
 * search engine coverage of the DSpace site and limit the server load caused by
 * crawlers.
 * 
 * @author Robert Tansley
 * @author Stuart Lewis
 */
public class SitemapsOrgGenerator extends AbstractGenerator
{
    /** Stem of URLs sitemaps will eventually appear at */
    private String indexURLStem;

    /** Tail of URLs sitemaps will eventually appear at */
    private String indexURLTail;

    /** The correct date format */
    private DateFormat w3dtfFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * Construct a sitemaps.org protocol sitemap generator, writing files to the
     * given directory, and with the sitemaps eventually exposed at starting
     * with the given URL stem and tail.
     * 
     * @param outputDirIn
     *            Directory to write sitemap files to
     * @param urlStem
     *            start of URL that sitemap files will appear at, e.g.
     *            {@code http://dspace.myu.edu/sitemap?sitemap=}
     * @param urlTail
     *            end of URL that sitemap files will appear at, e.g.
     *            {@code .html} or {@code null}
     */
    public SitemapsOrgGenerator(File outputDirIn, String urlStem, String urlTail)
    {
        super(outputDirIn);

        indexURLStem = urlStem;
        indexURLTail = (urlTail == null ? "" : urlTail);
    }

    public String getFilename(int number)
    {
        return "sitemap" + number + ".xml.gz";
    }

    public String getLeadingBoilerPlate()
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">";
    }

    public int getMaxSize()
    {
        // 10 Mb
        return 10485760;
    }

    public int getMaxURLs()
    {
        return 50000;
    }

    public String getTrailingBoilerPlate()
    {
        return "</urlset>";
    }

    public String getURLText(String url, Date lastMod)
    {
        StringBuffer urlText = new StringBuffer();

        urlText.append("<url><loc>").append(url).append("</loc>");
        if (lastMod != null)
        {
            urlText.append("<lastmod>").append(w3dtfFormat.format(lastMod))
                    .append("</lastmod>");
        }
        urlText.append("</url>");

        return urlText.toString();
    }

    public boolean useCompression()
    {
        return true;
    }

    public String getIndexFilename()
    {
        return "sitemap_index.xml.gz";
    }

    public void writeIndex(PrintStream output, int sitemapCount)
            throws IOException
    {
        String now = w3dtfFormat.format(new Date());

        output.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        output
                .println("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        for (int i = 0; i < sitemapCount; i++)
        {
            output.print("<sitemap><loc>" + indexURLStem + i + indexURLTail
                    + "</loc>");
            output.print("<lastmod>" + now + "</lastmod></sitemap>");
        }

        output.println("</sitemapindex>");
    }
}
