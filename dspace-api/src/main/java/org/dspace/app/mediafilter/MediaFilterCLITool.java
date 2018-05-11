/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import org.apache.commons.cli.*;
import org.dspace.app.mediafilter.factory.MediaFilterServiceFactory;
import org.dspace.app.mediafilter.service.MediaFilterService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;

import java.util.*;
import org.apache.commons.lang.ArrayUtils;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * MediaFilterManager is the class that invokes the media/format filters over the
 * repository's content. A few command line flags affect the operation of the
 * MFM: -v verbose outputs all extracted text to STDOUT; -f force forces all
 * bitstreams to be processed, even if they have been before; -n noindex does not
 * recreate index after processing bitstreams; -i [identifier] limits processing
 * scope to a community, collection or item; and -m [max] limits processing to a
 * maximum number of items.
 */
public class MediaFilterCLITool {

    //key (in dspace.cfg) which lists all enabled filters by name
    private static final String MEDIA_FILTER_PLUGINS_KEY = "filter.plugins";

    //prefix (in dspace.cfg) for all filter properties
    private static final String FILTER_PREFIX = "filter";

    //suffix (in dspace.cfg) for input formats supported by each filter
    private static final String INPUT_FORMATS_SUFFIX = "inputFormats";

    public static void main(String[] argv) throws Exception
    {
        // set headless for non-gui workstations
        System.setProperty("java.awt.headless", "true");

        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        int status = 0;

        Options options = new Options();

        options.addOption("v", "verbose", false,
                "print all extracted text and other details to STDOUT");
        options.addOption("q", "quiet", false,
                "do not print anything except in the event of errors.");
        options.addOption("f", "force", false,
                "force all bitstreams to be processed");
        options.addOption("i", "identifier", true,
                "ONLY process bitstreams belonging to identifier");
        options.addOption("m", "maximum", true,
                "process no more than maximum items");
        options.addOption("h", "help", false, "help");

        //create a "plugin" option (to specify specific MediaFilter plugins to run)
        OptionBuilder.withLongOpt("plugins");
        OptionBuilder.withValueSeparator(',');
        OptionBuilder.withDescription(
                "ONLY run the specified Media Filter plugin(s)\n" +
                        "listed from '" + MEDIA_FILTER_PLUGINS_KEY + "' in dspace.cfg.\n" +
                        "Separate multiple with a comma (,)\n" +
                        "(e.g. MediaFilterManager -p \n\"Word Text Extractor\",\"PDF Text Extractor\")");
        Option pluginOption = OptionBuilder.create('p');
        pluginOption.setArgs(Option.UNLIMITED_VALUES); //unlimited number of args
        options.addOption(pluginOption);

        //create a "skip" option (to specify communities/collections/items to skip)
        OptionBuilder.withLongOpt("skip");
        OptionBuilder.withValueSeparator(',');
        OptionBuilder.withDescription(
                "SKIP the bitstreams belonging to identifier\n" +
                        "Separate multiple identifiers with a comma (,)\n" +
                        "(e.g. MediaFilterManager -s \n 123456789/34,123456789/323)");
        Option skipOption = OptionBuilder.create('s');
        skipOption.setArgs(Option.UNLIMITED_VALUES); //unlimited number of args
        options.addOption(skipOption);

        boolean isVerbose = false;
        boolean isQuiet = false;
        boolean isForce = false; // default to not forced
        String identifier = null; // object scope limiter
        int max2Process = Integer.MAX_VALUE;
        Map<String, List<String>> filterFormats = new HashMap<>();

        CommandLine line = null;
        try
        {
            line = parser.parse(options, argv);
        }
        catch(MissingArgumentException e)
        {
            System.out.println("ERROR: " + e.getMessage());
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("MediaFilterManager\n", options);
            System.exit(1);
        }

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("MediaFilterManager\n", options);

            System.exit(0);
        }

        if (line.hasOption('v'))
        {
            isVerbose = true;
        }

        isQuiet = line.hasOption('q');

        if (line.hasOption('f'))
        {
            isForce = true;
        }

        if (line.hasOption('i'))
        {
            identifier = line.getOptionValue('i');
        }

        if (line.hasOption('m'))
        {
            max2Process = Integer.parseInt(line.getOptionValue('m'));
            if (max2Process <= 1)
            {
                System.out.println("Invalid maximum value '" +
                        line.getOptionValue('m') + "' - ignoring");
                max2Process = Integer.MAX_VALUE;
            }
        }

        String filterNames[] = null;
        if(line.hasOption('p'))
        {
            //specified which media filter plugins we are using
            filterNames = line.getOptionValues('p');

            if(filterNames==null || filterNames.length==0)
            {   //display error, since no plugins specified
                System.err.println("\nERROR: -p (-plugin) option requires at least one plugin to be specified.\n" +
                        "(e.g. MediaFilterManager -p \"Word Text Extractor\",\"PDF Text Extractor\")\n");
                HelpFormatter myhelp = new HelpFormatter();
                myhelp.printHelp("MediaFilterManager\n", options);
                System.exit(1);
            }
        }
        else
        {
            //retrieve list of all enabled media filter plugins!
            filterNames = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty(MEDIA_FILTER_PLUGINS_KEY);
        }

