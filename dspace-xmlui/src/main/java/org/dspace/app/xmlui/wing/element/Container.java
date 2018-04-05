/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

/**
 * This class represents a generic Wing Container. The Container class adds a
 * simple contents list which may be modified by the extending concrete classes.
 * When it comes time to process the element a toSAX method is provided that
 * will iterate over the contents. 
 * 
 * @author Scott Phillips
 */

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

public abstract class Container extends AbstractWingElement
{

    /** The internal contents of this container */
    protected List<AbstractWingElement> contents = new ArrayList<>();

    /**
     * @param context
     *            (Required) The context this element is contained in.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected Container(WingContext context) throws WingException
    {
        super(context);
    }

    /**
     * Translate this element and all contained elements into SAX events. The
     * events should be routed to the contentHandler found in the WingContext.
     * 
     * This method does not create an inclosing block, the implementors of
     * container class need to implement a method of toSAX() that provides the
     * surrounding element block for that specific application.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param lexicalHandler
     *            (Required) The registered lexicalHandler where lexical events
     *            (such as CDATA, DTD, etc) should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     * @throws org.xml.sax.SAXException passed through.
     */
    @Override
    public void toSAX(ContentHandler contentHandler,
            LexicalHandler lexicalHandler, NamespaceSupport namespaces)
            throws SAXException
    {
        for (AbstractWingElement content : contents)
        {
            content.toSAX(contentHandler, lexicalHandler, namespaces);
        }
    }

    @Override
    public void dispose()
    {

        if (this.contents != null)
        {
            for (AbstractWingElement element : contents)
            {
                element.dispose();
            }
            this.contents.clear();
        }
        this.contents = null;
        super.dispose();
    }

}
