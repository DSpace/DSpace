package org.dspace.app.cris.importexport;

import org.dspace.app.cris.util.UtilsXML;
import org.w3c.dom.Element;

public class XMLBulkFieldValue implements IBulkChangeFieldValue {
	private Element element;
	
	public XMLBulkFieldValue(Element element) {
		this.element = element;
	}

	@Override
	public String getValue() {
		return element.getTextContent();
	}
	
	@Override
	public String getVisibility() {
		return element
                .getAttribute(UtilsXML.NAMEATTRIBUTE_VISIBILITY);
	}
}
