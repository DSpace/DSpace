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
