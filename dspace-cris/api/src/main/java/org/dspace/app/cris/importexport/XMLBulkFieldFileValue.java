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

public class XMLBulkFieldFileValue extends XMLBulkFieldValue implements
		IBulkChangeFieldFileValue {
	private Element element;

	public XMLBulkFieldFileValue(Element element) {
		super(element);
		this.element = element;
	}

	@Override
	public boolean isLocal() {
		return Boolean.parseBoolean(element.getAttribute(UtilsXML.NAMEATTRIBUTE_LOCAL_FILE));
	}

    @Override
    public boolean isDelete()
    {
        return Boolean.parseBoolean(element.getAttribute(UtilsXML.NAMEATTRIBUTE_DELETE_FILE));
    }
}
