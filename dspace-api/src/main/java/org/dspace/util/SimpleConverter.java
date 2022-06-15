/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

/**
 * Interface for classes that allows to convert values or map them.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public interface SimpleConverter {

    /**
     * This method return a value that map the provided key
     * 
     * @param key
     * @return a value that map the key
     */
    public String getValue(String key);

}