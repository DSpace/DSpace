/*
 */
package org.datadryad.rest.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Package;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class AbstractPackageStorage extends AbstractStorage<Package> {
    @Override
    public final void checkCollectionPath(StoragePath path) throws StorageException {
        final List<String> expectedKeyPath = Arrays.asList(); // Collection is root
        checkPath(path, expectedKeyPath);

    }

    @Override
    public final void checkObjectPath(StoragePath path) throws StorageException {
        final List<String> expectedKeyPath = Arrays.asList(StoragePath.JOURNAL_PATH, StoragePath.MANUSCRIPT_PATH);
        checkPath(path, expectedKeyPath);
    }

    public List<Manuscript> getManuscriptsMatchingPath(StoragePath path, int limit) throws StorageException {
        return new ArrayList<Manuscript>();
    }
}
