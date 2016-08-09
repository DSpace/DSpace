/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

import java.text.DateFormat;
import java.util.Date;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This class represents data, by data we mean the translated and untranslated
 * characters in between XML elements.
 * 
 * <p>When data needs to be translated it is enclosed inside the cocoon i18n schema
 * while untranslated data is enclosed inside nothing.
 * 
 * @author Scott Phillips
 */

public class Data extends AbstractWingElement
{
    /** The name of the text element */
    public static final String E_TEXT = "text";

    /** The name of the translate element */
    public static final String E_TRANSLATE = "translate";

    /** The name of the {@code param} element */
    public static final String E_PARAM = "param";

    /** The name of the catalogue attribute (used inside text or i18n message) */
    public static final String A_CATALOGUE = "catalogue";

    /** The name of the type attribute (used inside params) */
    public static final String A_TYPE = "type";

    /** The name of the value attribute (used inside params) */
    public static final String A_VALUE = "value";

    /** The date parameter type */
    private static final String TYPE_DATE = "date";

    /** The number parameter type */
    private static final String TYPE_NUMBER = "number";


    /** Translated data key. */
    private final Message message;

    /** Untranslated data */
    private final String characters;

    /**
     * Construct a new data element using translated content.
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * @param message
     *            (Required) translatable data
     * @throws WingException passed through.
     */
    protected Data(WingContext context, Message message)
            throws WingException
    {
        super(context);
        this.message = message;
        this.characters = null;
    }

    /**
     * Construct a new data element with untranslated content.
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * @param characters
     *            (Required) Untranslated character data.
     * @throws WingException passed through.
     */
    protected Data(WingContext context, String characters) throws WingException
    {
        super(context);
        this.message = null;
        this.characters = characters;
    }

    /**
     * Translate this element into SAX
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
            NamespaceSupport namespaces)throws SAXException
    {
        if (this.characters != null)
        {
            sendCharacters(contentHandler, this.characters);
        }
        else if (this.message != null)
        {
            String catalogue = message.getCatalogue();
            Object[] dictionaryParameters = message.getDictionaryParameters();

            if (dictionaryParameters == null
                    || dictionaryParameters.length == 0)
            {
                // No parameters, we can use the simple method
                // <i18n:text> Text to be translated </i18n:text>

                AttributeMap attributes = new AttributeMap();
                attributes.setNamespace(WingConstants.I18N);
                attributes.put(A_CATALOGUE, catalogue);

                startElement(contentHandler, namespaces, WingConstants.I18N,
                        E_TEXT, attributes);
                sendCharacters(contentHandler, message.getKey());
                endElement(contentHandler, namespaces, WingConstants.I18N,
                        E_TEXT);

            }
            else
            {
                // There are parameters, we need to us the complex method.
                // <i18n:translate>
                // <i18n:text> some {0} was inserted {1}. </i18n:text>
                // <i18n:param> text </i18n:param>
                // <i18n:param> here </i18n:param>
                // </i18n:translate>

                startElement(contentHandler, namespaces, WingConstants.I18N,
                        E_TRANSLATE, null);

                AttributeMap attributes = new AttributeMap();
                attributes.setNamespace(WingConstants.I18N);
                attributes.put(A_CATALOGUE, catalogue);

                startElement(contentHandler, namespaces, WingConstants.I18N,
                        E_TEXT, attributes);
                sendCharacters(contentHandler, message.getKey());
                endElement(contentHandler, namespaces, WingConstants.I18N,
                        E_TEXT);

                // i18n:param tags
                for (Object dictionaryParameter : dictionaryParameters)
                {
                    if (dictionaryParameter != null)
                    {
                        toSAX(contentHandler, namespaces, dictionaryParameter);
                    }
                }

                endElement(contentHandler, namespaces, WingConstants.I18N,
                        E_TRANSLATE);
            }
        }
    }

    /**
     * Build the the correct param element for the given dictionaryParameter
     * based upon the class type. This method can deal with Dates, numbers, and
     * strings.
     * 
     * @param dictionaryParameter
     *            A dictionary parameter.
     */
    private void toSAX(ContentHandler contentHandler,
            NamespaceSupport namespaces, Object dictionaryParameter)
            throws SAXException
    {
        AttributeMap attributes = new AttributeMap();
        attributes.setNamespace(WingConstants.I18N);

        if (dictionaryParameter instanceof Date)
        {
            Date date = (Date) dictionaryParameter;
            DateFormat dateFormater = DateFormat
                    .getDateInstance(DateFormat.DEFAULT);

            attributes.put(A_TYPE, TYPE_DATE);
            attributes.put(A_VALUE, dateFormater.format(date));
            // If no pattern is given then the default format is assumed.
        }
        else if (dictionaryParameter instanceof Integer)
        {
            Integer value = (Integer) dictionaryParameter;
            attributes.put(A_TYPE, TYPE_NUMBER);
            attributes.put(A_VALUE, String.valueOf(value));
        }
        else if (dictionaryParameter instanceof Double)
        {
            Double value = (Double) dictionaryParameter;
            attributes.put(A_TYPE, TYPE_NUMBER);
            attributes.put(A_VALUE, String.valueOf(value));
        }
        else if (dictionaryParameter instanceof Long)
        {
            Long value = (Long) dictionaryParameter;
            attributes.put(A_TYPE, TYPE_NUMBER);
            attributes.put(A_VALUE, String.valueOf(value));
        }
        else if (dictionaryParameter instanceof Short)
        {
            Short value = (Short) dictionaryParameter;
            attributes.put(A_TYPE, TYPE_NUMBER);
            attributes.put(A_VALUE, String.valueOf(value));
        }
        else if (dictionaryParameter instanceof Float)
        {
            Float value = (Float) dictionaryParameter;
            attributes.put(A_TYPE, TYPE_NUMBER);
            attributes.put(A_VALUE, String.valueOf(value));
        }

        startElement(contentHandler, namespaces, WingConstants.I18N, E_PARAM, attributes);

        // If the type is unknown then the value is not included as an attribute
        // and instead is sent as the contents of the elements.

        // NOTE: Used to only do this if unknownType was true. However, due to flaw in logic above,
        // it was always true, and so always called.
        // In order not to break anything, we will now call sendCharacters regardless of whether we matched a Type
        sendCharacters(contentHandler, dictionaryParameter.toString());

        endElement(contentHandler, namespaces, WingConstants.I18N, E_PARAM);
    }
}
