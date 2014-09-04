/*
 */
package org.datadryad.rest.storage;

import java.util.Arrays;
import java.util.List;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Organization;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class AbstractManuscriptStorage extends AbstractStorage<Manuscript> {
    private static final String MANUSCRIPT_KEY = "manuscriptId";

    @Override
    public final void checkCollectionPath(StoragePath path) throws StorageException {
        final List<String> expectedKeyPath = Arrays.asList(Organization.ORGANIZATION_CODE);
        checkPath(path, expectedKeyPath);

    }

    public final void checkObjectPath(StoragePath path) throws StorageException {
        final List<String> expectedKeyPath = Arrays.asList(Organization.ORGANIZATION_CODE, MANUSCRIPT_KEY);
        checkPath(path, expectedKeyPath);
    }
}
