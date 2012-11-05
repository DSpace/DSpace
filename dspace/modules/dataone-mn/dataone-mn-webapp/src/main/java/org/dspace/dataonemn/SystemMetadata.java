package org.dspace.dataonemn;

import org.apache.log4j.Logger;

import nu.xom.Attribute;
import nu.xom.Element;

public class SystemMetadata extends Element implements Constants {
    

    private String submitter = null;
    private String originMN = null;
    private String authMN = null;
    private String embargoExpires = null; 
    private String rightsHolder = null;
    private String describes = null;
    private String describedBy = null;
    private String size = null;
    private String identifier = null;
    private String myChecksum = null;
    private String myChecksumAlgo = null;
    private String modDate = null;
    private String format =null;
    private String dateUploaded =null;
    
    public SystemMetadata(String aIdentifier) {
	super("d1:systemMetadata", D1_TYPES_NAMESPACE);
	identifier = aIdentifier;
    }

    public String getNamespace() {
	return null;
    }
    
    public void setSubmitter(String aSubmitter) {
	submitter = aSubmitter;
    }
    
    public void setOrigin(String aMemberNode) {
	originMN = aMemberNode;
    }
	
    public void setAuthoritative(String aMemberNode) {
	authMN = aMemberNode;
    }
	
    public void setEmbargoExpires(String aExpirationDate) {
	embargoExpires = aExpirationDate;
    }
	
    public void setRightsHolder(String aRightsHolder) {
	rightsHolder = aRightsHolder;
    }
	
	
    public void setSize(String aSize) {
	size = aSize;
    }

    public void setSize(long aSize) {
	size = "" + aSize;
    }
	
    public void setDescribes(String aDescribesRef) {
	describes = aDescribesRef;
    }
	
    public void setDescribedBy(String aDescribedByRef) {
	describedBy = aDescribedByRef;
    }

    public void setDateUploaded(String aDateUploaded) {
	dateUploaded = aDateUploaded;
    }

    public void setLastModified(String aModDate) {
	modDate = aModDate;
    }

    /**
       Sets the checksum for the XML (metadata) representation of this object.
    **/
    public void setChecksum(String aChecksum, String aAlgorithm) {
	myChecksum = aChecksum;
	myChecksumAlgo = aAlgorithm;
    }

    public void setObjectFormat(String aFormat) {
	format = aFormat;
    }
    

    
    /**
       Prepare this object for serialization. Takes all of the previously-submitted settings, and
       creates them as XML elements.
    **/
    public void formatOutput() {
	addAttribute(new Attribute("xsi:schemaLocation", XSI_NAMESPACE, XSD_LOCATION));
	
	// create the body of the xml in the correct order
	addElement("serialVersion", "1");
	addElement("identifier", identifier);
	addElement("formatId", format);
	addElement("size", size);
	addElement("checksum", myChecksum).addAttribute(
						   new Attribute("algorithm", myChecksumAlgo));
	
	addElement("submitter", submitter);
	addElement("rightsHolder", rightsHolder);

	// access policy currently hardcoded; does not take embargos into account
	Element accessPolicyElement = new Element("accessPolicy", getNamespace());
	Element allowElement = new Element("allow", getNamespace());
	Element publicElement = new Element("subject",getNamespace());
	publicElement.appendChild("public");
	Element permissionElement = new Element("permission",getNamespace());
	permissionElement.appendChild("read");
	allowElement.appendChild(publicElement);
	allowElement.appendChild(permissionElement);
	accessPolicyElement.appendChild(allowElement);
	appendChild(accessPolicyElement);

	addElement("dateUploaded", dateUploaded);
	addElement("dateSysMetadataModified", modDate);
	addElement("originMemberNode", originMN);
	addElement("authoritativeMemberNode", authMN);
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
