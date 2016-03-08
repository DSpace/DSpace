/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import javax.xml.xpath.XPathExpression;

/**
 * @author kevinvandevelde at atmire.com
 *
 */
public class MetadataWebServiceDataInfo {
    private XPathExpression expr; // compiled XPath espression for data
	private String xpsrc;		// uncompiled XPath expression
	private String label;		// label for data in result string
    private String mapping;		// data mapping symbol: ->,=>,~>, or null = unmapped
    private String schema;		// item metadata field mapping target, null = unmapped
    private String element;		// item metadata field mapping target, null = unmapped
    private String qualifier;	// item metadata field mapping target, null = unmapped
    private String transform;	// optional transformation of data before field assignment

   	public MetadataWebServiceDataInfo(MetadataWebService metadataWebService, String xpsrc, String label, String mapping, String field) {
   		this.xpsrc = xpsrc;
   		this.label = label;
   		this.mapping = mapping;
   		if (field != null) {
   			String[] parsed = metadataWebService.parseTransform(field);
   			String[] parts = parsed[0].split("\\.");
   			this.schema = parts[0];
   			this.element = parts[1];
   			this.qualifier = (parts.length == 3) ? parts[2] : null;
   			this.transform = parsed[1];
   		}
   	}


    public XPathExpression getExpr() {
        return expr;
    }

    public String getXpsrc() {
        return xpsrc;
    }

    public String getLabel() {
        return label;
    }

    public String getMapping() {
        return mapping;
    }

    public String getSchema() {
        return schema;
    }

    public String getElement() {
        return element;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getTransform() {
        return transform;
    }

    public void setExpr(XPathExpression expr) {
        this.expr = expr;
    }
}
