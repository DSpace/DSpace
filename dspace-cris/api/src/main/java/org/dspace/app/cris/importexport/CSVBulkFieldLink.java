/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
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
