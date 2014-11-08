/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.submit.lookup;

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
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import gr.ekt.bte.core.DataLoadingSpec;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.Value;
import gr.ekt.bte.dataloader.FileDataLoader;
import gr.ekt.bte.exceptions.MalformedSourceException;

/**
 * Load metadata from CiNii formated file
 *
 * @author Keiji Suzuki
 * 
 */
public class CiNiiFileDataLoader extends FileDataLoader
{

    private static Logger log = Logger.getLogger(CiNiiFileDataLoader.class);

    Map<String, String> fieldMap; // mapping between service fields and local
                                  // intermediate fields

    /**
     * Empty constructor
     */
    public CiNiiFileDataLoader()
    {
    }

    /**
     * @param filename
     */
    public CiNiiFileDataLoader(String filename)
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

            DocumentBuilder db = factory.newDocumentBuilder();
            Document inDoc = db.parse(inputStream);

            Element xmlRoot = inDoc.getDocumentElement();

            // There is no element to represent an record, so we can not process 
            // multi records at once.
            Record record = CiNiiUtils.convertCiNiiDomToRecord(xmlRoot);
            if (record != null)
            {
                recordSet.addRecord(convertFields(record));
            }
        }
        catch (FileNotFoundException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (ParserConfigurationException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (SAXException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
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
