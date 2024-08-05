/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.dspace.external.model.ExternalDataObject;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;

/**
 * This abstract class allows to configure the list of supported entity types
 * via spring. If no entity types are explicitly configured it is assumed that
 * the provider can be used with any entity type
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public abstract class AbstractExternalDataProvider implements ExternalDataProvider {

    private List<String> supportedEntityTypes;

    /**
     * Map of metadata fields, by "type" (this classification is up to the implementer)
     * set in spring configuration
     */
    protected Map<String, Map<String, MetadataFieldConfig>> metadataMapsByType;

    public void setSupportedEntityTypes(List<String> supportedEntityTypes) {
        this.supportedEntityTypes = supportedEntityTypes;
    }

    public List<String> getSupportedEntityTypes() {
        return supportedEntityTypes;
    }

    /**
     * Return true if the supportedEntityTypes list is empty or contains the requested entity type
     * 
     * @param entityType the entity type to check
     * @return true if the external provider can be used to search for items of the
     *         specified type
     */
    @Override
    public boolean supportsEntityType(String entityType) {
        return Objects.isNull(supportedEntityTypes) || supportedEntityTypes.contains(entityType);
    }

    /**
     * Get entity type for a given ExternalDataObject. We leave it to the provider to determine this
     * but the idea is to allow it to inspect the object itself rather than relying only on configuration
     * or the hardcoded nature of the provider itself.
     *
     * @param externalDataObject the external data object
     * @return a supported entity label or null to indicate no entities are supported, or this
     *         feature is not implemented
     */
    @Override
    public String getEntityTypeForExternalDataObject(ExternalDataObject externalDataObject) {
        return null;
    }

    /**
     * Search external data objects with an optional 'hint' String, which the provider will use to
     * help determine which filters or other URL parameters to apply to the query. This is a simple
     * way to give some additional context about what and why we are searching something, and could
     * be provided by the user, or a frontend component, etc.
     *
     * @param query the query text
     * @param hint a string which the provider will use to determine additional parameters
     * @param start start record / row
     * @param limit page size
     * @return
     */
    @Override
    public abstract List<ExternalDataObject> searchExternalDataObjects(String query, String hint, int start, int limit);

    /**
     * Get metadata maps by 'type', if present. See spring configuration for examples.
     * @return map of metadata types
     */
    @Override
    public Map<String, Map<String, MetadataFieldConfig>> getMetadataMapsByType() {
        return metadataMapsByType;
    }

    /**
     * Set metadata field maps (see external data spring configuration)
     * @param metadataFieldMapsByType
     */
    @Override
    public void setMetadataMapsByType(Map<String, Map<String, MetadataFieldConfig>> metadataFieldMapsByType) {
        this.metadataMapsByType = metadataFieldMapsByType;
    }
}