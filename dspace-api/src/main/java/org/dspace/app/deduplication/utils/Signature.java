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

public interface Signature {
    public List<String> getSignature(/* BrowsableDSpaceObject */DSpaceObject item, Context context);

    public int getResourceTypeID();

    public String getSignatureType();

    public String getMetadata();

    public List<String> getSearchSignature(DSpaceObject item, Context context);
}
