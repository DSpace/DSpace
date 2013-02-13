/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.dataonemn;

import nu.xom.Elements;
import nu.xom.Element;
import nu.xom.Attribute;

/**
 * @author Dan Leehr (dan.leehr@nescent.org)
 */
public class PackageInfo implements Constants {
    private String identifier;
    private String modificationDate;
    /* Two halves - first is XML metadata.  
     * Second is the Resource Map */
    private String xmlChecksum;
    private String xmlChecksumAlgo;
    private long xmlSize;
    
    private String resourceMapChecksum;
    private String resourceMapChecksumAlgo;
    private long resourceMapSize;
    
    public PackageInfo(String aIdentifier) {
	identifier = aIdentifier;
    }
    
    public String getNamespace() {
	return null;
    }

    /**
       Split this object into two separate elements -- one for the metadata and one for the resource map
    **/
    public Element[] createInfoElements() {    
	Element[] elements = new Element[2];
	
	// create objects and set identifiers
	String baseID = getIdentifier();
	ObjectInfo metadataElem = new ObjectInfo(baseID);
	ObjectInfo resourceMapElem = new ObjectInfo(baseID + "/d1rem");
        metadataElem.setObjectFormat(DRYAD_NAMESPACE);
        resourceMapElem.setObjectFormat(ORE_NAMESPACE);
        if(getXmlChecksum() != null && getXmlChecksumAlgo() != null) {
            metadataElem.setChecksum(getXmlChecksumAlgo(), getXmlChecksum());
        }
        if(getResourceMapChecksum() != null && getResourceMapChecksumAlgo() != null) {
            resourceMapElem.setChecksum(getResourceMapChecksumAlgo(), getResourceMapChecksum());
        }
	
        metadataElem.setLastModified(getModificationDate());
        resourceMapElem.setLastModified(getModificationDate());
	// modification date
	
	// size
	metadataElem.setSize(getXmlSize());
        resourceMapElem.setSize(getResourceMapSize());

        // fill out our results array
	elements[0] = metadataElem;
	elements[1] = resourceMapElem;
	
	return elements;
    }    

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the modificationDate
     */
    public String getModificationDate() {
        return modificationDate;
    }

    /**
     * @param modificationDate the modificationDate to set
     */
    public void setModificationDate(String modificationDate) {
        this.modificationDate = modificationDate;
    }

    /**
     * @return the xmlChecksum
     */
    public String getXmlChecksum() {
        return xmlChecksum;
    }

    /**
     * @param xmlChecksum the xmlChecksum to set
     */
    public void setXmlChecksum(String xmlChecksum) {
        this.xmlChecksum = xmlChecksum;
    }

    /**
     * @return the xmlChecksumAlgo
     */
    public String getXmlChecksumAlgo() {
        return xmlChecksumAlgo;
    }

    /**
     * @param xmlChecksumAlgo the xmlChecksumAlgo to set
     */
    public void setXmlChecksumAlgo(String xmlChecksumAlgo) {
        this.xmlChecksumAlgo = xmlChecksumAlgo;
    }

    /**
     * @return the xmlSize
     */
    public long getXmlSize() {
        return xmlSize;
    }

    /**
     * @param xmlSize the xmlSize to set
     */
    public void setXmlSize(long xmlSize) {
        this.xmlSize = xmlSize;
    }

    /**
     * @return the resourceMapChecksum
     */
    public String getResourceMapChecksum() {
        return resourceMapChecksum;
    }

    /**
     * @param resourceMapChecksum the resourceMapChecksum to set
     */
    public void setResourceMapChecksum(String resourceMapChecksum) {
        this.resourceMapChecksum = resourceMapChecksum;
    }

    /**
     * @return the resourceMapChecksumAlgo
     */
    public String getResourceMapChecksumAlgo() {
        return resourceMapChecksumAlgo;
    }

    /**
     * @param resourceMapChecksumAlgo the resourceMapChecksumAlgo to set
     */
    public void setResourceMapChecksumAlgo(String resourceMapChecksumAlgo) {
        this.resourceMapChecksumAlgo = resourceMapChecksumAlgo;
    }

    /**
     * @return the resourceMapSize
     */
    public long getResourceMapSize() {
        return resourceMapSize;
    }

    /**
     * @param resourceMapSize the resourceMapSize to set
     */
    public void setResourceMapSize(long resourceMapSize) {
        this.resourceMapSize = resourceMapSize;
    }
}
