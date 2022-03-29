/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.model.generator;

import de.digitalcollections.iiif.model.sharedcanvas.Resource;

/**
 * Interface for iiif resource generators.
 */
public interface IIIFResource {

    /**
     * Creates and returns a resource model.
     * @return resource model
     */
    Resource<?> generateResource();

}
