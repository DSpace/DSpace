package org.dspace.authority.orcid;

/** 
 * Based on works provided by TomDemeranville
 * 
 * https://github.com/TomDemeranville/orcid-java-client
 *
 * @author l.pascarelli
 * @author tom
 */
public enum OrcidExternalIdentifierType {
	OTHER_ID("other-id"),
	ARXIV("arxiv"),
	ASIN("asin"),
	ASIN_TLD("sin-tld"),
	BIBCODE("bibcode"),
	DOI("doi"),
	EID("eid"),
	ISBN("isbn"),
	ISSN("issn"),
	JFM("jfm"),
	JSTOR("jstor"),
	LCCN("lccn"),
	MR("mr"),
	OCLC("oclc"),
	OL("ol"),
	OSTI("osti"),
	PMC("pmc"),
	PMID("pmid"),
	RFC("rfc"),
	SSRN("ssrn"),
	ZBL("zbl");
	
	private final String stringValue;
	private OrcidExternalIdentifierType(final String s) { stringValue = s; }
	public String toString() { return stringValue; }
	
	public static OrcidExternalIdentifierType fromString(String text) {
	    if (text != null) {
	      for (OrcidExternalIdentifierType b : OrcidExternalIdentifierType.values()) {
	        if (text.equals(b.toString())) {
	          return b;
	        }
	      }
	    }
	    throw new IllegalArgumentException("Invalid identifier type");
	  }

}