        MediaFilterService mediaFilterService = MediaFilterServiceFactory.getInstance().getMediaFilterService();
        mediaFilterService.setForce(isForce);
        mediaFilterService.setQuiet(isQuiet);
        mediaFilterService.setVerbose(isVerbose);
        mediaFilterService.setMax2Process(max2Process);

        //initialize an array of our enabled filters
        List<FormatFilter> filterList = new ArrayList<FormatFilter>();

        //set up each filter
        for(int i=0; i< filterNames.length; i++)
        {
            //get filter of this name & add to list of filters
            FormatFilter filter = (FormatFilter) CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(FormatFilter.class, filterNames[i]);
            if(filter==null)
            {
                System.err.println("\nERROR: Unknown MediaFilter specified (either from command-line or in dspace.cfg): '" + filterNames[i] + "'");
                System.exit(1);
            }
            else
            {
                filterList.add(filter);

                String filterClassName = filter.getClass().getName();

                String pluginName = null;

                //If this filter is a SelfNamedPlugin,
                //then the input formats it accepts may differ for
                //each "named" plugin that it defines.
                //So, we have to look for every key that fits the
                //following format: filter.<class-name>.<plugin-name>.inputFormats
                if( SelfNamedPlugin.class.isAssignableFrom(filter.getClass()) )
                {
                    //Get the plugin instance name for this class
                    pluginName = ((SelfNamedPlugin) filter).getPluginInstanceName();
                }


                //Retrieve our list of supported formats from dspace.cfg
                //For SelfNamedPlugins, format of key is:
                //  filter.<class-name>.<plugin-name>.inputFormats
                //For other MediaFilters, format of key is:
                //  filter.<class-name>.inputFormats
                String[] formats = 
                        DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty(
                        FILTER_PREFIX + "." + filterClassName +
                        (pluginName!=null ? "." + pluginName : "") +
                        "." + INPUT_FORMATS_SUFFIX);

                //add to internal map of filters to supported formats
                if (ArrayUtils.isNotEmpty(formats))
                {
                    //For SelfNamedPlugins, map key is:
                    //  <class-name><separator><plugin-name>
                    //For other MediaFilters, map key is just:
                    //  <class-name>
                    filterFormats.put(filterClassName +
                                    (pluginName!=null ? MediaFilterService.FILTER_PLUGIN_SEPARATOR + pluginName : ""),
                            Arrays.asList(formats));
                }
            }//end if filter!=null
        }//end for

        //If verbose, print out loaded mediafilter info
        if(isVerbose)
        {
            System.out.println("The following MediaFilters are enabled: ");
            Iterator<String> i = filterFormats.keySet().iterator();
            while(i.hasNext())
            {
                String filterName = i.next();
                System.out.println("Full Filter Name: " + filterName);
                String pluginName = null;
                if(filterName.contains(MediaFilterService.FILTER_PLUGIN_SEPARATOR))
                {
                    String[] fields = filterName.split(MediaFilterService.FILTER_PLUGIN_SEPARATOR);
                    filterName=fields[0];
                    pluginName=fields[1];
                }

                System.out.println(filterName +
                        (pluginName!=null? " (Plugin: " + pluginName + ")": ""));
            }
        }

        mediaFilterService.setFilterFormats(filterFormats);
        //store our filter list into an internal array
        mediaFilterService.setFilterClasses(filterList);


        //Retrieve list of identifiers to skip (if any)
        String skipIds[] = null;
        if(line.hasOption('s'))
        {
            //specified which identifiers to skip when processing
            skipIds = line.getOptionValues('s');

            if(skipIds==null || skipIds.length==0)
            {   //display error, since no identifiers specified to skip
                System.err.println("\nERROR: -s (-skip) option requires at least one identifier to SKIP.\n" +
                        "Make sure to separate multiple identifiers with a comma!\n" +
                        "(e.g. MediaFilterManager -s 123456789/34,123456789/323)\n");
                HelpFormatter myhelp = new HelpFormatter();
                myhelp.printHelp("MediaFilterManager\n", options);
                System.exit(0);
            }

            //save to a global skip list
            mediaFilterService.setSkipList(Arrays.asList(skipIds));
        }

        Context c = null;

        try
        {
            c = new Context();

            // have to be super-user to do the filtering
            c.turnOffAuthorisationSystem();

            // now apply the filters
            if (identifier == null)
            {
                mediaFilterService.applyFiltersAllItems(c);
            }
            else  // restrict application scope to identifier
            {
                DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(c, identifier);
                if (dso == null)
                {
                    throw new IllegalArgumentException("Cannot resolve "
                            + identifier + " to a DSpace object");
                }

                switch (dso.getType())
                {
                    case Constants.COMMUNITY:
                        mediaFilterService.applyFiltersCommunity(c, (Community) dso);
                        break;
                    case Constants.COLLECTION:
                        mediaFilterService.applyFiltersCollection(c, (Collection) dso);
                        break;
                    case Constants.ITEM:
                        mediaFilterService.applyFiltersItem(c, (Item) dso);
                        break;
                }
            }

            c.complete();
            c = null;
        }
        catch (Exception e)
        {
            status = 1;
        }
        finally
        {
            if (c != null)
            {
                c.abort();
            }
        }
        System.exit(status);
    }
}
