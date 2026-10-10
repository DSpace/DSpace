/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

public interface ThrowingConsumer<T, E extends Exception> {
    /**
     * Performs this operation on the given input, possibly throwing a checked exception.
     *
     * @param t the input argument
     * @throws E if the operation fails
     */
    void accept(T t) throws E;
}