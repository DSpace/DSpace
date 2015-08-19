/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
    protected String indexURLStem;

    /** Tail of URLs sitemaps will eventually appear at */
    protected String indexURLTail;

    /** The correct date format */
    protected DateFormat w3dtfFormat = new SimpleDateFormat(
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

    @Override
    public String getFilename(int number)
    {
        return "sitemap" + number + ".xml.gz";
    }

    @Override
    public String getLeadingBoilerPlate()
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">";
    }

    @Override
    public int getMaxSize()
    {
        // 10 Mb
        return 10485760;
    }

    @Override
    public int getMaxURLs()
    {
        return 50000;
    }

    @Override
    public String getTrailingBoilerPlate()
    {
        return "</urlset>";
    }

    @Override
    public String getURLText(String url, Date lastMod)
    {
        StringBuffer urlText = new StringBuffer();

        urlText.append("<url><loc>").append(url).append("</loc>");
        if (lastMod != null)
        {
            urlText.append("<lastmod>").append(w3dtfFormat.format(lastMod))
                    .append("</lastmod>");
        }
        urlText.append("</url>\n");

        return urlText.toString();
    }

    @Override
    public boolean useCompression()
    {
        return true;
    }

    @Override
    public String getIndexFilename()
    {
        return "sitemap_index.xml.gz";
    }

    @Override
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
            output.print("<lastmod>" + now + "</lastmod></sitemap>\n");
        }

        output.println("</sitemapindex>");
    }
}
