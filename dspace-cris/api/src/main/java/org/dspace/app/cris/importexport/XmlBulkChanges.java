package org.dspace.app.cris.importexport;

import it.cilea.osd.common.utils.XMLUtils;

import java.util.List;

import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XmlBulkChanges implements IBulkChanges {
	private Document document;
	private List<Element> elementsChange; 
	
	public XmlBulkChanges(Document document) {
		this.document = document;
	}
	
	@Override
	public boolean hasPropertyDefinition(String shortName) {

		NodeList e = document.getElementsByTagName(shortName);
        if (e != null && e.getLength() > 0)
        {
        	return true;
        }
		return false;
	}
	
	@Override
	public int size() {
		initElements();
		return elementsChange.size();
	}
	
	private void initElements() {
		if (elementsChange == null) {
			elementsChange = XMLUtils.getElementList(
	                document.getDocumentElement(), "researcher");
		}
	}
	@Override
	public IBulkChange getChanges(int i) {
			initElements();
			return new XmlBulkChange(elementsChange.get(i));
	}
}
