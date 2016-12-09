/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.Properties;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.Serializer;

/**
 * A representation of a SWORD Service Document.
 * 
 * http://www.ukoln.ac.uk/repositories/digirep/index/SWORD_APP_Profile_0.5
 * 
 * @author Stuart Lewis
 * @author Neil Taylor
 */
public class ServiceDocument {
    /**
     * The Service object that is held by this object.
     */
    private Service service;

    /**
     * Create a new instance and set the initial service level to Zero.
     */
    public ServiceDocument() {
        
    }

    /**
     * Create a new instance and set the specified service level.
     * 
     * @param version 
     *             The SWORD version.
     */
    public ServiceDocument(String version) {
        service = new Service(version);
    }

    /**
     * Create a new instance and store the specified Service document.
     * 
     * @param service
     *            The Service object.
     */
    public ServiceDocument(Service service) {
        this.service = service;
    }

    /**
     * Set the service object associated with this document.
     * 
     * @param service
     *            The new Service object.
     */
    public void setService(Service service) {
        this.service = service;
    }

    /**
     * Retrieve the Service object associated with this document.
     * 
     * @return The Service object.
     */
    public Service getService() {
        return service;
    }

    /**
     * Return the Service Document in its XML form.
     * 
     * @return The ServiceDocument
     */
    public String toString() {
        return marshall();
    }

    /**
     * Marshall the data in the Service element and generate a String
     * representation. The returned string is UTF-8 format.
     * 
     * @return A string of XML, or <code>null</code> if there was an error
     *         marshalling the data.
     */
    public String marshall() {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Serializer serializer = new Serializer(stream, "UTF-8");
            serializer.setIndent(3);
            //serializer.setMaxLength(64);

            Document doc = new Document(service.marshall());
            serializer.write(doc);

            return stream.toString();
        } catch (IOException ex) {
            System.err.println(ex);
        }

        return null;
    }

    /**
     * Convert the specified XML string into a set of objects used within the
     * service. A new Service object will be created and stored. This will
     * dispose of any previous Service object associated with this object.
     * 
     * @param xml
     *            The XML string.
     * @throws UnmarshallException
     *     If there was a problem unmarshalling the data. This might be
     *     as a result of an error in parsing the XML string, extracting
     *     information.
     */
    public void unmarshall(String xml) throws UnmarshallException
    {
       unmarshall(xml, null);
    }

    /**
     * Convert the specified XML string into a set of objects used within the
     * service. A new Service object will be created and stored. This will
     * dispose of any previous Service object associated with this object.
     *
     * @param xml The XML string.
     * @param validationProperties FIXME: PLEASE DOCUMENT.
     * @return SWORD validation info
     * @throws UnmarshallException
     *     If there was a problem unmarshalling the data. This might be
     *     as a result of an error in parsing the XML string, extracting
     *     information.
     */
    public SwordValidationInfo unmarshall(String xml, Properties validationProperties)
    throws UnmarshallException
    {
        try {
            Builder builder = new Builder();
            Document doc = builder.build(xml, Namespaces.PREFIX_APP);
            Element root = doc.getRootElement();
            return unmarshall(root, validationProperties);
        } catch (ParsingException ex) {
            throw new UnmarshallException("Unable to parse the XML", ex);
        } catch (IOException ex) {
            throw new UnmarshallException("Error acessing the file?", ex);
        }
    }


    /**
     * Unmarshall the specified element. This version does not generate any
     * validation information.
     *
     * @param element
     *     element to unmarshall
     * @throws UnmarshallException
     *     If there was a problem unmarshalling the data. This might be
     *     as a result of an error in parsing the XML string, extracting
     *     information.
     */
    public void unmarshall(Element element)
    throws UnmarshallException
    {
       unmarshall(element, null);
    }

    /**
     * Unmarshall the specified element, and return the generated validation
     * information.
     * 
     * @param element
     *     element to unmarshall.
     * @param validationProperties
     *     FIXME: PLEASE DOCUMENT.
     * @return SWORD validation info
     * @throws UnmarshallException
     *     If there was a problem unmarshalling the data. This might be
     *     as a result of an error in parsing the XML string, extracting
     *     information.
     */
    public SwordValidationInfo unmarshall(Element element, Properties validationProperties)
    throws UnmarshallException
    {
        service = new Service();
        try {
            return service.unmarshall(element, validationProperties);
        } catch (UnmarshallException e) {
            throw new UnmarshallException("Unable to parse the XML", e);
        }
    }


    public SwordValidationInfo validate()
    {
        if ( service == null )
        {
            return null;
        }
        return service.validate(new Properties());
    }

    public SwordValidationInfo validate(Properties validationContext)
    {
        if ( service == null)
        {
            return null;
        }
        return service.validate(validationContext);
    }
}
