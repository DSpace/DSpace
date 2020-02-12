/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.external.provider.metadata.filter.MetadataSuggestionProviderFilter;

/**
 * This is the abstract class that all MetadataSuggestionProviders will extend. It contains a set of basic
 * properties that need to be defined for every MetadataSuggestionProvider.
 * @param <T>
 */
public abstract class MetadataSuggestionProvider<T extends ExternalDataProvider> {

    /**
     * This id for the MetadataSuggestionProvider
     */
    private String id;
    /**
     * A boolean indicating whether this MetadataSuggestionProvider is query based or not
     */
    private boolean queryBased;
    /**
     * A boolean indicating whether this MetadataSuggestionProvider is file based or not
     */
    private boolean fileBased;
    /**
     * A boolean indicating whethe this MetadataSuggestionProvider is metadata based or not
     */
    private boolean metadataBased;
    /**
     * The ExternalDataProvider for this MetadataSuggestionProvider
     */
    private T externalDataProvider;

    private List<MetadataSuggestionProviderFilter> metadataSuggestionProviderFilters;

    /**
     * Generic getter for the id
     * @return the id value of this MetadataSuggestionProvider
     */
    public String getId() {
        return id;
    }

    /**
     * Generic setter for the id
     * @param id   The id to be set on this MetadataSuggestionProvider
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Generic getter for the queryBased
     * @return the queryBased value of this MetadataSuggestionProvider
     */
    public boolean isQueryBased() {
        return queryBased;
    }

    /**
     * Generic setter for the queryBased
     * @param queryBased   The queryBased to be set on this MetadataSuggestionProvider
     */
    public void setQueryBased(boolean queryBased) {
        this.queryBased = queryBased;
    }

    /**
     * Generic getter for the fileBased
     * @return the fileBased value of this MetadataSuggestionProvider
     */
    public boolean isFileBased() {
        return fileBased;
    }

    /**
     * Generic setter for the fileBased
     * @param fileBased   The fileBased to be set on this MetadataSuggestionProvider
     */
    public void setFileBased(boolean fileBased) {
        this.fileBased = fileBased;
    }

    /**
     * Generic getter for the metadataBased
     * @return the metadataBased value of this MetadataSuggestionProvider
     */
    public boolean isMetadataBased() {
        return metadataBased;
    }

    /**
     * Generic setter for the metadataBased
     * @param metadataBased   The metadataBased to be set on this MetadataSuggestionProvider
     */
    public void setMetadataBased(boolean metadataBased) {
        this.metadataBased = metadataBased;
    }

    /**
     * Generic getter for the externalDataProvider
     * @return the externalDataProvider value of this MetadataSuggestionProvider
     */
    public T getExternalDataProvider() {
        return externalDataProvider;
    }

    /**
     * Generic setter for the externalDataProvider
     * @param externalDataProvider   The externalDataProvider to be set on this MetadataSuggestionProvider
     */
    public void setExternalDataProvider(T externalDataProvider) {
        this.externalDataProvider = externalDataProvider;
    }

    /**
     * Generic getter for the metadataSuggestionProviderFilters
     * @return the metadataSuggestionProviderFilters value of this MetadataSuggestionProvider
     */
    public List<MetadataSuggestionProviderFilter> getMetadataSuggestionProviderFilters() {
        return metadataSuggestionProviderFilters;
    }

    /**
     * Generic setter for the metadataSuggestionProviderFilters
     * @param metadataSuggestionProviderFilters   The metadataSuggestionProviderFilters to be set on this
     *                                            MetadataSuggestionProvider
     */
    public void setMetadataSuggestionProviderFilters(
        List<MetadataSuggestionProviderFilter> metadataSuggestionProviderFilters) {
        this.metadataSuggestionProviderFilters = metadataSuggestionProviderFilters;
    }

    /**
     * This method will decide whether the MetadataSuggestionProvider supports the given InProgressSubmission
     * or not
     * @param inProgressSubmission  The relevant InProgressSubmission
     * @param query
     * @param bitstream
     * @param useMetadata
     * @return A boolean indicating whether the argument is supported or not
     */
    public boolean supports(InProgressSubmission inProgressSubmission, String query,
                            Bitstream bitstream, boolean useMetadata) {
        if (StringUtils.isNotBlank(query) && !isQueryBased()) {
            return false;
        }
        if (bitstream != null && !isFileBased()) {
            return false;
        }
        if (useMetadata && !isMetadataBased()) {
            return false;
        }
        for (MetadataSuggestionProviderFilter metadataSuggestionProviderFilter : metadataSuggestionProviderFilters) {
            if (metadataSuggestionProviderFilter.supports(inProgressSubmission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method can be overridden in the implementing MetadataSuggestionProvider classes to support a query
     * based on a bitstream
     * @param bitstream The bitstream that the query will be based of
     * @return          A list of ExternalDataObjects resulting from this query
     */
    public List<ExternalDataObject> bitstreamQuery(Bitstream bitstream) {
        throw new UnsupportedOperationException("This method is not supported by the MetadataSuggestionProvider");
    }

    /**
     * This method can be overridden in the implementing MetadataSuggestionProvider classes to support a query
     * based on an item
     * @param item      The Item that the query will be based on
     * @param start     The start integer of the call
     * @param limit     The max number of records to be returned by the call
     * @return          A list of ExternalDataObjects resulting from this query
     */
    public List<ExternalDataObject> metadataQuery(Item item, int start, int limit) {
        throw new UnsupportedOperationException("This method is not supported by the MetadataSuggestionProvider");
    }

    /**
     * This method can be overridden in the implementing MetadataSuggestionProvider classes to support a query
     * @param query     the query for this call
     * @param start     The start integer of the call
     * @param limit     The max number of records to be returned by the call
     * @return          A list of ExternalDataObjects resulting from this query
     */
    public List<ExternalDataObject> query(String query, int start, int limit) {
        throw new UnsupportedOperationException("This method is not supported by the MetadataSuggestionProvider");
    }

    /**
     * This method can be overriden in the implementing MetadataSuggestionProvider classes to support retrieving the
     * total amount of results that will be return by performing a certain query
     * @param query The query
     * @return      The amount of results returned by this query
     */
    public int queryTotals(String query) {
        return 0;
    }

    /**
     * This method can be overriden in the implementing MetadataSuggestionProvider classes to support retrieving the
     * total amount of results that will be return by performing a certain query based on a bitstream
     * @param bitstream The bitstream
     * @return          The amount of results returned by this bitstream query
     */
    public int bitstreamQueryTotals(Bitstream bitstream) {
        return 0;
    }

    /**
     * This method can be overriden in the implementing MetadataSuggestionProvider classes to support retrieving the
     * total amount of results that will be return by performing a certain query based on an item
     * @param item  The item
     * @return      The amount of results returned by this item query
     */
    public int metadataQueryTotals(Item item) {
        return 0;
    }
}
