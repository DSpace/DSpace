/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * JAVA representation for a SHERPA API Response
 * 
 * @author Andrea Bollini
 * 
 */
public class SHERPAResponse
{
    private boolean error;

    private String message;

    private String license;

    private String licenseURL;

    private String disclaimer;

    private List<SHERPAJournal> journals;

    private List<SHERPAPublisher> publishers;

    public SHERPAResponse(InputStream xmlData)
    {
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder db = factory.newDocumentBuilder();
            Document inDoc = db.parse(xmlData);

            Element xmlRoot = inDoc.getDocumentElement();
            Element headersElement = XMLUtils.getSingleElement(xmlRoot,
                    "header");
            Element journalsElement = XMLUtils.getSingleElement(xmlRoot,
                    "journals");
            Element publishersElement = XMLUtils.getSingleElement(xmlRoot,
                    "publishers");

            message = XMLUtils.getElementValue(headersElement, "message");

            if (StringUtils.isNotBlank(message))
            {
                error = true;
                return;
            }

            license = XMLUtils.getElementValue(headersElement, "license");
            licenseURL = XMLUtils.getElementValue(headersElement, "licenseurl");
            disclaimer = XMLUtils.getElementValue(headersElement, "disclaimer");

            List<Element> journalsList = XMLUtils.getElementList(
                    journalsElement, "journal");
            List<Element> publishersList = XMLUtils.getElementList(
                    publishersElement, "publisher");

            if (journalsList != null)
            {
                journals = new LinkedList<SHERPAJournal>();
                for (Element journalElement : journalsList)
                {
                    journals.add(new SHERPAJournal(
                            XMLUtils.getElementValue(journalElement, "jtitle"),
                            XMLUtils.getElementValue(journalElement, "issn"),
                            XMLUtils.getElementValue(journalElement, "zetopub"),
                            XMLUtils.getElementValue(journalElement, "romeopub")));
                }
            }

            if (publishersList != null)
            {
                publishers = new LinkedList<SHERPAPublisher>();
                for (Element publisherElement : publishersList)
                {
                    Element preprintsElement = XMLUtils.getSingleElement(
                            publisherElement, "preprints");
                    Element preprintsRestrictionElement = XMLUtils
                            .getSingleElement(publisherElement,
                                    "prerestrictions");

                    Element postprintsElement = XMLUtils.getSingleElement(
                            publisherElement, "postprints");
                    Element postprintsRestrictionElement = XMLUtils
                            .getSingleElement(publisherElement,
                                    "postrestrictions");

                    Element pdfversionElement = XMLUtils.getSingleElement(
                            publisherElement, "pdfversion");
                    Element pdfversionRestrictionElement = XMLUtils
                            .getSingleElement(publisherElement,
                                    "pdfrestrictions");
                    
                    Element conditionsElement = XMLUtils.getSingleElement(
                            publisherElement, "conditions");
                    Element paidaccessElement = XMLUtils.getSingleElement(
                            publisherElement, "paidaccess");

                    Element copyrightlinksElement = XMLUtils.getSingleElement(
                            publisherElement, "copyrightlinks");

                    publishers
                            .add(new SHERPAPublisher(XMLUtils.getElementValue(
                                    publisherElement, "name"),
                                    XMLUtils.getElementValue(publisherElement,
                                            "alias"), XMLUtils.getElementValue(
                                            publisherElement, "homeurl"),
                                            
                                    XMLUtils.getElementValue(preprintsElement,
                                            "prearchiving"),
                                    XMLUtils.getElementValueList(
                                            preprintsRestrictionElement,
                                            "prerestriction"),
                                            
                                    XMLUtils.getElementValue(postprintsElement,
                                            "postarchiving"),
                                    XMLUtils.getElementValueList(
                                            postprintsRestrictionElement,
                                            "postrestriction"),
                                            
                                    XMLUtils.getElementValue(pdfversionElement,
                                            "pdfarchiving"),
                                    XMLUtils.getElementValueList(
                                            pdfversionRestrictionElement,
                                            "pdfrestriction"), 
                                    
                                    XMLUtils
                                            .getElementValueList(
                                                    conditionsElement,
                                                    "condition"), XMLUtils
                                            .getElementValue(paidaccessElement,
                                                    "paidaccessurl"), XMLUtils
                                            .getElementValue(paidaccessElement,
                                                    "paidaccessname"), XMLUtils
                                            .getElementValue(paidaccessElement,
                                                    "paidaccessnotes"),
                                    XMLUtils.getElementValueArrayList(
                                            copyrightlinksElement,
                                            "copyrightlink",
                                            "copyrightlinktext",
                                            "copyrightlinkurl"), XMLUtils
                                            .getElementValue(publisherElement,
                                                    "romeocolour"), XMLUtils
                                            .getElementValue(publisherElement,
                                                    "dateadded"), XMLUtils
                                            .getElementValue(publisherElement,
                                                    "dateupdated")));
                }
            }
        }
        catch (Exception e)
        {
            error = true;
        }
    }

    public SHERPAResponse(String message)
    {
        this.message = message;
        this.error = true;
    }

    public boolean isError()
    {
        return error;
    }

    public String getMessage()
    {
        return message;
    }

    public String getLicense()
    {
        return license;
    }

    public String getLicenseURL()
    {
        return licenseURL;
    }

    public String getDisclaimer()
    {
        return disclaimer;
    }

    public List<SHERPAJournal> getJournals()
    {
        return journals;
    }

    public List<SHERPAPublisher> getPublishers()
    {
        return publishers;
    }
}
