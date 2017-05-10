/*
 */
package org.datadryad.rest.storage;

import org.datadryad.rest.models.OAuthToken;

import javax.inject.Singleton;

/**
 * Class for
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Singleton
public interface OAuthTokenStorageInterface {
    public OAuthToken getToken(String token) throws StorageException;
}
