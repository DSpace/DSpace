/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.ezid;

/**
 * Convert metadata strings to other forms.
 *
 * @author mwood
 */
public interface Transform
{
    /**
     * Convert the input form to the desired output form.
     * @param from
     *     input form to transform from
     * @return transformed output form
     * @throws Exception
     *     A general exception
     */
    public String transform(String from)
            throws Exception;
}
