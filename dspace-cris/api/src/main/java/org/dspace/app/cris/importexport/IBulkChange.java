package org.dspace.app.cris.importexport;

public interface IBulkChange {

	String getSourceID();

	String getSourceRef();

	String getCrisID();

	String getUUID();

	String getAction();

	IBulkChangeField getFieldChanges(String field);

	IBulkChangeFieldLink getFieldLinkChanges(String shortName);
}
