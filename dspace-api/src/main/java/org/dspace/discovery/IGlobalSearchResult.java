package org.dspace.discovery;

import java.util.List;

import org.dspace.content.DCValue;

public interface IGlobalSearchResult {

	public String getHandle();
	
	public List<String> getMetadataValue(String mdString);
	public DCValue[] getMetadataValueInDCFormat(String mdString);
	
	public String getTypeText();
	
	public int getType();
	
	public int getID();
	
	public boolean isWithdrawn();

}
