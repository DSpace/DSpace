/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

public interface ThrowingMapper<T, R, E extends Exception> {
    /**
     * Applies this function to the given input, possibly throwing a checked exception.
     *
     * @param t the input argument
     * @return the mapped result
     * @throws E if the mapping fails
     */
    R accept(T t) throws E;
}