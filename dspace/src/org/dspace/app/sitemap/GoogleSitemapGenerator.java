package org.dspace.app.sitemap;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Utility for generating <a
 * href="http://www.google.com/webmasters/sitemaps/docs/en/protocol.html">Google
 * Sitemaps</a> to improve search engine coverage of the DSpace site and limit
 * the server load caused by indexing.
 * <p>
 * This is a separate tool that creates the Sitemaps as static files. This means
 * they can be created at low priority as a scheduled job (perhaps timed for
 * when server load tends to be low) and can be read by search engines very
 * cheaply.
 * <p>
 * The tool creates a Sitemap index file, a Sitemap that covers some "common"
 * areas of DSpace (e.g. the community-list page) and the community and
 * collection home pages, and then one or more Sitemaps containing the URLs and
 * last modified dates of each item home page. Since we're limited to 50k URLs
 * and 10Mb per Sitemap, in many sites there will be more than one Sitemap.
 * However we can create up to 999 Sitemaps containing item URLs, which means
 * this will scale up to sites of 50M items, which should suffice for now. (The
 * 50k limit also applies to the Sitemap with the community and collection URLs
 * in, however it's unlikely that sites will have more than 50k
 * communities/collections, and if they do, there are other issues they'll run
 * into before Sitemap problems!!)
 * 
 * @author Robert Tansley & Stuart Lewis
 * @version $Revision$
 */
public class GoogleSitemapGenerator
{
    /**
     * Max number of (uncompressed) bytes to write to a Sitemap (10MB). We
     * deduct 20 to make room for the ending boilerplate.
     */
    private final static int SITEMAP_FILESIZE_LIMIT = 10 * 1024 * 1024 - 20;

    /** Max number of URLs in a single Sitemap */
    private final static int SITEMAP_URL_LIMIT = 50000;

    /** The stem of all URLs */
    private final String URL_STEM = ConfigurationManager.getProperty("dspace.url") + 
                                    "/handle/";
    
    /** The stem for sitemaps */
    private final static String SITEMAP_STEM = ConfigurationManager.getProperty("dspace.url") +
                                               "/sitemap?google=";

    /** The correct date format */
    private DateFormat w3dtfFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    /** The destination of the output files */
    private static String output = ConfigurationManager.getProperty("dspace.dir") +
                                   "/sitemaps/";
    
    /** The sitemap leading boilerplate */
	private final String leadingBoilerplate = 
		               "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                       "<urlset xmlns=\"http://www.google.com/schemas/sitemap/0.84\">";
	
	/** The sitemap trailing boilerplate */
	private final String trailingBoilerplate = "</urlset>";

    /**
     * Create Sitemap files for all of the items in the DSpace instance. If
     * there are too many items to fit in a single Sitemap file (there's a 10MB
     * and 50k URL limit), multiple Sitemap files may be created.
     * <p>
     * The Sitemaps will be written as GZIP-compressed files in the given
     * directory, e.g. if three are written:
     * 
     * <pre>
     *  google-sitemap1.xml.gz
     *  google-sitemap2.xml.gz
     *  google-sitemap3.xml.gz
     * </pre>
     * 
     * @param context context object to use
     * @param outputDir directory to write Sitemaps to
     * @return number of Sitemaps created
     * 
     * @throws SQLException if a database error occurs
     * @throws IOException if some IO error occurs
     */
    public int generateItemSitemaps(Context context, File outputDir)
                                                throws SQLException, IOException
    {
    	int sitemapCount = 0;

    	ItemIterator allItems = Item.findAll(context);

    	while (allItems.hasNext())
    	{
    		// Increment here, so we'll start with sitemap1.xml.gz
    		sitemapCount++;

    		// Create the GZIPped output
    		CountingGZIPOutputStream gzipOut = new CountingGZIPOutputStream(
    				      new FileOutputStream(new File(outputDir, "google-sitemap"
                          + sitemapCount + ".xml.gz")));
    		
    		// For convenience
    		PrintStream out = new PrintStream(gzipOut);

    		// Leading boilerplate
    		out.println(leadingBoilerplate);

    		// Write items until either there are no more to write, or
    		// we reach the limit of a single Sitemap's size
		    for (int numWritten = 0; allItems.hasNext()
		            && gzipOut.getBytesWritten() < SITEMAP_FILESIZE_LIMIT
		            && numWritten < SITEMAP_URL_LIMIT; numWritten++)
		    {
		        Item item = allItems.next();
		        out.print("<url><loc>");
		        out.print(URL_STEM);
		        out.print(item.getHandle());
		        out.print("</loc><lastmod>");
		        out.print(w3dtfFormat.format(item.getLastModified()));
		        out.println("</lastmod></url>");

        	    /*
         		* Flush Context object cache every so often to prevent runaway
         		* memory use. We force a garbage collection which may be
         		* expensive but this tool should a) be only run once every day
         		* or so and b) run in a separate, lower-priority JVM to
         		* external online services.
         		*/
        		if (numWritten % 100 == 0)
        		{
            		context.removeAllCached();
            		System.gc(); // Force clearout
        		}
    		}

    		// Trailing boilerplate
    		out.println(trailingBoilerplate);
    		out.flush();
    		out.close();
		}

		return (sitemapCount);
	}

