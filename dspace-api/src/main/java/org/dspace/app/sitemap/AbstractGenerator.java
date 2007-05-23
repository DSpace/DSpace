/*
 * AbstractMETSDisseminator.java
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

/**
 * Base class for creating sitemaps of various kinds. A sitemap consists of one
 * or more files which list significant URLs on a site for search engines to
 * efficiently crawl. Dates of modification may also be included. A sitemap
 * index file that links to each of the sitemap files is also generated. It is
 * this index file that search engines should be directed towards.
 * <P>
 * Provides most of the required functionality, subclasses need just implement a
 * few methods that specify the "boilerplate" and text for including URLs.
 * <P>
 * Typical usage:
 * <pre>
 *   AbstractGenerator g = new FooGenerator(...);
 *   while (...) {
 *     g.addURL(url, date);
 *   }
 *   g.finish();
 * </pre>
 * 
 * @author Robert Tansley
 */
public abstract class AbstractGenerator
{
    /** Number of files written so far */
    protected int fileCount;

    /** Number of bytes written to current file */
    protected int bytesWritten;

    /** Number of URLs written to current file */
    protected int urlsWritten;

    /** Directory files are written to */
    protected File outputDir;

    /** Current output */
    protected PrintStream currentOutput;

    /** Size in bytes of trailing boilerplate */
    private int trailingByteCount;

    /**
     * Initialize this generator to write to the given directory. This must be
     * called by any subclass constructor.
     * 
     * @param outputDirIn
     *            directory to write sitemap files to
     */
    public AbstractGenerator(File outputDirIn)
    {
        fileCount = 0;
        outputDir = outputDirIn;
        trailingByteCount = getTrailingBoilerPlate().length();
        currentOutput = null;
    }

    /**
     * Start writing a new sitemap file.
     * 
     * @throws IOException
     *             if an error occurs creating the file
     */
    protected void startNewFile() throws IOException
    {
        String lbp = getLeadingBoilerPlate();

        OutputStream fo = new FileOutputStream(new File(outputDir,
                getFilename(fileCount)));

        if (useCompression())
        {
            fo = new GZIPOutputStream(fo);
        }

        currentOutput = new PrintStream(fo);
        currentOutput.print(lbp);
        bytesWritten = lbp.length();
        urlsWritten = 0;
    }

    /**
     * Add the given URL to the sitemap.
     * 
     * @param url
     *            Full URL to add
     * @param lastMod
     *            Date URL was last modified, or {@code null}
     * @throws IOException
     *             if an error occurs writing
     */
    public void addURL(String url, Date lastMod) throws IOException
    {
        // Kick things off if this is the first call
        if (currentOutput == null)
        {
            startNewFile();
        }

        String newURLText = getURLText(url, lastMod);

        if (bytesWritten + newURLText.length() + trailingByteCount > getMaxSize()
                || urlsWritten + 1 > getMaxURLs())
        {
            closeCurrentFile();
            startNewFile();
        }

        currentOutput.print(newURLText);
        bytesWritten += newURLText.length();
        urlsWritten++;
    }

    /**
     * Finish with the current sitemap file.
     * 
     * @throws IOException
     *             if an error occurs writing
     */
    protected void closeCurrentFile() throws IOException
    {
        currentOutput.print(getTrailingBoilerPlate());
        currentOutput.close();
        fileCount++;
    }

    /**
     * Complete writing sitemap files and write the index files. This is invoked
     * when all calls to {@link AbstractGenerator#addURL(String, Date)} have
     * been completed, and invalidates the generator.
     * 
     * @return number of sitemap files written.
     * 
     * @throws IOException
     *             if an error occurs writing
     */
    public int finish() throws IOException
    {
        closeCurrentFile();

        OutputStream fo = new FileOutputStream(new File(outputDir,
                getIndexFilename()));

        if (useCompression())
        {
            fo = new GZIPOutputStream(fo);
        }

        PrintStream out = new PrintStream(fo);
        writeIndex(out, fileCount);
        out.close();
        
        return fileCount;
    }

    /**
     * Return marked-up text to be included in a sitemap about a given URL.
     * 
     * @param url
     *            URL to add information about
     * @param lastMod
     *            date URL was last modified, or {@code null} if unknown or not
     *            applicable
     * @return the mark-up to include
     */
    public abstract String getURLText(String url, Date lastMod);

    /**
     * Return the boilerplate at the top of a sitemap file.
     * 
     * @return The boilerplate markup.
     */
    public abstract String getLeadingBoilerPlate();

    /**
     * Return the boilerplate at the end of a sitemap file.
     * 
     * @return The boilerplate markup.
     */
    public abstract String getTrailingBoilerPlate();

    /**
     * Return the maximum size in bytes that an individual sitemap file should
     * be.
     * 
     * @return the size in bytes.
     */
    public abstract int getMaxSize();

    /**
     * Return the maximum number of URLs that an individual sitemap file should
     * contain.
     * 
     * @return the maximum number of URLs.
     */
    public abstract int getMaxURLs();

    /**
     * Return whether the written sitemap files and index should be
     * GZIP-compressed.
     * 
     * @return {@code true} if GZIP compression should be used, {@code false}
     *         otherwise.
     */
    public abstract boolean useCompression();

    /**
     * Return the filename a sitemap at the given index should be stored at.
     * 
     * @param number
     *            index of the sitemap file (zero is first).
     * @return the filename to write the sitemap to.
     */
    public abstract String getFilename(int number);

    /**
     * Get the filename the index should be written to.
     * 
     * @return the filename of the index.
     */
    public abstract String getIndexFilename();

    /**
     * Write the index file.
     * 
     * @param output
     *            stream to write the index to
     * @param sitemapCount
     *            number of sitemaps that were generated
     * @throws IOException
     *             if an IO error occurs
     */
    public abstract void writeIndex(PrintStream output, int sitemapCount)
            throws IOException;
}
