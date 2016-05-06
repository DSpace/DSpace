package org.dspace.app.xmlui.aspect.journal.landing;

import org.jdom2.Document;
import org.jdom2.input.sax.SAXHandler;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * @author Nathan Day
 */
public class ReferenceHandlerTest extends JournalLandingBaseTest
{

    ReferenceHandler referenceHandler;

    @Before
    public void setUp()
    {
        super.setUp();
        referenceHandler = new ReferenceHandler();
        try {
            referenceHandler.setup(this.resolver, this.objectModel, this.src, this.parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String piXpath = "count(processing-instruction()) = 0";
    private static String refXpath = "count(//dri:reference) = 2";
    @Test
    public void testProcessingInstruction() throws IOException, SAXException, ParserConfigurationException
    {
        String path = this.getClass().getResource("/pi-test-in.xml").getFile();
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(referenceHandler);
        SAXHandler sh = new SAXHandler();
        referenceHandler.setContentHandler(sh);
        xmlReader.parse(path);
        Document doc = sh.getDocument();
        assertTrue(boolXpath(doc, piXpath));
        assertTrue(boolXpath(doc, refXpath));
    }
    private boolean boolXpath(Document doc, String xpathStr)
    {
        XPathExpression<Boolean> exp = XPathFactory.instance().compile(xpathStr, Filters.fboolean(), vars, driNs);
        return exp.evaluateFirst(doc);
    }
}
