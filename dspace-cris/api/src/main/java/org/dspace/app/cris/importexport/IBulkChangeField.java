package org.dspace.app.cris.importexport;


public interface IBulkChangeField {
	public int size();

	public IBulkChangeFieldValue get(int y);
}
