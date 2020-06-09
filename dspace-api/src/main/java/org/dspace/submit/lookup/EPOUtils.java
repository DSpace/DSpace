/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;
import org.apache.commons.lang3.StringUtils;
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

        return getDocumentNumber(biblographicData, section, formats, null);
    }

    private static EPODocumentId getDocumentNumber(Element biblographicData, String section, String[] formats,
            String format) {

        Element pubblicationRef = XMLUtils.getSingleElement(biblographicData, section);
        List<Element> documentIds = XMLUtils.getElementList(pubblicationRef, "document-id");
        EPOElementHolder holder = new EPOElementHolder(formats, documentIds, "document-id-type");
        Element documentId = (format == null) ? holder.get() : holder.get(format);

        if (documentId != null) {
            EPODocumentId epoDocumentId = new EPODocumentId(documentId);
            return epoDocumentId;
        } else {
            return null;
        }
    }

    private static EPOClassification getClassification(Element classificationData, String section, int sequence) {

        List<Element> classifications = XMLUtils.getElementList(classificationData, section);
        for (Element classification : classifications) {
            if (String.valueOf(sequence).equals(classification.getAttribute("sequence"))) {
                return new EPOClassification(classification);
            }
        }

        return null;
    }

    private static Element getApplicantOrInventor(Element parties, String sectionName, String elementName, int sequence,
            String[] formats) {

        Element section = XMLUtils.getSingleElement(parties, sectionName);
        if (section != null) {
            List<Element> element = XMLUtils.getElementList(section, elementName);
            EPOElementHolder holder = new EPOElementHolder(formats, element, "data-format", String.valueOf(sequence),
                    "sequence");
            return holder.get();
        }

        return null;
    }

    public static Record convertBibliographicData(Element exchangeDoc, String[] formats, String originFormat) {
        MutableRecord record = new SubmissionLookupPublication("");
        Element biblographicData = XMLUtils.getSingleElement(exchangeDoc, "bibliographic-data");

        EPODocumentId epoDocumentId = getDocumentNumber(biblographicData, "publication-reference", formats);
        if (epoDocumentId != null) {
            record.addValue("publicationnumber", new StringValue(epoDocumentId.getId()));
            if (StringUtils.isNotBlank(epoDocumentId.getDate())) {
                record.addValue("dateissued", new StringValue(formatDate(epoDocumentId.getDate())));
            }
        }

        EPODocumentId epoOriginDocumentId = getDocumentNumber(biblographicData, "application-reference", formats,
                originFormat);
        if (epoOriginDocumentId != null && StringUtils.isNotBlank(epoOriginDocumentId.getDate())) {
            record.addValue("datefilled", new StringValue(formatDate(epoOriginDocumentId.getDate())));
        } else {
            epoOriginDocumentId = null;
        }

        epoDocumentId = getDocumentNumber(biblographicData, "application-reference", formats);
        if (epoDocumentId != null) {
            record.addValue("applicationnumber", new StringValue(epoDocumentId.getId()));
            if (epoOriginDocumentId == null && StringUtils.isNotBlank(epoDocumentId.getDate())) {
                record.addValue("datefilled", new StringValue(formatDate(epoDocumentId.getDate())));
            }
        }

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

        int iclass = 1;
        EPOClassification classification = null;
        Element patentClassification = XMLUtils.getSingleElement(biblographicData, "patent-classifications");
        do {
            classification = getClassification(patentClassification, "patent-classification", iclass);
            if (classification != null) {
                record.addValue("ipc", new StringValue(classification.toString()));
            }

            iclass++;
        } while (classification != null);

        List<Element> inventionTitles = XMLUtils.getElementList(biblographicData, "invention-title");
        for (Element inventionTitle : inventionTitles) {
            record.addValue("inventiontitle", new StringValue(inventionTitle.getTextContent()));
        }

        List<Element> summaries = XMLUtils.getElementList(exchangeDoc, "abstract");
        for (Element summary : summaries) {
            record.addValue("abstract", new StringValue(summary.getTextContent()));
        }

        return record;
    }

    protected static String formatDate(String date) {
        String formattedDate = "";
        // input format: yyyyMMdd
        SimpleDateFormat parser = new SimpleDateFormat("yyyyMMdd");
        // output format: yyyy-MM-dd
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            formattedDate = formatter.format(parser.parse(date.trim()));
        } catch (ParseException e) {
            formattedDate = date;
        }

        return formattedDate;
    }
}