    /**
     * Generate the commuinity sitemaps
     * 
     * @param context
     * @param outputDir
     * @param sitemapCount
     * @return The number of community 
     * @throws SQLException
     * @throws IOException
     */
    public int generateCommunitySitemaps(Context context, File outputDir, int sitemapCount)
    throws SQLException, IOException
    {
    	Community community[] = Community.findAll(context);
    	Community c;
    	int numWritten = 0;
    	int sitemapOriginal = sitemapCount;
    	sitemapCount++;
    	    	
    	// Create the GZIPped output
		CountingGZIPOutputStream gzipOut = new CountingGZIPOutputStream(
				      new FileOutputStream(new File(outputDir, "google-sitemap"
                      + sitemapCount + ".xml.gz")));
		 
		// For convenience
		PrintStream out = new PrintStream(gzipOut);

		// Leading boilerplate
		out.println(leadingBoilerplate);
    	
    	for (int i = 0; i < community.length; i++)
    	{
    		// Write communities until either there are no more to write, or
    		// we reach the limit of a single Sitemap's size
		    
		    if ((gzipOut.getBytesWritten() >= SITEMAP_FILESIZE_LIMIT) ||
		        (numWritten >= SITEMAP_URL_LIMIT))
		    {
		    	// Trailing boilerplate
	    		out.println(trailingBoilerplate);
	    		out.flush();
	    		out.close();
	    		
	    		// Start writing to a new file
	    		gzipOut = new CountingGZIPOutputStream(
  				      new FileOutputStream(new File(outputDir, "sitemap"
                      + sitemapCount + ".xml.gz")));
	    		
	    		out = new PrintStream(gzipOut);
	    		numWritten = 0;
	    		sitemapCount++;

	    		// Leading boilerplate
	    		out.println(leadingBoilerplate);
		    }
		    
		    c = community[i];
		    out.print("<url><loc>");
	        out.print(URL_STEM);
	        out.print(c.getHandle());
	        out.println("</loc></url>");
	        numWritten++;

    	    /*
     		* Flush Context object cache every so often to prevent runaway
     		* memory use. We force a garbage collection which may be
     		* expensive but this tool should a) be only run once every day
     		* or so and b) run in a separate, lower-priority JVM to
     		* external online services.
     		*/
    		if (numWritten % 100 == 0)
    		{
        		context.removeAllCached();
        		System.gc(); // Force clearout
    		}
    		
    		if (i == (community.length - 1))
    		{
    			// Trailing boilerplate
    			out.println(trailingBoilerplate);
    			out.flush();
    			out.close();
    		}
    	}
    	
    	return (sitemapCount - sitemapOriginal);
	}    
    
