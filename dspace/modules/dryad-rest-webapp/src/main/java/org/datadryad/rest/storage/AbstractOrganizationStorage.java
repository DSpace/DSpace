/*
 */
package org.datadryad.rest.storage;

import java.util.Arrays;
import java.util.List;
import org.datadryad.rest.models.Organization;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class AbstractOrganizationStorage extends AbstractStorage<Organization> {
    private static final String ORGANIZATION_KEY = "organizationCode";

    @Override
    public final void checkCollectionPath(StoragePath path) throws StorageException {
        final List<String> expectedKeyPath = Arrays.asList(); // Collection is root
        checkPath(path, expectedKeyPath);

    }

    public final void checkObjectPath(StoragePath path) throws StorageException {
        final List<String> expectedKeyPath = Arrays.asList(ORGANIZATION_KEY);
        checkPath(path, expectedKeyPath);
    }
}
