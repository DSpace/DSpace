/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoveryConfiguration implements InitializingBean {

    private static final Logger log = LogManager.getLogger(DiscoveryConfiguration.class);

    /**
     * Minimum accepted value for {@link #spellCheckCount}: Solr rejects a non-positive
     * {@code spellcheck.count} with an {@code IllegalArgumentException}.
     */
    public static final int MIN_SPELL_CHECK_COUNT = 1;

    /**
     * Maximum accepted value for {@link #spellCheckCount}. The value of {@code spellcheck.count} is passed straight
     * to Solr, which allocates and ranks that many alternatives <em>per query term</em>: an excessively large value
     * (in the order of hundreds of millions) is enough to bring the Solr instance down with an OutOfMemoryError.
     * Since the alternatives returned by Solr are already deduplicated and sorted by frequency, the suggestions
     * beyond the first handful carry little value ("noe" starts suggesting "hope" or "love" well before the 50th
     * alternative), so capping the configurable value costs nothing in terms of usefulness.
     */
    public static final int MAX_SPELL_CHECK_COUNT = 20;

    /**
     * Default value for {@link #spellCheckCount}.
     * <p>
     * Solr defaults {@code spellcheck.count} to 1 when the parameter is omitted (and to 5 when the parameter is
     * present but holds no number), see the
     * <a href="https://solr.apache.org/guide/solr/9_0/query-guide/spell-checking.html">Solr spell checking
     * documentation</a>. We deliberately keep a small explicit default of 2 rather than adopting either of those:
     * the "did you mean" feature only needs the best correction plus one fallback for the frequent case where the
     * top alternative is not the one the user meant, and every extra alternative is additional work for Solr and
     * additional noise in the response. Increase it per configuration if a use case needs richer suggestions.
     */
    public static final int DEFAULT_SPELL_CHECK_COUNT = 2;

    /**
     * The configuration for the sidebar facets
     **/
    private List<DiscoverySearchFilterFacet> sidebarFacets = new ArrayList<>();

    private TagCloudFacetConfiguration tagCloudFacetConfiguration = new TagCloudFacetConfiguration();

    /**
     * The default filter queries which will be applied to any search & the recent submissions
     **/
    private List<String> defaultFilterQueries;

    /**
     * Configuration object for the recent submissions
     **/
    private DiscoveryRecentSubmissionsConfiguration recentSubmissionConfiguration;

    /**
     * The search filters which can be selected on the search page
     **/
    private List<DiscoverySearchFilter> searchFilters = new ArrayList<>();

    private DiscoverySortConfiguration searchSortConfiguration;

    private int defaultRpp = 10;

    private String id;
    private DiscoveryHitHighlightingConfiguration hitHighlightingConfiguration;
    private DiscoveryMoreLikeThisConfiguration moreLikeThisConfiguration;
    private boolean spellCheckEnabled;

    /**
     * Maximum number of spellcheck alternatives requested from Solr for each term of the query.
     * Always kept within [{@link #MIN_SPELL_CHECK_COUNT}, {@link #MAX_SPELL_CHECK_COUNT}] by
     * {@link #setSpellCheckCount(int)}.
     */
    private int spellCheckCount = DEFAULT_SPELL_CHECK_COUNT;
    private boolean indexAlways = false;

    /**
     * The `indexAlways` property determines whether the configuration should
     * always be included when indexing items.  The default value is false,
     * which implies the configuration is only used when it matches the
     * collection or if it's the default configuration.
     * When set to true, the configuration is also used to index an item without
     * a specific collection mapping.  This can be used for displaying different
     * facets depending on the type of item instead of the collection.
     * @return true if items without a specific collection mapping should be indexed.
     */
    public boolean isIndexAlways() {
        return indexAlways;
    }

    public void setIndexAlways(boolean indexAlways) {
        this.indexAlways = indexAlways;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<DiscoverySearchFilterFacet> getSidebarFacets() {
        return sidebarFacets;
    }

    @Autowired(required = true)
    public void setSidebarFacets(List<DiscoverySearchFilterFacet> sidebarFacets) {
        this.sidebarFacets = sidebarFacets;
    }

    public TagCloudFacetConfiguration getTagCloudFacetConfiguration() {
        return tagCloudFacetConfiguration;
    }

    public void setTagCloudFacetConfiguration(TagCloudFacetConfiguration tagCloudFacetConfiguration) {
        this.tagCloudFacetConfiguration = tagCloudFacetConfiguration;
    }

    public List<String> getDefaultFilterQueries() {
        //Since default filter queries are not mandatory we will return an empty list
        if (defaultFilterQueries == null) {
            return new ArrayList<>();
        } else {
            return defaultFilterQueries;
        }
    }

    public void setDefaultFilterQueries(List<String> defaultFilterQueries) {
        this.defaultFilterQueries = defaultFilterQueries;
    }

    public DiscoveryRecentSubmissionsConfiguration getRecentSubmissionConfiguration() {
        return recentSubmissionConfiguration;
    }

    public void setRecentSubmissionConfiguration(
        DiscoveryRecentSubmissionsConfiguration recentSubmissionConfiguration) {
        this.recentSubmissionConfiguration = recentSubmissionConfiguration;
    }

    public List<DiscoverySearchFilter> getSearchFilters() {
        return searchFilters;
    }

    public DiscoverySearchFilter getSearchFilter(String name) {
        for (DiscoverySearchFilter filter : CollectionUtils.emptyIfNull(searchFilters)) {
            if (Strings.CS.equals(name, filter.getIndexFieldName())) {
                return filter;
            }
        }
        return null;
    }

    @Autowired(required = true)
    public void setSearchFilters(List<DiscoverySearchFilter> searchFilters) {
        this.searchFilters = searchFilters;
    }

    public DiscoverySortConfiguration getSearchSortConfiguration() {
        return searchSortConfiguration;
    }

    @Autowired(required = true)
    public void setSearchSortConfiguration(DiscoverySortConfiguration searchSortConfiguration) {
        this.searchSortConfiguration = searchSortConfiguration;
    }

    public void setDefaultRpp(int defaultRpp) {
        this.defaultRpp = defaultRpp;
    }

    public int getDefaultRpp() {
        return defaultRpp;
    }

    public void setHitHighlightingConfiguration(DiscoveryHitHighlightingConfiguration hitHighlightingConfiguration) {
        this.hitHighlightingConfiguration = hitHighlightingConfiguration;
    }

    public DiscoveryHitHighlightingConfiguration getHitHighlightingConfiguration() {
        return hitHighlightingConfiguration;
    }

    public void setMoreLikeThisConfiguration(DiscoveryMoreLikeThisConfiguration moreLikeThisConfiguration) {
        this.moreLikeThisConfiguration = moreLikeThisConfiguration;
    }

    public DiscoveryMoreLikeThisConfiguration getMoreLikeThisConfiguration() {
        return moreLikeThisConfiguration;
    }

    public boolean isSpellCheckEnabled() {
        return spellCheckEnabled;
    }

    public void setSpellCheckEnabled(boolean spellCheckEnabled) {
        this.spellCheckEnabled = spellCheckEnabled;
    }

    /**
     * After all the properties are set check that the sidebar facets are a subset of our search filters
     *
     * @throws Exception throws an exception if this isn't the case
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Collection missingSearchFilters = CollectionUtils.subtract(getSidebarFacets(), getSearchFilters());
        if (CollectionUtils.isNotEmpty(missingSearchFilters)) {
            StringBuilder error = new StringBuilder();
            error.append("The following sidebar facet configurations are not present in the search filters list: ");
            for (Object missingSearchFilter : missingSearchFilters) {
                DiscoverySearchFilter searchFilter = (DiscoverySearchFilter) missingSearchFilter;
                error.append(searchFilter.getIndexFieldName()).append(" ");

            }
            error.append("all the sidebar facets MUST be a part of the search filters list.");

            throw new DiscoveryConfigurationException(error.toString());
        }

        Collection missingTagCloudSearchFilters = CollectionUtils
            .subtract(getTagCloudFacetConfiguration().getTagCloudFacets(), getSearchFilters());
        if (CollectionUtils.isNotEmpty(missingTagCloudSearchFilters)) {
            StringBuilder error = new StringBuilder();
            error.append("The following tagCloud facet configurations are not present in the search filters list: ");
            for (Object missingSearchFilter : missingTagCloudSearchFilters) {
                DiscoverySearchFilter searchFilter = (DiscoverySearchFilter) missingSearchFilter;
                error.append(searchFilter.getIndexFieldName()).append(" ");

            }
            error.append("all the tagCloud facets MUST be a part of the search filters list.");

            throw new DiscoveryConfigurationException(error.toString());
        }
    }

    public DiscoverySearchFilterFacet getSidebarFacet(final String facetName) {
        for (DiscoverySearchFilterFacet sidebarFacet : sidebarFacets) {
            if (Strings.CS.equals(sidebarFacet.getIndexFieldName(), facetName)) {
                return sidebarFacet;
            }
        }
        return null;
    }

    /**
     * @return the maximum number of spellcheck alternatives to request from Solr for each query term, guaranteed to
     *         be within [{@link #MIN_SPELL_CHECK_COUNT}, {@link #MAX_SPELL_CHECK_COUNT}].
     */
    public int getSpellCheckCount() {
        return spellCheckCount;
    }

    /**
     * Sets the maximum number of spellcheck alternatives to request from Solr for each query term.
     * <p>
     * The value is clamped to [{@link #MIN_SPELL_CHECK_COUNT}, {@link #MAX_SPELL_CHECK_COUNT}] instead of being
     * rejected, so that a misconfiguration cannot make Solr fail (a non-positive count makes Solr throw an
     * {@code IllegalArgumentException}, a huge one can exhaust its heap) nor prevent DSpace from starting up.
     * A warning is logged whenever the configured value is out of range.
     *
     * @param spellCheckCount the configured amount of alternatives
     */
    public void setSpellCheckCount(int spellCheckCount) {
        if (spellCheckCount < MIN_SPELL_CHECK_COUNT || spellCheckCount > MAX_SPELL_CHECK_COUNT) {
            int clamped = Math.min(Math.max(spellCheckCount, MIN_SPELL_CHECK_COUNT), MAX_SPELL_CHECK_COUNT);
            log.warn("The configured spellCheckCount {} for the discovery configuration {} is outside the allowed "
                         + "range [{}, {}] and has been adjusted to {}", spellCheckCount, id,
                     MIN_SPELL_CHECK_COUNT, MAX_SPELL_CHECK_COUNT, clamped);
            this.spellCheckCount = clamped;
        } else {
            this.spellCheckCount = spellCheckCount;
        }
    }
}
