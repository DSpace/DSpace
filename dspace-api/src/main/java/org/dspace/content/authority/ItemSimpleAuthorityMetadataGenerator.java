/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.solr.common.SolrDocument;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.enhancer.generator.RelationshipMetadataGenerator;
import org.dspace.content.enhancer.source.MetadataSource;
import org.dspace.content.enhancer.source.impl.SolrMetadataSource;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Implementation of {@link ItemAuthorityExtraMetadataGenerator} that extracts
 * metadata from DSpace items and adds them as extra information to authority choices.
 * <p>
 * This generator reads metadata values from Solr documents by delegating the extraction
 * to a {@link RelationshipMetadataGenerator} and populates the Choice's extras map.
 * It supports both simple and nested metadata fields.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li><strong>Data- prefixing:</strong> When {@code useAsData} is true, keys are prefixed
 *       with "data-" which are then passed to the frontend to automatically fill other submission
 *       fields.</li>
 *   <li><strong>Authority linking:</strong> Extracts authority references (e.g., affiliation UUIDs)
 *       and includes them in the format {@code value::authority}</li>
 *   <li><strong>Aggregate mode:</strong> When {@code singleResultOnAggregate} is false, a single
 *       item can produce multiple choices (one per metadata value)</li>
 * </ul>
 * </p>
 * <p>
 * Example: For a Person with two affiliations, this generator can create:
 * <ul>
 *   <li>display: "Author 1(University of Rome)",
 *   extras: {data-oairecerif_author_affiliation: "University of Rome::uuid1"}</li>
 *   <li>display: "Author 1(University of Milan)",
 *   extras: {data-oairecerif_author_affiliation: "University of Milan::uuid2"}</li>
 * </ul>
 * </p>
 *
 * @author Mykhaylo Boychuk (4Science.it)
 */
