package org.dspace.app.sitemap;


import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Utility for generating HTML sitemaps to improve search engine coverage of the 
 * DSpace site and limit the server load caused by indexing.
 * <p>
 * This is a separate tool that creates the Sitemaps as static files. This means
 * they can be created at low priority as a scheduled job (perhaps timed for
 * when server load tends to be low) and can be read by search engines very
 * cheaply.
 * 
 * @author Stuart Lewis
 * @version $Revision$
 */
public class HTMLSitemapGenerator
{
    /** Max number of URLs in a single Sitemap */
    private final static int SITEMAP_URL_LIMIT = 1000;

    /** The stem of all URLs */
    private final String URL_STEM = ConfigurationManager.getProperty("dspace.url") + 
                                    "/handle/";
    
    /** The stem for sitemaps */
    private final static String SITEMAP_STEM = ConfigurationManager.getProperty("dspace.url") +
                                               "/sitemap?html=";

    /** The destination of the output files */
    private static String output = ConfigurationManager.getProperty("dspace.dir") +
                                   "/sitemaps/";
    
    /** The sitemap leading boilerplate */
	private final String leadingBoilerplate = 
		               "<html><head>" +
		               "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">" +
		               "<title>Sitemap</title>" +
		               "</head>\n" +
                       "<body><ol>";
	
	/** The sitemap trailing boilerplate */
	private final String trailingBoilerplate = "</ol></body></html>";

