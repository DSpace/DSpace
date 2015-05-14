package org.dspace.app.cris.importexport;

import java.util.List;

import org.w3c.dom.Element;

public class XMLBulkField implements IBulkChangeField {

	private List<Element> elementList;
	
	public XMLBulkField(List<Element> elementList) {
		this.elementList = elementList;
	}

	@Override
	public int size() {
		return elementList.size();
	}
	
	@Override
	public IBulkChangeFieldValue get(int y) {
		return new XMLBulkFieldValue(elementList.get(y));
	}

}
