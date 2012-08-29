package org.dspace.app.importer.pubmed;


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
import org.dspace.app.importer.ItemImport;
import org.dspace.app.importer.ImporterUtils;
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
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);

        DocumentBuilder db = factory.newDocumentBuilder();
        Document inDoc = db.parse(xmlData);

        Element xmlRoot = inDoc.getDocumentElement();
        Element pubArticle = ImporterUtils
                .getSingleElement(xmlRoot, "PubmedArticle");
        
        Element medline = ImporterUtils.getSingleElement(pubArticle, "MedlineCitation");
        
        Element article = ImporterUtils.getSingleElement(medline, "Article");
        Element pubmed = ImporterUtils.getSingleElement(pubArticle, "PubmedData");
        
        Element identifierList = ImporterUtils.getSingleElement(pubmed, "ArticleIdList");
        if (identifierList != null)
        {
            List<Element> identifiers = ImporterUtils.getElementList(pubmed, "ArticleId");
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
        
        status = ImporterUtils.getElementValue(pubmed, "PublicationStatus");
        pubblicationModel = ImporterUtils.getElementAttribute(medline, "Article", "PubModel");
        title = ImporterUtils.getElementValue(article, "ArticleTitle");
        Element abstractElement = ImporterUtils.getSingleElement(medline, "Abstract");
        if (abstractElement == null)
        {
            abstractElement = ImporterUtils.getSingleElement(medline, "OtherAbstract");
        }
        if (abstractElement != null)
        {
            summary = ImporterUtils.getElementValue(abstractElement, "AbstractText");
        }
        
        Element authorList = ImporterUtils.getSingleElement(article, "AuthorList");
        if (authorList != null)
        {
            List<Element> authorsElement = ImporterUtils.getElementList(authorList, "Author");
            if (authorsElement != null)
            {
                for (Element author : authorsElement)
                {
                    if (StringUtils.isBlank(ImporterUtils.getElementValue(author, "CollectiveName")))
                    {
                        authors.add(new String[]{ImporterUtils.getElementValue(author, "ForeName"),ImporterUtils.getElementValue(author, "LastName")});
                    }
                }
            }
        }
        
        Element journal = ImporterUtils.getSingleElement(article, "Journal");
        if (journal != null)
        {
            List<Element> jnumbers = ImporterUtils.getElementList(journal, "ISSN");
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
            
            journalTitle = ImporterUtils.getElementValue(journal, "Title");
            Element journalIssueElement = ImporterUtils.getSingleElement(journal, "JournalIssue");
            if (journalIssueElement != null)
            {
                volume = ImporterUtils.getElementValue(journalIssueElement, "Volume");
                issue = ImporterUtils.getElementValue(journalIssueElement, "Issue");
                
                Element pubDataElement = ImporterUtils.getSingleElement(journalIssueElement, "PubDate");
                
                if (pubDataElement != null)
                {
                    year = ImporterUtils.getElementValue(pubDataElement, "Year");
                    String mounth = ImporterUtils.getElementValue(pubDataElement, "Month");
                    String day = ImporterUtils.getElementValue(pubDataElement, "Day");
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
            
            language = ImporterUtils.getElementValue(article, "Language");
            
            Element publicationTypeList = ImporterUtils.getSingleElement(article, "PublicationTypeList");
            if (publicationTypeList != null)
            {
                List<Element> publicationTypes = ImporterUtils.getElementList(publicationTypeList, "PublicationType");
                for (Element publicationType : publicationTypes)
                {
                    type.add(publicationType.getTextContent().trim());
                }
            }
            
            Element keywordsList = ImporterUtils.getSingleElement(medline, "KeywordList");
            if (keywordsList != null)
            {
                List<Element> keywords = ImporterUtils.getElementList(keywordsList, "Keyword");
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
            
            Element meshHeadingsList = ImporterUtils.getSingleElement(medline, "MeshHeadingList");
            if (meshHeadingsList != null)
            {
                List<Element> meshHeadings = ImporterUtils.getElementList(meshHeadingsList, "MeshHeading");
                for (Element meshHeading : meshHeadings)
                {
                    if ("Y".equals(ImporterUtils.getElementAttribute(meshHeading, "DescriptorName", "MajorTopicYN")))
                    {
                        primaryMeshHeadings.add(ImporterUtils.getElementValue(meshHeading, "DescriptorName"));
                    }
                    else
                    {
                        secondaryMeshHeadings.add(ImporterUtils.getElementValue(meshHeading, "DescriptorName"));
                    }
                }
            }
         
            Element paginationElement = ImporterUtils.getSingleElement(article, "Pagination");
            if (paginationElement != null)
            {
                startPage = ImporterUtils.getElementValue(paginationElement, "StartPage");
                endPage = ImporterUtils.getElementValue(paginationElement, "EndPage");
                if (StringUtils.isBlank(startPage))
                {
                    startPage = ImporterUtils.getElementValue(paginationElement, "MedlinePgn");
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