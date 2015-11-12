/*
 */
package org.datadryad.rest.storage;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class StorageException extends Exception {

    public StorageException() {
        super();
    }

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public StorageException(Throwable throwable) {
        super(throwable);
    }
}
