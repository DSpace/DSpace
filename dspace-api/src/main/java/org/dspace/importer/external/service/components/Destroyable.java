/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.service.components;

/**
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public interface Destroyable {

    /**
     * Destroy the object
     *
     * @throws Exception on generic exception
     */
    public void destroy() throws Exception;
}
