/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.importer.crossref;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

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
        Element queryResult = XMLUtils.getSingleElement(xmlRoot, "query_result");
        Element body = XMLUtils.getSingleElement(xmlRoot, "body");
        Element dataRoot = XMLUtils.getSingleElement(xmlRoot, "query");
        loadData(dataRoot);
    }

    public CrossrefItem(Element dataRoot) {
    	loadData(dataRoot);
	}
    
	private void loadData(Element dataRoot) {
		String status = dataRoot.getAttribute("status");
//        <xsd:enumeration value="resolved"/>
//        <xsd:enumeration value="unresolved"/>
//        <xsd:enumeration value="multiresolved"/>
//        <xsd:enumeration value="malformed"/>
        if (!"resolved".equals(status)) {
            String msg = XMLUtils.getElementValue(dataRoot, "msg");
            String exMsg = status + " - " + msg;
            throw new RuntimeException(exMsg);
        }
        doi = XMLUtils.getElementValue(dataRoot, "doi");
        itemType = doi != null ? XMLUtils.getElementAttribute(dataRoot, "doi",
                "type") : "unspecified";

        List<Element> identifier = XMLUtils.getElementList(dataRoot, "issn");
        for (Element ident : identifier)
        {
            if ("print".equalsIgnoreCase(ident.getAttribute("type"))
                    || StringUtils.isNotBlank(ident.getAttribute("type")))
            {
                issn = ident.getTextContent().trim();
            }
            else
            {
                eissn = ident.getTextContent().trim();
            }
        }

        isbn = XMLUtils.getElementValue(dataRoot, "isbn");
        editionNumber = XMLUtils.getElementValue(dataRoot, "editionNumber");
        volume = XMLUtils.getElementValue(dataRoot, "volume");
        issue = XMLUtils.getElementValue(dataRoot, "issue");
        year = XMLUtils.getElementValue(dataRoot, "year");
        firstPage = XMLUtils.getElementValue(dataRoot, "first_page");
        lastPage = XMLUtils.getElementValue(dataRoot, "last_page");
        seriesTitle = XMLUtils.getElementValue(dataRoot, "series_title");
        journalTitle = XMLUtils.getElementValue(dataRoot, "journal_title");
        volumeTitle = XMLUtils.getElementValue(dataRoot, "volume_title");
        articleTitle = XMLUtils.getElementValue(dataRoot, "article_title");
        publicationType = XMLUtils.getElementValue(dataRoot, "pubblication_type");

        List<Element> contributors = XMLUtils.getElementList(dataRoot, "contributors");
        List<Element> contributor = null;
        if (contributors != null && contributors.size() > 0)
        {
        	contributor = XMLUtils.getElementList(contributors.get(0), "contributor");
	        		
	        for (Element contrib : contributor) {
	        	
	            String givenName = XMLUtils.getElementValue(contrib, "given_name");
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
