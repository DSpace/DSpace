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

    @Override
    public final void checkCollectionPath(StoragePath path) throws StorageException {
        final List<String> expectedKeyPath = Arrays.asList(Organization.ORGANIZATION_CODE);
        checkPath(path, expectedKeyPath);

    }

    @Override
    public final void checkObjectPath(StoragePath path) throws StorageException {
        final List<String> expectedKeyPath = Arrays.asList(Organization.ORGANIZATION_CODE, Manuscript.MANUSCRIPT_ID);
        checkPath(path, expectedKeyPath);
    }
}