    public int generateCollectionSitemaps(Context context, File outputDir, int sitemapCount)
    throws SQLException, IOException
    {
    	Collection collection[] = Collection.findAll(context);
    	Collection c;
    	int numWritten = 0;
    	int sitemapOriginal = sitemapCount;
    	sitemapCount++;
    	
    	// Create the GZIPped output
		CountingGZIPOutputStream gzipOut = new CountingGZIPOutputStream(
				      new FileOutputStream(new File(outputDir, "google-sitemap"
                      + sitemapCount + ".xml.gz")));
		
		// For convenience
		PrintStream out = new PrintStream(gzipOut);

		// Leading boilerplate
		out.println(leadingBoilerplate);

		for (int i = 0; i < collection.length; i++)
    	{
    		// Write communities until either there are no more to write, or
    		// we reach the limit of a single Sitemap's size
		    
		    if ((gzipOut.getBytesWritten() >= SITEMAP_FILESIZE_LIMIT) ||
		        (numWritten >= SITEMAP_URL_LIMIT))
		    {
		    	// Trailing boilerplate
	    		out.println("trailingBoilerplate");
	    		out.flush();
	    		out.close();
	    		
	    		// Start writing to a new file
	    		gzipOut = new CountingGZIPOutputStream(
  				      new FileOutputStream(new File(outputDir, "sitemap"
                      + sitemapCount + ".xml.gz")));
	    		
	    		out = new PrintStream(gzipOut);
	    		numWritten = 0;
	    		sitemapCount++;

	    		// Leading boilerplate
	    		out.println(leadingBoilerplate);
		    }
		    
		    c = collection[i];
		    out.print("<url><loc>");
	        out.print(URL_STEM);
	        out.print(c.getHandle());
	        out.println("</loc></url>");
	        numWritten++;

    	    /*
     		* Flush Context object cache every so often to prevent runaway
     		* memory use. We force a garbage collection which may be
     		* expensive but this tool should a) be only run once every day
     		* or so and b) run in a separate, lower-priority JVM to
     		* external online services.
     		*/
    		if (numWritten % 100 == 0)
    		{
        		context.removeAllCached();
        		System.gc(); // Force clearout
    		}
    		
    		if (i == (collection.length - 1))
    		{
    			// Trailing boilerplate
    			out.println(trailingBoilerplate);
    			out.flush();
    			out.close();
    		}
    	}

		return (sitemapCount - sitemapOriginal);
	}
    
    /**
     * Generate the index file that links to each of the sitemaps.
     * 
     * @param count
     * @param outputDir
     * @throws FileNotFoundException 
     */
    public void generateIndexFile(int count, File outputDir) throws FileNotFoundException
    {
    	// Open the output file
    	PrintStream out = new PrintStream(
    			          new FileOutputStream(
    			          new File(outputDir, "google-sitemap_index.xml")));
    	
    	// Leading boilerplate 
    	out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<sitemapindex xmlns=\"http://www.google.com/schemas/sitemap/0.84\">");
        
        // Each sitemp
        for (int i = 1; i <= count; i++)
        {
        	out.println("<sitemap>");
        	out.println("<loc>" + SITEMAP_STEM + i + "</loc>");
        	out.println("<lastmod>" + w3dtfFormat.format(new Date()) + "</lastmod>");
        	out.println("</sitemap>");
        }
        
