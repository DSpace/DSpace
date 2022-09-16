/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider;
import java.util.List;
import java.util.Objects;

/**
 * This abstract class allows to configure the list of supported entity types
 * via spring. If no entity types are explicitly configured it is assumed that
 * the provider can be used with any entity type
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public abstract class AbstractExternalDataProvider implements ExternalDataProvider {

    private List<String> supportedEntityTypes;

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

}