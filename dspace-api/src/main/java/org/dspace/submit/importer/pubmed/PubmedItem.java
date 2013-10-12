/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.importer.pubmed;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.util.XMLUtils;
import org.dspace.submit.importer.ItemImport;
import org.jdom.JDOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class PubmedItem implements ItemImport
{
	// Nome della classe istanziata
	private String source;
	// Valore del metadato source
	private String record;
	
    private String pubmedID;

    private String doi;

    private String issn;

    private String eissn;

    private String journalTitle;
    
    private String title;

    private String pubblicationModel;

    private String year;

    private String volume;
    
    private String issue;

    private String language;

    private List<String> type = new LinkedList<String>();

    private List<String> primaryKeywords = new LinkedList<String>();

    private List<String> secondaryKeywords = new LinkedList<String>();
    
    private List<String> primaryMeshHeadings = new LinkedList<String>();

    private List<String> secondaryMeshHeadings = new LinkedList<String>();

    private String startPage;

    private String endPage;

    private String summary;

    private String status;

    private List<String[]> authors = new LinkedList<String[]>();

    public PubmedItem(InputStream xmlData) throws JDOMException, IOException,
            ParserConfigurationException, SAXException
    {
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);

        DocumentBuilder db = factory.newDocumentBuilder();
        Document inDoc = db.parse(xmlData);

        Element xmlRoot = inDoc.getDocumentElement();
        Element pubArticle = XMLUtils
                .getSingleElement(xmlRoot, "PubmedArticle");

        loadData(pubArticle);
    }
    
    public PubmedItem(Element xmlArticle) {
    	loadData(xmlArticle);
	}

	private void loadData(Element pubArticle) {
		Map<String, String> mounthToNum = new HashMap<String, String>();
        mounthToNum.put("Jan", "01");
        mounthToNum.put("Feb", "02");
        mounthToNum.put("Mar", "03");
        mounthToNum.put("Apr", "04");
        mounthToNum.put("May", "05");
        mounthToNum.put("Jun", "06");
        mounthToNum.put("Jul", "07");
        mounthToNum.put("Aug", "08");
        mounthToNum.put("Sep", "09");
        mounthToNum.put("Oct", "10");
        mounthToNum.put("Nov", "11");
        mounthToNum.put("Dec", "12");
                
        Element medline = XMLUtils.getSingleElement(pubArticle, "MedlineCitation");
        
        Element article = XMLUtils.getSingleElement(medline, "Article");
        Element pubmed = XMLUtils.getSingleElement(pubArticle, "PubmedData");
        
        Element identifierList = XMLUtils.getSingleElement(pubmed, "ArticleIdList");
        if (identifierList != null)
        {
            List<Element> identifiers = XMLUtils.getElementList(identifierList, "ArticleId");
            if (identifiers != null)
            {
                for (Element id : identifiers)
                {
                    if ("pubmed".equals(id.getAttribute("IdType")))
                    {
                        pubmedID = id.getTextContent().trim();
                    }
                    else if ("doi".equals(id.getAttribute("IdType")))
                    {
                        doi = id.getTextContent().trim();
                    }
                }
            }
        }
        
        status = XMLUtils.getElementValue(pubmed, "PublicationStatus");
        pubblicationModel = XMLUtils.getElementAttribute(medline, "Article", "PubModel");
        title = XMLUtils.getElementValue(article, "ArticleTitle");
        Element abstractElement = XMLUtils.getSingleElement(medline, "Abstract");
        if (abstractElement == null)
        {
            abstractElement = XMLUtils.getSingleElement(medline, "OtherAbstract");
        }
        if (abstractElement != null)
        {
            summary = XMLUtils.getElementValue(abstractElement, "AbstractText");
        }
        
        Element authorList = XMLUtils.getSingleElement(article, "AuthorList");
        if (authorList != null)
        {
            List<Element> authorsElement = XMLUtils.getElementList(authorList, "Author");
            if (authorsElement != null)
            {
                for (Element author : authorsElement)
                {
                    if (StringUtils.isBlank(XMLUtils.getElementValue(author, "CollectiveName")))
                    {
                        authors.add(new String[]{XMLUtils.getElementValue(author, "ForeName"),XMLUtils.getElementValue(author, "LastName")});
                    }
                }
            }
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
                        issn = jnumber.getTextContent().trim();
                    }
                    else
                    {
                        eissn = jnumber.getTextContent().trim();
                    }
                }
            }
            
            journalTitle = XMLUtils.getElementValue(journal, "Title");
            Element journalIssueElement = XMLUtils.getSingleElement(journal, "JournalIssue");
            if (journalIssueElement != null)
            {
                volume = XMLUtils.getElementValue(journalIssueElement, "Volume");
                issue = XMLUtils.getElementValue(journalIssueElement, "Issue");
                
                Element pubDataElement = XMLUtils.getSingleElement(journalIssueElement, "PubDate");
                
                if (pubDataElement != null)
                {
                    year = XMLUtils.getElementValue(pubDataElement, "Year");
                    String mounth = XMLUtils.getElementValue(pubDataElement, "Month");
                    String day = XMLUtils.getElementValue(pubDataElement, "Day");
                    if (StringUtils.isNotBlank(mounth) && mounthToNum.containsKey(mounth))
                    {
                        year += "-" + mounthToNum.get(mounth);
                        if (StringUtils.isNotBlank(day))
                        {
                            year += "-" + (day.length() == 1 ? "0" + day : day);
                        }
                    }
                }
            }
            
            language = XMLUtils.getElementValue(article, "Language");
            
            Element publicationTypeList = XMLUtils.getSingleElement(article, "PublicationTypeList");
            if (publicationTypeList != null)
            {
                List<Element> publicationTypes = XMLUtils.getElementList(publicationTypeList, "PublicationType");
                for (Element publicationType : publicationTypes)
                {
                    type.add(publicationType.getTextContent().trim());
                }
            }
            
            Element keywordsList = XMLUtils.getSingleElement(medline, "KeywordList");
            if (keywordsList != null)
            {
                List<Element> keywords = XMLUtils.getElementList(keywordsList, "Keyword");
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
            
            Element meshHeadingsList = XMLUtils.getSingleElement(medline, "MeshHeadingList");
            if (meshHeadingsList != null)
            {
                List<Element> meshHeadings = XMLUtils.getElementList(meshHeadingsList, "MeshHeading");
                for (Element meshHeading : meshHeadings)
                {
                    if ("Y".equals(XMLUtils.getElementAttribute(meshHeading, "DescriptorName", "MajorTopicYN")))
                    {
                        primaryMeshHeadings.add(XMLUtils.getElementValue(meshHeading, "DescriptorName"));
                    }
                    else
                    {
                        secondaryMeshHeadings.add(XMLUtils.getElementValue(meshHeading, "DescriptorName"));
                    }
                }
            }
         
            Element paginationElement = XMLUtils.getSingleElement(article, "Pagination");
            if (paginationElement != null)
            {
                startPage = XMLUtils.getElementValue(paginationElement, "StartPage");
                endPage = XMLUtils.getElementValue(paginationElement, "EndPage");
                if (StringUtils.isBlank(startPage))
                {
                    startPage = XMLUtils.getElementValue(paginationElement, "MedlinePgn");
                }
            }
        }
	}

    public String getPubmedID()
    {
        return pubmedID;
    }

    public String getDoi()
    {
        return doi;
    }

    public String getIssn()
    {
        return issn;
    }

    public String getEissn()
    {
        return eissn;
    }

    public String getJournalTitle()
    {
        return journalTitle;
    }

    public String getTitle()
    {
        return title;
    }

    public String getPubblicationModel()
    {
        return pubblicationModel;
    }

    public String getYear()
    {
        return year;
    }

    public String getVolume()
    {
        return volume;
    }

    public String getIssue()
    {
        return issue;
    }

    public String getLanguage()
    {
        return language;
    }

    public List<String> getType()
    {
        return type;
    }

    public List<String> getPrimaryKeywords()
    {
        return primaryKeywords;
    }

    public List<String> getSecondaryKeywords()
    {
        return secondaryKeywords;
    }

    public List<String> getPrimaryMeshHeadings()
    {
        return primaryMeshHeadings;
    }

    public List<String> getSecondaryMeshHeadings()
    {
        return secondaryMeshHeadings;
    }

    public String getStartPage()
    {
        return startPage;
    }

    public String getEndPage()
    {
        return endPage;
    }

    public String getSummary()
    {
        return summary;
    }

    public String getStatus()
    {
        return status;
    }

    public List<String[]> getAuthors()
    {
        return authors;
    }
    
    @Override
	public void setSource(String source) {
		this.source = source;
	}

	@Override
	public void setRecord(String record) {
		this.record = record;
	}

	@Override
	public String getRecord() {
		return record;
	}
	
	@Override
	public String getSource() {
		return source;
	}
}