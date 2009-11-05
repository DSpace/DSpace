/**
 * Copyright (c) 2008-2009, Aberystwyth University
 *
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *  - Redistributions of source code must retain the above 
 *    copyright notice, this list of conditions and the 
 *    following disclaimer.
 *  
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 *    
 *  - Neither the name of the Centre for Advanced Software and 
 *    Intelligent Systems (CASIS) nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF 
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
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
	 * 			The SWORD version.
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
	 * Return the Service Document in it's XML form.
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
	 *             If there was a problem unmarshalling the data. This might be
	 *             as a result of an error in parsing the XML string, extracting
	 *             information.
	 */
	public void unmarshall(String xml) throws UnmarshallException
    {
       unmarshall(xml, null);
    }

    /**
     * 
     * @param xml
     * @param validationProperties
     * @return
     * @throws org.purl.sword.base.UnmarshallException
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
     * valiation information.
     *
     * @param element
     * @throws org.purl.sword.base.UnmarshallException
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
     * @param validationProperties
     * @return
     * @throws org.purl.sword.base.UnmarshallException
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
        if( service == null )
        {
            return null;
        }
        return service.validate(new Properties());
    }

    public SwordValidationInfo validate(Properties validationContext)
    {
        if( service == null)
        {
            return null;
        }
        return service.validate(validationContext);
    }
}