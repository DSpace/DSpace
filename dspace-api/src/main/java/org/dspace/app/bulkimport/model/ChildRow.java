/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.model;

/**
 * Interface that marks classes that model row that are children of a specific
 * instance of {@link EntityRow}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface ChildRow {

    /**
     * Returns the id of the parent entity row.
     *
     * @return the id of the parent
     */
    String getParentId();

}
