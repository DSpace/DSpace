/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.openaire;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.utils.DiscoverQueryBuilder;
import org.dspace.discovery.utils.parameter.QueryBuilderSearchFilter;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.sort.SortOption;
import org.dspace.utils.DSpace;

/**
 * Runner responsible to import metadata about authors from OpenAIRE to Solr.
 * This runner works in two ways:
 * If -s parameter with a valid UUID is received, then the specific researcher
 * with this UUID will be used.
 * Invocation without any parameter results in massive import, processing all
 * authors registered in DSpace.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class PublicationLoaderRunnable
    extends DSpaceRunnable<PublicationLoaderScriptConfiguration<PublicationLoaderRunnable>> {

    private static final Logger LOGGER = LogManager.getLogger();

    private PublicationLoader oairePublicationLoader = null;

    protected Context context;

    protected String profile;

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public PublicationLoaderScriptConfiguration<PublicationLoaderRunnable> getScriptConfiguration() {
        PublicationLoaderScriptConfiguration configuration = new DSpace().getServiceManager()
                .getServiceByName("import-openaire-suggestions", PublicationLoaderScriptConfiguration.class);
        return configuration;
    }

    @Override
    public void setup() throws ParseException {

        oairePublicationLoader = new DSpace().getServiceManager().getServiceByName(
                "OpenairePublicationLoader", PublicationLoader.class);

        profile = commandLine.getOptionValue("s");
        if (profile == null) {
            LOGGER.info("No argument for -s, process all profiles");
        } else {
            LOGGER.info("Process eperson item with UUID {}", profile);
        }
    }

    @Override
    public void internalRun() throws Exception {

        context = new Context();

        Iterator<Item> researchers = getResearchers(profile);
        while (researchers.hasNext()) {
            Item researcher = researchers.next();
            oairePublicationLoader.importAuthorRecords(context, researcher);
        }

    }

    /**
     * Get the Item(s) which map a researcher from Solr. If the uuid is specified,
     * the researcher with this UUID will be chosen. If the uuid doesn't match any
     * researcher, the method returns an empty array list. If uuid is null, all
     * research will be return.
     *
     * @param  profileUUID uuid of the researcher. If null, all researcher will be
     *                     returned.
     * @return             the researcher with specified UUID or all researchers
     */
    @SuppressWarnings("rawtypes")
    private Iterator<Item> getResearchers(String profileUUID) {
        SearchService searchService = new DSpace().getSingletonService(SearchService.class);
        DiscoverQueryBuilder queryBuilder = SearchUtils.getQueryBuilder();
        List<QueryBuilderSearchFilter> filters = new ArrayList<>();
        String query = "*:*";
        if (profileUUID != null) {
            query = "search.resourceid:" + profileUUID;
        }
        try {
            DiscoverQuery discoverQuery = queryBuilder.buildQuery(context, null,
                SearchUtils.getDiscoveryConfigurationByName("person"),
                query, filters,
                "Item", 10, Long.getLong("0"), null, SortOption.DESCENDING);
            return searchService.iteratorSearch(context, null, discoverQuery);
        } catch (SearchServiceException e) {
            LOGGER.error("Unable to read researcher on solr", e);
        }
        return null;
    }
}
