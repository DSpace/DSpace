/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
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
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * MediaFilterManager is the class that invokes the media/format filters over the
 * repository's content. A few command line flags affect the operation of the
 * MFM: -v verbose outputs all extracted text to STDOUT; -f force forces all
 * bitstreams to be processed, even if they have been before; -n noindex does not
 * recreate index after processing bitstreams; -i [identifier] limits processing
 * scope to a community, collection or item; and -m [max] limits processing to a
 * maximum number of items.
 */
public class MediaFilterScript extends DSpaceRunnable<MediaFilterScriptConfiguration> {

    //key (in dspace.cfg) which lists all enabled filters by name
    private static final String MEDIA_FILTER_PLUGINS_KEY = "filter.plugins";

    //prefix (in dspace.cfg) for all filter properties
    private static final String FILTER_PREFIX = "filter";

    //suffix (in dspace.cfg) for input formats supported by each filter
    private static final String INPUT_FORMATS_SUFFIX = "inputFormats";

    private boolean help;
    private boolean isVerbose = false;
    private boolean isQuiet = false;
    private boolean isForce = false; // default to not forced
    private String identifier = null; // object scope limiter
    private int max2Process = Integer.MAX_VALUE;
    private String[] filterNames;
    private String[] skipIds = null;
    private Map<String, List<String>> filterFormats = new HashMap<>();

    public MediaFilterScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
                           .getServiceByName("filter-media", MediaFilterScriptConfiguration.class);
    }

    public void setup() throws ParseException {

        // set headless for non-gui workstations
        System.setProperty("java.awt.headless", "true");


        help = commandLine.hasOption('h');

        if (commandLine.hasOption('v')) {
            isVerbose = true;
        }

        isQuiet = commandLine.hasOption('q');

        if (commandLine.hasOption('f')) {
            isForce = true;
        }

        if (commandLine.hasOption('i')) {
            identifier = commandLine.getOptionValue('i');
        }

        if (commandLine.hasOption('m')) {
            max2Process = Integer.parseInt(commandLine.getOptionValue('m'));
            if (max2Process <= 1) {
                handler.logWarning("Invalid maximum value '" +
                                           commandLine.getOptionValue('m') + "' - ignoring");
                max2Process = Integer.MAX_VALUE;
            }
        }

        if (commandLine.hasOption('p')) {
            //specified which media filter plugins we are using
            filterNames = commandLine.getOptionValues('p');
        } else {
            //retrieve list of all enabled media filter plugins!
            filterNames = DSpaceServicesFactory.getInstance().getConfigurationService()
                                               .getArrayProperty(MEDIA_FILTER_PLUGINS_KEY);
        }

        //save to a global skip list
        if (commandLine.hasOption('s')) {
            //specified which identifiers to skip when processing
            skipIds = commandLine.getOptionValues('s');
        }


    }

    public void internalRun() throws Exception {
        if (help) {
            printHelp();
            return;
        }

        MediaFilterService mediaFilterService = MediaFilterServiceFactory.getInstance().getMediaFilterService();
        mediaFilterService.setLogHandler(handler);
        mediaFilterService.setForce(isForce);
        mediaFilterService.setQuiet(isQuiet);
        mediaFilterService.setVerbose(isVerbose);
        mediaFilterService.setMax2Process(max2Process);

        //initialize an array of our enabled filters
        List<FormatFilter> filterList = new ArrayList<>();


        //set up each filter
        for (int i = 0; i < filterNames.length; i++) {
            //get filter of this name & add to list of filters
            FormatFilter filter = (FormatFilter) CoreServiceFactory.getInstance().getPluginService()
                                                                   .getNamedPlugin(FormatFilter.class, filterNames[i]);
            if (filter == null) {
                handler.handleException("ERROR: Unknown MediaFilter specified (either from command-line or in " +
                                                "dspace.cfg): '" + filterNames[i] + "'");
                handler.logError("ERROR: Unknown MediaFilter specified (either from command-line or in " +
                                         "dspace.cfg): '" + filterNames[i] + "'");
            } else {
                filterList.add(filter);

                String filterClassName = filter.getClass().getName();

                String pluginName = null;

                //If this filter is a SelfNamedPlugin,
                //then the input formats it accepts may differ for
                //each "named" plugin that it defines.
                //So, we have to look for every key that fits the
                //following format: filter.<class-name>.<plugin-name>.inputFormats
                if (SelfNamedPlugin.class.isAssignableFrom(filter.getClass())) {
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
                                        (pluginName != null ? "." + pluginName : "") +
                                        "." + INPUT_FORMATS_SUFFIX);

                //add to internal map of filters to supported formats
                if (ArrayUtils.isNotEmpty(formats)) {
                    //For SelfNamedPlugins, map key is:
                    //  <class-name><separator><plugin-name>
                    //For other MediaFilters, map key is just:
                    //  <class-name>
                    filterFormats.put(filterClassName +
                                              (pluginName != null ? MediaFilterService.FILTER_PLUGIN_SEPARATOR +
                                                      pluginName : ""),
                                      Arrays.asList(formats));
                }
            } //end if filter!=null
        } //end for

        //If verbose, print out loaded mediafilter info
        if (isVerbose) {
            handler.logInfo("The following MediaFilters are enabled: ");
            Iterator<String> i = filterFormats.keySet().iterator();
            while (i.hasNext()) {
                String filterName = i.next();
                handler.logInfo("Full Filter Name: " + filterName);
                String pluginName = null;
                if (filterName.contains(MediaFilterService.FILTER_PLUGIN_SEPARATOR)) {
                    String[] fields = filterName.split(MediaFilterService.FILTER_PLUGIN_SEPARATOR);
                    filterName = fields[0];
                    pluginName = fields[1];
                }

                handler.logInfo(filterName + (pluginName != null ? " (Plugin: " + pluginName + ")" : ""));
            }
        }

        mediaFilterService.setFilterFormats(filterFormats);
        //store our filter list into an internal array
        mediaFilterService.setFilterClasses(filterList);


        //Retrieve list of identifiers to skip (if any)

        if (skipIds != null && skipIds.length > 0) {
            //save to a global skip list
            mediaFilterService.setSkipList(Arrays.asList(skipIds));
        }

        Context c = null;

        try {
            c = new Context();

            // have to be super-user to do the filtering
            c.turnOffAuthorisationSystem();

            // now apply the filters
            if (identifier == null) {
                mediaFilterService.applyFiltersAllItems(c);
            } else {
                // restrict application scope to identifier
                DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(c, identifier);
                if (dso == null) {
                    throw new IllegalArgumentException("Cannot resolve "
                                                               + identifier + " to a DSpace object");
                }

                switch (dso.getType()) {
                    case Constants.COMMUNITY:
                        mediaFilterService.applyFiltersCommunity(c, (Community) dso);
                        break;
                    case Constants.COLLECTION:
                        mediaFilterService.applyFiltersCollection(c, (Collection) dso);
                        break;
                    case Constants.ITEM:
                        mediaFilterService.applyFiltersItem(c, (Item) dso);
                        break;
                    default:
                        break;
                }
            }

            c.complete();
            c = null;
        } catch (Exception e) {
            handler.handleException(e);
        } finally {
            if (c != null) {
                c.abort();
            }
        }
    }
}
