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
    public final void checkFindParameters(String fields[], String values[]) throws StorageException {
        // field and value should both be length 1
        if(fields == null || fields.length == 0) {
            throw new StorageException("Empty field");
        } else if(values == null || values.length == 0) {
            throw new StorageException("Empty values");
        } else if(fields.length != 1 || values.length != 1) {
            throw new StorageException("Path length should be 1");
        } else if(!fields[0].equalsIgnoreCase(SEARCH_FIELD)) {
            throw new StorageException("Unsupported path" + fields[0]);
        } else if(values[0] == null || values[0].length() == 0) {
            throw new StorageException("Empty value");
        }
    }
}
