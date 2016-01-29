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

public class XMLBulkFieldPointerValue extends XMLBulkFieldValue implements
		IBulkChangeFieldPointerValue {
	private Element element;

	public XMLBulkFieldPointerValue(Element element) {
		super(element);
		this.element = element;
	}

	@Override
	public String getCrisID() {
		return element.getAttribute(UtilsXML.NAMEATTRIBUTE_CRISID);
	}

    @Override
    public String getSourceRef()
    {
        return element.getAttribute(UtilsXML.NAMEATTRIBUTE_SOURCEREF);
    }

    @Override
    public String getSourceID()
    {
        return element.getAttribute(UtilsXML.NAMEATTRIBUTE_SOURCEID);
    }
    
    @Override
    public String getUuid()
    {
        return element.getAttribute(UtilsXML.NAMEATTRIBUTE_UUID);
    }
}
