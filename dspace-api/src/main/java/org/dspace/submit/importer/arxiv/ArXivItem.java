/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.importer.arxiv;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.dspace.app.util.XMLUtils;
import org.dspace.submit.importer.ItemImport;
import org.jdom.JDOMException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class ArXivItem implements ItemImport {
	
	// Nome della classe istanziata
	private String source;
	// Valore del metadato source
	private String record;

    private List<String[]> authors = new LinkedList<String[]>();

    private String year;

    private String articleTitle;

    private String splashPageUrl;

    private String comment;

    private String summary;

    private String pdfUrl;
    
    private String journalRef;
    
    private String doi;

    private List<String> primaryCategory = new LinkedList<String>();

    private List<String> category = new LinkedList<String>();

    public ArXivItem(Element dataRoot) throws JDOMException, IOException,
            ParserConfigurationException, SAXException
    {
        articleTitle = XMLUtils.getElementValue(dataRoot, "title");
        summary = XMLUtils.getElementValue(dataRoot, "summary");
        year = XMLUtils.getElementValue(dataRoot, "published");
        splashPageUrl = XMLUtils.getElementValue(dataRoot, "id");
        comment= XMLUtils.getElementValue(dataRoot, "arxiv:comment");

        List<Element> links = XMLUtils.getElementList(dataRoot, "link");
        if (links != null)
        {
            for (Element link : links)
            {
                if ("related".equals(link.getAttribute("rel")) && "pdf".equals(link.getAttribute("title")))
                {
                        pdfUrl = link.getAttribute("href");
                }
            }
        }
        
        doi = XMLUtils.getElementValue(dataRoot, "arxiv:doi");
        journalRef = XMLUtils.getElementValue(dataRoot, "arxiv:journal_ref");
        
        List<Element> primaryCategoryList = XMLUtils.getElementList(dataRoot, "arxiv:primary_category");
        if (primaryCategoryList != null)
        {
            for (Element primaryCategoryElement : primaryCategoryList)
            {
                primaryCategory.add(primaryCategoryElement.getAttribute("term"));
            }
        }
        
        List<Element> categoryList = XMLUtils.getElementList(dataRoot, "category");
        if (categoryList != null)
        {
            for (Element categoryElement : categoryList)
            {
                category.add(categoryElement.getAttribute("term"));
            }
        }
        
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
                authors.add(new String[]{nome, cognome});
            }
        }
    }

    public List<String[]> getAuthors()
    {
        return authors;
    }

    public String getYear()
    {
        return year;
    }
    
    public String getArticleTitle()
    {
        return articleTitle;
    }

    public String getSplashPageUrl()
    {
        return splashPageUrl;
    }

    public String getComment()
    {
        return comment;
    }

    public String getSummary()
    {
        return summary;
    }

    public String getPdfUrl()
    {
        return pdfUrl;
    }

    public String getDoi()
    {
        return doi;
    }
    
    public String getJournalRef()
    {
        return journalRef;
    }
    
    public List<String> getPrimaryCategory()
    {
        return primaryCategory;
    }

    public List<String> getCategory()
    {
        return category;
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