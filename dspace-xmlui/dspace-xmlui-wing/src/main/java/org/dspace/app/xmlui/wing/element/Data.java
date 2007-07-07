/*
 * Data.java
 *
 * Version: $Revision: 1.6 $
 *
 * Date: $Date: 2006/04/21 02:20:52 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
 * When data needs to be translated it is enclosed inside the cocoon i18n schema
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

    /** The name of the param element */
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
     * @throws WingException
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
     * @throws WingException
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
     */
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
                    toSAX(contentHandler, namespaces, dictionaryParameter);
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

        boolean unknownType = false;

        if (dictionaryParameter.getClass().equals(Date.class.getName()))
        {
            Date date = (Date) dictionaryParameter;
            DateFormat dateFormater = DateFormat
                    .getDateInstance(DateFormat.DEFAULT);

            attributes.put(A_TYPE, TYPE_DATE);
            attributes.put(A_VALUE, dateFormater.format(date));
            // If no pattern is given then the default format is assumed.
        }
        else if (dictionaryParameter.getClass().equals(Integer.class.getName()))
        {
            Integer value = (Integer) dictionaryParameter;
            attributes.put(A_TYPE, TYPE_NUMBER);
            attributes.put(A_VALUE, String.valueOf(value));
        }
        else if (dictionaryParameter.getClass().equals(Double.class.getName()))
        {
            Double value = (Double) dictionaryParameter;
            attributes.put(A_TYPE, TYPE_NUMBER);
            attributes.put(A_VALUE, String.valueOf(value));
        }
        else if (dictionaryParameter.getClass().equals(Long.class.getName()))
        {
            Long value = (Long) dictionaryParameter;
            attributes.put(A_TYPE, TYPE_NUMBER);
            attributes.put(A_VALUE, String.valueOf(value));
        }
        else if (dictionaryParameter.getClass().equals(Short.class.getName()))
        {
            Short value = (Short) dictionaryParameter;
            attributes.put(A_TYPE, TYPE_NUMBER);
            attributes.put(A_VALUE, String.valueOf(value));
        }
        else if (dictionaryParameter.getClass().equals(Float.class.getName()))
        {
            Float value = (Float) dictionaryParameter;
            attributes.put(A_TYPE, TYPE_NUMBER);
            attributes.put(A_VALUE, String.valueOf(value));
        }
        else
        {
            // Unknown types or String
            unknownType = true;
        }

        startElement(contentHandler, namespaces, WingConstants.I18N, E_PARAM,
                attributes);

        // If the type is unknown then the value is not included as an attribute
        // and instead is sent as the contents of the elements.
        if (unknownType)
            sendCharacters(contentHandler, dictionaryParameter.toString());

        endElement(contentHandler, namespaces, WingConstants.I18N, E_PARAM);
    }

    /**
     * dispose
     */
    public void dispose()
    {
        super.dispose();
    }
}
