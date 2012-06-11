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
	    super("objectInfo", aIdentifier);
	}

	public String getNamespace() {
		return null;
	}

	public void setXMLChecksum(String aChecksum, String aAlgorithm) {
		myXMLChecksum = aChecksum;
		myChecksumAlgo = aAlgorithm;
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
	    // the size doesn't apply to the dap metadata, so it is only added to the bitstreamElem
	    bitstreamElem.appendChild((Element)this.getChildElements("size").get(0).copy());
	    
	    
	    // fill out our results array
	    elements[0] = metadataElem;
	    elements[1] = bitstreamElem;
	    
	    return elements;
	}
}
