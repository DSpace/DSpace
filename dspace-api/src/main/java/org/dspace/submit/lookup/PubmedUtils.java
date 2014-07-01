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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
public class PubmedUtils
{

    public static Record convertPubmedDomToRecord(Element pubArticle)
    {
        MutableRecord record = new SubmissionLookupPublication("");

        Map<String, String> monthToNum = new HashMap<String, String>();
        monthToNum.put("Jan", "01");
        monthToNum.put("Feb", "02");
        monthToNum.put("Mar", "03");
        monthToNum.put("Apr", "04");
        monthToNum.put("May", "05");
        monthToNum.put("Jun", "06");
        monthToNum.put("Jul", "07");
        monthToNum.put("Aug", "08");
        monthToNum.put("Sep", "09");
        monthToNum.put("Oct", "10");
        monthToNum.put("Nov", "11");
        monthToNum.put("Dec", "12");

        Element medline = XMLUtils.getSingleElement(pubArticle,
                "MedlineCitation");

        Element article = XMLUtils.getSingleElement(medline, "Article");
        Element pubmed = XMLUtils.getSingleElement(pubArticle, "PubmedData");

        Element identifierList = XMLUtils.getSingleElement(pubmed,
                "ArticleIdList");
        if (identifierList != null)
        {
            List<Element> identifiers = XMLUtils.getElementList(identifierList,
                    "ArticleId");
            if (identifiers != null)
            {
                for (Element id : identifiers)
                {
                    if ("pubmed".equals(id.getAttribute("IdType")))
                    {
                        String pubmedID = id.getTextContent().trim();
                        if (pubmedID != null)
                            record.addValue("pubmedID", new StringValue(
                                    pubmedID));
                    }
                    else if ("doi".equals(id.getAttribute("IdType")))
                    {
                        String doi = id.getTextContent().trim();
                        if (doi != null)
                            record.addValue("doi", new StringValue(doi));
                    }
                }
            }
        }

        String status = XMLUtils.getElementValue(pubmed, "PublicationStatus");
        if (status != null)
            record.addValue("publicationStatus", new StringValue(status));

        String pubblicationModel = XMLUtils.getElementAttribute(medline,
                "Article", "PubModel");
        if (pubblicationModel != null)
            record.addValue("pubModel", new StringValue(
                    pubblicationModel));

        String title = XMLUtils.getElementValue(article, "ArticleTitle");
        if (title != null)
            record.addValue("articleTitle", new StringValue(title));

        Element abstractElement = XMLUtils
                .getSingleElement(article, "Abstract");
        if (abstractElement == null)
        {
            abstractElement = XMLUtils.getSingleElement(medline,
                    "OtherAbstract");
        }
        if (abstractElement != null)
        {
            String summary = XMLUtils.getElementValue(abstractElement,
                    "AbstractText");
            if (summary != null)
                record.addValue("abstractText", new StringValue(summary));
        }

        List<String[]> authors = new LinkedList<String[]>();
        Element authorList = XMLUtils.getSingleElement(article, "AuthorList");
        if (authorList != null)
        {
            List<Element> authorsElement = XMLUtils.getElementList(authorList,
                    "Author");
            if (authorsElement != null)
            {
                for (Element author : authorsElement)
                {
                    if (StringUtils.isBlank(XMLUtils.getElementValue(author,
                            "CollectiveName")))
                    {
                        authors.add(new String[] {
                                XMLUtils.getElementValue(author, "ForeName"),
                                XMLUtils.getElementValue(author, "LastName") });
                    }
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
            record.addField("author", values);
        }

        Element journal = XMLUtils.getSingleElement(article, "Journal");
        if (journal != null)
        {
            List<Element> jnumbers = XMLUtils.getElementList(journal, "ISSN");
            if (jnumbers != null)
            {
                for (Element jnumber : jnumbers)
                {
                    if ("Print".equals(jnumber.getAttribute("IssnType")))
                    {
                        String issn = jnumber.getTextContent().trim();
                        if (issn != null)
                            record.addValue("printISSN", new StringValue(issn));
                    }
                    else
                    {
                        String eissn = jnumber.getTextContent().trim();
                        if (eissn != null)
                            record.addValue("electronicISSN", new StringValue(eissn));
                    }
                }
            }

            String journalTitle = XMLUtils.getElementValue(journal, "Title");
            if (journalTitle != null)
                record.addValue("journalTitle", new StringValue(journalTitle));

            Element journalIssueElement = XMLUtils.getSingleElement(journal,
                    "JournalIssue");
            if (journalIssueElement != null)
            {
                String volume = XMLUtils.getElementValue(journalIssueElement,
                        "Volume");
                if (volume != null)
                    record.addValue("journalVolume", new StringValue(volume));

                String issue = XMLUtils.getElementValue(journalIssueElement,
                        "Issue");
                if (issue != null)
                    record.addValue("journalIssue", new StringValue(issue));

                Element pubDateElement = XMLUtils.getSingleElement(
                        journalIssueElement, "PubDate");

                String pubDate = null;
                if (pubDateElement != null)
                {
                	pubDate = XMLUtils.getElementValue(pubDateElement, "Year");

                    String mounth = XMLUtils.getElementValue(pubDateElement,
                            "Month");
                    String day = XMLUtils
                            .getElementValue(pubDateElement, "Day");
                    if (StringUtils.isNotBlank(mounth)
                            && monthToNum.containsKey(mounth))
                    {
                    	pubDate += "-" + monthToNum.get(mounth);
                        if (StringUtils.isNotBlank(day))
                        {
                        	pubDate += "-" + (day.length() == 1 ? "0" + day : day);
                        }
                    }
                }
                if (pubDate == null){
                	pubDate = XMLUtils.getElementValue(pubDateElement, "MedlineDate");
                }
                if (pubDate != null)
                    record.addValue("pubDate", new StringValue(pubDate));
            }

            String language = XMLUtils.getElementValue(article, "Language");
            if (language != null)
                record.addValue("language", new StringValue(language));

            List<String> type = new LinkedList<String>();
            Element publicationTypeList = XMLUtils.getSingleElement(article,
                    "PublicationTypeList");
            if (publicationTypeList != null)
            {
                List<Element> publicationTypes = XMLUtils.getElementList(
                        publicationTypeList, "PublicationType");
                for (Element publicationType : publicationTypes)
                {
                    type.add(publicationType.getTextContent().trim());
                }
            }
            if (type.size() > 0)
            {
                List<Value> values = new LinkedList<Value>();
                for (String s : type)
                {
                    values.add(new StringValue(s));
                }
                record.addField("publicationType", values);
            }

            List<String> primaryKeywords = new LinkedList<String>();
            List<String> secondaryKeywords = new LinkedList<String>();
            Element keywordsList = XMLUtils.getSingleElement(medline,
                    "KeywordList");
            if (keywordsList != null)
            {
                List<Element> keywords = XMLUtils.getElementList(keywordsList,
                        "Keyword");
                for (Element keyword : keywords)
                {
                    if ("Y".equals(keyword.getAttribute("MajorTopicYN")))
                    {
                        primaryKeywords.add(keyword.getTextContent().trim());
                    }
                    else
                    {
                        secondaryKeywords.add(keyword.getTextContent().trim());
                    }
                }
            }
            if (primaryKeywords.size() > 0)
            {
                List<Value> values = new LinkedList<Value>();
                for (String s : primaryKeywords)
                {
                    values.add(new StringValue(s));
                }
                record.addField("primaryKeyword", values);
            }
            if (secondaryKeywords.size() > 0)
            {
                List<Value> values = new LinkedList<Value>();
                for (String s : secondaryKeywords)
                {
                    values.add(new StringValue(s));
                }
                record.addField("secondaryKeyword", values);
            }

            List<String> primaryMeshHeadings = new LinkedList<String>();
            List<String> secondaryMeshHeadings = new LinkedList<String>();
            Element meshHeadingsList = XMLUtils.getSingleElement(medline,
                    "MeshHeadingList");
            if (meshHeadingsList != null)
            {
                List<Element> meshHeadings = XMLUtils.getElementList(
                        meshHeadingsList, "MeshHeading");
                for (Element meshHeading : meshHeadings)
                {
                    if ("Y".equals(XMLUtils.getElementAttribute(meshHeading,
                            "DescriptorName", "MajorTopicYN")))
                    {
                        primaryMeshHeadings.add(XMLUtils.getElementValue(
                                meshHeading, "DescriptorName"));
                    }
                    else
                    {
                        secondaryMeshHeadings.add(XMLUtils.getElementValue(
                                meshHeading, "DescriptorName"));
                    }
                }
            }
            if (primaryMeshHeadings.size() > 0)
            {
                List<Value> values = new LinkedList<Value>();
                for (String s : primaryMeshHeadings)
                {
                    values.add(new StringValue(s));
                }
                record.addField("primaryMeshHeading", values);
            }
            if (secondaryMeshHeadings.size() > 0)
            {
                List<Value> values = new LinkedList<Value>();
                for (String s : secondaryMeshHeadings)
                {
                    values.add(new StringValue(s));
                }
                record.addField("secondaryMeshHeading", values);
            }

            Element paginationElement = XMLUtils.getSingleElement(article,
                    "Pagination");
            if (paginationElement != null)
            {
                String startPage = XMLUtils.getElementValue(paginationElement,
                        "StartPage");
                String endPage = XMLUtils.getElementValue(paginationElement,
                        "EndPage");
                if (StringUtils.isBlank(startPage))
                {
                    startPage = XMLUtils.getElementValue(paginationElement,
                            "MedlinePgn");
                }

                if (startPage != null)
                    record.addValue("startPage", new StringValue(startPage));
                if (endPage != null)
                    record.addValue("endPage", new StringValue(endPage));
            }
        }

        return record;
    }
}
