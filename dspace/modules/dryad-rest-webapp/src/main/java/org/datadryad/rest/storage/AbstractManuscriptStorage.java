/*
 */
package org.datadryad.rest.storage;

import org.datadryad.rest.models.Manuscript;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class AbstractManuscriptStorage extends AbstractStorage<Manuscript> {
    private static final String ORGANIZATION_SEARCH_FIELD = "organizationCode";
    private static final String MANUSCRIPT_SEARCH_FIELD = "manuscriptId";
    @Override
    public final void checkFindParameters(String fields[], String values[]) throws StorageException {
        // field and value should both be length 1
        if(fields == null || fields.length == 0) {
            throw new StorageException("Empty field");
        } else if(values == null || values.length == 0) {
            throw new StorageException("Empty values");
        } else if(fields.length != 2 || values.length != 2) {
            throw new StorageException("Path length should be 2");
        } else if(!fields[0].equalsIgnoreCase(ORGANIZATION_SEARCH_FIELD)) {
            throw new StorageException("Unsupported path" + fields[0]);
        } else if(!fields[1].equalsIgnoreCase(MANUSCRIPT_SEARCH_FIELD)) {
            throw new StorageException("Unsupported path" + fields[1]);
        } else if(values[0] == null || values[0].length() == 0) {
            throw new StorageException("Empty value");
        } else if(values[1] == null || values[1].length() == 0) {
            throw new StorageException("Empty value");
        }
    }
}
