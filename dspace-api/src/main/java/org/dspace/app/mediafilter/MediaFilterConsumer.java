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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.mediafilter.factory.MediaFilterServiceFactory;
import org.dspace.app.mediafilter.service.MediaFilterService;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Consumer that automatically runs media filter for archived items
 *
 */
public class MediaFilterConsumer implements Consumer {

    private static final Logger log = LogManager.getLogger(MediaFilterConsumer.class);

    //key (in dspace.cfg) which lists all enabled filters by name
    private static final String MEDIA_FILTER_PLUGINS_KEY = "filter.plugins";

    //prefix (in dspace.cfg) for all filter properties
    private static final String FILTER_PREFIX = "filter";

    //suffix (in dspace.cfg) for input formats supported by each filter
    private static final String INPUT_FORMATS_SUFFIX = "inputFormats";

    private Map<String, List<String>> filterFormats = new HashMap<>();

    private MediaFilterService mediaFilterService;
    private ConfigurationService configurationService;

    @Override
    public void initialize() throws Exception {
        mediaFilterService = MediaFilterServiceFactory.getInstance().getMediaFilterService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {

        if (!configurationService.getBooleanProperty("mediafilter.archived.items", false)) {
            return;
        }

        if (event.getSubjectType() != Constants.ITEM ||
            event.getEventType() != Event.INSTALL) {
            return;
        }

        Item item = (Item) event.getSubject(context);
        if (item == null || !item.isArchived() || item.isWithdrawn()) {
            return;
        }

        try {
            context.turnOffAuthorisationSystem();

            initializeFilters();

            mediaFilterService.applyFiltersItem(context, item);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void initializeFilters() throws Exception {
        //initialize an array of our enabled filters
        List<FormatFilter> filterList = new ArrayList<>();

        String[] filterNames = configurationService.getArrayProperty(MEDIA_FILTER_PLUGINS_KEY);

        //set up each filter
        for (int i = 0; i < filterNames.length; i++) {
            //get filter of this name & add to list of filters
            FormatFilter filter = (FormatFilter) CoreServiceFactory.getInstance().getPluginService()
                                                                   .getNamedPlugin(FormatFilter.class, filterNames[i]);
            if (filter == null) {
                log.error("ERROR: Unknown MediaFilter specified in dspace.cfg: '" + filterNames[i] + "'");
                throw new Exception("ERROR: Unknown MediaFilter specified in dspace.cfg: '" + filterNames[i] + "'");
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
                        configurationService.getArrayProperty(
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

        mediaFilterService.setFilterFormats(filterFormats);
        //store our filter list into an internal array
        mediaFilterService.setFilterClasses(filterList);
    }


    @Override
    public void end(Context ctx) throws Exception {

    }

    @Override
    public void finish(Context ctx) throws Exception {

    }

}
