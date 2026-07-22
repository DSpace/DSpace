/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import static org.dspace.app.mediafilter.service.MediaFilterService.FILTER_PLUGIN_SEPARATOR;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * MediaFilterManager is the class that invokes the media/format filters over the
 * repository's content. A few command line flags affect the operation of the
 * MFM: -v verbose outputs all extracted text to STDOUT; -f force forces all
 * bitstreams to be processed, even if they have been before; -n noindex does not
 * recreate index after processing bitstreams; -i [identifier] limits processing
 * scope to a community, collection or item; -m [max] limits processing to a
 * maximum number of items; -fd [fromdate] takes only items starting from this date,
 * filtering by last_modified in the item table.
 */
public class MediaFilterScript extends DSpaceRunnable<MediaFilterScriptConfiguration> {

    //key (in dspace.cfg) which lists al enabled filters by name
    private static final String MEDIA_FILTER_PLUGINS_KEY = "filter.plugins";

    //prefix (in dspace.cfg) for all filter properties
    private static final String FILTER_PREFIX = "filter";

    //suffix (in dspace.cfg) for input formats supported by each filter
    private static final String INPUT_FORMATS_SUFFIX = "inputFormats";

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private boolean help;
    private boolean isVerbose = false;
    private boolean isQuiet = false;
    private boolean isForce = false; // default to not forced
    private String identifier = null; // object scope limiter
    private int max2Process = Integer.MAX_VALUE;
    private String[] filterNames;
    private String[] skipIds = null;
    private Map<String, List<String>> filterFormats = new HashMap<>();
    private LocalDate fromDate = null;

    public MediaFilterScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("filter-media", MediaFilterScriptConfiguration.class);
    }

    public void setup() throws ParseException {
        // set headless for non-gui workstations
        System.setProperty("java.awt.headless", "true");

        help = commandLine.hasOption('h');
        isVerbose = commandLine.hasOption('v');
        isQuiet = commandLine.hasOption('q');
        isForce = commandLine.hasOption('f');
        identifier = commandLine.getOptionValue('i');
        max2Process = commandLine.hasOption('m') ?
                Integer.parseInt(commandLine.getOptionValue('m')) : Integer.MAX_VALUE;
        filterNames = commandLine.hasOption('p') ?
                commandLine.getOptionValues('p') : configurationService.getArrayProperty(MEDIA_FILTER_PLUGINS_KEY);
        skipIds = commandLine.hasOption('s') ? commandLine.getOptionValues('s') : null;
        fromDate = commandLine.hasOption('d') ? LocalDate.parse(commandLine.getOptionValue('d')) : null;
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

        List<FormatFilter> filterList = new ArrayList<>();
        for (String filterName : filterNames) {
            try {
                FormatFilter filter = (FormatFilter) CoreServiceFactory.getInstance()
                                                                       .getPluginService()
                                                                       .getNamedPlugin(FormatFilter.class, filterName);
                if (filter == null) {
                    handler.logError("Unknown MediaFilter specified: '" + filterName + "' will not be processed.");
                } else {
                    filterList.add(filter);
                    String filterClassName = filter.getClass().getName();
                    String pluginName = SelfNamedPlugin.class.isAssignableFrom(filter.getClass()) ?
                            ((SelfNamedPlugin) filter).getPluginInstanceName() : null;
                    String[] formats = configurationService.getArrayProperty(FILTER_PREFIX + "." + filterClassName +
                            (pluginName != null ? "." + pluginName : "") + "." + INPUT_FORMATS_SUFFIX);
                    if (ArrayUtils.isNotEmpty(formats)) {
                        filterFormats.put(filterClassName + (pluginName != null ?
                                FILTER_PLUGIN_SEPARATOR + pluginName : ""), Arrays.asList(formats));
                    }
                }
            } catch (Exception e) {
                handler.logError("MediaFilter: '" + filterName +
                        "' was not initialized successfully and will not be processed", e);
            }
        }

        if (isVerbose) {
            handler.logInfo("The following MediaFilters are enabled: ");
            for (String filterName : filterFormats.keySet()) {
                handler.logInfo("Full Filter Name: " + filterName);
                String pluginName = filterName.contains(FILTER_PLUGIN_SEPARATOR) ?
                        filterName.split(FILTER_PLUGIN_SEPARATOR)[1] : null;
                handler.logInfo(filterName + (pluginName != null ? " (Plugin: " + pluginName + ")" : ""));
            }
        }

        mediaFilterService.setFilterFormats(filterFormats);
        mediaFilterService.setFilterClasses(filterList);
        mediaFilterService.setSkipList(skipIds != null ? Arrays.asList(skipIds) : null);
        mediaFilterService.setFromDate(fromDate);

        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            if (identifier == null) {
                mediaFilterService.applyFiltersAllItems(c);
            } else {
                DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(c, identifier);
                if (dso == null) {
                    throw new IllegalArgumentException("Cannot resolve " + identifier + " to a DSpace object");
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
        } catch (Exception e) {
            handler.handleException(e);
        }
    }
}
