/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.model.generator;

/**
 * Interface for iiif value generators.
 */
public interface IIIFValue {

    /**
     * creates and returns a value model.
     * @return a value model.
     */
    Object generateValue();
}
