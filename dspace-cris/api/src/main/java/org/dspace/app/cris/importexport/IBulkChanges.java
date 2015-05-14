package org.dspace.app.cris.importexport;


public interface IBulkChanges {

	boolean hasPropertyDefinition(String shortName);

	int size();

	IBulkChange getChanges(int i);

}
