/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external;

import org.dspace.importer.external.service.other.MetadataSource;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 27 Oct 2014
 */
public abstract interface SourceExceptionHandler<T extends MetadataSource> {

    public abstract void handle(T source);

}
