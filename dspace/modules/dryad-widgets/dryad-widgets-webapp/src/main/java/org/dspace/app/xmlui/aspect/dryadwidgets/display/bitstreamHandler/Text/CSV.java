/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.CSVGenerator2;
import org.apache.excalibur.source.Source;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.BaseBitstreamHandler;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.BitstreamXMLConsumer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
/**
 *
 * @author Nathan Day
 */
public class CSV extends BaseBitstreamHandler {

    public CSV(ContentHandler contentHandler, LexicalHandler lexicalHandler) {
        super(contentHandler, lexicalHandler);
    }

    /**
     * Generate a plain text data section, wrapped in <!CDATA[]]>.
     * @throws SAXException
     * @throws IOException
     * @throws org.apache.cocoon.ProcessingException
     */
    @Override
    public void generate() throws SAXException, IOException, ProcessingException {
        // TODO: CSVGenerator/FileGenerator from cocoon throw errors
        CSVGenerator2 csv = new CSVGenerator2();
        Parameters params2 = new Parameters();
        /*
            params.setParameter("encoding", "");
            params.setParameter("separator", "");
            params.setParameter("escape", "");
            params.setParameter("buffer-size", "");
        */
        // cap it at 100 records for performance in pipeline
        // TODO: parameterize this
        params2.setParameter("max-records", "100");
        BitstreamXMLConsumer consumer = new BitstreamXMLConsumer(contentHandler, lexicalHandler);
        try {
            csv.setup(resolver, objectModel, source, params2);
            csv.setConsumer(consumer);
            csv.generate();
        } catch (IOException e) {
            log.error(e.getClass() + " exception: " + e.getMessage());
            throw(e);
        } catch (ProcessingException e) {
            log.error(e.getClass() + " exception: " + e.getMessage());
            throw(e);            
        } catch (SAXException e) {
            log.error(e.getClass() + " exception: " + e.getMessage());
            throw(e);
        } finally {
            if (csv != null) {
                csv.dispose();
            }
        }
    }
}
