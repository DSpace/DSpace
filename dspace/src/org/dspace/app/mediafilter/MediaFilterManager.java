package org.dspace.app.mediafilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.commons.cli.Options; 
import org.apache.commons.cli.CommandLineParser; 
import org.apache.commons.cli.CommandLine; 
import org.apache.commons.cli.HelpFormatter; 
import org.apache.commons.cli.PosixParser; 

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.search.DSIndexer;


/**
 *  MediaFilterManager is the class that invokes the media filters over the repository's content.
 *   a few command line flags affect the operation of the MFM:
 *    -v verbose outputs all extracted text to SDTDOUT
 *    -f force forces all bitstreams to be processed, even if they have been before
 *    -n noindex does not recreate index after processing bitstreams
 *
 */

public class MediaFilterManager
{
    private static Map filterNames = new HashMap();
    private static Map filterCache = new HashMap();
    
    public static boolean createIndex = true; // default to creating index
    public static boolean isVerbose = false; // default to not verbose
    public static boolean isForce   = false; // default to not forced

    public static void main(String [] argv)
        throws Exception
    {
        // set headless for non-gui workstations
        System.setProperty("java.awt.headless", "true");

        // create an options object and populate it
        CommandLineParser parser = new PosixParser(); 

        Options options = new Options();

        options.addOption( "v", "verbose", false, "print all extracted text and other details to STDOUT");
        options.addOption( "f", "force",   false, "force all bitstreams to be processed");
        options.addOption( "n", "noindex", false, "do NOT re-create search index after filtering bitstreams");
        options.addOption( "h", "help",    false, "help");

        CommandLine line = parser.parse( options, argv );

        if( line.hasOption('h') )
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp( "MediaFilter\n", options );

            System.exit(0);
        }
        
        if( line.hasOption( 'v' ) ) { isVerbose   = true;  } 
        if( line.hasOption( 'n' ) ) { createIndex = false; } 
        if( line.hasOption( 'f' ) ) { isForce     = true;  }
        
        // get path to config file
        String myPath = ConfigurationManager.getProperty("dspace.dir") + File.separator +
            "config" + File.separator + "mediafilter.cfg";
        
        // format name, classname
        System.out.println("Using configuration in " + myPath);

        Context c = new Context();

        // have to be super-user to do the filtering
        c.setIgnoreAuthorization(true);

        // read in the mediafilter.cfg file, store in HashMap
        BufferedReader is = new BufferedReader( new FileReader( myPath ) );
        String myLine = null;
        
        while( ( myLine = is.readLine() ) != null )
        {
            // skip any lines beginning with #
            if(myLine.indexOf("#") == 0) continue;

            // no comment, so try and parse line
            StringTokenizer st = new StringTokenizer(myLine);
            
            // has to have at least 2 tokens
            if(st.countTokens() >= 2)
            {
                String [] tokens = new String[st.countTokens()];
                
                // grab all tokens and stuff in array
                for(int i=0; i<tokens.length; i++)
                {
                    tokens[i] = st.nextToken();
                }
                
                // class is the last token
                String myClass = tokens[tokens.length-1];
                String myFormat = tokens[0];
                
                // everything else is the format
                for(int i=1; i<(tokens.length-1); i++)
                {
                    myFormat = myFormat + " " + tokens[i];
                }
                
                System.out.println("Format: '"+myFormat+"' Filtering Class: '"+myClass+"'");
                
                // now convert format name to a format ID (int) for the hash key
                int formatID = BitstreamFormat.findByShortDescription(c, myFormat).getID();
                
                filterNames.put(new Integer(formatID), myClass);
            }
        }
        is.close();

        // now apply the filters
        applyFiltersAllItems(c);
       
        // create search index?
        if( createIndex )
        {
            System.out.println("Creating search index:");
            DSIndexer.createIndex(c);
        }        


        c.complete();
    }
    


    public static void applyFiltersAllItems(Context c)
        throws Exception
    {

        ItemIterator i = Item.findAll(c);
        
        while(i.hasNext())
        {
            Item myItem = i.next();
            
            filterItem(c, myItem);
        }
    }
    
    
    /**
     * iterate through the item's bitstreams in the ORIGINAL
     *  bundle, applying filters if possible
     */
    public static void filterItem(Context c, Item myItem)
        throws Exception
    {
        // get 'original' bundles
        Bundle [] myBundles = myItem.getBundles();
            
        for(int i = 0; i<myBundles.length; i++)
        {
            // could have multiple 'ORIGINAL' bundles (hmm, probably not)
            if(myBundles[i].getName().equals("ORIGINAL"))
            {
                // now look at all of the bitstreams
                Bitstream [] myBitstreams = myBundles[i].getBitstreams();
                    
                for(int k = 0; k<myBitstreams.length; k++)
                {
                    filterBitstream(c, myItem, myBitstreams[k]);
                }
            }
        }
    }
    
    
    /**
     * Attempt to filter a bitstream
     *
     * An exception will be thrown if the media filter class cannot be instantiated,
     *  exceptions from filtering will be logged to STDOUT and swallowed.
     */
    public static void filterBitstream(Context c, Item myItem, Bitstream myBitstream)
        throws Exception
    {
        // do we have a filter for that format?
        Integer formatID = new Integer(myBitstream.getFormat().getID());
                    
        if(filterNames.containsKey(formatID))
        {
            // now, have we instantiated the class already?
            if(!filterCache.containsKey(formatID))
            {
                // given a class name, load the class
                Class f = Class.forName((String)filterNames.get(formatID));
                MediaFilter myFilter = (MediaFilter)f.newInstance();
                            
                filterCache.put(formatID, myFilter);
            }

            // now get the filter and use it
            MediaFilter myFilter = (MediaFilter)filterCache.get(formatID);
                       
            try
            {
                myFilter.processBitstream(c, myItem, myBitstream);
                myItem.update(); // Make sure new bitstream has a sequence number
            }
            catch(Exception e)
            {
                System.out.println("ERROR filtering, skipping bitstream #" + myBitstream.getID() + " " + e);
                e.printStackTrace();
            }
        }
    }
}