public class ItemSimpleAuthorityMetadataGenerator implements ItemAuthorityExtraMetadataGenerator, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ItemSimpleAuthorityMetadataGenerator.class);

    private String keyId;
    // use with aggregate mode
    private boolean singleResultOnAggregate = true;
    // limit the generator to a specific authority
    private String authorityName;
    private boolean useForDisplay = true;
    private boolean useAsData = true;
    // required delegate used to extract the metadata values from the Solr document
    private RelationshipMetadataGenerator delegate;

    /**
     * Verify that the required delegate has been configured, failing fast at startup
     * otherwise.
     *
     * @throws IllegalStateException if no delegate has been configured
     */
    @Override
    public void afterPropertiesSet() {
        if (delegate == null) {
            throw new IllegalStateException("No delegate configured for ItemSimpleAuthorityMetadataGenerator "
                + "with keyId " + keyId + ": a RelationshipMetadataGenerator is required");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the {@code authorityName} matches the configuration, it populates the
     * {@code keyId} with values found in the Solr document's stored projection fields.
     * </p>
     */
    @Override
    public Map<String, String> build(String checkAuthorityName, SolrDocument document) {
        Map<String, String> extras = new HashMap<String, String>();
        if (StringUtils.isBlank(authorityName) || Strings.CS.equals(authorityName, checkAuthorityName)) {
            buildSingleExtraByRP(document, extras);
        }
        return extras;
    }

    /**
     * {@inheritDoc}
     * <p>
     * When {@code singleResultOnAggregate} is set to {@code false}, this method
     * "explodes" a single Solr document into multiple {@link Choice} objects—one for
     * each value found in the configured metadata field (e.g., creating one choice
     * per affiliation for a single person).
     * </p>
     */
    @Override
    public List<Choice> buildAggregate(String checkAuthorityName, SolrDocument document) {
        List<Choice> choiceList = new LinkedList<Choice>();
        String title = getFirstValue(document, "dc.title");
        if (StringUtils.isNotBlank(authorityName) && !Strings.CS.equals(authorityName, checkAuthorityName)) {
            choiceList.add(
                new Choice(getFirstValue(document, "search.resourceid"),
                           title, title, new HashMap<String, String>()));
        } else {
            if (isSingleResultOnAggregate()) {
                Map<String, String> extras = new HashMap<String, String>();
                buildSingleExtraByRP(document, extras);
                choiceList.add(new Choice(getFirstValue(document, "search.resourceid"),
                                          title, title, extras));
            } else {
                List<MetadataValueDTO> metadataValues = getMetadataValueDTOs(document);
                if (!metadataValues.isEmpty()) {
                    for (MetadataValueDTO metadataValue : metadataValues) {
                        Map<String, String> extras = new HashMap<String, String>();
                        buildSingleExtraByMetadata(metadataValue, extras);
                        choiceList.add(new Choice(getFirstValue(document, "search.resourceid"),
                                                  title + "(" + metadataValue.getValue() + ")", title, extras));
                    }
                } else {
                    Map<String, String> extras = new HashMap<String, String>();
                    choiceList.add(new Choice(getFirstValue(document, "search.resourceid"),
                                              title, title, extras));
                }
            }
        }
        return choiceList;
    }

    /**
     * Return the first value stored in the given Solr field, supporting both single
     * valued and multi valued fields.
     *
     * @param document the Solr document
     * @param field    the field name
     * @return the first value, or {@code null} when the field is absent
     */
    private String getFirstValue(SolrDocument document, String field) {
        Object fieldValue = document.getFieldValue(field);
        if (fieldValue instanceof List) {
            List<?> values = (List<?>) fieldValue;
            return values.isEmpty() ? null : String.valueOf(values.get(0));
        }
        return fieldValue == null ? null : String.valueOf(fieldValue);
    }

    protected void buildSingleExtraByMetadata(MetadataValueDTO metadataValue, Map<String, String> extras) {
        if (metadataValue == null) {
            return;
        }
        if (isDuplicatedValue(metadataValue, extras)) {
            return;
        }
        if (StringUtils.isNotBlank(metadataValue.getAuthority())) {
            putValueInExtras(extras, metadataValue.getValue() + "::" + metadataValue.getAuthority());
        } else {
            putValueInExtras(extras, metadataValue.getValue());
        }
    }

    private boolean isDuplicatedValue(MetadataValueDTO metadataValue, Map<String, String> extras) {
        String key = keyId;
        if (useAsData) {
            key = "data-" + keyId;
        }
        String values = extras.get(key);
        if (values == null) {
            return false;
        }

        String valueToFind = StringUtils.isNotBlank(metadataValue.getAuthority())
            ? metadataValue.getValue() + "::" + metadataValue.getAuthority()
            : metadataValue.getValue();

        if (values.contains("|||")) {
            // so it's multivalues
            String[] splittedVals = values.split(Pattern.quote("|||"));
            return ArrayUtils.contains(splittedVals, valueToFind);
        }
        return values.equals(valueToFind);

    }

    protected void buildSingleExtraByRP(SolrDocument solrDocument, Map<String, String> extras) {
        List<MetadataValueDTO> metadataValues = getMetadataValueDTOs(solrDocument);
        if (metadataValues.isEmpty()) {
            buildSingleExtraByMetadata(null, extras);
        } else {
            metadataValues.forEach(metadataValue -> {
                buildSingleExtraByMetadata(metadataValue, extras);
            });
        }
    }

    private void putValueInExtras(Map<String, String> extras, String value) {
        if (useAsData) {
            String key = "data-" + keyId;
            if (extras.containsKey(key)) {
                extras.put(key, extras.get(key) + "|||" + value);
            } else {
                extras.put(key, value);
            }
        }
        if (useForDisplay) {
            if (extras.containsKey(keyId)) {
                extras.put(keyId, extras.get(keyId) + "|||" + value);
            } else {
                extras.put(keyId, value);
            }
        }
    }

    /**
     * Extract the metadata values for the configured delegate from the given Solr document.
     * <p>
     * The configured delegate source field must be part of the discovery projection, otherwise
     * an error is logged and an empty list is returned. The returned list is never {@code null}.
     * </p>
     *
     * @param solrDocument the Solr document from which the values are extracted
     * @return the extracted metadata values, possibly empty
     */
    private List<MetadataValueDTO> getMetadataValueDTOs(SolrDocument solrDocument) {
        if (!projectionFieldsContain(delegate.getSourceField())) {
            log.error("the metadata {} is not configured to be stored in solr, check the discovery.index.projection " +
                          "properties in the discovery.cfg",
                delegate.getSourceField());
            return Collections.emptyList();
        }
        MetadataSource source = new SolrMetadataSource(solrDocument);
        List<MetadataValueDTO> metadataValues = delegate.extract(source);
        return metadataValues != null ? metadataValues : Collections.emptyList();
    }

    /**
     * Check whether the given source field is configured in the discovery projection, either
     * with its full name or with the qualifier replaced by the {@link Item#ANY} wildcard.
     *
     * @param sourceField the field to check, in the form {@code schema.element[.qualifier]}
     * @return {@code true} if the field is part of the discovery projection
     */
    private boolean projectionFieldsContain(String sourceField) {
        String[] projectionFields = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                         .getArrayProperty("discovery.index.projection");
        MetadataFieldName field = new MetadataFieldName(sourceField);
        String wildcardField = new MetadataFieldName(field.schema, field.element, Item.ANY).toString();
        return ArrayUtils.contains(projectionFields, sourceField) ||
            ArrayUtils.contains(projectionFields, wildcardField);
    }

    public void setRelatedInputformMetadata(String keyId) {
        log.warn("Used deprecated setRelatedInputformMetadata to set keyId on "
                     + "ItemSimpleAuthorityMetadataGenerator, setKeyId should be preferred");
        this.keyId = keyId;
    }

    /**
     * Return the delegate used to extract the metadata values from the Solr document.
     *
     * @return the configured delegate
     */
    public RelationshipMetadataGenerator getDelegate() {
        return delegate;
    }

    /**
     * Set the delegate used to extract the metadata values from the Solr document.
     *
     * @param delegate the required {@link RelationshipMetadataGenerator}
     */
    public void setDelegate(RelationshipMetadataGenerator delegate) {
        this.delegate = delegate;
    }

    public boolean isSingleResultOnAggregate() {
        return singleResultOnAggregate;
    }

    public void setSingleResultOnAggregate(boolean singleResultOnAggregate) {
        this.singleResultOnAggregate = singleResultOnAggregate;
    }

    public String getAuthorityName() {
        return authorityName;
    }

    public void setAuthorityName(String authorityName) {
        this.authorityName = authorityName;
    }

    public boolean isUseForDisplay() {
        return useForDisplay;
    }

    public void setUseForDisplay(boolean useForDisplay) {
        this.useForDisplay = useForDisplay;
    }

    public boolean isUseAsData() {
        return useAsData;
    }

    public void setUseAsData(boolean useAsData) {
        this.useAsData = useAsData;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

}
