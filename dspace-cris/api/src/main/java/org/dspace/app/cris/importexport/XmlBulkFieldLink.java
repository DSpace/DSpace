package org.dspace.app.cris.importexport;

import java.util.List;

import org.w3c.dom.Element;

public class XmlBulkFieldLink implements IBulkChangeFieldLink {
	private List<Element> elementList;
	
	public XmlBulkFieldLink(List<Element> elementList) {
		this.elementList = elementList;
	}

	@Override
	public int size() {
		return elementList.size();
	}
	
	@Override
	public IBulkChangeFieldLinkValue get(int y) {
		return new XMLBulkFieldLinkValue(elementList.get(y));
	}
}
