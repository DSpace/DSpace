/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.exception;

import org.dspace.importer.external.service.components.AbstractRemoteMetadataSource;

/**
 * Represent a  handler that forces implementations to define their own behaviour for exceptions originating from
 * @author Antoine Snyers (antoine at atmire dot com)
 */
public abstract interface SourceExceptionHandler<T extends AbstractRemoteMetadataSource> {

    /**
     * Represents a method contract to handle Exceptions originating from the source in a specific way
     * Implementations define their own desired behaviour
     * @param   source   The source of the exception
     */
    public abstract void handle(T source);

}
