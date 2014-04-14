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

import org.dspace.app.util.XMLUtils;
import org.dspace.submit.util.SubmissionLookupPublication;
import org.w3c.dom.Element;

/**
 * 
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 * 
 */
public class ArxivUtils
{

    public static Record convertArxixDomToRecord(Element dataRoot)
    {
        MutableRecord record = new SubmissionLookupPublication("");

        String articleTitle = XMLUtils.getElementValue(dataRoot, "title");
        if (articleTitle != null)
            record.addValue("title", new StringValue(articleTitle));
        String summary = XMLUtils.getElementValue(dataRoot, "summary");
        if (summary != null)
            record.addValue("summary", new StringValue(summary));
        String year = XMLUtils.getElementValue(dataRoot, "published");
        if (year != null)
            record.addValue("published", new StringValue(year));
        String splashPageUrl = XMLUtils.getElementValue(dataRoot, "id");
        if (splashPageUrl != null)
            record.addValue("id", new StringValue(splashPageUrl));
        String comment = XMLUtils.getElementValue(dataRoot, "arxiv:comment");
        if (comment != null)
            record.addValue("comment", new StringValue(comment));

        List<Element> links = XMLUtils.getElementList(dataRoot, "link");
        if (links != null)
        {
            for (Element link : links)
            {
                if ("related".equals(link.getAttribute("rel"))
                        && "pdf".equals(link.getAttribute("title")))
                {
                    String pdfUrl = link.getAttribute("href");
                    if (pdfUrl != null)
                        record.addValue("pdfUrl", new StringValue(pdfUrl));
                }
            }
        }

        String doi = XMLUtils.getElementValue(dataRoot, "arxiv:doi");
        if (doi != null)
            record.addValue("doi", new StringValue(doi));
        String journalRef = XMLUtils.getElementValue(dataRoot,
                "arxiv:journal_ref");
        if (journalRef != null)
            record.addValue("journalRef", new StringValue(journalRef));

        List<String> primaryCategory = new LinkedList<String>();
        List<Element> primaryCategoryList = XMLUtils.getElementList(dataRoot,
                "arxiv:primary_category");
        if (primaryCategoryList != null)
        {
            for (Element primaryCategoryElement : primaryCategoryList)
            {
                primaryCategory
                        .add(primaryCategoryElement.getAttribute("term"));
            }
        }

        if (primaryCategory.size() > 0)
        {
            List<Value> values = new LinkedList<Value>();
            for (String s : primaryCategory)
            {
                values.add(new StringValue(s));
            }
            record.addField("primaryCategory", values);
        }

        List<String> category = new LinkedList<String>();
        List<Element> categoryList = XMLUtils.getElementList(dataRoot,
                "category");
        if (categoryList != null)
        {
            for (Element categoryElement : categoryList)
            {
                category.add(categoryElement.getAttribute("term"));
            }
        }

        if (category.size() > 0)
        {
            List<Value> values = new LinkedList<Value>();
            for (String s : category)
            {
                values.add(new StringValue(s));
            }
            record.addField("category", values);
        }

        List<String> authors = new LinkedList<String>();
        List<String> authorsWithAffiliations = new LinkedList<String>();
        List<Element> authorList = XMLUtils.getElementList(dataRoot, "author");
        if (authorList != null)
        {
            for (Element authorElement : authorList)
            {
            	String authorName = XMLUtils.getElementValue(authorElement, "name");
            	String authorAffiliation = XMLUtils.getElementValue(authorElement, "arxiv:affiliation");
            	
            	authors.add(authorName);
            	authorsWithAffiliations.add(authorName +": " + authorAffiliation);
            }
        }

        if (authors.size() > 0)
        {
            List<Value> values = new LinkedList<Value>();
            for (String sArray : authors)
            {
                values.add(new StringValue(sArray));
            }
            record.addField("author", values);
        }
        
        if (authorsWithAffiliations.size() > 0)
        {
            List<Value> values = new LinkedList<Value>();
            for (String sArray : authorsWithAffiliations)
            {
                values.add(new StringValue(sArray));
            }
            record.addField("authorWithAffiliation", values);
        }

        return record;
    }

}
