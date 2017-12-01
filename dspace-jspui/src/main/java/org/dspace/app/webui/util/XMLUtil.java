/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.Map;

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
    public static Document transformDocument(Document xmlDocument, Map<String, String> parameters, String xsltFilename) throws Exception
    {

        // Generate a Transformer.
        Transformer transformer = TransformerFactory.newInstance()
                .newTransformer(new StreamSource(xsltFilename));

        // set transformation parameters
        if (parameters != null)
        {
            for (Map.Entry<String, String> param : parameters.entrySet())
            {
                transformer.setParameter(param.getKey(), param.getValue());
            }

        }

        // Create an empty DOMResult object for the output.
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
    public static String transformDocumentAsString(Document xmlDocument, Map<String, String> parameters, String xsltFilename) throws Exception
    {

        // Generate a Transformer.
        Transformer transformer = TransformerFactory.newInstance()
                .newTransformer(new StreamSource(xsltFilename));

        // set transformation parameters
        if (parameters != null)
        {
            for (Map.Entry<String, String> param : parameters.entrySet())
            {
                transformer.setParameter(param.getKey(), param.getValue());
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
