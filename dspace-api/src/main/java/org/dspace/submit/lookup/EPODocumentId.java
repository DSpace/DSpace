/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import org.dspace.app.util.XMLUtils;
import org.w3c.dom.Element;

/***
 * Document identifier in EPO.
 * 
 * Format supported "docdb" and "epodoc".
 * 
 * {@code
 * <document-id document-id-type="docdb">
 *  <country>AT</country>
 *  <doc-number>232441</doc-number>
 *  <kind>T</kind>
 *  <date>20030215</date>
 * </document-id>
 * 
 * <document-id document-id-type="epodoc">
 *    <doc-number>AT232441T</doc-number>
 *    <date>20030215</date>
 * </document-id>
 * }
 * 
 * @author fcadili (franceso.cadili at 4science.it)
 */
public class EPODocumentId {
    private String documentIdType;
    private String country;
    private String docNumber;
    private String kind;
    private String date;

    public static String DOCDB = "docdb";
    public static String EPODOC = "epodoc";
    public static String ORIGIN = "origin";

    public EPODocumentId(Element documentId) {
        documentIdType = documentId.getAttribute("document-id-type");
        country = XMLUtils.getElementValue(documentId, "country");
        docNumber = XMLUtils.getElementValue(documentId, "doc-number");
        kind = XMLUtils.getElementValue(documentId, "kind");
        date = XMLUtils.getElementValue(documentId, "date");
    }

    public String getDocumentIdType() {
        return documentIdType;
    }

    public String getCountry() {
        return country;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public String getKind() {
        return kind;
    }

    public String getDate() {
        return date;
    }

    public String getId() {
        if (DOCDB.equals(documentIdType)) {
            return country + "." + docNumber + "." + kind;

        } else if (EPODOC.equals(documentIdType)) {
            return docNumber + ((kind != null) ? kind : "");
        } else {
            return "";
        }
    }

    @Override
    public String toString() {
        return "country: " + ((country != null) ? country : "") + ", docnumber: "
                + ((docNumber != null) ? docNumber : "") + ", kind: " + ((kind != null) ? kind : "") + ", date: "
                + ((date != null) ? date : "");
    }
}
