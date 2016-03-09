package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Reference;
import org.dspace.content.Item;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.NamespaceSupport;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Transformer to replace <?reference nnnn?> processing instructions
 * containing a data file's internal id with a DRI <reference/>
 * element.
 *
 * @author Nathan Day
 */
public class ReferenceHandler extends AbstractDSpaceTransformer
{
    private static final Logger log = Logger.getLogger(ReferenceHandler.class);

    // helper class for serializing references
    private class PrivateRef extends Reference
    {
        public PrivateRef(WingContext context, Object object) throws WingException {
            super(context, object);
        }
    }

    private WingContext wingContext;
    NamespaceSupport ns;

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters)
            throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver, objectModel, src, parameters);
        wingContext = new WingContext();
        wingContext.setObjectManager(this.getObjectManager());
        ns = new NamespaceSupport();
    }

    @Override
    public void dispose() {
        try {
            if (context != null && context.isValid())
                context.complete();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * With journal-landing.xsl stylesheet, response contains <?reference nnnn?>
     * processing instructions to be replaced here with Reference objects.
     */
    @Override
    public void processingInstruction(String target, String data) throws SAXException
    {
        if (target.equals("reference")) {
            Integer id = Integer.parseInt(data);
            try {
                Item item = Item.find(this.context,id);
                PrivateRef reference = new PrivateRef(wingContext, item);
                reference.toSAX(this.contentHandler, this.lexicalHandler, ns);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } else {
            super.processingInstruction(target, data);
        }
    }
}
