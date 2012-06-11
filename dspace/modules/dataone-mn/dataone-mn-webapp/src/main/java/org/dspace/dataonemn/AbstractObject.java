package org.dspace.dataonemn;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;

public abstract class AbstractObject extends Element {

	protected AbstractObject(String aName, String aNamespace, String aIdentifier) {
	    super(aName, aNamespace); 
	    addElement("identifier", aIdentifier);
	}

	protected AbstractObject(String aName, String aIdentifier) {
	    super(aName); 
	    addElement("identifier", aIdentifier);
	}
    
	public void setObjectFormat(String aFormat) {
		addElement("formatId", aFormat);
	}
	
	public void setSize(int aSize) {
		addElement("size", Integer.toString(aSize));
	}
	
	public void setChecksum(String aAlgorithm, String aHash) {
		addElement("checksum", aHash).addAttribute(
				new Attribute("algorithm", aAlgorithm));
	}
	
	public void setLastModified(String aLastModified) {
		addElement("dateSysMetadataModified", aLastModified);
	}

	public void setDateUploaded(String aCreatedDate) {
		addElement("dateUploaded", aCreatedDate);
	}
	
	public void setSize(long aSize) {
		addElement("size", Long.toString(aSize));
	}
	
	public int getSize() {
		try {
			Elements elements = getChildElements("size", getNamespace());
			
			if (elements.size() < 1) {
				return -1;
			}
			
			return Integer.parseInt(elements.get(0).getValue());
		}
		catch (NumberFormatException details) {
			return -1;
		}
	}
	
	protected abstract String getNamespace();
	
	protected Element addElement(String aName, String aValue) {
		Element element = new Element(aName, getNamespace());
		element.appendChild(aValue);
		appendChild(element);
		return element;
	}
	
}
