package org.dspace.app.cris.importexport;


public interface IBulkChangeFieldLink extends IBulkChangeField {
	@Override
	public IBulkChangeFieldLinkValue get(int y);
}
