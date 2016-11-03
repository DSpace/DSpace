/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.deduplication.utils;

import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public interface Signature {
	public List<String> getSignature(DSpaceObject item, Context context);
	public int getResourceTypeID();
	public String getSignatureType();
	public String getMetadata();
}
