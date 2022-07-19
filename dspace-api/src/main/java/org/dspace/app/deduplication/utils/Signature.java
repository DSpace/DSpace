/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.utils;

import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * Deduplication signature interface, for matching potential duplicate items
 *
 * @author 4Science
 */
public interface Signature {
    List<String> getSignature(Context context, DSpaceObject item);

    int getResourceTypeID();

    String getSignatureType();

    String getMetadata();

    List<String> getSearchSignature(Context context, DSpaceObject item);
}
