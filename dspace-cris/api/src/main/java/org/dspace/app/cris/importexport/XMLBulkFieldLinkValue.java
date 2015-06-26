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

public class XMLBulkFieldLinkValue extends XMLBulkFieldValue implements
		IBulkChangeFieldLinkValue {
	private Element element;

	public XMLBulkFieldLinkValue(Element element) {
		super(element);
		this.element = element;
	}

	@Override
	public String getLinkURL() {
		return element.getAttribute(UtilsXML.NAMEATTRIBUTE_SRC_LINK);
	}
}
