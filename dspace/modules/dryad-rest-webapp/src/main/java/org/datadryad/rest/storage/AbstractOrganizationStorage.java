/*
 */
package org.datadryad.rest.storage;

import org.datadryad.rest.models.Organization;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class AbstractOrganizationStorage extends AbstractStorage<Organization> {
    private static final String SEARCH_FIELD = "organizationCode";
    @Override
    public final void checkFindParameters(String field, String value) throws StorageException {
        if(field == null) {
            throw new StorageException("Empty field");
        } else if(!field.equalsIgnoreCase(SEARCH_FIELD)) {
            throw new StorageException("Unsupported search field" + field);
        } else if(value == null || value.length() == 0) {
            throw new StorageException("Empty value");
        }
    }
}
