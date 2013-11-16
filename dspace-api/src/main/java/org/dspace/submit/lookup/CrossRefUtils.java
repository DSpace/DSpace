/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * 
 */
package org.dspace.submit.lookup;

import java.util.LinkedList;
import java.util.List;

import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.util.XMLUtils;
import org.dspace.submit.util.SubmissionLookupPublication;
import org.w3c.dom.Element;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class CrossRefUtils
{

    /**
	 * 
	 */
    public CrossRefUtils()
    {
        // TODO Auto-generated constructor stub
    }

    public static Record convertCrossRefDomToRecord(Element dataRoot)
    {
        MutableRecord record = new SubmissionLookupPublication("");

        String status = dataRoot.getAttribute("status");
        if (!"resolved".equals(status))
        {
            String msg = XMLUtils.getElementValue(dataRoot, "msg");
            String exMsg = status + " - " + msg;
            throw new RuntimeException(exMsg);
        }

        String doi = XMLUtils.getElementValue(dataRoot, "doi");
        if (doi != null)
            record.addValue("doi", new StringValue(doi));

        String itemType = doi != null ? XMLUtils.getElementAttribute(dataRoot,
                "doi", "type") : "unspecified";
        if (itemType != null)
            record.addValue("doiType", new StringValue(itemType));

        List<Element> identifier = XMLUtils.getElementList(dataRoot, "issn");
        for (Element ident : identifier)
        {
            if ("print".equalsIgnoreCase(ident.getAttribute("type"))
                    || StringUtils.isNotBlank(ident.getAttribute("type")))
            {
                String issn = ident.getTextContent().trim();
                if (issn != null)
                    record.addValue("printISSN", new StringValue(issn));
            }
            else
            {
                String eissn = ident.getTextContent().trim();
                if (eissn != null)
                    record.addValue("electronicISSN", new StringValue(eissn));
            }
        }
        
        List<Element> identifierisbn = XMLUtils.getElementList(dataRoot, "isbn");
        for (Element ident : identifierisbn)
        {
            if ("print".equalsIgnoreCase(ident.getAttribute("type"))
                    || StringUtils.isNotBlank(ident.getAttribute("type")))
            {
                String issn = ident.getTextContent().trim();
                if (issn != null)
                    record.addValue("printISBN", new StringValue(issn));
            }
            else
            {
                String eissn = ident.getTextContent().trim();
                if (eissn != null)
                    record.addValue("electronicISBN", new StringValue(eissn));
            }
        }

        String editionNumber = XMLUtils.getElementValue(dataRoot,
                "editionNumber");
        if (editionNumber != null)
            record.addValue("editionNumber", new StringValue(editionNumber));

        String volume = XMLUtils.getElementValue(dataRoot, "volume");
        if (volume != null)
            record.addValue("volume", new StringValue(volume));

        String issue = XMLUtils.getElementValue(dataRoot, "issue");
        if (issue != null)
            record.addValue("issue", new StringValue(issue));

        String year = XMLUtils.getElementValue(dataRoot, "year");
        if (year != null)
            record.addValue("year", new StringValue(year));

        String firstPage = XMLUtils.getElementValue(dataRoot, "first_page");
        if (firstPage != null)
            record.addValue("firstPage", new StringValue(firstPage));

        String lastPage = XMLUtils.getElementValue(dataRoot, "last_page");
        if (lastPage != null)
            record.addValue("lastPage", new StringValue(lastPage));

        String seriesTitle = XMLUtils.getElementValue(dataRoot, "series_title");
        if (seriesTitle != null)
            record.addValue("seriesTitle", new StringValue(seriesTitle));

        String journalTitle = XMLUtils.getElementValue(dataRoot,
                "journal_title");
        if (journalTitle != null)
            record.addValue("journalTitle", new StringValue(journalTitle));

        String volumeTitle = XMLUtils.getElementValue(dataRoot, "volume_title");
        if (volumeTitle != null)
            record.addValue("volumeTitle", new StringValue(volumeTitle));

        String articleTitle = XMLUtils.getElementValue(dataRoot,
                "article_title");
        if (articleTitle != null)
            record.addValue("articleTitle", new StringValue(articleTitle));

        String publicationType = XMLUtils.getElementValue(dataRoot,
                "pubblication_type");
        if (publicationType != null)
            record.addValue("publicationType", new StringValue(publicationType));

        List<String[]> authors = new LinkedList<String[]>();
        List<String[]> editors = new LinkedList<String[]>();
        List<String[]> translators = new LinkedList<String[]>();
        List<String[]> chairs = new LinkedList<String[]>();

        List<Element> contributors = XMLUtils.getElementList(dataRoot,
                "contributors");
        List<Element> contributor = null;
        if (contributors != null && contributors.size() > 0)
        {
            contributor = XMLUtils.getElementList(contributors.get(0),
                    "contributor");

            for (Element contrib : contributor)
            {

                String givenName = XMLUtils.getElementValue(contrib,
                        "given_name");
                String surname = XMLUtils.getElementValue(contrib, "surname");

                if ("editor".equalsIgnoreCase(contrib
                        .getAttribute("contributor_role")))
                {
                    editors.add(new String[] { givenName, surname });
                }
                else if ("chair".equalsIgnoreCase(contrib
                        .getAttribute("contributor_role")))
                {
                    chairs.add(new String[] { givenName, surname });
                }
                else if ("translator".equalsIgnoreCase(contrib
                        .getAttribute("contributor_role")))
                {
                    translators.add(new String[] { givenName, surname });
                }
                else
                {
                    authors.add(new String[] { givenName, surname });
                }
            }
        }

        if (authors.size() > 0)
        {
            List<Value> values = new LinkedList<Value>();
            for (String[] sArray : authors)
            {
                values.add(new StringValue(sArray[1] + ", " + sArray[0]));
            }
            record.addField("authors", values);
        }

        if (editors.size() > 0)
        {
            List<Value> values = new LinkedList<Value>();
            for (String[] sArray : editors)
            {
                values.add(new StringValue(sArray[1] + ", " + sArray[0]));
            }
            record.addField("editors", values);
        }

        if (translators.size() > 0)
        {
            List<Value> values = new LinkedList<Value>();
            for (String[] sArray : translators)
            {
                values.add(new StringValue(sArray[1] + ", " + sArray[0]));
            }
            record.addField("translators", values);
        }

        if (chairs.size() > 0)
        {
            List<Value> values = new LinkedList<Value>();
            for (String[] sArray : chairs)
            {
                values.add(new StringValue(sArray[1] + ", " + sArray[0]));
            }
            record.addField("chairs", values);
        }
        return record;
    }
}
