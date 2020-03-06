/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;

import java.util.List;

import org.dspace.app.util.XMLUtils;
import org.dspace.submit.util.SubmissionLookupPublication;
import org.w3c.dom.Element;

public final class EPOUtils {

    /**
     * Default constructor
     */
    private EPOUtils() {
    }

    private static EPODocumentId getDocumentNumber(Element biblographicData, String section, String[] formats) {
        EPOElementHolder.clear();

        Element pubblicationRef = XMLUtils.getSingleElement(biblographicData, section);
        List<Element> documentIds = XMLUtils.getElementList(pubblicationRef, "document-id");
        for (Element documentId : documentIds) {
            EPOElementHolder.add(new EPOElementHolder(documentId, "document-id-type"));
        }
        Element documentId = EPOElementHolder.get(formats);
        EPODocumentId epoDocumentId = new EPODocumentId(documentId);

        return epoDocumentId;
    }

    private static Element getApplicantOrInventor(Element biblographicData, String section, int sequence,
            String[] formats) {
        EPOElementHolder.clear();

        Element applicantsRef = XMLUtils.getSingleElement(biblographicData, section);
        List<Element> applicants = XMLUtils.getElementList(applicantsRef, "applicant");
        for (Element applicant : applicants) {
            if (String.valueOf(sequence).equals(applicant.getAttribute("sequence")))
                EPOElementHolder.add(new EPOElementHolder(applicant, "data-format"));
        }

        return EPOElementHolder.get(formats);
    }

    public static Record convertBibliographicData(Element biblographicData, String[] formats) {
        MutableRecord record = new SubmissionLookupPublication("");

        EPODocumentId epoDocumentId = getDocumentNumber(biblographicData, "publication-reference", formats);
        record.addValue("publicationnumber", new StringValue(epoDocumentId.getId()));
        record.addValue("dateissued", new StringValue(epoDocumentId.getDate()));

        epoDocumentId = getDocumentNumber(biblographicData, "application-reference", formats);
        record.addValue("applicationnumber", new StringValue(epoDocumentId.getId()));

        int iseq = 1;
        Element applicant = null;
        do {
            applicant = getApplicantOrInventor(biblographicData, "applicants", iseq, formats);
            if (applicant != null) {
                String applicantName = XMLUtils.getElementValue(applicant, "name");
                record.addValue("applicant", new StringValue(applicantName));
            }

            iseq++;
        } while (applicant != null);

        int iinv = 1;
        Element inventor = null;
        do {
            inventor = getApplicantOrInventor(biblographicData, "inventor", iinv, formats);
            if (inventor != null) {
                String applicantName = XMLUtils.getElementValue(applicant, "name");
                record.addValue("inventor", new StringValue(applicantName));
            }

            iinv++;
        } while (inventor != null);

        String inventionTitle = XMLUtils.getElementValue(biblographicData, "invention-title");
        record.addValue("inventiontitle", new StringValue(inventionTitle));

        String summary = XMLUtils.getElementValue(biblographicData, "abstract");
        record.addValue("abstract", new StringValue(summary));

        return record;
    }
}
