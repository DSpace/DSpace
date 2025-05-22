/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.runnable;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.suggestion.SolrSuggestionProvider;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultItemIterator;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.UUIDUtils;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Runner responsible to import metadata about authors from loader to Solr. This
 * runner works in two ways: If -s parameter with a valid UUID is received, then
 * the specific researcher with this UUID will be used. Invocation without any
 * parameter results in massive import, processing all authors registered in
 * DSpace.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 **/
public class PublicationLoaderRunnable
    extends DSpaceRunnable<ScriptConfiguration<?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicationLoaderRunnable.class);
    protected Context context;
    protected String profile;
    protected String loader;
    protected String filterQuery;
    private SolrSuggestionProvider publicationLoader = null;
    private ConfigurationService configurationService;
    private ItemService itemService;
    private Integer itemLimit;
    private List<SolrSuggestionProvider> providers;


    /**
     * Retrieves the script configuration for this runnable.
     * The configuration is fetched from the DSpace service manager.
     *
     * @return The {@link ScriptConfiguration} instance for the import-loader-suggestions script.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ScriptConfiguration<?> getScriptConfiguration() {
        return new DSpace().getServiceManager()
                           .getServiceByName("import-loader-suggestions", ScriptConfiguration.class);
    }


    /**
     * Initializes the script execution environment by setting up necessary services,
     * retrieving command-line arguments, and setting default values where necessary.
     *
     * @throws ParseException If there is an error parsing command-line options.
     */
    @Override
    public void setup() throws ParseException {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        providers = new DSpace().getServiceManager().getServicesByType(SolrSuggestionProvider.class);
        loader = commandLine.getOptionValue("l");
        profile = commandLine.getOptionValue("s");
        filterQuery = !commandLine.hasOption("f") ? "" : commandLine.getOptionValue("f");
        if (profile == null) {
            LOGGER.info("No argument for -s, process all profile");
        } else {
            LOGGER.info("Process eperson item with UUID " + profile);
        }
        if (commandLine.hasOption("m")) {
            this.itemLimit = Integer.valueOf(commandLine.getOptionValue("m"));
        } else {
            this.itemLimit = getDefaultLimit();
        }
    }

    /**
     * Main execution method for the script.
     * It validates input parameters, retrieves researcher items, and triggers the publication import process.
     *
     * @throws Exception If an error occurs during execution.
     */
    @Override
    public void internalRun() throws Exception {
        if (loader == null) {
            throw new NullPointerException("loader can't be null");
        }
        if (profile != null && UUIDUtils.fromString(profile) == null) {
            throw new IllegalArgumentException("The provided argument -s is not a valid uuid");
        }

        publicationLoader = getPublicationLoader(loader);

        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            DiscoverResultItemIterator researchers = findResearchers();
            while (researchers.hasNext()) {
                Item researcher = researchers.next();
                researcher = context.reloadEntity(researcher);
                publicationLoader.importRecords(context, researcher);
                setLastImportMetadataValue(researcher);
                context.commit();
                context.uncacheEntity(researcher);
            }
        } finally {
            context.restoreAuthSystemState();
            context.complete();
        }
    }

    /**
     * Retrieves the {@link SolrSuggestionProvider} based on the provided loader name.
     *
     * @param loader The name of the publication loader.
     * @return The corresponding {@link SolrSuggestionProvider}.
     * @throws IllegalArgumentException If no provider matching the loader name is found.
     */
    private SolrSuggestionProvider getPublicationLoader(String loader) {
        return providers
            .stream()
            .filter(provider -> StringUtils.equals(provider.getSourceName(), loader))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("IllegalArgumentException: " +
                                                                "Provider for: " + loader + " couldn't be found"));
    }


    /**
     * Get the Item(s) which map a researcher from Solr. If the uuid is specified,
     * the researcher with this UUID will be chosen. If the uuid doesn't match any
     * researcher, the method returns an empty array list. If uuid is null, all
     * research will be return.
     *
     * @return the researcher with specified UUID or all researchers
     */
    private DiscoverResultItemIterator findResearchers() {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(profile)) {
            discoverQuery.setQuery("search.resourceid:" + profile);
        }
        discoverQuery.addFilterQueries("search.resourcetype:Item");
        discoverQuery.addFilterQueries("dspace.entity.type:Person");
        discoverQuery.setSortField("lastModified", DiscoverQuery.SORT_ORDER.asc);

        if (!filterQuery.isEmpty()) {
            discoverQuery.addFilterQueries(filterQuery);
        }

        DiscoverResultItemIterator iterator = new DiscoverResultItemIterator(context, discoverQuery, itemLimit);
        return iterator;
    }


    /**
     * Updates the researcher's item metadata to store the last import timestamp.
     *
     * @param item The researcher item whose metadata should be updated.
     */
    private void setLastImportMetadataValue(Item item) {
        try {
            item = context.reloadEntity(item);
            String metadataField = String.format("dspace.%s.lastimport", loader);
            String currentDate = DCDate.getCurrent().toString();
            itemService.setMetadataSingleValue(context, item, new MetadataFieldName(metadataField), null, currentDate);
            itemService.update(context, item);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the default item limit for the publication loader.
     *
     * @return The default item limit, as defined in the DSpace configuration.
     */
    private Integer getDefaultLimit() {
        return configurationService.getIntProperty("suggestion.publication-loader.max", -1);
    }
}
