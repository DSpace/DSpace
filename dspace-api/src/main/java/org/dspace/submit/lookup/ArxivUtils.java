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
            record.addValue("articleTitle", new StringValue(articleTitle));
        String summary = XMLUtils.getElementValue(dataRoot, "summary");
        if (summary != null)
            record.addValue("summary", new StringValue(summary));
        String year = XMLUtils.getElementValue(dataRoot, "published");
        if (year != null)
            record.addValue("year", new StringValue(year));
        String splashPageUrl = XMLUtils.getElementValue(dataRoot, "id");
        if (splashPageUrl != null)
            record.addValue("splashPageUrl", new StringValue(splashPageUrl));
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

        List<String[]> authors = new LinkedList<String[]>();
        List<Element> authorList = XMLUtils.getElementList(dataRoot, "author");
        if (authorList != null)
        {
            for (Element authorElement : authorList)
            {
                String nomeCompleto = authorElement.getTextContent();
                String[] nomeSplit = nomeCompleto.split("\\s+");
                String nome = "";
                String cognome = "";
                if (nomeSplit.length > 2)
                {
                    String senzaPunti = nomeCompleto.replace("\\.", "");
                    String[] tmp = senzaPunti.split("\\s+");

                    int start = 1;
                    if (tmp.length == nomeSplit.length)
                    {
                        for (int idx = 2; idx < tmp.length; idx++)
                        {
                            if (tmp[idx].length() > 1)
                            {
                                start = idx;
                            }
                        }
                    }
                    for (int i = 0; i < start; i++)
                    {
                        nome += nomeSplit[i];
                    }
                    for (int i = start; i < nomeSplit.length; i++)
                    {
                        cognome += nomeSplit[i];
                    }
                }
                else if (nomeSplit.length == 2)
                {
                    nome = nomeSplit[0];
                    cognome = nomeSplit[1];
                }
                else
                {
                    cognome = nomeCompleto;
                }
                authors.add(new String[] { nome, cognome });
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

        return record;
    }

}
