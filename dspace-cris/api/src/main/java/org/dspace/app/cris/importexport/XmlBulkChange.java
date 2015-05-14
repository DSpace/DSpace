package org.dspace.app.cris.importexport;

import it.cilea.osd.common.utils.XMLUtils;

import org.dspace.app.cris.util.UtilsXML;
import org.w3c.dom.Element;

public class XmlBulkChange implements IBulkChange {
	private Element node;

	public XmlBulkChange(Element element) {
		this.node = element;
	}

	@Override
	public String getSourceID() {
		return node.getAttribute(UtilsXML.NAMEATTRIBUTE_SOURCEID);
	}

	@Override
	public String getSourceRef() {
		return node.getAttribute(UtilsXML.NAMEATTRIBUTE_SOURCEREF);
	}

	@Override
	public String getCrisID() {
		return node.getAttribute(UtilsXML.NAMEATTRIBUTE_CRISID);
	}

	@Override
	public String getUUID() {
		return node.getAttribute(UtilsXML.NAMEATTRIBUTE_UUID);
	}
	
	@Override
	public String getAction() {
		return "update";
	}
	
	@Override
	public IBulkChangeField getFieldChanges(String field) {
		return new XMLBulkField(XMLUtils.getElementList(node,
				 field));
	}
	
	@Override
	public IBulkChangeFieldLink getFieldLinkChanges(String field) {
		return new XmlBulkFieldLink(XMLUtils.getElementList(node,
				 field));
	}
}
