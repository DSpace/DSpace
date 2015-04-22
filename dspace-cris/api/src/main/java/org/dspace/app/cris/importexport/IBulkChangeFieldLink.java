package org.dspace.app.cris.importexport;

import org.dspace.app.cris.util.IBulkChangeFieldLinkValue;

public interface IBulkChangeFieldLink extends IBulkChangeField {
	@Override
	public IBulkChangeFieldLinkValue get(int y);
}
