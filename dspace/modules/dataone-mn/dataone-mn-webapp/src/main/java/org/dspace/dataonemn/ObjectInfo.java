package org.dspace.dataonemn;

import nu.xom.Elements;
import nu.xom.Element;

public class ObjectInfo extends AbstractObject implements Constants {

    // We don't store these in DSpace so we have to set them here on the fly
    private String myXMLChecksum;
    private String myChecksumAlgo;
    private long myXMLSize;
    
    // Not used for XML generation, but for appending to the identifiers
    private String myFormatExtension;
    
    public ObjectInfo(String aIdentifier) {
	super("objectInfo", aIdentifier);
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

    
    public void setFormatExtension(String aExtension) {
	myFormatExtension = aExtension;
    }
    
    /**
       Split this object into two separate elements -- one for the metadata and one for the primary bitstream.
    **/
    public Element[] createInfoElements() {    
	Element[] elements = new Element[2];

	// create objects and set identifiers
	Element thisID = getChildElements("identifier").get(0);
	String baseID = thisID.getValue();
	ObjectInfo metadataElem = new ObjectInfo(baseID + "/dap");
	ObjectInfo bitstreamElem = new ObjectInfo(baseID + "/" + myFormatExtension);
	
	// formats
	Element thisFormat = getChildElements("formatId").get(0);
	String format = thisFormat.getValue();
	metadataElem.setObjectFormat(DRYAD_NAMESPACE);
	bitstreamElem.setObjectFormat(format);
	
	// checksum
	if (myXMLChecksum != null && myChecksumAlgo != null) {
	    metadataElem.setChecksum(myChecksumAlgo, myXMLChecksum);
	}
	bitstreamElem.appendChild((Element)this.getChildElements("checksum").get(0).copy());
	
	// modification date
	Element thisModDate = this.getChildElements("dateSysMetadataModified").get(0);
	metadataElem.appendChild((Element)thisModDate.copy());
	bitstreamElem.appendChild((Element)thisModDate.copy());
	
	// size
	metadataElem.addElement("size", "" + myXMLSize);
	bitstreamElem.appendChild((Element)this.getChildElements("size").get(0).copy());
	
	
	    // fill out our results array
	    elements[0] = metadataElem;
	    elements[1] = bitstreamElem;
	    
	    return elements;
	}
}
