/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
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
