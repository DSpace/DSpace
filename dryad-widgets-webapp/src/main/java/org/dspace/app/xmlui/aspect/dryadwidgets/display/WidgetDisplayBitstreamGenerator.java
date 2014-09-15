
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.dryadwidgets.display;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.AbstractGenerator;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.BaseBitstreamHandler;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.DefaultBitstreamHandler;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text.CSV;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text.Plain;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;


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
    
    private final static HashMap<String,String> bitstreamHandlerClasses = new HashMap<String, String>();
    static {
        bitstreamHandlerClasses.put("text/plain",   "org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text.Plain");
        bitstreamHandlerClasses.put("text/csv",     "org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text.CSV");
    }

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
        connection.disconnect();
        connection = null;
        objUrl = null;

        // check for content-specific handler
        BaseBitstreamHandler handler = null;
        if (bitstreamHandlerClasses.containsKey(dataOneFormat)) {
            try {
                Class cls = Class.forName(bitstreamHandlerClasses.get(dataOneFormat));
                Constructor ctor = cls.getConstructor(String.class, String.class, ContentHandler.class, LexicalHandler.class, SourceResolver.class);
                handler = (BaseBitstreamHandler) ctor.newInstance(object_url, dataOneFormat, super.contentHandler, super.lexicalHandler, super.resolver);
            } catch (Exception ex) {
                log.error(ex.getMessage());
                throw new ProcessingException("Bitstream handler instantiaion error for doi: " + doi);
            }
        }
        if (handler == null) {
            try {
                handler = new DefaultBitstreamHandler(object_url, dataOneFormat, super.contentHandler, super.lexicalHandler, super.resolver);
            } catch (Exception ex) {
                log.error(ex.getMessage());
                throw new ProcessingException("Bitstream handler instantiaion error for doi: " + doi);
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
        } catch (IOException e) {
            log.error("IOException: Failed to generate bitstream content: " + e.getMessage().toString());
            throw new ProcessingException("Bitstream generator error (IOException) for doi: " + doi);            
        }
    }    
}
