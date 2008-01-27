/*
 * XMLUtil.java
 * 
 * Version: $Revision$
 * 
 * Date: $Date$
 * 
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts Institute of
 * Technology. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: -
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. - Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. - Neither the name of the Hewlett-Packard Company nor
 * the name of the Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.dspace.app.webui.util;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * This class provides a set of static methods to load and transform XML
 * documents. It supports parameter-aware stylesheets (XSLT).
 * 
 * @author Miguel Ferreira
 * 
 */
public class XMLUtil
{

    /**
     * Loads a W3C XML document from a file.
     * 
     * @param filename
     *            The name of the file to be loaded
     * @return a document object model object representing the XML file
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static Document loadXML(String filename) throws IOException,
            ParserConfigurationException, SAXException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
        return builder.parse(new File(filename));
    }

    /**
     * Applies a stylesheet to a given xml document.
     * 
     * @param xmlDocument
     *            the xml document to be transformed
     * @param xsltFilename
     *            the filename of the stylesheet
     * @return the transformed xml document
     * @throws Exception
     */
    public static Document transformDocument(Document xmlDocument,
            String xsltFilename) throws Exception
    {
        return transformDocument(xmlDocument, new Hashtable(), xsltFilename);
    }

    /**
     * Applies a stylesheet (that receives parameters) to a given xml document.
     * 
     * @param xmlDocument
     *            the xml document to be transformed
     * @param parameters
     *            the hashtable with the parameters to be passed to the
     *            stylesheet
     * @param xsltFilename
     *            the filename of the stylesheet
     * @return the transformed xml document
     * @throws Exception
     */
    public static Document transformDocument(Document xmlDocument,
            Hashtable parameters, String xsltFilename) throws Exception
    {

        // Generate a Transformer.
        Transformer transformer = TransformerFactory.newInstance()
                .newTransformer(new StreamSource(xsltFilename));

        // set transformation parameters
        if (parameters != null)
        {
            Enumeration keys = parameters.keys();
            while (keys.hasMoreElements())
            {
                String key = (String) keys.nextElement();
                String value = (String) parameters.get(key);
                transformer.setParameter(key, value);
            }

        }

        // Create an empy DOMResult object for the output.
        DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
        dFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
        Document dstDocument = dBuilder.newDocument();

        DOMResult domResult = new DOMResult(dstDocument);

        // Perform the transformation.
        transformer.transform(new DOMSource(xmlDocument), domResult);
        // Now you can get the output Node from the DOMResult.
        return dstDocument;
    }

    /**
     * Applies a stylesheet (that receives parameters) to a given xml document.
     * The resulting XML document is converted to a string after transformation.
     * 
     * @param xmlDocument
     *            the xml document to be transformed
     * @param parameters
     *            the hashtable with the parameters to be passed to the
     *            stylesheet
     * @param xsltFilename
     *            the filename of the stylesheet
     * @return the transformed xml document as a string
     * @throws Exception
     */
    public static String transformDocumentAsString(Document xmlDocument,
            Hashtable parameters, String xsltFilename) throws Exception
    {

        // Generate a Transformer.
        Transformer transformer = TransformerFactory.newInstance()
                .newTransformer(new StreamSource(xsltFilename));

        // set transformation parameters
        if (parameters != null)
        {
            Enumeration keys = parameters.keys();
            while (keys.hasMoreElements())
            {
                String key = (String) keys.nextElement();
                String value = (String) parameters.get(key);
                transformer.setParameter(key, value);
            }

        }

        StringWriter stringWriter = new StringWriter();
        StreamResult streamResult = new StreamResult(stringWriter);

        // Perform the transformation.
        transformer.transform(new DOMSource(xmlDocument), streamResult);
        // Now you can get the output Node from the DOMResult.
        return stringWriter.toString();
    }

    /**
     * Applies a stylesheet to a given xml document.
     * 
     * @param xmlDocument
     *            the xml document to be transformed
     * @param xsltFilename
     *            the filename of the stylesheet
     * @return the transformed xml document
     * @throws Exception
     */
    public static String transformDocumentAsString(Document xmlDocument,
            String xsltFilename) throws Exception
    {
        // Generate a Transformer.
        Transformer transformer = TransformerFactory.newInstance()
                .newTransformer(new StreamSource(xsltFilename));

        StringWriter stringWriter = new StringWriter();
        StreamResult streamResult = new StreamResult(stringWriter);

        // Perform the transformation.
        transformer.transform(new DOMSource(xmlDocument), streamResult);
        // Now you can get the output Node from the DOMResult.
        return stringWriter.toString();
    }

}
