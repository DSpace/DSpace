/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.dspace.discovery.SearchUtils.AUTHORITY_SEPARATOR;
import static org.dspace.discovery.SearchUtils.FILTER_SEPARATOR;
import static org.dspace.discovery.SolrServiceImpl.SOLR_FIELD_SUFFIX_FACET_PREFIXES;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.DCInputAuthority;
import org.dspace.content.authority.DSpaceControlledVocabulary;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.MultiLanguageDiscoverSearchFilterFacet;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.ConfigurationService;
import org.dspace.web.ContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link SolrServiceIndexPlugin} that indexes controlled vocabulary
 * (value pairs) metadata fields into Solr for faceted search support.
 *
 * <p>This plugin processes metadata fields that have a Choice Authority configured (e.g.,
 * controlled vocabularies, authority-controlled fields) and adds multiple Solr fields
 * to enable flexible searching and filtering:
 * <ul>
 *   <li>{@code <language>_<field>_keyword} - The value with authority suffix for keyword search</li>
 *   <li>{@code <language>_<field>_acid} - Lowercase value with separator for acid search</li>
 *   <li>{@code <language>_<field>_filter} - Lowercase value for filter queries</li>
 *   <li>{@code <language>_<field>_facet} - Value with separator for faceting</li>
 *   <li>{@code <language>_<field>_ac} - Autocomplete field</li>
 *   <li>{@code <language>_<field>_authority} - Authority key when present</li>
 * </ul>
 *
 * <p>The plugin supports multilingual indexing by iterating through all configured locales
 * and creating language-specific field variants. It handles both {@link DSpaceControlledVocabulary}
 * and {@link DCInputAuthority} authority implementations.
 *
 * <p>Configuration: The separator character used in field values can be customized via
 * the {@code discovery.solr.facets.split.char} configuration property (defaults to
 * {@link SearchUtils#FILTER_SEPARATOR}).
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @see SolrServiceIndexPlugin
 * @see DiscoverySearchFilter
 */
public class SolrServiceValuePairsIndexPlugin implements SolrServiceIndexPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrServiceValuePairsIndexPlugin.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private ChoiceAuthorityService cas;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * {@inheritDoc}
     *
     * <p>Indexes all controlled vocabulary metadata fields for the given item.
     * Only processes items that have a parent Collection with configured Choice Authorities.
     * For each metadata field with choices configured, adds language-specific Solr fields
     * for faceted search support.
     *
     * @param context the DSpace context
     * @param object the indexable object (must be an {@link IndexableItem})
     * @param document the Solr document to add fields to
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void additionalIndex(Context context, IndexableObject object, SolrInputDocument document) {
        if (isNotIndexableItem(object)) {
            return;
        }

        Item item = ((IndexableItem)object).getIndexedObject();
        try {
            Collection collection = (Collection) itemService.getParentObject(context, item);
            for (MetadataValue metadata : item.getItemService().getMetadata(item, Item.ANY, Item.ANY, Item.ANY,
                    Item.ANY)) {
                for (Locale locale : I18nUtil.getSupportedLocales()) {
                    String language = locale.getLanguage();
                    if (cas.isChoicesConfigured(metadata.getMetadataField().toString(), item.getType(), collection)) {
                        additionalIndex(collection, item, metadata, language, document);
                    }
                }
            }

        } catch (Exception ex) {
            LOGGER.error("An error occurs indexing value pairs for item {}", item.getID(), ex);
        }
    }

    /**
     * Adds discovery field values for a specific metadata value in a given language.
     *
     * @param collection the parent collection used to resolve the choice authority
     * @param item the item being indexed
     * @param metadataValue the metadata value to process
     * @param language the language code for the indexed fields
     * @param document the Solr document to add fields to
     */
    private void additionalIndex(Collection collection, Item item, MetadataValue metadataValue, String language,
            SolrInputDocument document) {
        String metadataField = metadataValue.getMetadataField().toString('.');
        List<DiscoverySearchFilter> searchFilters = findSearchFiltersByMetadataField(item, metadataField);
        String authority = metadataValue.getAuthority();
        String value = getMetadataValue(collection, metadataValue, language);
        if (StringUtils.isNotBlank(value)) {
            for (DiscoverySearchFilter searchFilter : searchFilters) {
                addDiscoveryFieldFields(language, document, value, authority, searchFilter);
            }
        }
    }

    /**
     * Resolves the display label for a metadata value based on its authority.
     *
     * <p>For controlled vocabularies, returns the authority label if available,
     * otherwise falls back to the raw metadata value. For DCInputAuthority fields,
     * always attempts to resolve the label from the authority.
     *
     * @param collection the parent collection used to resolve the choice authority
     * @param metadataValue the metadata value to resolve
     * @param language the language for label resolution
     * @return the display label, or null if no suitable label can be found
     */
    private String getMetadataValue(Collection collection, MetadataValue metadataValue, String language) {
        String fieldKey = metadataValue.getMetadataField().toString();
        ChoiceAuthority choiceAuthority = cas.getAuthorityByFieldKeyCollection(fieldKey, Constants.ITEM, collection);
        String authority = metadataValue.getAuthority();
        if (choiceAuthority instanceof DSpaceControlledVocabulary) {
            String label = StringUtils.isNotBlank(authority) ? choiceAuthority.getLabel(authority, language)
                    : metadataValue.getValue();
            if (StringUtils.isBlank(label)) {
                label = metadataValue.getValue();
            }
            return label;
        } else if (choiceAuthority instanceof DCInputAuthority) {
            String label = choiceAuthority.getLabel(metadataValue.getValue(), language);
            if (StringUtils.isBlank(label)) {
                label = metadataValue.getValue();
            }
            return label;
        }
        return null;
    }

    /**
     * Adds multiple Solr fields for a discovery search filter value.
     *
     * <p>Creates the following fields (with language prefix):
     * <ul>
     *   <li>{@code _keyword} - Value with optional authority suffix for keyword search</li>
     *   <li>{@code _acid} - Lowercase value with separator for acid search</li>
     *   <li>{@code _filter} - Lowercase value for filter queries</li>
     *   <li>{@code _facet} - Value with separator for faceting (uses {@link SolrServiceImpl#SOLR_FIELD_SUFFIX_FACET_PREFIXES})</li>
     *   <li>{@code _ac} - Lowercase value with separator for autocomplete</li>
     *   <li>{@code _authority} - The authority key if present and existing authority field exists</li>
     * </ul>
     *
     * @param language the language code to prefix field names with
     * @param document the Solr document to add fields to
     * @param value the display value to index
     * @param authority the authority key (may be null or blank)
     * @param searchFilter the discovery search filter configuration
     */
    private void addDiscoveryFieldFields(String language, SolrInputDocument document, String value, String authority,
        DiscoverySearchFilter searchFilter) {
        String separator = configurationService.getProperty("discovery.solr.facets.split.char", FILTER_SEPARATOR);
        String fieldNameWithLanguage = language + "_" + searchFilter.getIndexFieldName();
        String valueLowerCase = value.toLowerCase();

        String keywordField = appendAuthorityIfNotBlank(value, authority);
        String acidField = appendAuthorityIfNotBlank(valueLowerCase + separator + value, authority);
        String filterField = appendAuthorityIfNotBlank(valueLowerCase + separator + value, authority);
        String prefixField = appendAuthorityIfNotBlank(valueLowerCase + separator + value, authority);

        document.addField(fieldNameWithLanguage + "_keyword", keywordField);
        document.addField(fieldNameWithLanguage + "_acid", acidField);
        document.addField(fieldNameWithLanguage + "_filter", filterField);
        document.addField(fieldNameWithLanguage + SOLR_FIELD_SUFFIX_FACET_PREFIXES, prefixField);
        document.addField(fieldNameWithLanguage + "_ac", valueLowerCase + separator + value);
        if (document.containsKey(searchFilter.getIndexFieldName() + "_authority")) {
            document.addField(fieldNameWithLanguage + "_authority", authority);
        }
    }

    /**
     * Appends the authority separator and authority key to a field value if the authority
     * is not blank.
     *
     * @param fieldValue the base field value
     * @param authority the authority key (may be null or blank)
     * @return the field value with authority appended if present, otherwise just the field value
     */
    private String appendAuthorityIfNotBlank(String fieldValue, String authority) {
        return isNotBlank(authority) ? fieldValue + AUTHORITY_SEPARATOR + authority : fieldValue;
    }

    /**
     * Returns all the search fields configured for the given metadataField. Filters
     * returned are not filtered by instance type equal to
     * {@link MultiLanguageDiscoverSearchFilterFacet} to allow for language-based
     * searches.
     *
     * @param item the item being indexed (used to resolve discovery configuration)
     * @param metadataField the metadata field key to search for (e.g., "dc.subject")
     * @return list of discovery search filters that include this metadata field
     */
    private List<DiscoverySearchFilter> findSearchFiltersByMetadataField(Item item, String metadataField) {
        return getAllDiscoveryConfiguration(item).stream()
            .flatMap(discoveryConfiguration -> discoveryConfiguration.getSearchFilters().stream())
            .filter(searchFilter -> searchFilter.getMetadataFields().contains(metadataField))
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Retrieves all discovery configurations associated with an item.
     *
     * @param item the item to get configurations for
     * @return list of discovery configurations for the item's collections
     * @throws SQLRuntimeException if a database error occurs
     */
    private List<DiscoveryConfiguration> getAllDiscoveryConfiguration(Item item) {
        try {
            return SearchUtils.getAllDiscoveryConfigurations(ContextUtil.obtainCurrentRequestContext(), item);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    /**
     * Checks whether the given indexable object is not an indexable item.
     *
     * @param object the indexable object to check
     * @return true if the object is not an IndexableItem instance
     */
    @SuppressWarnings("rawtypes")
    private boolean isNotIndexableItem(IndexableObject object) {
        return !(object instanceof IndexableItem);
    }

}
