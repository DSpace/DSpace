/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.importexport;

public interface IBulkChange {

	public static String ACTION_HIDE = "HIDE";
	public static String ACTION_SHOW = "SHOW";
	public static String ACTION_DELETE = "DELETE";
	public static String ACTION_CREATE = "CREATE";
	public static String ACTION_UPDATE = "UPDATE";
	
	String getSourceID();

	String getSourceRef();

	String getCrisID();

	String getUUID();

	String getAction();

	IBulkChangeField getFieldChanges(String field);

	IBulkChangeFieldFile getFieldFileChanges(String shortName);
	   
	IBulkChangeFieldLink getFieldLinkChanges(String shortName);
	
	IBulkChangeFieldPointer getFieldPointerChanges(String field);
	
	boolean isANestedBulkChange();
}
