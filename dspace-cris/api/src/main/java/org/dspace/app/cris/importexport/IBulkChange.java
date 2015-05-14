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

	IBulkChangeFieldLink getFieldLinkChanges(String shortName);
}
