package org.dspace.app.mediafilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.search.DSIndexer;
import org.dspace.handle.HandleManager;

/**
 * MediaFilterManager is the class that invokes the media filters over the
 * repository's content. a few command line flags affect the operation of the
 * MFM: -v verbose outputs all extracted text to SDTDOUT -f force forces all
 * bitstreams to be processed, even if they have been before -n noindex does not
 * recreate index after processing bitstreams
 *  
 */
public class MediaFilterManager
{
  /**
   * <pre>
   * Revision History:
   *   
   *   2006/06/26: Ben
   *     - more specific error messaging
   *     - remove ITEM and BITSTREAM messages
   *
   *   2006/01/12: Ben
   *     - index each updated item individually
   *
   *   2005/09/23: Ben
   *     - add an option to process a single item.
   */

    private static Map filterNames = new HashMap();

    private static Map filterCache = new HashMap();

    public static boolean createIndex = true; // default to creating index

    public static boolean isVerbose = false; // default to not verbose

    public static boolean isForce = false; // default to not forced

    public static boolean isSingle = false; // default to all items
    public static String itemid = null;

    public static void main(String[] argv) throws Exception
    {
        // set headless for non-gui workstations
        System.setProperty("java.awt.headless", "true");

        // set tmpdir
        System.setProperty("java.io.tmpdir", "/drum2/tmp");

        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("v", "verbose", false,
                "print all extracted text and other details to STDOUT");
        options.addOption("f", "force", false,
                "force all bitstreams to be processed");
        options.addOption("n", "noindex", false,
                "do NOT re-create search index after filtering bitstreams");
        options.addOption("h", "help", false, "help");
        options.addOption("i", "item", true, "process a single item (handle or id)");

        CommandLine line = parser.parse(options, argv);

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("MediaFilter\n", options);

            System.exit(0);
        }

        if (line.hasOption('v'))
        {
            isVerbose = true;
        }

        if (line.hasOption('n'))
        {
            createIndex = false;
        }

        if (line.hasOption('f'))
        {
            isForce = true;
        }

        if (line.hasOption('i')) {
            isSingle = true;
            itemid = line.getOptionValue('i');
        }

        // get path to config file
        String myPath = ConfigurationManager.getProperty("dspace.dir")
                + File.separator + "config" + File.separator
                + "mediafilter.cfg";

        // format name, classname
        System.out.println("Using configuration in " + myPath);

        Context c = null;

