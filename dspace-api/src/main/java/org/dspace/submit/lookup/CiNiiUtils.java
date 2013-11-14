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
 * 
 * @author Keiji Suzuki
 * 
 */
public class CiNiiUtils
{
    public static Record convertCiNiiDomToRecord(Element xmlRoot)
    {
        MutableRecord record = new SubmissionLookupPublication("");

        List<Element> list = XMLUtils.getElementList(xmlRoot, "rdf:Description");
        // Valid CiNii record should have three rdf:Description elements
        if (list.size() < 3)
        {
            return record;
        }

        Element description_ja = list.get(0);     // Japanese description
        Element description_en = list.get(1);     // English description
        // Element description3 = list.get(2);  // Authors information: NOT USE here

        String language = XMLUtils.getElementValue(description_ja, "dc:language");
        language = language != null ? language.toLowerCase() : "ja";
        record.addValue("language", new StringValue(language));

        if ("ja".equals(language) || "jpn".equals(language))
        {
            String title = XMLUtils.getElementValue(description_ja, "dc:title");
            if (title != null)
            {
                record.addValue("title", new StringValue(title));
            }
            String titleAlternative = XMLUtils.getElementValue(description_en, "dc:title");
            if (titleAlternative != null)
            {
                record.addValue("titleAlternative", new StringValue(titleAlternative));
            }

            List<Value> authors = getAuthors(description_ja);
            if (authors.size() > 0)
            {
                record.addField("authors", authors);
            }
            List<Value> authorAlternative = getAuthors(description_en);
            if (authorAlternative.size() > 0)
            {
                record.addField("auhtorAlternative", authorAlternative);
            }

            String publisher = XMLUtils.getElementValue(description_ja, "dc:publisher");
            if (publisher != null)
            {
                record.addValue("publisher", new StringValue(publisher));
            }
        }
        else
        {
            String title = XMLUtils.getElementValue(description_en, "dc:title");
            if (title != null)
            {
                record.addValue("title", new StringValue(title));
            }
            String titleAlternative = XMLUtils.getElementValue(description_ja, "dc:title");
            if (titleAlternative != null)
            {
                record.addValue("titleAlternative", new StringValue(titleAlternative));
            }

            List<Value> authors = getAuthors(description_en);
            if (authors.size() > 0)
            {
                record.addField("authors", authors);
            }
            List<Value> authorAlternative = getAuthors(description_ja);
            if (authorAlternative.size() > 0)
            {
                record.addField("authorAlternative", authorAlternative);
            }

            String publisher = XMLUtils.getElementValue(description_en, "dc:publisher");
            if (publisher != null)
            {
                record.addValue("publisher", new StringValue(publisher));
            }
        }

        String abstract_ja = XMLUtils.getElementValue(description_ja, "dc:description");
        String abstract_en = XMLUtils.getElementValue(description_en, "dc:description");
        if (abstract_ja != null && abstract_en != null)
        {
            List<Value> description = new LinkedList<Value>();
            description.add(new StringValue(abstract_ja));
            description.add(new StringValue(abstract_en));
            record.addField("description", description);
        }
        else if (abstract_ja != null)
        {
            record.addValue("description", new StringValue(abstract_ja));
        }
        else if (abstract_en != null)
        {
            record.addValue("description", new StringValue(abstract_en));
        }

        List<Value> subjects = getSubjects(description_ja);
        subjects.addAll(getSubjects(description_en));
        if (subjects.size() > 0)
        {
            record.addField("subjects", subjects);
        }

        String journal_j = XMLUtils.getElementValue(description_ja, "prism:publicationName");
        String journal_e = XMLUtils.getElementValue(description_en, "prism:publicationName");
        if (journal_j != null && journal_e != null)
        {
            record.addValue("journal", new StringValue(journal_j+" = "+journal_e));
        }
        else if (journal_j != null)
        {
            
            record.addValue("journal", new StringValue(journal_j));
        }
        else if (journal_e != null)
        {
            
            record.addValue("journal", new StringValue(journal_e));
        }

        String volume = XMLUtils.getElementValue(description_ja, "prism:volume");
        if (volume != null)
        {
            record.addValue("volume", new StringValue(volume));
        }

        String issue = XMLUtils.getElementValue(description_ja, "prism:number");
        if (issue != null)
        {
            record.addValue("issue", new StringValue(issue));
        }

        String spage = XMLUtils.getElementValue(description_ja, "prism:startingPage");
        if (spage != null)
        {
            record.addValue("spage", new StringValue(spage));
        }

        String epage = XMLUtils.getElementValue(description_ja, "prism:endingPage");
        if (epage != null)
        {
            record.addValue("epage", new StringValue(epage));
        }

        String pages = XMLUtils.getElementValue(description_ja, "prism:pageRange");
        if (pages != null && spage == null) 
        {
            int pos = pages.indexOf("-");
            if (pos > -1)
            {
                spage = pages.substring(0, pos);
                epage = pages.substring(pos+1, pages.length() - pos);
                if (!epage.equals("") && spage.length() > epage.length())
                {
                    epage = spage.substring(0, spage.length() - epage.length()) + epage;
                }
            }
            else
            {
                spage = pages;
                epage = "";
            }
            record.addValue("spage", new StringValue(spage));
            if (!epage.equals("") && epage == null)
            {
                record.addValue("epage", new StringValue(epage));
            }
        }

        String issn = XMLUtils.getElementValue(description_ja, "prism:issn");
        if (issn != null)
        {
            record.addValue("issn", new StringValue(issn));
        }

        String issued = XMLUtils.getElementValue(description_ja, "prism:publicationDate");
        if (issued != null)
        {
            record.addValue("issued", new StringValue(issued));
        }

        String ncid = XMLUtils.getElementValue(description_ja, "cinii:ncid");
        if (ncid != null)
        {
            record.addValue("ncid", new StringValue(ncid));
        }

        String naid = XMLUtils.getElementValue(description_ja, "cinii:naid");
        if (naid != null)
        {
            record.addValue("naid", new StringValue(naid));
        }

        return record;
    }

    private static List<Value> getAuthors(Element element)
    {
        List<Value> authors = new LinkedList<Value>();

        List<String> authorList = XMLUtils.getElementValueList(element, "dc:creator");
        if (authorList != null && authorList.size() > 0)
        {
            for (String author : authorList)
            {
                int pos = author.indexOf(" ");
                if (pos > -1)
                    author = author.substring(0, pos) + "," + author.substring(pos);
                authors.add(new StringValue(author));
            }
        }

        return authors;
    }

    private static List<Value> getSubjects(Element element)
    {
        List<Value> subjects = new LinkedList<Value>();

        List<Element> topicList = XMLUtils.getElementList(element, "foaf:topic");
        String attrValue = null;
        for (Element topic : topicList)
        {
            attrValue = topic.getAttribute("dc:title");
            if (StringUtils.isNotBlank(attrValue))
            {
                subjects.add(new StringValue(attrValue.trim()));
            }
        }

        return subjects;
    }

}
