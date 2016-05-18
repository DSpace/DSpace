/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

import java.util.ArrayList;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Class representing a set of referenced metadata.
 * 
 * @author Scott Phillips
 */

public class ReferenceSet extends AbstractWingElement implements
        StructuralElement
{
    /** The name of the referenceSet element */
    public static final String E_REFERENCE_SET = "referenceSet";

    /** The name of the orderBy attribute */
    public static final String A_ORDER_BY = "orderBy";

    /** The name of the type attribute */
    public static final String A_TYPE = "type";

    /** The possible interactive division methods: get,post, or multipart. */
    public static final String TYPE_SUMMARY_LIST = "summaryList";

    public static final String TYPE_SUMMARY_VIEW = "summaryView";

    public static final String TYPE_DETAIL_LIST = "detailList";

    public static final String TYPE_DETAIL_VIEW = "detailView";

    /** The possible interactive division methods names collected into one array */
    public static final String[] TYPES = { TYPE_SUMMARY_LIST, TYPE_SUMMARY_VIEW, TYPE_DETAIL_LIST, TYPE_DETAIL_VIEW };

    /** The name assigned to this metadata set */
    private final String name;

    /** The ordering mechanism to use. */
    private final String orderBy;

    /** The reference type, see TYPES defined above */
    private final String type;

    /** Special rendering instructions */
    private final String rend;

    /** The head label for this reference set */
    private Head head;

    /** All content of this container, items & lists */
    private java.util.List<AbstractWingElement> contents = new ArrayList<>();

    /**
     * Construct a new referenceSet
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * @param childreference
     *            Whether this is a child reference (not requiring a name).
     * @param name
     *            (May be null) a local identifier used to differentiate the
     *            element from its siblings.
     * @param type
     *            (Required) The type of reference set which determines the level
     *            of detail for the metadata rendered. See TYPES for a list of
     *            available types.
     * @param orderBy
     *            (May be null) Determines the ordering of referenced metadata.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected ReferenceSet(WingContext context, boolean childreference, String name, String type, String orderBy, String rend)
            throws WingException
    {
        super(context);
        // Names are only required for parent reference sets.
        if (!childreference)
        {
            require(name, "The 'name' parameter is required for reference sets.");
        }
        restrict(
                type,
                TYPES,
                "The 'method' parameter must be one of these values: 'summaryList', 'summaryView', 'detailList', or 'detailView'.");

        this.name = name;
        this.type = type;
        this.orderBy = orderBy;
        this.rend = rend;
    }

    /**
     * Set the head element which is the label associated with this referenceset.
     * @return the new head.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Head setHead() throws WingException
    {
        this.head = new Head(context, null);
        return head;
    }

    /**
     * Set the head element which is the label associated with this reference set.
     * 
     * @param characters
     *            (May be null) Unprocessed characters to be referenced
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setHead(String characters) throws WingException
    {
        Head newHead = this.setHead();
        newHead.addContent(characters);
    }

    /**
     * Set the head element which is the label associated with this referenceset.
     * 
     * @param message
     *            (Required) A key into the i18n catalogue for translation into
     *            the user's preferred language.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setHead(Message message) throws WingException
    {
        Head newHead = this.setHead();
        newHead.addContent(message);
    }

    /**
     * Add an object reference.
     * 
     * @param object
     *            (Required) The referenced object.
     * @return the reference.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Reference addReference(Object object)
            throws WingException
    {
        Reference reference = new Reference(context, object);
        contents.add(reference);
        return reference;
    }

    /**
     * Translate this metadata inclusion set to SAX
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param lexicalHandler
     *            (Required) The registered lexicalHandler where lexical 
     *            events (such as CDATA, DTD, etc) should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     * @throws org.xml.sax.SAXException passed through.
     */
    @Override
    public void toSAX(ContentHandler contentHandler, LexicalHandler lexicalHandler, 
            NamespaceSupport namespaces) throws SAXException
    {
        AttributeMap attributes = new AttributeMap();
        if (name != null)
        {
            attributes.put(A_NAME, name);
        }
        if (name != null)
        {
            attributes.put(A_ID, context.generateID(E_REFERENCE_SET, name));
        }

        attributes.put(A_TYPE, type);
        if (orderBy != null)
        {
            attributes.put(A_ORDER_BY, orderBy);
        }
        if (rend != null)
        {
            attributes.put(A_RENDER, rend);
        }

        startElement(contentHandler, namespaces, E_REFERENCE_SET, attributes);

        if (head != null)
        {
            head.toSAX(contentHandler, lexicalHandler, namespaces);
        }

        for (AbstractWingElement content : contents)
        {
            content.toSAX(contentHandler, lexicalHandler, namespaces);
        }

        endElement(contentHandler, namespaces, E_REFERENCE_SET);
    }

    @Override
    public void dispose()
    {
        if (contents != null)
        {
            for (AbstractWingElement content : contents)
            {
                content.dispose();
            }
            contents.clear();
        }
        contents = null;
        super.dispose();
    }
}
