/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.impl;

import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.enhancer.AbstractItemEnhancer;
import org.dspace.content.enhancer.ItemEnhancer;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ItemEnhancer} that creates virtual metadata fields by copying
 * values from related entities referenced through authority-controlled metadata fields.
 *
 * <p><strong>Purpose and Functionality:</strong></p>
 * <p>This enhancer enables the CRIS virtual metadata system, which automatically
 * duplicates metadata from related entities onto items that reference them. This provides
 * denormalized access to related entity data without requiring complex joins, improving
 * search performance and enabling faceted search on related entity properties.</p>
 *
 * <p><strong>Configuration:</strong></p>
 * <p>The enhancer is configured through Spring configuration (in
 * {@code dspace/config/spring/api/metadata-enhancers.xml}) with four key properties:</p>
 * <ul>
 *   <li><strong>sourceEntityTypes:</strong> Entity types that can be enhanced (e.g., "Publication", "Product",
 *   "Patent")</li>
 *   <li><strong>sourceItemMetadataFields:</strong> Metadata fields containing entity references
 *       (e.g., "dc.contributor.author", "dc.contributor.editor")</li>
 *   <li><strong>relatedItemMetadataFields:</strong> Metadata fields to copy from related entities
 *       (e.g., "person.identifier.orcid", "person.affiliation.name")</li>
 *   <li><strong>virtualQualifier:</strong> Qualifier for the virtual metadata fields (e.g., "orcid", "department")</li>
 * </ul>
 *
 * <p><strong>Real-world Configuration Examples:</strong></p>
 * <pre>{@code
 * <!-- Example 1: Copy ORCID identifiers from Person entities to Publications -->
 * <bean class="org.dspace.content.enhancer.impl.RelatedEntityItemEnhancer">
 *     <property name="sourceEntityTypes">
 *         <list>
 *             <value>Publication</value>
 *             <value>Product</value>
 *             <value>Patent</value>
 *         </list>
 *     </property>
 *     <property name="sourceItemMetadataFields">
 *         <list>
 *             <value>dc.contributor.author</value>
 *             <value>dc.contributor.editor</value>
 *         </list>
 *     </property>
 *     <property name="relatedItemMetadataFields">
 *         <list>
 *             <value>person.identifier.orcid</value>
 *         </list>
 *     </property>
 *     <property name="virtualQualifier" value="orcid" />
 * </bean>
 * }</pre>
 *
 * <p><strong>Virtual Metadata Structure:</strong></p>
 * <p>Creates two types of virtual metadata fields with the configured qualifier:</p>
 * <ul>
 *   <li><strong>{@code dspace.virtual.[qualifier]}:</strong> Contains the actual copied values</li>
 *   <li><strong>{@code dspace.virtualsource.[qualifier]}:</strong> Contains the UUID of the source entity</li>
 * </ul>
 *
 * <p><strong>Use Cases:</strong></p>
 * <ul>
 *   <li><strong>Search Enhancement:</strong> Enable faceted search on author ORCIDs without complex joins</li>
 *   <li><strong>Analytics:</strong> Generate reports showing publications by department affiliation</li>
 *   <li><strong>Data Quality:</strong> Identify publications with authors missing ORCID identifiers</li>
 *   <li><strong>Export Integration:</strong> Include related entity metadata in metadata exports</li>
 * </ul>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @see ItemEnhancer
 * @see AbstractItemEnhancer
 * @see org.dspace.content.enhancer.consumer.ItemEnhancerConsumer
 * @see org.dspace.content.dao.ItemForMetadataEnhancementUpdateDAO
 */
