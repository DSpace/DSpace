package org.dspace.app.mediafilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.Map;
import java.util.HashMap;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;


// possible args - collection, item, mediafilter

public class MediaFilterManager
{
    private static Map filterNames = new HashMap();
    private static Map filterCache = new HashMap();
    private static boolean isForce = false;

    public static void main(String [] argv)
        throws Exception
    {
        // only valid argument is FORCE
        if(argv.length > 0)
        {
            if(argv[0].equals("FORCE"))
            {
                isForce = true;
            }
            else
            {
                System.out.println("args: FORCE\nforces filtering of bitstreams");
                return;
            }
        }
        
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
        String line = null;
        
        while( ( line = is.readLine() ) != null )
        {
            // skip any lines beginning with #
            if(line.indexOf("#") == 0) continue;
            
            // look for the last space - it should be between Format Name and className
            int lastSpace = line.lastIndexOf(" ");
            
            if(lastSpace != -1)
            {
                String myFormat = line.substring(0, lastSpace);
                String myClass  = line.substring(lastSpace+1, line.length());
                
                System.out.println("Format: '"+myFormat+"' Filtering Class: '"+myClass+"'");
                
                // now convert format name to a format ID (int) for the hash key
                int formatID = BitstreamFormat.findByShortDescription(c, myFormat).getID();
                
                filterNames.put(new Integer(formatID), myClass);
            }
        }
        is.close();

        // now apply the filters
        applyFiltersAllItems(c);
        
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
                myFilter.processBitstream(c, myItem, myBitstream, isForce);
            }
            catch(Exception e)
            {
                System.out.println("ERROR filtering, skipping bitstream #" + myBitstream.getID() + " " + e);
            }
        }
    }
}
