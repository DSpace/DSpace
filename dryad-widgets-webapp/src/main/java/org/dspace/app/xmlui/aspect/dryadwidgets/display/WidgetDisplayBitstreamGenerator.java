
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.dryadwidgets.display;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.IOException;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.generation.AbstractGenerator;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.BaseBitstreamHandler;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.DefaultBitstreamHandler;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text_Plain;


/**
 * This generator uses a DOI to locate a bitstream object and convert its content
 * to a type handlable by the Data Display Widget's data frame.
 * The publisher parameter must be provided, but is not currently recorded here.
 *
 * @author Nathan Day
 */
public class WidgetDisplayBitstreamGenerator extends AbstractGenerator {
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WidgetDisplayBitstreamGenerator.class);

    private static final String DOFid = "DataONE-formatId";
    
    /*
    @Override 
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par) 
            throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver, objectModel, src, par);
    }
    @Override
    public void recycle()
    */
    
    @Override
    public void generate() throws IOException, SAXException, ProcessingException {
        String referrer     = parameters.getParameter("referrer","");
        String doi          = parameters.getParameter("doi","");
        String object_url   = parameters.getParameter("object-url",""); // value="http://localhost:{request:serverPort}/mn/object/{request-param:doi}/bitstream"
        if (doi == null || doi.equals("")) {
            throw new IllegalArgumentException("Empty or null doi provided to WidgetDisplayBitstreamGenerator");
        }
        log.debug("In WidgetDisplayBitstreamGenerator for referrer '" + referrer + "' and doi: " + doi);

        URL objUrl = new URL(object_url);
        HttpURLConnection connection;
        String dataOneFormat;
        int responseCode;
        
        // Do an initial HTTP HEAD request from Data-One service to determine
        // - if requested resource exists
        // - if the requested resource has a handlable content type
        // If successful on both counts, GET the resource
        try {
            log.debug("Opening connection to DataOne Server for url: " + object_url);
            connection = (HttpURLConnection) objUrl.openConnection();
            connection.setRequestMethod("HEAD");
            responseCode = connection.getResponseCode();
            log.debug("DataOne Server sent response code " + Integer.toString(responseCode) + "' for url '" + object_url + "'");
            dataOneFormat = connection.getHeaderField(DOFid);
            log.debug("DataOne reports format if '" + dataOneFormat + "' for doi '" + doi + "'");
        } catch (IOException e) {
            throw new IOException("Failed to connect to DataOne service");
        }
        if (responseCode != HttpURLConnection.HTTP_OK) {
            log.error("Resource not found for doi: " + doi);
            throw new ResourceNotFoundException("Resource not found for doi: " + doi);
        } else if (dataOneFormat == null || dataOneFormat.equals("")) {
            log.error("Unavailable content type for doi: " + doi);
            throw new IOException("Unavailable content type for doi: " + doi);
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        // check for content-specific handler
        BaseBitstreamHandler handler = null;
        if (dataOneFormat.equals("text/plain")) {
            try {
                handler = new Text_Plain(bufferedReader, super.contentHandler, super.lexicalHandler, dataOneFormat);
            } catch (SAXException e) {
                log.error("Failed to instantiate Text_Plain bitstream handler for format '" + dataOneFormat + "':  " + e.toString());
                throw new ProcessingException("Bitstream handler error for doi: " + doi);            
            }
        }
        if (handler == null) {
            try {
                handler = new DefaultBitstreamHandler(bufferedReader, super.contentHandler, super.lexicalHandler, dataOneFormat);
            } catch (SAXException e) {
                log.error("Failed to instantiate default bitstream handler: " + e.getMessage());
                throw new ProcessingException("Bitstream handler error for doi: " + doi);
            }
        }
        log.trace("Using handler '" + handler.getClass().getName() + "' for doi: " + doi);
        try {
            handler.start();    // should be BaseBitstreamHandler.start()
            handler.generate(); // should be subclass of BaseBitstreamHandler
            handler.end();      // should be BaseBitstreamHandler.end()
        } catch (SAXException e) {
            log.error("SAX Exception: Failed to generate bitstream content: " + e.getMessage().toString());
            throw new ProcessingException("Bitstream generator error (SAXException) for doi: " + doi);

        // TODO: work out this cleanup scope
        } catch (IOException e) {
            log.error("IOException: Failed to generate bitstream content: " + e.getMessage().toString());
            throw new ProcessingException("Bitstream generator error (IOException) for doi: " + doi);
            
            // TODO: work out this cleanup scope
        } finally {
            connection.disconnect();
            objUrl = null;
        }
    }    
}