public class RelatedEntityItemEnhancer extends AbstractItemEnhancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedEntityItemEnhancer.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private RelatedEntityItemEnhancerUtils relatedEntityItemEnhancerUtils;

    /**
     * the entities that can be extended by this enhancer, i.e. Publication
     */
    private List<String> sourceEntityTypes;

    /**
     * the metadata used to navigate the relation, i.e. dc.contributor.author
     */
    private List<String> sourceItemMetadataFields;

    /**
     * the metadata that is copied from the linked entity, i.e. person.identifier.orcid
     */
    private List<String> relatedItemMetadataFields;

    @Override
    public boolean canEnhance(Context context, Item item) {
        return sourceEntityTypes == null || sourceEntityTypes.contains(itemService.getEntityTypeLabel(item));
    }

    /**
     * Enhances the given item by synchronizing virtual metadata fields with values
     * copied from related entities referenced through authority-controlled metadata.
     * 
     * <p><strong>How Enhancement Works:</strong></p>
     * <ol>
     *   <li><strong>Identify Related Entities:</strong> Scans the item's configured source
     *       metadata fields (e.g., dc.contributor.author) for authority values that contain
     *       UUIDs of related entities</li>
     *   <li><strong>Copy Metadata:</strong> For each related entity found, copies values from
     *       configured metadata fields (e.g., person.identifier.orcid) into virtual metadata
     *       fields on the current item</li>
     *   <li><strong>Maintain Relationships:</strong> Creates {@code dspace.virtualsource.*} fields
     *       to track which virtual metadata came from which related entity for dependency management</li>
     *   <li><strong>Handle Missing Entities:</strong> When a related entity is not found or has
     *       no values, inserts placeholder values to maintain metadata field consistency</li>
     * </ol>
     * 
     * <p><strong>Processing Modes:</strong></p>
     * <ul>
     *   <li><strong>Incremental Mode (deepMode=false):</strong> Only adds missing virtual metadata
     *       and removes obsolete entries. More efficient for routine updates.</li>
     *   <li><strong>Deep Mode (deepMode=true):</strong> Completely rebuilds all virtual metadata
     *       by comparing current state with expected state. Used for comprehensive synchronization.</li>
     * </ul>
     * 
     * <p><strong>Virtual Metadata Structure:</strong></p>
     * <ul>
     *   <li><strong>{@code dspace.virtual.[qualifier]}:</strong> Contains the actual copied values
     *       from related entities (e.g., ORCID identifiers, names, affiliations)</li>
     *   <li><strong>{@code dspace.virtualsource.[qualifier]}:</strong> Contains the UUID of the
     *       related entity that provided each virtual value, enabling dependency tracking</li>
     * </ul>
     * 
     * <p><strong>Example Scenario:</strong></p>
     * <pre>
     * Publication item has: dc.contributor.author = "John Doe" with authority = "uuid-123"
     * Person entity uuid-123 has: person.identifier.orcid = "0000-0000-0000-0000"
     * 
     * After enhancement, Publication gains:
     * - dspace.virtual.author-orcid = "0000-0000-0000-0000"
     * - dspace.virtualsource.author-orcid = "uuid-123"
     * </pre>
     *
     * 
     * @param context the DSpace context for database operations
     * @param item the item to enhance with virtual metadata from related entities
     * @param deepMode if true, performs complete rebuild; if false, performs incremental update
     * @return true if any virtual metadata was added, updated, or removed; false if no changes were made
     * @throws SQLRuntimeException if database operations fail during enhancement
     * 
     * @see #performEnhancement(Context, Item) for incremental enhancement logic
     * @see #getToBeVirtualMetadata(Context, Item) for deep mode comparison logic
     * @see #cleanObsoleteVirtualFields(Context, Item) for cleanup of obsolete virtual fields
     */
    @Override
    public boolean enhance(Context context, Item item, boolean deepMode) {
        boolean result = false;
        if (!deepMode) {
            try {
                result = cleanObsoleteVirtualFields(context, item);
                result = performEnhancement(context, item) || result;
            } catch (SQLException e) {
                LOGGER.error("An error occurs enhancing item with id {}: {}", item.getID(), e.getMessage(), e);
                throw new SQLRuntimeException(e);
            }
        } else {
            Map<String, List<MetadataValue>> currMetadataValues = relatedEntityItemEnhancerUtils
                    .getCurrentVirtualsMap(item, getVirtualQualifier());
            Map<String, List<MetadataValueDTO>> toBeMetadataValues = getToBeVirtualMetadata(context, item);
            if (!equivalent(currMetadataValues, toBeMetadataValues)) {
                try {
                    clearAllVirtualMetadata(context, item);
                    addMetadata(context, item, toBeMetadataValues);
                } catch (SQLException e) {
                    throw new SQLRuntimeException(e);
                }
                result = true;
            }
        }
        return result;
    }

    private void clearAllVirtualMetadata(Context context, Item item) throws SQLException {
        itemService.clearMetadata(context, item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_SOURCE_METADATA_ELEMENT,
                getVirtualQualifier(), Item.ANY);
        itemService.clearMetadata(context, item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_METADATA_ELEMENT,
                getVirtualQualifier(), Item.ANY);
    }

    private void addMetadata(Context context, Item item, Map<String, List<MetadataValueDTO>> toBeMetadataValues)
            throws SQLException {
        for (Entry<String, List<MetadataValueDTO>> metadataValues : toBeMetadataValues.entrySet()) {
            for (MetadataValueDTO dto : metadataValues.getValue()) {
                addVirtualSourceField(context, item, metadataValues.getKey());
                addVirtualField(context, item, dto.getValue(), dto.getAuthority(), dto.getLanguage(),
                        dto.getConfidence());
            }
        }
    }

    private boolean equivalent(Map<String, List<MetadataValue>> currMetadataValues,
            Map<String, List<MetadataValueDTO>> toBeMetadataValues) {
        if (currMetadataValues.size() != toBeMetadataValues.size()) {
            return false;
        } else {
            for (String key : currMetadataValues.keySet()) {
                if (!equivalent(currMetadataValues.get(key), toBeMetadataValues.get(key))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean equivalent(List<MetadataValue> metadataValue, List<MetadataValueDTO> metadataValueDTO) {
        if ((Objects.isNull(metadataValue) && !Objects.isNull(metadataValueDTO)) ||
                (!Objects.isNull(metadataValue) && Objects.isNull(metadataValueDTO)) ||
                    metadataValue.size() != metadataValueDTO.size()) {
            return false;
        } else if (!Objects.isNull(metadataValue) && !Objects.isNull(metadataValueDTO)) {
            for (int i = 0; i < metadataValue.size(); i++) {
                if (!equivalent(metadataValue.get(i), metadataValueDTO.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean equivalent(MetadataValue metadataValue, MetadataValueDTO metadataValueDTO) {
        return StringUtils.equals(metadataValue.getMetadataField().getMetadataSchema().getName(),
                metadataValueDTO.getSchema())
                && StringUtils.equals(metadataValue.getMetadataField().getElement(), metadataValueDTO.getElement())
                && StringUtils.equals(metadataValue.getMetadataField().getQualifier(), metadataValueDTO.getQualifier())
                && StringUtils.equals(metadataValue.getValue(), metadataValueDTO.getValue())
                && StringUtils.equals(metadataValue.getAuthority(), metadataValueDTO.getAuthority());
    }

    private Map<String, List<MetadataValueDTO>> getToBeVirtualMetadata(Context context, Item item) {
        Map<String, List<MetadataValueDTO>> tobeVirtualMetadataMap = new HashMap<String, List<MetadataValueDTO>>();

        Set<String> virtualSources = getVirtualSources(item);
        for (String authority : virtualSources) {
            List<MetadataValueDTO> tobeVirtualMetadata = new ArrayList<>();
            Item relatedItem = null;
            relatedItem = findRelatedEntityItem(context, authority);
            if (relatedItem == null) {
                MetadataValueDTO mvRelated = new MetadataValueDTO();
                mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
                mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
                mvRelated.setQualifier(getVirtualQualifier());
                mvRelated.setValue(PLACEHOLDER_PARENT_METADATA_VALUE);
                tobeVirtualMetadata.add(mvRelated);
            } else {
                boolean foundAtLeastOneValue = false;
                for (String relatedItemMetadataField : relatedItemMetadataFields) {
                    List<MetadataValue> relatedItemMetadataValues = getMetadataValues(relatedItem,
                            relatedItemMetadataField);
                    for (MetadataValue relatedItemMetadataValue : relatedItemMetadataValues) {
                        MetadataValueDTO mvRelated = new MetadataValueDTO();
                        mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
                        mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
                        mvRelated.setQualifier(getVirtualQualifier());
                        mvRelated.setValue(relatedItemMetadataValue.getValue());
                        String authorityRelated = relatedItemMetadataValue.getAuthority();
                        if (StringUtils.isNotBlank(authorityRelated)) {
                            mvRelated.setAuthority(authorityRelated);
                            mvRelated.setConfidence(Choices.CF_ACCEPTED);
                        }
                        tobeVirtualMetadata.add(mvRelated);
                        foundAtLeastOneValue = true;
                    }
                }
                if (!foundAtLeastOneValue) {
                    MetadataValueDTO mvRelated = new MetadataValueDTO();
                    mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
                    mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
                    mvRelated.setQualifier(getVirtualQualifier());
                    mvRelated.setValue(PLACEHOLDER_PARENT_METADATA_VALUE);
                    tobeVirtualMetadata.add(mvRelated);
                }
            }
            tobeVirtualMetadataMap.put(authority, tobeVirtualMetadata);
        }
        return tobeVirtualMetadataMap;
    }

    private boolean cleanObsoleteVirtualFields(Context context, Item item) throws SQLException {
        boolean result = false;
        List<MetadataValue> metadataValuesToDelete = getObsoleteVirtualFields(item);
        if (!metadataValuesToDelete.isEmpty()) {
            itemService.removeMetadataValues(context, item, metadataValuesToDelete);
            result = true;
        }
        return result;
    }

    private List<MetadataValue> getObsoleteVirtualFields(Item item) {

        List<MetadataValue> obsoleteVirtualFields = new ArrayList<>();
        Map<String, List<MetadataValue>> currentVirtualsMap = relatedEntityItemEnhancerUtils
                .getCurrentVirtualsMap(item, getVirtualQualifier());
        Set<String> virtualSources = getVirtualSources(item);
        for (String authority : currentVirtualsMap.keySet()) {
            if (!virtualSources.contains(authority)) {
                for (MetadataValue mv : getVirtualSourceFields(item, authority)) {
                    obsoleteVirtualFields.add(mv);
                    getRelatedVirtualField(item, mv.getPlace()).ifPresent(obsoleteVirtualFields::add);
                }
            }
        }

        return obsoleteVirtualFields;

    }

    private Set<String> getVirtualSources(Item item) {
        return sourceItemMetadataFields.stream()
                .flatMap(field -> itemService.getMetadataByMetadataString(item, field).stream())
                .filter(mv -> UUIDUtils.fromString(mv.getAuthority()) != null)
                .map(mv -> mv.getAuthority())
                .collect(Collectors.toSet());
    }

    private Optional<MetadataValue> getRelatedVirtualField(Item item, int pos) {
        return getVirtualFields(item).stream()
            .skip(pos)
            .findFirst();
    }

    private boolean performEnhancement(Context context, Item item) throws SQLException {
        boolean result = false;
        Map<String, List<MetadataValue>> currentVirtualsMap = relatedEntityItemEnhancerUtils
                .getCurrentVirtualsMap(item, getVirtualQualifier());
        Set<String> virtualSources = getVirtualSources(item);
        for (String authority : virtualSources) {
            boolean foundAtLeastOne = false;
            if (!currentVirtualsMap.containsKey(authority)) {
                result = true;
                Item relatedItem = findRelatedEntityItem(context, authority);
                if (relatedItem == null) {
                    addVirtualField(context, item, PLACEHOLDER_PARENT_METADATA_VALUE, null, null, Choices.CF_UNSET);
                    addVirtualSourceField(context, item, authority);
                    continue;
                }

                for (String relatedItemMetadataField : relatedItemMetadataFields) {
                    List<MetadataValue> relatedItemMetadataValues = getMetadataValues(relatedItem,
                            relatedItemMetadataField);
                    for (MetadataValue relatedItemMetadataValue : relatedItemMetadataValues) {
                        foundAtLeastOne = true;
                        addVirtualField(context, item, relatedItemMetadataValue.getValue(),
                                relatedItemMetadataValue.getAuthority(), relatedItemMetadataValue.getLanguage(),
                                relatedItemMetadataValue.getConfidence());
                        addVirtualSourceField(context, item, authority);
                    }
                }
                if (!foundAtLeastOne) {
                    addVirtualField(context, item, PLACEHOLDER_PARENT_METADATA_VALUE, null, null, Choices.CF_UNSET);
                    addVirtualSourceField(context, item, authority);
                    continue;
                }
            }
        }
        return result;
    }

    private Item findRelatedEntityItem(Context context, String authority) {
        try {
            UUID relatedItemUUID = UUIDUtils.fromString(authority);
            return relatedItemUUID != null ? itemService.find(context, relatedItemUUID) : null;
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    private List<MetadataValue> getVirtualSourceFields(Item item, String authority) {
        return getMetadataValues(item, getVirtualSourceMetadataField()).stream()
                .filter(mv -> StringUtils.equals(authority, mv.getValue())).collect(Collectors.toList());
    }

    private List<MetadataValue> getVirtualFields(Item item) {
        return getMetadataValues(item, getVirtualMetadataField());
    }

    private void addVirtualField(Context context, Item item, String value, String authority, String lang,
            int confidence) throws SQLException {
        if (StringUtils.startsWith(authority, AuthorityValueService.GENERATE)
                || StringUtils.startsWith(authority, AuthorityValueService.REFERENCE)) {
            itemService.addMetadata(context, item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_METADATA_ELEMENT,
                    getVirtualQualifier(), lang, value, null, Choices.CF_UNSET);
        } else {
            itemService.addMetadata(context, item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_METADATA_ELEMENT,
                    getVirtualQualifier(), lang, value, authority, confidence);
        }
    }

    private void addVirtualSourceField(Context context, Item item, String sourceValueAuthority) throws SQLException {
        itemService.addMetadata(context, item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_SOURCE_METADATA_ELEMENT,
                                getVirtualQualifier(), null, sourceValueAuthority);
    }

    public void setSourceEntityTypes(List<String> sourceEntityTypes) {
        this.sourceEntityTypes = sourceEntityTypes;
    }

    public void setRelatedItemMetadataFields(List<String> relatedItemMetadataFields) {
        this.relatedItemMetadataFields = relatedItemMetadataFields;
    }

    public void setSourceItemMetadataFields(List<String> sourceItemMetadataFields) {
        this.sourceItemMetadataFields = sourceItemMetadataFields;
    }

}
