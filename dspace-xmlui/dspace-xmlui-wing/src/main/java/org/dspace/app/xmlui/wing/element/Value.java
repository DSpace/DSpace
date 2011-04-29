/*
 * Value.java
 *
 * Version: $Revision: 4365 $
 *
 * Date: $Date: 2009-10-05 19:52:42 -0400 (Mon, 05 Oct 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This class represents field values.
 *
 * @author Scott Phillips
 */

public class Value extends RichTextContainer
{
    /** The name of the value element */
    public static final String E_VALUE = "value";

    /** The name of the value type attribute */
    public static final String A_TYPE = "type";
    
    /** The name of the option value attribute */
    public static final String A_OPTION = "option";

    /** The name of the checked attribute */
    public static final String A_CHECKED = "checked";
    
    /** The name of the confidence attribute */
    public static final String A_CONFIDENCE = "confidence";
    
    
    /** The possible value types */
    public static final String TYPE_RAW = "raw";

    public static final String TYPE_INTERPRETED = "interpreted";

    public static final String TYPE_OPTION = "option";

    /** value of the metadata authority code associated with a raw value */
    public static final String TYPE_AUTHORITY = "authority";

    /** All the possible value types collected into one array. */
    public static final String[] TYPES = { TYPE_RAW, TYPE_INTERPRETED,
            TYPE_OPTION, TYPE_AUTHORITY};

    /** The type of this value element */
    private String type;

    /** The submited value for this option */
    private String option;
    
    /** The checked attribute */
    private boolean checked;

    /** The confidence attribute, for authority values; must be symbolic value in org.dspace.content.authority.Choices  */
    private String confidence = null;

    /**
     * Construct a new field value, when used in a multiple value context
     *
     * @param context
     *            (Required) The context this element is contained in
     * @param type
     *            (may be null) Determine the value's type, raw, default or
     *            interpreted. If the value is null, then raw is used.
     */
    protected Value(WingContext context, String type) throws WingException
    {
        super(context);

        if (type == null)
        {
            // if no type specified just default to raw.
            type = TYPE_RAW;
        }

        restrict(type,TYPES,
                "The 'type' parameter must be one of these values: 'raw', 'interpreted', or 'option'");

        this.type = type;
    }

    /**
     * Construct a new field value, when used in a multiple value context
     *
     * @param context
     *            (Required) The context this element is contained in
     * @param type
     *            (may be null) Determine the value's type, raw, default or
     *            interpreted. If the value is null, then raw is used.
     * @param optionOrConfidence
     *            (May be null) when type is TYPE_AUTHORITY, this is the
     *            symbolic confidence value, otherwise it is the option value.
     */
    protected Value(WingContext context, String type, String optionOrConfidence) throws WingException
    {
        super(context);

        if (type == null)
        {
            // if no type specified just default to raw.
            type = TYPE_RAW;
        }
        restrict(type,TYPES,
                "The 'type' parameter must be one of these values: 'raw', 'interpreted', or 'option'.");

        this.type = type;
        if (type.equals(TYPE_AUTHORITY))
            this.confidence = optionOrConfidence;
        else
            this.option = optionOrConfidence;
    }

    /**
     * Construct a new field value, when used in a multiple value context
     *
     * @param context
     *            (Required) The context this element is contained in
     * @param type
     *            (may be null) Determine the value's type, raw, default or
     *            interpreted. If the value is null, then raw is used.
     * @param checked
     *            (Required) Determine if the value is checked, only valid for
     *            checkboxes and radio buttons
     */
    protected Value(WingContext context, String type, boolean checked) throws WingException
    {
        super(context);

        if (type == null)
        {
            // if no type specified just default to raw.
            type = TYPE_RAW;
        }
        restrict(type,TYPES,
                "The 'type' parameter must be one of these values: 'raw', 'interpreted', or 'option'.");

        this.type = type;
        this.checked = checked;
    }

    /**
     * @return the type of this value.
     */
    protected String getType()
    {
        return type;
    }

    /**
     * Translate this element and all contained elements into SAX events. The
     * events should be routed to the contentHandler found in the WingContext.
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
     */

    public void toSAX(ContentHandler contentHandler,
            LexicalHandler lexicalHandler, NamespaceSupport namespaces)
            throws SAXException
    {
        AttributeMap attributes = new AttributeMap();
        attributes.put(A_TYPE, this.type);

        if (this.option != null)
            attributes.put(A_OPTION, this.option);
        if (this.checked)
            attributes.put(A_CHECKED, this.checked);
        if (this.type.equals(TYPE_AUTHORITY))
            attributes.put(A_CONFIDENCE, this.confidence);
        
        startElement(contentHandler, namespaces, E_VALUE, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_VALUE);
    }

    /**
     * dispose
     */
    public void dispose()
    {
        super.dispose();
    }

}
