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
import java.util.Date;

/**
 * Class for generating HTML "sitemaps" which contain links to various pages in
 * a DSpace site. This should improve search engine coverage of the DSpace site
 * and limit the server load caused by crawlers.
 * 
 * @author Robert Tansley
 * @author Stuart Lewis
 */
public class HTMLSitemapGenerator extends AbstractGenerator
{
    /** Stem of URLs sitemaps will eventually appear at */
    protected String indexURLStem;

    /** Tail of URLs sitemaps will eventually appear at */
    protected String indexURLTail;

    /**
     * Construct an HTML sitemap generator, writing files to the given
     * directory, and with the sitemaps eventually exposed at starting with the
     * given URL stem and tail.
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
    public HTMLSitemapGenerator(File outputDirIn, String urlStem, String urlTail)
    {
        super(outputDirIn);

        indexURLStem = urlStem;
        indexURLTail = (urlTail == null ? "" : urlTail);
    }

    @Override
    public String getFilename(int number)
    {
        return "sitemap" + number + ".html";
    }

    @Override
    public String getLeadingBoilerPlate()
    {
        return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n"
                + "<html><head><title>URL List</title></head><body><ul>";
    }

    @Override
    public int getMaxSize()
    {
        // 50k
        return 51200;
    }

    @Override
    public int getMaxURLs()
    {
        return 1000;
    }

    @Override
    public String getTrailingBoilerPlate()
    {
        return "</ul></body></html>\n";
    }

    @Override
    public String getURLText(String url, Date lastMod)
    {
        StringBuffer urlText = new StringBuffer();

        urlText.append("<li><a href=\"").append(url).append("\">").append(url)
                .append("</a></li>\n");

        return urlText.toString();
    }

    @Override
    public boolean useCompression()
    {
        return false;
    }

    @Override
    public String getIndexFilename()
    {
        return "sitemap_index.html";
    }

    @Override
    public void writeIndex(PrintStream output, int sitemapCount)
            throws IOException
    {
        output.println(getLeadingBoilerPlate());

        for (int i = 0; i < sitemapCount; i++)
        {
            output.print("<li><a href=\"" + indexURLStem + i + indexURLTail
                    + "\">sitemap " + i);
            output.print("</a></li>\n");
        }

        output.println(getTrailingBoilerPlate());
    }
}