        try
        {
            c = new Context();

            // have to be super-user to do the filtering
            c.setIgnoreAuthorization(true);

            // read in the mediafilter.cfg file, store in HashMap
            BufferedReader is = new BufferedReader(new FileReader(myPath));
            String myLine = null;

            while ((myLine = is.readLine()) != null)
            {
                // skip any lines beginning with #
                if (myLine.indexOf("#") == 0)
                {
                    continue;
                }

                // no comment, so try and parse line
                StringTokenizer st = new StringTokenizer(myLine);

                // has to have at least 2 tokens
                if (st.countTokens() >= 2)
                {
                    String[] tokens = new String[st.countTokens()];

                    // grab all tokens and stuff in array
                    for (int i = 0; i < tokens.length; i++)
                    {
                        tokens[i] = st.nextToken();
                    }

                    // class is the last token
                    String myClass = tokens[tokens.length - 1];
                    String myFormat = tokens[0];

                    // everything else is the format
                    for (int i = 1; i < (tokens.length - 1); i++)
                    {
                        myFormat = myFormat + " " + tokens[i];
                    }

                    System.out.println("Format: '" + myFormat
                            + "' Filtering Class: '" + myClass + "'");

                    // now convert format name to a format ID (int) for the hash
                    // key
                    int formatID = BitstreamFormat.findByShortDescription(c,
                            myFormat).getID();

                    filterNames.put(new Integer(formatID), myClass);
                }
            }

            is.close();

            // now apply the filters
            if (isSingle) {
                applyFiltersSingleItem(c);
            } else {
                applyFiltersAllItems(c);
            }

            System.out.println("Completing DSpace transactions");
            c.complete();
            System.out.println("Completing done");
            c = null;
        }
		  catch (Throwable t) {
				System.out.println("Uncaught Error:");
				t.printStackTrace();
		  }
        finally
        {
            if (c != null)
            {
                System.out.println("Aborting DSpace transactions");
                c.abort();
                System.exit(1);
            }
            System.exit(0);
        }
    }

    public static void applyFiltersAllItems(Context c) throws Exception
    {
        ItemIterator i = Item.findAll(c);

        while (i.hasNext())
        {
            Item myItem = i.next();

            if (filterItem(c, myItem)) {

                // commit changes after each filtered item
                c.commit();

                // create search index?
                if (createIndex)
                {
                    System.out.println("indexing");
                    DSIndexer.reIndexContent(c, myItem);
                }
            }
        }
    }

    public static void applyFiltersSingleItem(Context c) throws Exception
    {
      Item myItem = null;

      // Try handle first
      Object o = HandleManager.resolveToObject(c, itemid);
      if (o != null) {
        if (o instanceof Item) {
          myItem = (Item)o;
        } else {
          throw new Exception("Error: '" + itemid + "' is a handle for an object which is not an Item");
        }
      }

      // Try item id next
      if (myItem == null) {
        try {
          int id = Integer.parseInt(itemid);

          myItem = Item.find(c, id);
        }
        catch (Exception e) {};
      }

      // Get anything?
      if (myItem == null) {
        throw new Exception("Error: could not find item for '" + itemid + "'");
      }

      // Filter the item
      if (filterItem(c, myItem)) {

          // commit changes
          c.commit();

          // create search index?
          if (createIndex)
          {
              System.out.println("indexing");
              DSIndexer.reIndexContent(c, myItem);
          }
      }

    }

    /**
     * iterate through the item's bitstreams in the ORIGINAL bundle, applying
     * filters if possible
     */
    public static boolean filterItem(Context c, Item myItem) throws Exception
    {
        boolean bUpdate = false;

        // get 'original' bundles
        Bundle[] myBundles = myItem.getBundles();

        for (int i = 0; i < myBundles.length; i++)
        {
            // could have multiple 'ORIGINAL' bundles (hmm, probably not)
            if ("ORIGINAL".equals(myBundles[i].getName()))
            {
                // now look at all of the bitstreams
                Bitstream[] myBitstreams = myBundles[i].getBitstreams();

                for (int k = 0; k < myBitstreams.length; k++)
                {
                    if (filterBitstream(c, myItem, myBitstreams[k])) {
                        bUpdate = true;
                    }
                }
            }
        }

        return bUpdate;
    }

    /**
     * Attempt to filter a bitstream
     * 
     * An exception will be thrown if the media filter class cannot be
     * instantiated, exceptions from filtering will be logged to STDOUT and
     * swallowed.
     */
    public static boolean filterBitstream(Context c, Item myItem,
            Bitstream myBitstream) throws Exception
    {
        boolean bUpdate = false;

        // do we have a filter for that format?
        Integer formatID = new Integer(myBitstream.getFormat().getID());

        if (filterNames.containsKey(formatID))
        {
            // now, have we instantiated the class already?
            if (!filterCache.containsKey(formatID))
            {
                // given a class name, load the class
                Class f = Class.forName((String) filterNames.get(formatID));
                MediaFilter myFilter = (MediaFilter) f.newInstance();

                filterCache.put(formatID, myFilter);
            }

            // now get the filter and use it
            MediaFilter myFilter = (MediaFilter) filterCache.get(formatID);

            try
            {
                // only update item if bitstream not skipped
                if (myFilter.processBitstream(c, myItem, myBitstream))
                {
                    myItem.update(); // Make sure new bitstream has a sequence
                                     // number
                    bUpdate = true;
                }
            }
            catch (Exception e)
            {
                System.out.println("ERROR filtering: "
                                   + "item " + myItem.getHandle()
                                   + " (" + myItem.getID() + ")"
                                   + ", bitstream " + myBitstream.getName()
                                   + " (" + myBitstream.getID() + ")"
                                   );
                e.printStackTrace();
            }
        }

        return bUpdate;
    }
}
