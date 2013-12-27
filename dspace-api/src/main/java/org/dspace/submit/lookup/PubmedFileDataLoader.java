/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.submit.lookup;

import gr.ekt.bte.core.DataLoadingSpec;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.Value;
import gr.ekt.bte.dataloader.FileDataLoader;
import gr.ekt.bte.exceptions.MalformedSourceException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class PubmedFileDataLoader extends FileDataLoader
{

    Map<String, String> fieldMap; // mapping between service fields and local
                                  // intermediate fields

    /**
	 * 
	 */
    public PubmedFileDataLoader()
    {
    }

    /**
     * @param filename
     */
    public PubmedFileDataLoader(String filename)
    {
        super(filename);
    }

    /*
     * (non-Javadoc)
     * 
     * @see gr.ekt.bte.core.DataLoader#getRecords()
     */
    @Override
    public RecordSet getRecords() throws MalformedSourceException
    {

        RecordSet recordSet = new RecordSet();

        try
        {
            InputStream inputStream = new FileInputStream(new File(filename));

            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document inDoc = builder.parse(inputStream);

            Element xmlRoot = inDoc.getDocumentElement();
            List<Element> pubArticles = XMLUtils.getElementList(xmlRoot,
                    "PubmedArticle");

            for (Element xmlArticle : pubArticles)
            {
                Record record = null;
                try
                {
                    record = PubmedUtils.convertPubmedDomToRecord(xmlArticle);
                    recordSet.addRecord(convertFields(record));
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (ParserConfigurationException e)
        {
            e.printStackTrace();
        }
        catch (SAXException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return recordSet;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gr.ekt.bte.core.DataLoader#getRecords(gr.ekt.bte.core.DataLoadingSpec)
     */
    @Override
    public RecordSet getRecords(DataLoadingSpec spec)
            throws MalformedSourceException
    {
        if (spec.getOffset() > 0) 
        {
            return new RecordSet();
        }
        return getRecords();
    }

    public Record convertFields(Record publication)
    {
        for (String fieldName : fieldMap.keySet())
        {
            String md = null;
            if (fieldMap != null)
            {
                md = this.fieldMap.get(fieldName);
            }

            if (StringUtils.isBlank(md))
            {
                continue;
            }
            else
            {
                md = md.trim();
            }

            if (publication.isMutable())
            {
                List<Value> values = publication.getValues(fieldName);
                publication.makeMutable().removeField(fieldName);
                publication.makeMutable().addField(md, values);
            }
        }

        return publication;
    }

    public void setFieldMap(Map<String, String> fieldMap)
    {
        this.fieldMap = fieldMap;
    }
}
