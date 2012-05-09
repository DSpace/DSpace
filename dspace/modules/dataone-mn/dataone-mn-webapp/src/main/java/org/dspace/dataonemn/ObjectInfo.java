package org.dspace.dataonemn;

import nu.xom.Elements;
import nu.xom.Element;

public class ObjectInfo extends AbstractObject implements Constants {

	// We don't store these in DSpace so we have to set them here on the fly
	private String myXMLChecksum;
	private String myChecksumAlgo;
	
	// Not used for XML generation, but for appending to the identifiers
	private String myFormatExtension;

	public ObjectInfo(String aIdentifier) {
		super("objectInfo", LIST_OBJECTS_NAMESPACE, aIdentifier);
	}

	public String getNamespace() {
		return LIST_OBJECTS_NAMESPACE;
	}

	private Element insertElement(String aName, String aValue) {
		Element element = new Element(aName, LIST_OBJECTS_NAMESPACE);
		element.appendChild(aValue);
		insertChild(element, 0);
		return element;
	}

	public void setXMLChecksum(String aChecksum, String aAlgorithm) {
		myXMLChecksum = aChecksum;
		myChecksumAlgo = aAlgorithm;
	}

	public void setFormatExtension(String aExtension) {
		myFormatExtension = aExtension;
	}
	
	public Element[] split() {
		Element[] elements = new Element[2];
		Element thisID = getChildElements("identifier", LIST_OBJECTS_NAMESPACE)
				.get(0);
		Element thisFormat = getChildElements("objectFormat",
				LIST_OBJECTS_NAMESPACE).get(0);
		String baseID = thisID.getValue();
		String format = thisFormat.getValue();
		Element copy;

		removeChild(thisFormat);
		removeChild(thisID);
		copy = (Element) copy();

		insertElement("objectFormat", DRYAD_NAMESPACE);
		Element newFormat = new Element("objectFormat", LIST_OBJECTS_NAMESPACE);
		newFormat.appendChild(format);
		copy.insertChild(newFormat, 0);


		Elements checksumChildren = getChildElements("checksum", LIST_OBJECTS_NAMESPACE);
		if(checksumChildren != null && checksumChildren.size() > 0) {
		    removeChild(checksumChildren.get(0));
		}

		if (myXMLChecksum != null && myChecksumAlgo != null) {
			setChecksum(myChecksumAlgo, myXMLChecksum);
		}

		// this don't apply to the dap metadata, so remove (it's on copy)
		removeChild(getChildElements("size", LIST_OBJECTS_NAMESPACE).get(0));

		insertElement("identifier", baseID + "/dap");
		Element newID = new Element("identifier", LIST_OBJECTS_NAMESPACE);
		newID.appendChild(baseID + "/" + myFormatExtension);
		copy.insertChild(newID, 0);

		// fill out our results array
		elements[0] = this;
		elements[1] = copy;

		return elements;
	}
}
