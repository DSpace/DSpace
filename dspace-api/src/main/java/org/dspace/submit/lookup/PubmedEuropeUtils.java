/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.util.XMLUtils;
import org.dspace.submit.util.SubmissionLookupPublication;
import org.w3c.dom.Element;

/**
 * @author cineca
 */
public class PubmedEuropeUtils {

    private static final String URL_PREFIX = "http://europepmc.org/abstract/";

    private PubmedEuropeUtils() {
    }

    public static Record convertPubmedEuropeDomToRecord(Element pubArticle) {
        MutableRecord record = new SubmissionLookupPublication("");

        String pmID = XMLUtils.getElementValue(pubArticle,
            "pmid");
        record.addValue("pmid", new StringValue(pmID));

        String source = XMLUtils.getElementValue(pubArticle,
            "source");

        record.addValue("url", new StringValue(URL_PREFIX + source + "/" + pmID));

        Element pmc = XMLUtils.getSingleElement(pubArticle, "pmcid");
        String pmcID = "";
        if (pmc != null) {
            pmcID = XMLUtils.getElementValue(pubArticle,
                "pmcid");
            record.addValue("pmcid", new StringValue(pmcID));
        }

        String doi = XMLUtils.getElementValue(pubArticle,
            "doi");
        record.addValue("doi", new StringValue(doi));

        String title = XMLUtils.getElementValue(pubArticle, "title");
        record.addValue("title", new StringValue(title));

        String allPeople = XMLUtils.getElementValue(pubArticle, "authorString");
        record.addValue("allpeople", new StringValue(allPeople));

        List<String> authors = new ArrayList<String>();

        Element authorsList = XMLUtils.getSingleElement(pubArticle, "authorList");
        if (authorsList != null) {
            List<Element> auths = XMLUtils.getElementList(authorsList, "author");
            for (Element auth : auths) {
                String author = "";
                Element firstName = XMLUtils.getSingleElement(auth, "firstName");
                Element lastName = XMLUtils.getSingleElement(auth, "lastName");
                if (lastName != null) {
                    author += lastName.getTextContent();
                }
                if (firstName != null) {
                    author += author.length() > 0 ? (", " + firstName.getTextContent()) : firstName.getTextContent();
                }
                authors.add(author);

            }
        }
        if (authors.size() > 0) {
            List<Value> values = new LinkedList<Value>();
            for (String strAuthor : authors) {
                values.add(new StringValue(strAuthor));
            }
            record.addField("author", values);
        }


        List<String> inv = new ArrayList<String>();

        Element investigatorList = XMLUtils.getSingleElement(pubArticle, "investigatorList");
        if (investigatorList != null) {
            List<Element> investigators = XMLUtils.getElementList(investigatorList, "investigator");
            for (Element auth : investigators) {
                String investigator = "";
                Element firstName = XMLUtils.getSingleElement(auth, "firstName");
                Element lastName = XMLUtils.getSingleElement(auth, "lastName");
                if (lastName != null) {
                    investigator += lastName.getTextContent();
                }
                if (firstName != null) {
                    investigator += investigator.length() > 0 ?
                        (", " + firstName.getTextContent()) :
                        firstName.getTextContent();
                }
                inv.add(investigator);

            }
        }
        if (inv.size() > 0) {
            List<Value> values = new LinkedList<Value>();
            for (String strInv : inv) {
                values.add(new StringValue(strInv));
            }
            record.addField("investigator", values);
        }


        Element journalInfo = XMLUtils.getSingleElement(pubArticle,
            "journalInfo");
        String volume = "";
        String issue = "";
        String journalTitle = "";
        String jISSN = "";
        String jEISSN = "";
        if (journalInfo != null) {
            Element vol = XMLUtils.getSingleElement(journalInfo, "volume");
            if (vol != null) {
                volume = vol.getTextContent();
                record.addValue("volume", new StringValue(volume));
            }
            Element issueTag = XMLUtils.getSingleElement(journalInfo, "issue");
            if (issueTag != null) {
                issue = issueTag.getTextContent();
                record.addValue("issue", new StringValue(issue));
            }

            Element journal = XMLUtils.getSingleElement(journalInfo, "journal");
            if (journal != null) {
                Element jTitle = XMLUtils.getSingleElement(journal, "title");
                if (jTitle != null) {
                    journalTitle = jTitle.getTextContent();
                    record.addValue("jTitle", new StringValue(journalTitle));
                }
                Element issn = XMLUtils.getSingleElement(journal, "ISSN");
                if (issn != null) {
                    jISSN = issn.getTextContent();
                    record.addValue("ISSN", new StringValue(jISSN));
                }
                Element eissn = XMLUtils.getSingleElement(journal, "ESSN");
                if (eissn != null) {
                    jEISSN = eissn.getTextContent();
                    record.addValue("EISSN", new StringValue(jEISSN));
                }
            }
        }

        Element pageInfo = XMLUtils.getSingleElement(pubArticle, "pageInfo");
        String startPage = "";
        String endPage = "";
        if (pageInfo != null) {
            String pages = pageInfo.getTextContent();
            String[] page = StringUtils.split(pages, "-");
            startPage = page[0];
            record.addValue("startPage", new StringValue(startPage));
            if (page.length > 1) {
                endPage = page[1];
                record.addValue("endPage", new StringValue(endPage));
            }
        }

        Element absText = XMLUtils.getSingleElement(pubArticle, "abstractText");
        String abs = "";
        if (absText != null) {
            abs = absText.getTextContent();
            record.addValue("abstractText", new StringValue(abs));
        }

        Element language = XMLUtils.getSingleElement(pubArticle, "language");
        String lang = "";
        if (language != null) {
            lang = language.getTextContent();
            record.addValue("language", new StringValue(lang));
        }

        Element publTypeList = XMLUtils.getSingleElement(pubArticle, "pubTypeList");
        List<String> publicationTypes = new ArrayList<String>();
        if (publTypeList != null) {
            List<Element> pubTypes = XMLUtils.getElementList(publTypeList, "pubType");
            for (Element pubType : pubTypes) {
                publicationTypes.add(pubType.getTextContent());
            }
        }
        if (publicationTypes.size() > 0) {
            List<Value> values = new LinkedList<Value>();
            for (String strType : publicationTypes) {
                values.add(new StringValue(strType));
            }
            record.addField("pubType", values);
        }

        Element firstPublicationDate = XMLUtils.getSingleElement(pubArticle, "firstPublicationDate");
        String dateIssued = "";
        if (firstPublicationDate != null) {
            dateIssued = firstPublicationDate.getTextContent();
            record.addValue("pubDate", new StringValue(dateIssued));
        }
        List<String> primaryKeywords = new LinkedList<String>();

        Element keywordsList = XMLUtils.getSingleElement(pubArticle,
            "keywordList");
        if (keywordsList != null) {
            List<Element> keywords = XMLUtils.getElementList(keywordsList,
                "keyword");
            for (Element keyword : keywords) {
                primaryKeywords.add(keyword.getTextContent().trim());
            }
        }
        if (primaryKeywords.size() > 0) {
            List<Value> values = new LinkedList<Value>();
            for (String s : primaryKeywords) {
                values.add(new StringValue(s));
            }
            record.addField("keyword", values);
        }

        List<String> primaryMeshHeadings = new LinkedList<String>();
        List<String> secondaryMeshHeadings = new LinkedList<String>();

        Element meshHeadingsList = XMLUtils.getSingleElement(pubArticle,
            "meshHeadingList");
        if (meshHeadingsList != null) {
            List<Element> meshHeadings = XMLUtils.getElementList(
                meshHeadingsList, "meshHeading");
            for (Element meshHeading : meshHeadings) {
                if ("Y".equals(XMLUtils.getElementAttribute(meshHeading,
                    "DescriptorName", "majorTopic_YN"))) {
                    primaryMeshHeadings.add(XMLUtils.getElementValue(
                        meshHeading, "DescriptorName"));
                } else {
                    secondaryMeshHeadings.add(XMLUtils.getElementValue(
                        meshHeading, "DescriptorName"));
                }
            }
        }
        if (primaryMeshHeadings.size() > 0) {
            List<Value> values = new LinkedList<Value>();
            for (String s : primaryMeshHeadings) {
                values.add(new StringValue(s));
            }
            record.addField("primaryMeshHeading", values);
        }
        if (secondaryMeshHeadings.size() > 0) {
            List<Value> values = new LinkedList<Value>();
            for (String s : secondaryMeshHeadings) {
                values.add(new StringValue(s));
            }
            record.addField("secondaryMeshHeading", values);
        }

        Element book = XMLUtils.getSingleElement(pubArticle, "bookOrReportDetails");
        String publisher = "";
        String edition = "";
        String series = "";
        String isbn = "";
        String bookTitle = "";
        String sISSN = "";
        if (book != null) {
            Element publisherTag = XMLUtils.getSingleElement(book, "publisher");
            if (publisherTag != null) {
                publisher = XMLUtils.getElementValue(book, "publisher");
                record.addValue("publisher", new StringValue(publisher));
            }
            Element editionTag = XMLUtils.getSingleElement(book, "edition");
            if (editionTag != null) {
                edition = XMLUtils.getElementValue(book, "edition");
                record.addValue("edition", new StringValue(edition));
            }
            Element seriesTag = XMLUtils.getSingleElement(book, "seriesName");
            if (seriesTag != null) {
                series = XMLUtils.getElementValue(book, "seriesName");
                record.addValue("series", new StringValue(series));
            }
            Element isbn10Tag = XMLUtils.getSingleElement(book, "isbn10");
            if (isbn10Tag != null) {
                isbn = XMLUtils.getElementValue(book, "isbn10");
                record.addValue("isbn", new StringValue(isbn));
            }
            Element isbn13Tag = XMLUtils.getSingleElement(book, "isbn13");
            if (isbn13Tag != null) {
                isbn = XMLUtils.getElementValue(book, "isbn13");
                record.addValue("isbn", new StringValue(isbn));
            }
            Element bookTitleTag = XMLUtils.getSingleElement(book, "comprisingTitle");
            if (bookTitleTag != null) {
                bookTitle = XMLUtils.getElementValue(book, "comprisingTitle");
                record.addValue("bookTitle", new StringValue(bookTitle));
            }
            Element sISSNTag = XMLUtils.getSingleElement(book, "seriesISSN");
            if (sISSNTag != null) {
                sISSN = XMLUtils.getElementValue(book, "seriesISSN");
                record.addValue("sISSN", new StringValue(sISSN));
            }
        }

        return record;
    }
}
