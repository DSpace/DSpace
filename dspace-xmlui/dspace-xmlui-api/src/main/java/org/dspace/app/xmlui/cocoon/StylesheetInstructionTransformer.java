/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.transformation.AbstractTransformer;
import org.xml.sax.SAXException;

public class StylesheetInstructionTransformer extends AbstractTransformer
{

    /** The location of the XSL stylesheet relative to the application */
    private String stylesheet;

    /**
     * Setup the processing instruction transformer. The only parameter that
     * matters in the src parameter which should be the path to an XSL
     * stylesheet to be applied by the clients browser.
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String contextPath = request.getContextPath();
        this.stylesheet = contextPath + src;
    }

    /**
     * Receive notification of the beginning of a document.
     */
    public void startDocument() throws SAXException
    {
        super.startDocument();
        // <?xml-stylesheet type="text/xsl" href="<stylesheet>"?>
        super.processingInstruction("xml-stylesheet",
                "type=\"text/xsl\" href=\"" + stylesheet + "\"");
    }
}