    /**
     * Create Sitemap files for all of the items in the DSpace instance.
     *
     * The items are created first, and the collections, and communities. An index file
     * then links to each of the other sitemaps
     * 
     * <pre>
     *  sitemap1.html
     *  sitemap2.html
     *  sitemap3.html
     * </pre>
     * 
     * @param context
     *            context object to use
     * @param outputDir
     *            directory to write Sitemaps to
     * @return number of Sitemaps created
     * @throws SQLException
     *             if a database error occurs
     * @throws IOException
     *             if some IO error occurs
     */
    public int generateItemSitemaps(Context context, File outputDir)
                                               throws SQLException, IOException
    {
    	int sitemapCount = 0;

    	ItemIterator allItems = Item.findAll(context);

    	while (allItems.hasNext())
    	{
    		// Increment here, so we'll start with sitemap1.html
    		sitemapCount++;

    		// Create the output
    		FileOutputStream fos = new FileOutputStream(outputDir + "/html-sitemap" 
    				                                    + sitemapCount + ".html");
    		OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
    		PrintWriter out = new PrintWriter(osr);
    		DCValue[] titleValue = null;
    		
    		// Leading boilerplate
    		out.println(leadingBoilerplate);

    		// Write items until either there are no more to write, or
    		// we reach the limit of a single Sitemap's size
		    for (int numWritten = 0; allItems.hasNext()
		         && numWritten < SITEMAP_URL_LIMIT; numWritten++)
		    {
		        Item item = allItems.next();
		        out.print("<li><a href=" + URL_STEM + item.getHandle() + ">");
		        titleValue = item.getDC("title", null, Item.ANY);
		        if (titleValue.length > 0)
		        {
		        	// Print out the title of the item as the text link
		        	out.print(titleValue[0].value);
		        }
		        else
		        {
		        	// No title, so use the URL isnstead
		        	out.print(URL_STEM + item.getHandle());
		        }
		        out.println("</a></li>");

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
    	
    	// Create the output
		FileOutputStream fos = new FileOutputStream(outputDir + "/html-sitemap" 
				                                    + sitemapCount + ".html");
		OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
		PrintWriter out = new PrintWriter(osr);
		
		// Leading boilerplate
		out.println(leadingBoilerplate);
		
    	for (int i = 0; i < community.length; i++)
    	{
    		// Write communities until either there are no more to write, or
    		// we reach the limit of a single Sitemap's size
		    if (numWritten >= SITEMAP_URL_LIMIT)
		    {
		    	// Trailing boilerplate
		    	out.println(trailingBoilerplate);
	    		out.flush();
	    		out.close();
	    		
	    		// Start writing to a new file
	    		fos = new FileOutputStream(outputDir + "/html-sitemap" + 
	    				                   sitemapCount + ".html");
	    		osr = new OutputStreamWriter(fos, "UTF-8");
	    		out = new PrintWriter(osr);
	    		numWritten = 0;
	    		sitemapCount++;

	    		// Leading boilerplate
	    		out.println(leadingBoilerplate);
	    	}
		    
		    c = community[i];
		    out.print("<li><a href=" + URL_STEM + c.getHandle() + ">");
		    out.print(c.getMetadata("name"));
	        out.println("</a></li>");
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
    	
    	// Create the file output
		FileOutputStream fos = new FileOutputStream(outputDir + "/html-sitemap" + 
                                                    sitemapCount + ".html");
		OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
		PrintWriter out = new PrintWriter(osr);
		
		// Leading boilerplate
		out.println(leadingBoilerplate);
    	
    	for (int i = 0; i < collection.length; i++)
    	{
    		// Write communities until either there are no more to write, or
    		// we reach the limit of a single Sitemap's size
		    
		    if (numWritten >= SITEMAP_URL_LIMIT)
		    {
		    	// Trailing boilerplate
	    		out.println("trailingBoilerplate");
	    		out.flush();
	    		out.close();
	    		
	    		// Start writing to a new file
	    		fos = new FileOutputStream(outputDir + "/html-sitemap" + sitemapCount + ".html");
	    		osr = new OutputStreamWriter(fos, "UTF-8");
	    		out = new PrintWriter(osr);
	    		numWritten = 0;
	    		sitemapCount++;

	    		// Leading boilerplate
	    		out.println(leadingBoilerplate);
		    }
		    
		    c = collection[i];
		    out.print("<li><a href=" + URL_STEM + c.getHandle() + ">");
		    out.print(c.getMetadata("name"));
	        out.println("</a></li>");
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
     * @throws UnsupportedEncodingException 
     */
    public void generateIndexFile(int count, File outputDir) 
                         throws FileNotFoundException, UnsupportedEncodingException
    {
    	// Open the output file
    	FileOutputStream fos = new FileOutputStream(outputDir + "/html-sitemap_index.html");
		OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
		PrintWriter out = new PrintWriter(osr);
    	
    	// Leading boilerplate 
    	out.println(leadingBoilerplate);
        
        // Each sitemp
        for (int i = 1; i <= count; i++)
        {
        	out.print("<li><a href=" + SITEMAP_STEM + i + ">");
        	out.print("Sitemap " + i);
        	out.println("</a></li>");
        }
        
        // Trailing boilerplate
        out.println(trailingBoilerplate);
        out.flush();
        out.close();
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
        HTMLSitemapGenerator html = new HTMLSitemapGenerator();
        
        // Write the item sitemaps
        int sitemapsWritten = html.generateItemSitemaps(context, fBuild);
        System.out.println(sitemapsWritten + " item sitemaps written");
        
         // Write the community sitemaps
        int communitySitemapsWritten = html.generateCommunitySitemaps(context, fBuild, sitemapsWritten);
        sitemapsWritten += communitySitemapsWritten;
        System.out.println(communitySitemapsWritten + " community sitemaps written");
                
        // Write the collection sitemaps
        int collectionSitemapsWritten = html.generateCollectionSitemaps(context, fBuild, sitemapsWritten);
        sitemapsWritten += collectionSitemapsWritten;
        System.out.println(collectionSitemapsWritten + " collection sitemaps written");
        
        // Write the index file
        html.generateIndexFile(sitemapsWritten, fBuild);
        System.out.println("1 sitemap index written");
        
        // Unlock any locked files where java is being silly
        System.gc();
        
        // Delete the old sitemaps
        FileFilter filter = new FileFilter()
        {
        	public boolean accept(File f)
    	    {
    	        if (f.isDirectory()) return false;
    	        return f.getName().startsWith("html-sitemap");
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
    }
}
