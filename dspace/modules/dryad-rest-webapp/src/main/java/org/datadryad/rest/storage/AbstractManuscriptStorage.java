/*
 */
package org.datadryad.rest.storage;

import java.util.Arrays;
import java.util.List;
import org.datadryad.rest.models.Manuscript;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class AbstractManuscriptStorage extends AbstractStorage<Manuscript> {
    private static final String ORGANIZATION_KEY = "organizationCode";
    private static final String MANUSCRIPT_KEY = "manuscriptId";

    @Override
    public final void checkCollectionPath(StoragePath path) throws StorageException {
        final List<String> expectedKeyPath = Arrays.asList(ORGANIZATION_KEY);
        checkPath(path, expectedKeyPath);

    }

    public final void checkObjectPath(StoragePath path) throws StorageException {
        final List<String> expectedKeyPath = Arrays.asList(ORGANIZATION_KEY, MANUSCRIPT_KEY);
        checkPath(path, expectedKeyPath);
    }
}
