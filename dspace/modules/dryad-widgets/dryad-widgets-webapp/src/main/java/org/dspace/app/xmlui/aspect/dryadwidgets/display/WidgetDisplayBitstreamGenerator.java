
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.dryadwidgets.display;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.AbstractGenerator;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.BaseBitstreamHandler;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.DefaultBitstreamHandler;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text.CSV;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text.Plain;
//import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Application.vnd_openxmlformats_officedocument_spreadsheetml_sheet;

/**
 * This generator uses a DOI to locate a bitstream object and convert its content
 * to a type handlable by the Data Display Widget's data frame.
 * The publisher parameter must be provided, but is not currently recorded here.
 *
 * @author Nathan Day
 */
public class WidgetDisplayBitstreamGenerator extends AbstractGenerator {
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WidgetDisplayBitstreamGenerator.class);

    // Data-One service response headers
    private static final String DOFid = "DataONE-formatId";
    private static final String ContLen = "Content-Length";
    
    private final static HashMap<String,String> bitstreamHandlerClasses = new HashMap<String, String>();
    static {
        bitstreamHandlerClasses.put("text/plain",           "org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text.Plain");
        bitstreamHandlerClasses.put("application/x-python", "org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text.Plain");
        bitstreamHandlerClasses.put("text/csv",             "org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text.CSV");
        /*
        bitstreamHandlerClasses.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Application.vnd_openxmlformats_officedocument_spreadsheetml_sheet");
        */
        bitstreamHandlerClasses.put("image/png","org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.DefaultBitstreamHandler");
        bitstreamHandlerClasses.put("image/jpeg","org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.DefaultBitstreamHandler");
    }

    private SourceResolver resolver;
    private Map objectModel;
    private Parameters parameters;
    private String source;

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters)
            throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver, objectModel, src, parameters);
        this.resolver = resolver;
        this.objectModel = objectModel;
        this.source = src;
        this.parameters = parameters;
    }
    @Override
    public void recycle() {
        super.recycle();
        this.resolver = null;
        this.objectModel = null;
        this.source = null;
        this.parameters = null;
    }

    @Override
    public void generate() throws IOException, SAXException, ProcessingException {
        String referrer     = parameters.getParameter("referrer","");
        String doi          = parameters.getParameter("doi","");
        String bitstream    = parameters.getParameter("bitstream",""); // value="http://localhost:{request:serverPort}/mn/object/{request-param:doi}/bitstream"
    
        Parameters p2 = new Parameters();
        p2.merge(parameters);

        if (doi == null || doi.equals("")) {
            throw new IllegalArgumentException("Empty or null doi provided to WidgetDisplayBitstreamGenerator");
        }
        log.debug("In WidgetDisplayBitstreamGenerator for referrer '" + referrer + "' and doi: " + doi);
        URL objUrl = new URL(bitstream);
        HttpURLConnection connection;
        String dataOneFormat;
        Integer responseCode;
        // Do an initial HTTP HEAD request from Data-One service to determine
        // - if requested resource exists
        // - if the requested resource has a handlable content type
        // If successful on both counts, GET the resource
        try {
            log.debug("Opening connection to DataOne Server for url: " + bitstream);
            connection = (HttpURLConnection) objUrl.openConnection();
            connection.setRequestMethod("HEAD");
            responseCode = connection.getResponseCode();
            log.debug("DataOne Server sent response code " + Integer.toString(responseCode) + "' for url '" + bitstream + "'");
            dataOneFormat = connection.getHeaderField(DOFid);
            p2.setParameter(DOFid, dataOneFormat);
            p2.setParameter(ContLen, connection.getHeaderField(ContLen));
            log.debug("DataOne reports format '" + dataOneFormat + "' for doi '" + doi + "'");
        } catch (IOException e) {
            throw new IOException("Failed to connect to DataOne service");
        }
        if (responseCode != HttpURLConnection.HTTP_OK) {
            log.error("Response code '" + responseCode + "' for doi '" + doi + "' at url: " + bitstream);
            throw new ResourceNotFoundException("Response code '" + responseCode + "' for doi '" + doi + "' at url: " + bitstream);
        } else if (dataOneFormat == null || dataOneFormat.equals("")) {
            log.error("Unavailable content type for doi: " + doi);
            throw new IOException("Unavailable content type for doi '" + doi + "' at url: " + bitstream);
        }
        connection.disconnect();
        connection = null;
        objUrl = null;

        // check for content-specific handler
        BaseBitstreamHandler handler = null;
        if (bitstreamHandlerClasses.containsKey(dataOneFormat)) {
            try {
                Class cls = Class.forName(bitstreamHandlerClasses.get(dataOneFormat));
                Constructor ctor = cls.getConstructor(ContentHandler.class, LexicalHandler.class);
                handler = (BaseBitstreamHandler) ctor.newInstance(super.contentHandler, super.lexicalHandler);
            } catch (Exception ex) {
                log.error(ex.getMessage());
                throw new ProcessingException("Bitstream handler instantiaion error for doi: " + doi);
            }
        }
        if (handler == null) {
            try {
                handler = new DefaultBitstreamHandler(super.contentHandler, super.lexicalHandler);
            } catch (Exception ex) {
                log.error(ex.getMessage());
                throw new ProcessingException("Bitstream handler instantiaion error for doi: " + doi);
            }
        }
        log.trace("Using handler '" + handler.getClass().getName() + "' for doi: " + doi);
        try {
            handler.setup(resolver, objectModel, bitstream, p2);    // should be BaseBitstreamHandler.setup()
            handler.generate(); // should be subclass of BaseBitstreamHandler
            handler.recycle();  // should be BaseBitstreamHandler.recycle()
        } catch (SAXException e) {
            log.error("SAX Exception: Failed to generate bitstream content: " + e.getMessage().toString());
            throw new ProcessingException("Bitstream generator error (SAXException) for doi: " + doi);
        } catch (IOException e) {
            log.error("IOException: Failed to generate bitstream content: " + e.getMessage().toString());
            throw new ProcessingException("Bitstream generator error (IOException) for doi: " + doi);
        }
    }
}
