/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

/**
 * Functional interface that can be used to returns an object and potentially
 * throws a Exception.
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> {

    /**
     * Returns an object.
     *
     * @return   an object
     * @throws E if some error occurs
     */
    T get() throws E;
}
