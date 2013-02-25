package org.dspace.dataonemn;

import nu.xom.Elements;
import nu.xom.Element;
import nu.xom.Attribute;

public class ObjectInfo extends Element implements Constants {

    private String myXMLChecksum;
    private String myChecksumAlgo;
    private long myXMLSize;
    private String identifier;
    private String idTimestamp;
      
    public ObjectInfo(String identifier, String idTimestamp) {
	super("objectInfo"); 
	addElement("identifier", identifier + idTimestamp);
	this.identifier = identifier;
	this.idTimestamp = idTimestamp;
    }
    
    public String getNamespace() {
	return null;
    }
    
    /**
       Sets the checksum for the XML (metadata) representation of this object.
    **/
    public void setXMLChecksum(String aChecksum, String aAlgorithm) {
	myXMLChecksum = aChecksum;
	myChecksumAlgo = aAlgorithm;
    }

    
    /**
       Sets the size for the XML (metadata) representation of this object.
    **/
    public void setXMLSize(long aSize) {
	myXMLSize = aSize;
    }

    
    /**
       Split this object into two separate elements -- one for the metadata and one for the primary bitstream.
    **/
    public Element[] createInfoElements() {    
	Element[] elements = new Element[2];
	
	// create objects and set identifiers
	// bitstreams never receive timestamps, because they are explicitly versioned
	ObjectInfo metadataElem = new ObjectInfo(identifier, idTimestamp);
	ObjectInfo bitstreamElem = new ObjectInfo(identifier + "/bitstream", "");
	
	// formats
	Element thisFormat = getChildElements("formatId").get(0);
	String format = thisFormat.getValue();
	metadataElem.setObjectFormat(DRYAD_NAMESPACE);
	bitstreamElem.setObjectFormat(format);
	
	// checksum
	if (myXMLChecksum != null && myChecksumAlgo != null) {
	    metadataElem.setChecksum(myChecksumAlgo, myXMLChecksum);
	}
        if(this.getChildElements("checksum").size() > 0) {
            bitstreamElem.appendChild((Element)this.getChildElements("checksum").get(0).copy());
        } else {
            // checksum is required
            bitstreamElem.setChecksum(myChecksumAlgo, "");
        }
	
	// modification date
	Element thisModDate = this.getChildElements("dateSysMetadataModified").get(0);
	metadataElem.appendChild((Element)thisModDate.copy());
	bitstreamElem.appendChild((Element)thisModDate.copy());
	
	// size
	metadataElem.addElement("size", "" + myXMLSize);
        if(this.getChildElements("size").size() > 0) {
            bitstreamElem.appendChild((Element)this.getChildElements("size").get(0).copy());
        } else {
            // size is required
            bitstreamElem.setSize(0);
        }
	
	
	// fill out our results array
	elements[0] = metadataElem;
	elements[1] = bitstreamElem;
	
	return elements;
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

    public void setLastModified(String aModDate) {
	addElement("dateSysMetadataModified", aModDate);
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
	} catch (NumberFormatException details) {
	    return -1;
	}
    }
    
    
    /**
       Adds a new element as a child of this element. The added element will be appended
       to the list of children, so it will always occur at the end.
    **/
    private Element addElement(String aName, String aValue) {
	Element element = new Element(aName, getNamespace());
	element.appendChild(aValue);
	appendChild(element);
	return element;
    }
    
}
