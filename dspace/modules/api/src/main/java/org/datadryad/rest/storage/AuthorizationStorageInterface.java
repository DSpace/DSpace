/*
 */
package org.datadryad.rest.storage;

import org.datadryad.rest.auth.AuthorizationTuple;

import javax.inject.Singleton;

/**
 * Interface for checking with storage if a request is authorized
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Singleton
public interface AuthorizationStorageInterface {
    public Boolean isAuthorized(AuthorizationTuple tuple) throws StorageException;
}
