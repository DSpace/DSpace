package org.dspace.app.importer.crossref;


import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

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

public class CrossrefItem implements ItemImport
{
	// Nome della classe istanziata
	private String source;
	// Valore del metadato source
	private String record;
	
    private String doi;

    private String itemType;

    private String issn;

    private String eissn;

    private String isbn;

    private String seriesTitle;

    private String journalTitle;

    private String volumeTitle;

    private List<String[]> authors = new LinkedList<String[]>();

    private List<String[]> editors = new LinkedList<String[]>();

    private List<String[]> translators = new LinkedList<String[]>();

    private List<String[]> chairs = new LinkedList<String[]>();

    private String volume;

    private String issue;

    private String firstPage;

    private String lastPage;

    private String editionNumber;

    private String year;

    private String publicationType;

    private String articleTitle;

    public CrossrefItem(InputStream xmlData) throws JDOMException, IOException,
            ParserConfigurationException, SAXException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);

        DocumentBuilder db = factory.newDocumentBuilder();
        Document inDoc = db.parse(xmlData);

        Element xmlRoot = inDoc.getDocumentElement();
        Element queryResult = ImporterUtils.getSingleElement(xmlRoot, "query_result");
        Element body = ImporterUtils.getSingleElement(xmlRoot, "body");
        Element dataRoot = ImporterUtils.getSingleElement(xmlRoot, "query");
        String status = dataRoot.getAttribute("status");
//        <xsd:enumeration value="resolved"/>
//        <xsd:enumeration value="unresolved"/>
//        <xsd:enumeration value="multiresolved"/>
//        <xsd:enumeration value="malformed"/>
        if (!"resolved".equals(status)) {
            String msg = ImporterUtils.getElementValue(dataRoot, "msg");
            String exMsg = status + " - " + msg;
            throw new RuntimeException(exMsg);
        }
        doi = ImporterUtils.getElementValue(dataRoot, "doi");
        itemType = doi != null ? ImporterUtils.getElementAttribute(dataRoot, "doi",
                "type") : "unspecified";

        List<Element> identifier = ImporterUtils.getElementList(dataRoot, "issn");
        for (Element ident : identifier)
        {
            if ("print".equalsIgnoreCase(ident.getAttribute("type"))
                    || StringUtils.isEmpty(ident.getAttribute("type")))
            {
                issn = ident.getTextContent().trim();
            }
            else
            {
                eissn = ident.getTextContent().trim();
            }
        }

        isbn = ImporterUtils.getElementValue(dataRoot, "isbn");
        editionNumber = ImporterUtils.getElementValue(dataRoot, "editionNumber");
        volume = ImporterUtils.getElementValue(dataRoot, "volume");
        issue = ImporterUtils.getElementValue(dataRoot, "issue");
        year = ImporterUtils.getElementValue(dataRoot, "year");
        firstPage = ImporterUtils.getElementValue(dataRoot, "first_page");
        lastPage = ImporterUtils.getElementValue(dataRoot, "last_page");
        seriesTitle = ImporterUtils.getElementValue(dataRoot, "series_title");
        journalTitle = ImporterUtils.getElementValue(dataRoot, "journal_title");
        volumeTitle = ImporterUtils.getElementValue(dataRoot, "volume_title");
        articleTitle = ImporterUtils.getElementValue(dataRoot, "article_title");
        publicationType = ImporterUtils.getElementValue(dataRoot, "pubblication_type");

        List<Element> contributors = ImporterUtils.getElementList(dataRoot, "contributors");
        List<Element> contributor = ImporterUtils.getElementList(contributors.get(0), "contributor");
        		
        for (Element contrib : contributor) {
        	
            String givenName = ImporterUtils.getElementValue(contrib, "given_name");
            String surname = ImporterUtils.getElementValue(contrib, "surname");

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

    public String getDoi()
    {
        return doi;
    }

    public String getItemType()
    {
        return itemType;
    }

    public String getIssn()
    {
        return issn;
    }

    public String getEissn()
    {
        return eissn;
    }

    public String getIsbn()
    {
        return isbn;
    }

    public String getSeriesTitle()
    {
        return seriesTitle;
    }

    public String getJournalTitle()
    {
        return journalTitle;
    }

    public String getVolumeTitle()
    {
        return volumeTitle;
    }

    public List<String[]> getAuthors()
    {
        return authors;
    }

    public List<String[]> getEditors()
    {
        return editors;
    }

    public List<String[]> getTranslators()
    {
        return translators;
    }

    public List<String[]> getChairs()
    {
        return chairs;
    }

    public String getVolume()
    {
        return volume;
    }

    public String getIssue()
    {
        return issue;
    }

    public String getFirstPage()
    {
        return firstPage;
    }

    public String getLastPage()
    {
        return lastPage;
    }

    public String getEditionNumber()
    {
        return editionNumber;
    }

    public String getYear()
    {
        return year;
    }

    public String getPublicationType()
    {
        return publicationType;
    }

    public String getArticleTitle()
    {
        return articleTitle;
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
