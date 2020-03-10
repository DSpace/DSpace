/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.util.List;

import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;

import org.dspace.app.util.XMLUtils;
import org.dspace.submit.util.SubmissionLookupPublication;
import org.w3c.dom.Element;

/***
 * Class used to convert the dom elements to Records.
 * 
 * @See gr.ekt.bte.core.Record
 * 
 * @author fcadili (franceso.cadili at 4science.it)
 */
public final class EPOUtils {

    /**
     * Default constructor
     */
    private EPOUtils() {
    }

    private static EPODocumentId getDocumentNumber(Element biblographicData, String section, String[] formats) {

        Element pubblicationRef = XMLUtils.getSingleElement(biblographicData, section);
        List<Element> documentIds = XMLUtils.getElementList(pubblicationRef, "document-id");
        EPOElementHolder holder = new EPOElementHolder(formats, documentIds, "document-id-type");
        Element documentId = holder.get();

        EPODocumentId epoDocumentId = new EPODocumentId(documentId);
        return epoDocumentId;
    }

    private static Element getApplicantOrInventor(Element parties, String sectionName, String elementName, int sequence,
            String[] formats) {

        Element section = XMLUtils.getSingleElement(parties, sectionName);
        List<Element> element = XMLUtils.getElementList(section, elementName);
        EPOElementHolder holder = new EPOElementHolder(formats, element, "data-format", String.valueOf(sequence),
                "sequence");
        return holder.get();
    }

    public static Record convertBibliographicData(Element exchangeDoc, String[] formats) {
        MutableRecord record = new SubmissionLookupPublication("");
        Element biblographicData = XMLUtils.getSingleElement(exchangeDoc, "bibliographic-data");

        EPODocumentId epoDocumentId = getDocumentNumber(biblographicData, "publication-reference", formats);
        record.addValue("publicationnumber", new StringValue(epoDocumentId.getId()));
        record.addValue("dateissued", new StringValue(epoDocumentId.getDate()));

        epoDocumentId = getDocumentNumber(biblographicData, "application-reference", formats);
        record.addValue("applicationnumber", new StringValue(epoDocumentId.getId()));

        int iseq = 1;
        Element applicant = null;
        Element parties = XMLUtils.getSingleElement(biblographicData, "parties");
        do {
            applicant = getApplicantOrInventor(parties, "applicants", "applicant", iseq, formats);
            if (applicant != null) {
                String applicantName = XMLUtils.getElementValue(applicant, "applicant-name");
                record.addValue("applicant", new StringValue(applicantName));
            }

            iseq++;
        } while (applicant != null);

        int iinv = 1;
        Element inventor = null;
        do {
            inventor = getApplicantOrInventor(parties, "inventors", "inventor", iinv, formats);
            if (inventor != null) {
                String inventorName = XMLUtils.getElementValue(inventor, "inventor-name");
                record.addValue("inventor", new StringValue(inventorName));
            }

            iinv++;
        } while (inventor != null);

        String inventionTitle = XMLUtils.getElementValue(biblographicData, "invention-title");
        record.addValue("inventiontitle", new StringValue(inventionTitle));

        String summary = XMLUtils.getElementValue(exchangeDoc, "abstract");
        record.addValue("abstract", new StringValue(summary));

        return record;
    }
}
