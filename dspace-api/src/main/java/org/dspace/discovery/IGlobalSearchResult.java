/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.List;

import org.dspace.content.Metadatum;

public interface IGlobalSearchResult {

	public String getHandle();
	
	public List<String> getMetadataValue(String mdString);
	public Metadatum[] getMetadataValueInDCFormat(String mdString);
	
	public String getTypeText();
	
	public int getType();
	
	public int getID();
	
	public boolean isWithdrawn();

}
