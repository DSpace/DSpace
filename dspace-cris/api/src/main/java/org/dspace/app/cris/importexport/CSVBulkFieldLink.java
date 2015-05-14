package org.dspace.app.cris.importexport;

import jxl.Cell;

public class CSVBulkFieldLink extends CSVBulkField implements IBulkChangeFieldLink {
	
	public CSVBulkFieldLink(Cell element) {
		super(element);
	}

	@Override
	public IBulkChangeFieldLinkValue get(int y) {
		return new CSVBulkFieldLinkValue(getElement(), y);
	}
	
}
