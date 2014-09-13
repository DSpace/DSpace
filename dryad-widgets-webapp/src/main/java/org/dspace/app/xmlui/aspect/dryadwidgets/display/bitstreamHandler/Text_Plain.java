/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler;

import java.io.BufferedReader;
import java.io.IOException;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

/**
 *
 * @author Nathan Day
 */
public class Text_Plain extends BaseBitstreamHandler {
    public Text_Plain(BufferedReader bufferedReader, ContentHandler contentHandler, LexicalHandler lexicalHandler, String format) throws SAXException {
        super(bufferedReader, contentHandler, lexicalHandler, format);
    }
    /**
     * Generate a plain text data section, wrapped in <!CDATA[]]>.
     * @throws SAXException
     * @throws IOException
     */
    @Override
    public void generate() throws SAXException, IOException {
        lexicalHandler.startCDATA();
        String output;
        while((output = bufferedReader.readLine()) != null) {
            contentHandler.characters(output.toCharArray(), 0, output.length());
        }
        lexicalHandler.endCDATA();
    }
}
