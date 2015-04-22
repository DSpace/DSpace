package org.dspace.app.cris.util;

import org.dspace.app.cris.importexport.IBulkChangeFieldValue;

public interface IBulkChangeFieldLinkValue extends IBulkChangeFieldValue {
	public String getLinkURL();
}