        // Trailing boilerplate
        out.println("</sitemapindex>");
        out.flush();
        out.close();
    }

    /**
     * Class that counts the number of <em>uncompressed</em> bytes that go
     * into the GZIP output stream.
     */
    private class CountingGZIPOutputStream extends GZIPOutputStream
    {
        private int byteCount;

        CountingGZIPOutputStream(OutputStream out) throws IOException
        {
            super(out);
            this.byteCount = 0;
        }

        public void write(byte[] buf, int off, int len) throws IOException
        {
            super.write(buf, off, len);
            byteCount += len;
        }

        public void write(int b) throws IOException
        {
            super.write(b);
            byteCount++;
        }

        public void write(byte[] b) throws IOException
        {
            super.write(b);
            byteCount++;
        }

        public int getBytesWritten()
        {
            return this.byteCount;
        }
    }

    /**
     * The main method to call to generate the sitemaps.
     * 
     * @throws Exception
     */
    public static void main(String argv[]) throws Exception
    {
    	// Check the destination exists ok
    	File f = new File(output);
    	if ((!f.exists()) ||  (!f.isDirectory()))
    	{
    		System.out.println("Creating output directory: " +
    				           f.getAbsolutePath());
    		f.mkdir();
    	}
    	
    	// Check that the temp build directory exists
    	File fBuild = new File(output + "/build");
    	if ((!fBuild.exists()) ||  (!fBuild.isDirectory()))
    	{
    		System.out.println("Creating build directory: " + 
    				           fBuild.getAbsolutePath());
    		fBuild.mkdir();
    	}
    	
    	// Setup the environment
        Context context = new Context();
        GoogleSitemapGenerator g = new GoogleSitemapGenerator();
        
        // Write the item sitemaps
        int sitemapsWritten = g.generateItemSitemaps(context, fBuild);
        System.out.println(sitemapsWritten + " item sitemaps written");
        
         // Write the community sitemaps
        int communitySitemapsWritten = g.generateCommunitySitemaps(context, fBuild, sitemapsWritten);
        sitemapsWritten += communitySitemapsWritten;
        System.out.println(communitySitemapsWritten + " community sitemaps written");
                
        // Write the collection sitemaps
        int collectionSitemapsWritten = g.generateCollectionSitemaps(context, fBuild, sitemapsWritten);
        sitemapsWritten += collectionSitemapsWritten;
        System.out.println(collectionSitemapsWritten + " collection sitemaps written");
        
        // Write the index file
        g.generateIndexFile(sitemapsWritten, fBuild);
        System.out.println("1 sitemap index written");
        
        // Unlock any locked files where java is being silly
        System.gc();
                
        // Delete the old sitemaps
        FileFilter filter = new FileFilter()
        {
        	public boolean accept(File f)
    	    {
    	        if (f.isDirectory()) return false;
    	        return f.getName().startsWith("google-sitemap");
    	    }
        };
        File[] toRemove = f.listFiles(filter);
        File temp;
        for (int i = 0; i < toRemove.length; i++)
        {
        	temp = toRemove[i];
        	System.out.println("Deleting old sitemap: " + temp.getAbsolutePath());
        	temp.delete();
        }
        toRemove = null;
        
        // Move the new sitemaps
        File[] toMove = fBuild.listFiles(filter);
        File newLoc;
        for (int i = 0; i < toMove.length; i++)
        {
        	temp = toMove[i];
        	newLoc = new File(f, temp.getName());
        	System.out.println("Moving new sitemap: " + temp.getAbsolutePath() +
        	                 " to " + newLoc.getAbsolutePath());
        	temp.renameTo(newLoc);
        }
        toMove = null;
        
        // Closedown the environment
        context.abort();
        
        // Register the sitempas with Google
        if ((ConfigurationManager.getProperty("http.proxy.host") != null) &&
            (ConfigurationManager.getProperty("http.proxy.port") != null))
        {
        	System.getProperties().put("proxySet", 
        			                   "true");
        	System.getProperties().put("proxyHost", 
        			                   ConfigurationManager.getProperty("http.proxy.host"));
        	System.getProperties().put("proxyPort", 
        			                   ConfigurationManager.getProperty("http.proxy.port"));
        }
        
        // 'Ping' google with the update
        URL url = new URL("http://www.google.com/webmasters/sitemaps/ping?sitemap=" +
        		          URLEncoder.encode(SITEMAP_STEM + "0"));
        System.out.println("Updating Google:\n" + url.toString());
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.connect();

    	BufferedReader in = new BufferedReader(
    				new InputStreamReader(
    				url.openStream()));
    	
    	String inputLine;
    	StringBuffer resp = new StringBuffer();
    	while ((inputLine = in.readLine()) != null)
    	{
    	    resp.append(inputLine).append("\n");
    	}
    	in.close();
    	
    	if (connection.getResponseCode() == 200)
    	{
    		System.out.println("Successfull");
    	}
    	else
    	{
    		System.out.println("Failed:\n\n" + resp.toString());
    	}
    }
}
