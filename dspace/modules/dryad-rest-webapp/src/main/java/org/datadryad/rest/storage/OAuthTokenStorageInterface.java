/*
 */
package org.datadryad.rest.storage;

import org.datadryad.rest.models.OAuthToken;

/**
 * Class for
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public interface OAuthTokenStorageInterface {
    public OAuthToken getToken(String token) throws StorageException;
}
