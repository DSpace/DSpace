package org.dspace.dataonemn;

import nu.xom.Attribute;

public class SystemMetadata extends AbstractObject implements Constants {
	
	public SystemMetadata(String aIdentifier) {
		super("systemMetadata", SYS_META_NAMESPACE, aIdentifier);
		addAttribute(new Attribute("xsi:schemaLocation", XSI_NAMESPACE, XSD_LOCATION));
	}

	public String getNamespace() {
		return SYS_META_NAMESPACE;
	}
	
	public void setSubmitter(String aSubmitter) {
		addElement("submitter", aSubmitter);
	}
	
	public void setOrigin(String aMemberNode) {
		addElement("originMemberNode", aMemberNode);
	}
	
	public void setAuthoritative(String aMemberNode) {
		addElement("authoritativeMemberNode", aMemberNode);
	}
	
	public void setEmbargoExpires(String aExpirationDate) {
		addElement("embargoExpires", aExpirationDate);
	}
	
	public void setRightsHolder(String aRightsHolder) {
		addElement("rightsHolder", aRightsHolder);
	}
	
	public void setDescribes(String aDescribesRef) {
		addElement("describes", aDescribesRef);
	}
	
	public void setDescribedBy(String aDescribedByRef) {
		addElement("describedBy", aDescribedByRef);
	}
}
